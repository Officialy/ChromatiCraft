package reika.chromaticraft.container;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.item.ItemInfoFragment;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ResearchFragmentData;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaMenus;

/** V33a one-fragment, three-category decoding container. */
public final class MenuFragmentSelection extends AbstractContainerMenu {

	private static final String CATEGORY_RESOURCE =
			"/assets/chromaticraft/lexicon/fragment_categories.tsv";
	private static final Map<String, EnumMap<FragmentCategory, Integer>> CATEGORY_WEIGHTS =
			loadCategoryWeights();
	private final Inventory playerInventory;
	private final SimpleContainer fragment = new SimpleContainer(1);
	/** page catalog ordinal in low 11 bits, original category ordinal above it. */
	private final int[] choices = {-1, -1, -1};

	public MenuFragmentSelection(int id, Inventory inventory, FriendlyByteBuf ignored) {
		this(id, inventory);
	}

	public MenuFragmentSelection(int id, Inventory inventory) {
		super(ChromaMenus.FRAGMENT_SELECTION.get(), id);
		playerInventory = inventory;
		addSlot(new Slot(fragment, 0, 82, 8) {
			@Override public boolean mayPlace(ItemStack stack) {
				ResearchFragmentData data = ResearchFragmentData.read(stack);
				return stack.is(ChromaItems.INFO_FRAGMENT.get()) && data.blank() && !data.random();
			}
			@Override public void onTake(Player player, ItemStack stack) {
				programRandom(stack, player);
				super.onTake(player, stack);
			}
		});
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlot(new Slot(inventory, column + row * 9 + 9, 9 + column * 18, 77 + row * 18));
		for (int column = 0; column < 9; column++)
			addSlot(new Slot(inventory, column, 9 + column * 18, 135));

		if (!inventory.player.level().isClientSide())
			pickChoices();
		for (int index = 0; index < choices.length; index++) {
			final int choice = index;
			addDataSlot(new DataSlot() {
				@Override public int get() { return choices[choice]; }
				@Override public void set(int value) { choices[choice] = value; }
			});
		}
	}

	private void pickChoices() {
		ArrayList<LexiconCatalog.Entry> next = new ArrayList<>(PlayerResearch.nextResearch(playerInventory.player));
		if (next.isEmpty())
			return;
		long seed = playerInventory.player.getUUID().getMostSignificantBits()
				^ playerInventory.player.getUUID().getLeastSignificantBits()
				^ Integer.toUnsignedLong(containerId * 0x9e3779b9);
		Random random = new Random(seed);
		LinkedHashSet<FragmentCategory> available = new LinkedHashSet<>();
		for (LexiconCatalog.Entry page : next)
			available.addAll(weights(page).keySet());
		ArrayList<FragmentCategory> categories = new ArrayList<>(available);
		Collections.shuffle(categories, random);
		int count = 0;
		for (FragmentCategory category : categories) {
			LexiconCatalog.Entry page = weightedPage(next, category, random);
			if (page != null)
				choices[count++] = encode(page, category);
			if (count == choices.length)
				return;
		}
		// Exact V33a fallback when fewer than three usable categories remain: pick a next page, then
		// choose one of that page's own categories by its authored weight. The first result is repeated
		// only if the progression frontier itself has no additional identity.
		while (count < choices.length) {
			LexiconCatalog.Entry page = next.get(random.nextInt(next.size()));
			FragmentCategory category = weightedCategory(page, random);
			choices[count++] = encode(page, category);
		}
	}

	private static int encode(LexiconCatalog.Entry page, FragmentCategory category) {
		return page.ordinal() | category.ordinal() << 11;
	}

	private static LexiconCatalog.Entry weightedPage(List<LexiconCatalog.Entry> pages,
			FragmentCategory category, Random random) {
		int total = 0;
		for (LexiconCatalog.Entry page : pages)
			total += weights(page).getOrDefault(category, 0);
		if (total <= 0)
			return null;
		int value = random.nextInt(total);
		for (LexiconCatalog.Entry page : pages) {
			value -= weights(page).getOrDefault(category, 0);
			if (value < 0)
				return page;
		}
		throw new IllegalStateException("Weighted fragment page selection fell through");
	}

	private static FragmentCategory weightedCategory(LexiconCatalog.Entry page, Random random) {
		EnumMap<FragmentCategory, Integer> weights = weights(page);
		int total = weights.values().stream().mapToInt(Integer::intValue).sum();
		int value = random.nextInt(total);
		for (Map.Entry<FragmentCategory, Integer> entry : weights.entrySet()) {
			value -= entry.getValue();
			if (value < 0)
				return entry.getKey();
		}
		throw new IllegalStateException("Weighted fragment category selection fell through for " + page.id());
	}

