/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.processing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.interfaces.EnchantableItem;
import reika.chromaticraft.auxiliary.interfaces.ChromaExtractable;
import reika.chromaticraft.auxiliary.interfaces.ChromaPowered;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.VariableTexture;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.base.tileentity.InventoriedChromaticBase;
import reika.chromaticraft.container.MenuAutoEnchanter;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.ChromaItemTags;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code TileEntityAutoEnchanter}, the Chromatic Enchanter: chosen enchantments (and levels, some beyond vanilla's
 * maximum) are put on the item in its main slot over 40 ticks, for liquid chroma -- 500 mB per level, times the
 * enchantment's tier (worthless 0.25 to rare 2), rounded to 100. An enchanted item in the second slot lowers the cost
 * of the enchantments it carries (taking wear for them), and an enchanting table on top, surrounded by bookshelves as a
 * table would be, divides the cost by up to 2.5. Books become enchanted books.
 */
public class TileEntityAutoEnchanter extends InventoriedChromaticBase implements ChromaPowered, ChromaExtractable,
		OperationInterval, VariableTexture, MenuProvider {

	private LinkedHashMap<ResourceKey<Enchantment>, Integer> selected = new LinkedHashMap<>();

	public static final int CHROMA_PER_LEVEL_BASE = 500;
	private static final int CAPACITY = 12000;//6000;
	private static final HashMap<ResourceKey<Enchantment>, EnchantmentTier> tiers = new HashMap<>();
	private static final HashMap<ResourceKey<Enchantment>, Integer> boostedLevels = new HashMap<>();
	private static final HashSet<ResourceKey<Enchantment>> blacklist = new HashSet<>();
	private static final HashSet<ImmutablePair<ResourceKey<Enchantment>, ResourceKey<Enchantment>>> allowedPairs = new HashSet<>();

	static {
		tiers.put(Enchantments.BANE_OF_ARTHROPODS, EnchantmentTier.WORTHLESS);
		tiers.put(Enchantments.SMITE, EnchantmentTier.WORTHLESS);

		tiers.put(Enchantments.KNOCKBACK, EnchantmentTier.BASIC);
		tiers.put(Enchantments.PUNCH, EnchantmentTier.BASIC);
		tiers.put(Enchantments.LURE, EnchantmentTier.BASIC);
		tiers.put(ChromaEnchants.FASTSINK.key(), EnchantmentTier.BASIC);

		tiers.put(Enchantments.FORTUNE, EnchantmentTier.VALUABLE);
		tiers.put(Enchantments.SHARPNESS, EnchantmentTier.VALUABLE);
		tiers.put(Enchantments.LOOTING, EnchantmentTier.VALUABLE);
		tiers.put(Enchantments.POWER, EnchantmentTier.VALUABLE);
		tiers.put(Enchantments.PROTECTION, EnchantmentTier.VALUABLE);
		tiers.put(ChromaEnchants.USEREPAIR.key(), EnchantmentTier.VALUABLE);
		tiers.put(ChromaEnchants.ENDERLOCK.key(), EnchantmentTier.VALUABLE);
		tiers.put(ChromaEnchants.AGGROMASK.key(), EnchantmentTier.VALUABLE);

		tiers.put(Enchantments.SILK_TOUCH, EnchantmentTier.RARE);
		tiers.put(Enchantments.INFINITY, EnchantmentTier.RARE);
		tiers.put(ChromaEnchants.RARELOOT.key(), EnchantmentTier.RARE);
		tiers.put(ChromaEnchants.WEAPONAOE.key(), EnchantmentTier.RARE);
		tiers.put(ChromaEnchants.HARVESTLEVEL.key(), EnchantmentTier.RARE);
		tiers.put(ChromaEnchants.PHASING.key(), EnchantmentTier.RARE);
		tiers.put(ChromaEnchants.BOSSKILL.key(), EnchantmentTier.RARE);

		boostedLevels.put(Enchantments.FORTUNE, 5);
		boostedLevels.put(Enchantments.LOOTING, 5);
		boostedLevels.put(Enchantments.RESPIRATION, 5);
		boostedLevels.put(Enchantments.LUCK_OF_THE_SEA, 5);
		boostedLevels.put(Enchantments.POWER, 10);
		boostedLevels.put(Enchantments.SHARPNESS, 10);
		boostedLevels.put(Enchantments.PROTECTION, 5);

		allowedPairs.add(new ImmutablePair<>(Enchantments.FORTUNE, Enchantments.SILK_TOUCH));
		allowedPairs.add(new ImmutablePair<>(Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS));
		allowedPairs.add(new ImmutablePair<>(Enchantments.SHARPNESS, Enchantments.SMITE));
	}

	private StepTimer progress = new StepTimer(40);
	public int progressTimer;

	private int chroma;

	private final EnchanterFluidHandler fluidHandler = new EnchanterFluidHandler();

	public TileEntityAutoEnchanter(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.AUTO_ENCHANTER.get(), pos, state);
	}

	public static Map<ResourceKey<Enchantment>, Integer> getBoostedLevels() {
		return Collections.unmodifiableMap(boostedLevels);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.ENCHANTER;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (this.canProgress()) {
			progress.update();
			if (progress.checkCap()) {
				if (!world.isClientSide())
					this.applyEnchants();
			}
		}
		else {
			progress.reset();
		}
		progressTimer = progress.getTick();
	}

	public int getProgressScaled(int a) {
		return a*progressTimer / progress.getCap();
	}

	private boolean canProgress() {
		return this.isValid(inv.get(0)) && this.hasSufficientChroma() && this.enchanting();
	}

	private boolean hasSufficientChroma() {
		return this.getChromaLevel() >= this.getConsumedChroma();
	}

	private boolean enchanting() {
		if (selected.isEmpty())
			return false;
		for (int level : selected.values()) {
			if (level > 0)
				return true;
		}
		return false;
	}

	@Override
	public int getChromaLevel() {
		return chroma;
	}

	public int getCapacity() {
		return CAPACITY;
	}

	@Override
	public boolean addChroma(int amt) {
		if (chroma+amt <= CAPACITY) {
			chroma += amt;
			this.setChanged();
			return true;
		}
		return false;
	}

	@Override
	public void removeLiquid(int amt) {
		chroma = Math.max(0, chroma-amt);
		this.setChanged();
	}

	private HolderLookup.RegistryLookup<Enchantment> enchantments() {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}

	private Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
		return this.enchantments().get(key).orElse(null);
	}

	/** The enchantments on a stack, stored ones for enchanted books (V33a {@code ReikaEnchantmentHelper.getEnchantments}). */
	private static ItemEnchantments enchantmentsOn(ItemStack is) {
		return EnchantmentHelper.getEnchantmentsForCrafting(is);
	}

	private static int levelOn(Holder<Enchantment> e, ItemStack is) {
		return enchantmentsOn(is).getLevel(e);
	}

	private boolean isValid(ItemStack is) {
		return !is.isEmpty() && this.isItemEnchantable(is) && this.areEnchantsValid(is);
	}

	private boolean isItemEnchantable(ItemStack is) {
		if (is.is(Items.BOOK) || is.is(Items.ENCHANTED_BOOK))
			return true;
		if (is.getItem() instanceof EnchantableItem)
			return true;
		// CHROMA-PORT: V33a also took the Lumen Alveary bee frame (BEEFRAME, Forestry), which is not ported.
		if (this.isSoulboundable(is, is.getItem()))
			return true;
		return is.has(net.minecraft.core.component.DataComponents.ENCHANTABLE);
	}

	private boolean areEnchantsValid(ItemStack is) {
		Item i = is.getItem();
		Collection<Holder<Enchantment>> has = enchantmentsOn(is).keySet();
		for (ResourceKey<Enchantment> key : selected.keySet()) {
			Holder<Enchantment> e = this.holder(key);
			if (e == null)
				return false;
			if (!this.isEnchantValid(e, is, i, true))
				return false;
			if (!this.isCompatible(has, e))
				return false;
		}
		return true;
	}

	public static enum EnchantValidity {
		VALID("Valid"),
		WRONGITEM("Invalid for Item"),
		INCOMPATIBLEWITHEXISTING("Incompatible with Existing"),
		INCOMPATIBLEWITHSELF("Incompatible with Selected"),
		;

		public final String desc;

		private EnchantValidity(String s) {
			desc = s;
		}

		public int getTextColor() {
			return switch(this) {
				case VALID -> 0xffffff;
				case WRONGITEM -> 0xfff740;
				case INCOMPATIBLEWITHEXISTING -> 0xffc740;
				case INCOMPATIBLEWITHSELF -> 0xff9090;
			};
		}
	}

	public EnchantValidity isCompatible(Holder<Enchantment> e) {
		Collection<Holder<Enchantment>> sel = new java.util.ArrayList<>();
		for (ResourceKey<Enchantment> key : selected.keySet()) {
			Holder<Enchantment> h = this.holder(key);
			if (h != null)
				sel.add(h);
		}
		if (!this.isCompatible(sel, e))
			return EnchantValidity.INCOMPATIBLEWITHSELF;
		ItemStack in = inv.get(0);
		if (!in.isEmpty()) {
			if (!this.isCompatible(enchantmentsOn(in).keySet(), e))
				return EnchantValidity.INCOMPATIBLEWITHEXISTING;
			if (!this.isEnchantValid(e, in, in.getItem(), false))
				return EnchantValidity.WRONGITEM;
		}
		return EnchantValidity.VALID;
	}

	private boolean isCompatible(Collection<Holder<Enchantment>> c, Holder<Enchantment> e) {
		for (Holder<Enchantment> e2 : c) {
			if (!isCompatible(e, e2))
				return false;
		}
		return true;
	}

	private static boolean isCompatible(Holder<Enchantment> e, Holder<Enchantment> e2) {
		if (e.equals(e2))
			return true;
		var k1 = e.unwrapKey().orElse(null);
		var k2 = e2.unwrapKey().orElse(null);
		if (k1 != null && k2 != null && (allowedPairs.contains(new ImmutablePair<>(k1, k2)) || allowedPairs.contains(new ImmutablePair<>(k2, k1))))
			return true;
		return Enchantment.areCompatible(e, e2);
	}

	/**
	 * V33a {@code isEnchantValid}. Books take anything (26.2 has no enchantment barred from books); an
	 * {@link EnchantableItem} decides for itself; soulbinding goes on anything soulboundable; shears take the digging,
	 * mining-loot and durability enchantments; otherwise the enchantment's supported items decide.
	 */
	public boolean isEnchantValid(Holder<Enchantment> e, ItemStack is, Item i, boolean checkLevels) {
		if (checkLevels) {
			ResourceKey<Enchantment> key = e.unwrapKey().orElse(null);
			if (levelOn(e, is) >= selected.getOrDefault(key, 0))
				return false;
		}

		if (i instanceof EnchantableItem ei) {
			TriState res = ei.getEnchantValidity(e, is);
			if (res == TriState.TRUE)
				return true;
			if (res == TriState.FALSE)
				return false;
		}

		// CHROMA-PORT: V33a limited the bee frame (BEEFRAME) to Unbreaking here.

		boolean soulbound = e.unwrapKey().map(k -> k.identifier().getPath().toLowerCase(Locale.ENGLISH).contains("soulbound")).orElse(false);

		if (soulbound && this.isSoulboundable(is, i))
			return true;

		if (is.is(Items.SHEARS)) {
			return isInCategory(e, ItemTags.MINING_ENCHANTABLE) || isInCategory(e, ItemTags.MINING_LOOT_ENCHANTABLE)
					|| isInCategory(e, ItemTags.DURABILITY_ENCHANTABLE) || isInCategory(e, ChromaItemTags.ENCHANTABLE_DIGGER);
		}

		// CHROMA-PORT: V33a limited Tinkers' Construct tools and weapons to ChromatiCraft digger/weapon enchantments; no
		// 26.2 Tinkers' Construct is on the classpath.

		return is.is(Items.BOOK) || is.is(Items.ENCHANTED_BOOK) ? true : is.supportsEnchantment(e);
	}

	/** Whether an enchantment's supported items are this tag (26.2's version of 1.7.10's {@code EnumEnchantmentType}). */
	private static boolean isInCategory(Holder<Enchantment> e, TagKey<Item> tag) {
		return e.value().definition().supportedItems().unwrapKey().map(k -> k.equals(tag)).orElse(false);
	}

	private boolean isSoulboundable(ItemStack is, Item i) {
		if (is.is(ChromaItems.LEXICON.get()))
			return true;
		// CHROMA-PORT: V33a also soulbound backpacks (Backpack), ender pouches (EnderStorage), Thaumcraft focus pouches
		// and wands, Thaumic Tinkerer ichor pouches and Tinkers' tools, none of which have 26.2 ports here.
		if (i instanceof ItemChromaTool || is.has(net.minecraft.core.component.DataComponents.TOOL) || is.is(ItemTags.SWORDS)
				|| is.is(Items.SHEARS))
			return true;
		var equippable = is.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
			return true;
		return false;
	}

	private void applyEnchants() {
		if (inv.get(0).is(Items.BOOK))
			inv.set(0, new ItemStack(Items.ENCHANTED_BOOK));
		ItemStack out = inv.get(0);
		HashMap<Holder<Enchantment>, Integer> apply = new HashMap<>();
		for (Entry<ResourceKey<Enchantment>, Integer> en : selected.entrySet()) {
			Holder<Enchantment> h = this.holder(en.getKey());
			if (h != null)
				apply.put(h, en.getValue());
		}
		ItemStack catalyst = inv.get(1);
		if (!catalyst.isEmpty()) {
			EnchantmentHelper.updateEnchantments(catalyst, m -> m.removeIf(h -> apply.containsKey(h)));
			int dmg = 0;
			for (Entry<Holder<Enchantment>, Integer> e : apply.entrySet())
				dmg += e.getValue()*ReikaRandomHelper.getRandomBetween(4D, 15D)*this.getCostFactor(e.getKey().unwrapKey().orElse(null));
			if (catalyst.isDamageableItem() && level instanceof ServerLevel sl)
				catalyst.hurtAndBreak(dmg, sl, null, item -> {});
			inv.set(1, catalyst);
		}
		// V33a removed then re-added the selected enchantments, so each is set to exactly the selected level.
		EnchantmentHelper.updateEnchantments(out, m -> {
			for (Entry<Holder<Enchantment>, Integer> e : apply.entrySet()) {
				if (e.getValue() > 0)
					m.set(e.getKey(), e.getValue());
			}
		});
		this.removeLiquid(this.getConsumedChroma());
		this.setChanged();
		this.syncAllData(true);
	}

	public int getConsumedChroma() {
		int total = 0;
		ItemStack catalyst = inv.get(1);
		for (Entry<ResourceKey<Enchantment>, Integer> en : selected.entrySet()) {
			float lvl = en.getValue();
			if (!catalyst.isEmpty() && level != null) {
				Holder<Enchantment> h = this.holder(en.getKey());
				if (h != null)
					lvl = Math.max(0.25F, lvl-0.8F*levelOn(h, catalyst));
			}
			float add = lvl*CHROMA_PER_LEVEL_BASE*this.getCostFactor(en.getKey());
			if (this.isAssisted())
				add /= Math.min(2.5F, 1+this.getAssistPower());
			add = Math.max(add, 50*lvl);
			add = ReikaMathLibrary.roundToNearestX(100, Math.round(add));
			total += (int)add;
		}
		return total;
	}

	private float getCostFactor(ResourceKey<Enchantment> e) {
		EnchantmentTier t = e == null ? null : tiers.get(e);
		if (t == null && e != null) {
			// V33a looked "Multishot" and "Soulbound" up by name among the registered enchantments and made them rare.
			String path = e.identifier().getPath();
			if (path.equals("multishot") || path.equals("soulbound"))
				t = EnchantmentTier.RARE;
		}
		if (t == null)
			t = EnchantmentTier.NORMAL;
		return t.costFactor;
	}

	public boolean setEnchantment(Holder<Enchantment> e, int level) {
		ResourceKey<Enchantment> key = e.unwrapKey().orElseThrow();
		this.onEnchantChanged();
		level = Math.min(getMaxEnchantmentLevel(e), level);
		if (level <= 0) {
			this.removeEnchantment(key);
			return true;
		}
		else {
			selected.put(key, level);
			this.setChanged();
			return true;
		}
	}

	private void onEnchantChanged() {
		progress.reset();
		progressTimer = 0;
	}

	public static int getMaxEnchantmentLevel(Holder<Enchantment> e) {
		if (e.is(Enchantments.FORTUNE))
			return 5;
		if (e.is(Enchantments.LOOTING))
			return 5;
		if (e.is(Enchantments.RESPIRATION))
			return 5;
		if (e.is(Enchantments.LUCK_OF_THE_SEA))
			return 5;
		if (e.is(Enchantments.POWER))
			return 10;
		if (e.is(Enchantments.SHARPNESS))
			return 10;
		if (e.is(Enchantments.UNBREAKING))
			return 5;
		ChromaEnchants ce = chromatic(e);
		return ce != null ? ce.getEnchantment().getMaxLevel() : e.value().getMaxLevel();
	}

	private static ChromaEnchants chromatic(Holder<Enchantment> e) {
		for (ChromaEnchants ce : ChromaEnchants.enchantmentList) {
			if (e.is(ce.key()))
				return ce;
		}
		return null;
	}

	public void removeEnchantment(ResourceKey<Enchantment> e) {
		selected.remove(e);
		this.onEnchantChanged();
		this.setChanged();
	}

	public boolean incrementEnchantment(Holder<Enchantment> e, boolean toMax, Player ep) {
		int level = this.getEnchantment(e);
		int max = getAvailableEnchantmentLevel(e, ep);
		int newlevel = Math.min(max, toMax ? getMaxEnchantmentLevel(e) : level+1);
		return this.setEnchantment(e, newlevel);
	}

	public void decrementEnchantment(Holder<Enchantment> e, boolean toZero) {
		int level = this.getEnchantment(e);
		int newlevel = toZero ? 0 : Math.max(level-1, 0);
		this.setEnchantment(e, newlevel);
	}

	public void clearEnchantments() {
		selected.clear();
		this.setChanged();
	}

	public int getEnchantment(Holder<Enchantment> e) {
		return e.unwrapKey().map(k -> selected.getOrDefault(k, 0)).orElse(0);
	}

	public Map<ResourceKey<Enchantment>, Integer> getEnchantments() {
		return Collections.unmodifiableMap(selected);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return this.isValid(itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int i, ItemStack itemstack, Direction side) {
		return EnchantmentHelper.hasAnyEnchantments(itemstack) || itemstack.is(Items.ENCHANTED_BOOK);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		CompoundTag ench = new CompoundTag();
		for (Entry<ResourceKey<Enchantment>, Integer> e : selected.entrySet())
			ench.putInt(e.getKey().identifier().toString(), e.getValue());
		NBT.put("enchants", ench);
		NBT.putInt("chroma", chroma);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		selected = new LinkedHashMap<>();
		CompoundTag ench = NBT.getCompoundOrEmpty("enchants");
		for (String k : ench.keySet()) {
			Identifier id = Identifier.tryParse(k);
			int lvl = ench.getIntOr(k, 0);
			if (id != null && lvl > 0)
				selected.put(ResourceKey.create(Registries.ENCHANTMENT, id), lvl);
		}
		chroma = Math.clamp(NBT.getIntOr("chroma", 0), 0, CAPACITY);
	}

	@Override
	public float getOperationFraction() {
		return !this.canProgress() ? 0 : progress.getFraction();
	}

	@Override
	public OperationState getState() {
		return this.isValid(inv.get(0)) && this.enchanting() ? (this.hasSufficientChroma() ? OperationState.RUNNING : OperationState.PENDING) : OperationState.INVALID;
	}

	private static enum EnchantmentTier {
		WORTHLESS(0.25F),
		BASIC(0.75F),
		NORMAL(1F),
		VALUABLE(1.5F),
		RARE(2);

		public final float costFactor;

		private EnchantmentTier(float f) {
			costFactor = f;
		}
	}

	/** V33a also blacklisted Minechem's molecule enchantments and Thaumcraft infusion enchantments by name. */
	public static boolean isBlacklisted(ResourceKey<Enchantment> e) {
		if (e.identifier().getPath().toLowerCase(Locale.ENGLISH).startsWith("molecule"))
			return true;
		return blacklist.contains(e);
	}

	/** V33a API: other mods may keep an enchantment out of the enchanter (never a vanilla or ChromatiCraft one). */
	public static void blacklistEnchantment(ResourceKey<Enchantment> e) {
		String ns = e.identifier().getNamespace();
		if (ns.equals("minecraft")) {
			ChromatiCraft.LOGGER.error("You cannot blacklist vanilla enchantments!");
		}
		else if (ns.equals(ChromatiCraft.MODID)) {
			ChromatiCraft.LOGGER.error("You cannot blacklist ChromatiCraft enchantments!");
		}
		else {
			blacklist.add(e);
			ChromatiCraft.LOGGER.info("Received request to blacklist enchantment "+e.identifier());
		}
	}

	public static int getAvailableEnchantmentLevel(Holder<Enchantment> e, Player ep) {
		ChromaEnchants ce = chromatic(e);
		return ce != null ? getMaxEnchantment(ep, ce) : getMaxEnchantmentLevel(e);
	}

	private static int getMaxEnchantment(Player ep, ChromaEnchants e) {
		int max = e.getEnchantment().getMaxLevel();
		for (int i = 1; i <= max; i++) {
			if (!e.getEnchantment().isVisibleToPlayer(ep, i)) {
				return i-1;
			}
		}
		return max;
	}

	public boolean isAssisted() {
		return level != null && level.getBlockState(this.getBlockPos().above()).getBlock() instanceof EnchantingTableBlock;
	}

	/**
	 * V33a: the bookshelf power around the table above -- each of the eight directions whose two blocks next to the
	 * table are air counts the two-high column two blocks out (and, on diagonals, the two flanking columns), as a
	 * fraction of the most it could be.
	 */
	private float getAssistPower() {
		float ret = 0;
		float max = 0;

		BlockPos p = this.getBlockPos();
		int x = p.getX();
		int y = p.getY()+1;
		int z = p.getZ();
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				if ((dz != 0 || dx != 0) && level.isEmptyBlock(new BlockPos(x+dx, y, z+dz)) && level.isEmptyBlock(new BlockPos(x+dx, y+1, z+dz))) {
					ret += this.enchantPower(x+dx*2, y, z+dz*2);
					ret += this.enchantPower(x+dx*2, y+1, z+dz*2);
					max += 2;

					if (dx != 0 && dz != 0) {
						ret += this.enchantPower(x+dx*2, y, z+dz);
						ret += this.enchantPower(x+dx*2, y+1, z+dz);
						ret += this.enchantPower(x+dx, y, z+dz*2);
						ret += this.enchantPower(x+dx, y+1, z+dz*2);
						max += 4;
					}
				}
			}
		}
		return max > 0 ? ret/max : 0;
	}

	/** Forge {@code ForgeHooks.getEnchantPower}: NeoForge's per-block enchant power. */
	private float enchantPower(int x, int y, int z) {
		BlockPos at = new BlockPos(x, y, z);
		return level.getBlockState(at).getEnchantPowerBonus(level, at);
	}

	@Override
	public int getIconState(int side) {
		return side > 1 && this.isAssisted() ? 1 : 0;
	}

	/** V33a OperationInterval.hasWork (the port's interface does not carry it). */
	public boolean hasWork() {
		return this.getState() == OperationState.RUNNING;
	}

	/** V33a {@code FluidReceiverInventoryBase}: liquid chroma in from any side, never out. */
	public ResourceHandler<FluidResource> fluidHandler() {
		return fluidHandler;
	}

	/** The menu's synchronised values: progress and chroma level (V33a sent a progress bar and a tank packet). */
	public final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch(index) {
				case 0 -> progressTimer;
				case 1 -> chroma;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch(index) {
				case 0 -> progressTimer = value;
				case 1 -> chroma = value;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	};

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuAutoEnchanter(id, inventory, this, data);
	}

	private final class EnchanterFluidHandler extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
		@Override public int size() { return 1; }
		@Override public FluidResource getResource(int index) {
			return index == 0 && chroma > 0 ? FluidResource.of(ChromaFluids.CHROMA.get()) : FluidResource.EMPTY;
		}
		@Override public long getAmountAsLong(int index) { return index == 0 ? chroma : 0; }
		@Override public long getCapacityAsLong(int index, FluidResource resource) {
			return index == 0 && (resource.isEmpty() || this.isValid(index, resource)) ? CAPACITY : 0;
		}
		@Override public boolean isValid(int index, FluidResource resource) {
			return index == 0 && !resource.isEmpty() && resource.getFluid() == ChromaFluids.CHROMA.get();
		}
		@Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
			if (!this.isValid(index, resource))
				return 0;
			int accepted = Math.min(amount, CAPACITY-chroma);
			if (accepted <= 0)
				return 0;
			this.updateSnapshots(transaction);
			chroma += accepted;
			return accepted;
		}
		@Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			return 0;
		}
		@Override protected Integer createSnapshot() { return chroma; }
		@Override protected void revertToSnapshot(Integer snapshot) { chroma = snapshot; }
		@Override protected void onRootCommit(Integer originalState) { setChanged(); }
	}

}
