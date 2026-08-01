package reika.chromaticraft.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItemTags;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Ordinary crafting-grid recipes, ported from V33a {@code ChromaRecipes}.
 *
 * <p>Almost all of ChromatiCraft is crafted on the Casting Table, so this provider is small by
 * design — its job is the handful of bootstrap recipes that let a player reach the table at all.
 */
public final class ChromaRecipeProvider extends RecipeProvider.Runner {

	public ChromaRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public String getName() {
		return "ChromatiCraft Recipes";
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput out) {
		return new Recipes(registries, out);
	}

	private static final class Recipes extends RecipeProvider {

		private final RecipeOutput out;

		Recipes(HolderLookup.Provider registries, RecipeOutput out) {
			super(registries, out);
			this.out = out;
		}

		@Override
		protected void buildRecipes() {
			// V33a: GameRegistry.addRecipe(TABLE, "SCS", "SsS", "sss",
			//         'S', Blocks.stone, 's', SHARD wildcard-meta, 'C', Blocks.crafting_table)
			// The wildcard metadata covers all 32 shard subtypes, so boosted shards qualify too.
			Ingredient anyShard = anyShard();
			shaped(RecipeCategory.DECORATIONS, ChromaBlocks.CASTING_TABLE.get())
					.define('S', Blocks.STONE)
					.define('C', Blocks.CRAFTING_TABLE)
					.define('s', anyShard)
					.pattern("SCS")
					.pattern("SsS")
					.pattern("sss")
					.unlockedBy("has_shard", has(ChromaItems.SHARDS.get(CrystalElement.BLUE).get()))
					.save(out, key("casting_table"));

			// V33a: ChromaItems.TOOL.addRecipe("  s", " S ", "S  ", 'S', Items.stick, 's', SHARD wildcard)
			shaped(RecipeCategory.TOOLS, ChromaItems.MANIPULATOR.get())
					.define('S', Items.STICK)
					.define('s', anyShard)
					.pattern("  s")
					.pattern(" S ")
					.pattern("S  ")
					.unlockedBy("has_shard", has(ChromaItems.SHARDS.get(CrystalElement.BLUE).get()))
					.save(out, key("manipulator"));

			// CHROMA-PORT: V33a also crafts the guide book (ChromaItems.HELP, "Chromic Lexicon") here:
			//   "abc", "gBg", "def" with 'B' book, 'g' glowstone dust, and a/b/c/d/e/f the black,
			//   blue, green, yellow, red and white shards. ItemChromaBook is not ported -- it needs
			//   ChromaResearch, the gui/book family and the XML help-data pipeline first.
		}

		/**
		 * V33a's wildcard-metadata shard stack: any colour, plain or boosted. The tag already spans
		 * both forms in all sixteen colours, so this no longer spells out the 32 items.
		 */
		private Ingredient anyShard() {
			return tag(ChromaItemTags.CRYSTAL_SHARDS);
		}

		private static ResourceKey<Recipe<?>> key(String name) {
			return ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name));
		}
	}
}
