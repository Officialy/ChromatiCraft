package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe;
import reika.chromaticraft.client.gui.LexiconIconResolver;
import reika.chromaticraft.item.ItemChromaBook;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaRecipeTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The client's copy of the synced casting and pool-alloying recipes (see {@code ChromaRecipeSync}), and the rule
 * for which of them a player may see in a recipe viewer: the same knowledge the Chromic Lexicon grants. A recipe
 * is shown once the player holds the Lexicon page for its output (if that output has a page) and has the
 * research level or progress the recipe itself demands. Nothing here refers to JEI, so it loads without it.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ChromaClientRecipes {

	private static List<RecipeHolder<CastingTableRecipe>> casting = List.of();
	private static List<RecipeHolder<PoolAlloyingRecipe>> pool = List.of();
	private static int version;

	private static @Nullable Map<Item, LexiconCatalog.Entry> pages;

	private ChromaClientRecipes() {}

	/** HIGH so the store fills before a recipe viewer reacts to the same event. */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRecipesReceived(RecipesReceivedEvent event) {
		var map = event.getRecipeMap();
		casting = List.copyOf(map.byType(ChromaRecipeTypes.CASTING.get()));
		pool = List.copyOf(map.byType(ChromaRecipeTypes.POOL_ALLOYING.get()));
		version++;
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		casting = List.of();
		pool = List.of();
		version++;
	}

	public static List<RecipeHolder<CastingTableRecipe>> casting() {
		return casting;
	}

	public static List<RecipeHolder<PoolAlloyingRecipe>> pool() {
		return pool;
	}

	/** Changes whenever a new set of recipe instances arrives. */
	public static int version() {
		return version;
	}

	/** The Lexicon page listing this item, if any; the first page (in catalog order) that shows it. */
	public static LexiconCatalog.@Nullable Entry pageFor(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		if (pages == null) {
			Map<Item, LexiconCatalog.Entry> map = new HashMap<>();
			for (LexiconCatalog.Entry entry : LexiconCatalog.entries()) {
				for (ItemStack icon : LexiconIconResolver.icons(entry)) {
					if (!icon.isEmpty())
						map.putIfAbsent(icon.getItem(), entry);
				}
			}
			pages = map;
		}
		return pages.get(stack.getItem());
	}

	/** The player's fragment set, fetched once per visibility pass (it is rebuilt on every call). */
	public static Set<String> fragments(Player player) {
		return PlayerResearch.fragments(player);
	}

	private static boolean knowsPage(ItemStack output, Set<String> fragments) {
		LexiconCatalog.Entry page = pageFor(output);
		return page == null || page.readableWithoutFragment() || fragments.contains(page.id());
	}

	public static boolean isVisible(Player player, CastingTableRecipe recipe, Set<String> fragments) {
		return recipe.minimumResearch().playerHas(player) && knowsPage(recipe.output(), fragments);
	}

	public static boolean isVisible(Player player, PoolAlloyingRecipe recipe, Set<String> fragments) {
		return ProgressStage.ALLOY.playerHasPrerequisites(player) && recipe.playerHasProgress(player)
				&& knowsPage(recipe.result(), fragments);
	}

	/** A Lexicon on the player (any slot, offhand included) that holds this page, for "open in Lexicon". */
	public static ItemStack carriedLexiconWith(LexiconCatalog.Entry page) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return ItemStack.EMPTY;
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ChromaItems.LEXICON.get()) && ItemChromaBook.hasPage(stack, page))
				return stack;
		}
		return ItemStack.EMPTY;
	}

}
