package reika.chromaticraft.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
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
public final class ChromaRecipeProvider {

    private ChromaRecipeProvider() {}

    public static void bootstrap(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        new Recipes(recipes, advancements).buildRecipes();
    }

	private static final class Recipes extends RecipeProvider {

		private final RecipeOutput out;

		Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
            this.out = super.output;
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
					.unlockedBy("has_shard", has(ChromaItemTags.CRYSTAL_SHARDS))
					.save(out, key("casting_table"));

			// V33a: ChromaItems.TOOL.addRecipe("  s", " S ", "S  ", 'S', Items.stick, 's', SHARD wildcard)
			shaped(RecipeCategory.TOOLS, ChromaItems.MANIPULATOR.get())
					.define('S', Items.STICK)
					.define('s', anyShard)
					.pattern("  s")
					.pattern(" S ")
					.pattern("S  ")
					.unlockedBy("has_shard", has(ChromaItemTags.CRYSTAL_SHARDS))
					.save(out, key("manipulator"));

			// V33a ChromaRecipes line 69, with metadata shards mapped to their separate 26.2 items.
			shaped(RecipeCategory.MISC, ChromaItems.LEXICON.get())
					.define('B', Items.BOOK)
					.define('g', Items.GLOWSTONE_DUST)
					.define('a', ChromaItems.SHARDS.get(CrystalElement.BLACK).get())
					.define('b', ChromaItems.SHARDS.get(CrystalElement.BLUE).get())
					.define('c', ChromaItems.SHARDS.get(CrystalElement.GREEN).get())
					.define('d', ChromaItems.SHARDS.get(CrystalElement.YELLOW).get())
					.define('e', ChromaItems.SHARDS.get(CrystalElement.RED).get())
					.define('f', ChromaItems.SHARDS.get(CrystalElement.WHITE).get())
					.pattern("abc")
					.pattern("gBg")
					.pattern("def")
					.unlockedBy("has_shard", has(ChromaItemTags.CRYSTAL_SHARDS))
					.save(out, key("chromic_lexicon"));

			// V33a ChromatiCraft.load: GameRegistry.addShapelessRecipe(used, flower), where `used` is the
			// vanilla dye when isVanillaDyeMoreCommon (getVanillaDyeChance > 50) and ChromatiCraft's
			// own dye otherwise. The chance defaults to 100, so on default config it is always the
			// vanilla dye. CHROMA-PORT: the crystal-dye branch returns with ItemCrystalDye and the
			// per-colour dye-chance config.
			for (CrystalElement element : CrystalElement.elements) {
				Item dye = Items.DYE.pick(reika.dragonapi.libraries.registry.ReikaDyeHelper.dyes[element.ordinal()].getDye());
				shapeless(RecipeCategory.MISC, dye)
						.requires(ChromaBlocks.dyeFlower(element).get())
						.unlockedBy("has_dye_flower", has(ChromaBlocks.dyeFlower(element).get()))
						.save(out, key("dye_from_dye_flower_" + element.getEnglishName()));
			}

			// V33a ChromaRecipes: sparkling clay bakes to sparkling bricks (0.2 xp) and sparkling
			// cobblestone to sparkling stone (no xp), exactly as their proxies do.
			sparkleSmelting(reika.chromaticraft.block.worldgen26.BlockSparkle.Type.CLAY,
					reika.chromaticraft.block.worldgen26.BlockSparkle.Type.BRICKS, 0.2F);
			sparkleSmelting(reika.chromaticraft.block.worldgen26.BlockSparkle.Type.COBBLE,
					reika.chromaticraft.block.worldgen26.BlockSparkle.Type.STONE, 0);
		}

		private void sparkleSmelting(reika.chromaticraft.block.worldgen26.BlockSparkle.Type from,
				reika.chromaticraft.block.worldgen26.BlockSparkle.Type to, float xp) {
			net.minecraft.world.level.block.Block input = ChromaBlocks.sparkle(from).get();
			net.minecraft.data.recipes.SimpleCookingRecipeBuilder.smelting(Ingredient.of(input),
					RecipeCategory.BUILDING_BLOCKS, net.minecraft.world.item.crafting.CookingBookCategory.BLOCKS,
					ChromaBlocks.sparkle(to).get(), xp, 200)
					.unlockedBy("has_" + from.registryName(), has(input))
					.save(out, key(to.registryName() + "_from_smelting"));
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