	private static EnumMap<FragmentCategory, Integer> weights(LexiconCatalog.Entry page) {
		EnumMap<FragmentCategory, Integer> weights = CATEGORY_WEIGHTS.get(page.id());
		if (weights == null || weights.isEmpty())
			throw new IllegalStateException("V33a fragment page has no category weights: " + page.id());
		return weights;
	}

	private static Map<String, EnumMap<FragmentCategory, Integer>> loadCategoryWeights() {
		java.util.LinkedHashMap<String, EnumMap<FragmentCategory, Integer>> result =
				new java.util.LinkedHashMap<>();
		try (InputStream stream = MenuFragmentSelection.class.getResourceAsStream(CATEGORY_RESOURCE)) {
			if (stream == null)
				throw new IllegalStateException("Missing V33a fragment-category table " + CATEGORY_RESOURCE);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.charAt(0) == '#')
						continue;
					String[] fields = line.split("\\t");
					EnumMap<FragmentCategory, Integer> weights = new EnumMap<>(FragmentCategory.class);
					for (int field = 1; field < fields.length; field++) {
						String[] pair = fields[field].split("=", 2);
						weights.put(FragmentCategory.valueOf(pair[0]), Integer.parseInt(pair[1]));
					}
					result.put(fields[0], weights);
				}
			}
		}
		catch (java.io.IOException | RuntimeException exception) {
			throw new ExceptionInInitializerError(exception);
		}
		for (LexiconCatalog.Entry page : LexiconCatalog.obtainablePages())
			if (!result.containsKey(page.id()))
				throw new IllegalStateException("Missing V33a fragment-category row for " + page.id());
		return java.util.Collections.unmodifiableMap(result);
	}

	public boolean hasFragment() {
		return !fragment.getItem(0).isEmpty();
	}

	public LexiconCatalog.Entry choice(int index) {
		if (index < 0 || index >= choices.length || choices[index] < 0)
			return null;
		int ordinal = choices[index] & 2047;
		return ordinal < LexiconCatalog.entries().size() ? LexiconCatalog.entries().get(ordinal) : null;
	}

	public FragmentCategory category(int index) {
		if (index < 0 || index >= choices.length || choices[index] < 0)
			return FragmentCategory.INFO;
		int ordinal = choices[index] >> 11 & 31;
		return ordinal < FragmentCategory.values().length ? FragmentCategory.values()[ordinal]
				: FragmentCategory.INFO;
	}

	public boolean select(int index) {
		if (!(playerInventory.player instanceof ServerPlayer player) || !hasFragment())
			return false;
		LexiconCatalog.Entry page = choice(index);
		ItemStack stack = fragment.getItem(0);
		ResearchFragmentData data = ResearchFragmentData.read(stack);
		if (page == null || !data.blank() || data.random()
				|| !PlayerResearch.nextResearch(player).contains(page))
			return false;
		data.withPage(page).writeTo(stack);
		PlayerResearch.giveFragment(player, page, true);
		player.closeContainer();
		return true;
	}

	@Override
	public void removed(Player player) {
		if (!player.level().isClientSide()) {
			ItemStack stack = fragment.removeItemNoUpdate(0);
			if (!stack.isEmpty()) {
				ResearchFragmentData data = ResearchFragmentData.read(stack);
				if (data.blank())
					programRandom(stack, player);
				player.getInventory().placeItemBackInInventory(stack);
			}
		}
		super.removed(player);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index == 0) {
			programRandom(stack, player);
			copy = stack.copy();
			if (!moveItemStackTo(stack, 1, slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!moveItemStackTo(stack, 0, 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}

	private static void programRandom(ItemStack stack, Player player) {
		if (player.level().isClientSide())
			return;
		ResearchFragmentData data = ResearchFragmentData.read(stack);
		if (!stack.is(ChromaItems.INFO_FRAGMENT.get()) || !data.blank())
			return;
		LexiconCatalog.Entry random = PlayerResearch.randomNextResearch(player, player.getRandom());
		if (random != null) {
			data.withPage(random).writeTo(stack);
			PlayerResearch.giveFragment(player, random, true);
		}
	}

	/** Exact V33a atlas and controlled-config category order. */
	public enum FragmentCategory {
		AUTOMATION, DEFENCE, ATTACK, BUILDING, COLLECTION, TRAVEL, CRAFTING, LUMENS,
		WORLD, CONVERSION, STORAGE, ARTIFACT, IMPROVEMENT, MODINTERFACE, INFO, BLOCK,
		TOOLARMOR, ABILITY, RESOURCE, STRUCTURE;
	}
}
