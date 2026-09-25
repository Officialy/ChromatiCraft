package reika.chromaticraft.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe.CountedIngredient;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.magic.progression.ProgressStage;

/** Datapack transcription of V33a {@code PoolRecipes}; expanded as its registered ingredients land. */
public final class ChromaPoolRecipeProvider extends RecipeProvider.Runner {
	public ChromaPoolRecipeProvider(PackOutput output,
			CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override public String getName() { return "ChromatiCraft Liquid Chroma Pool Recipes"; }

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
		return new Recipes(registries, output);
	}

	private static final class Recipes extends RecipeProvider {
		private final RecipeOutput output;

		private Recipes(HolderLookup.Provider registries, RecipeOutput output) {
			super(registries, output);
			this.output = output;
		}

		@Override
		protected void buildRecipes() {
			// V33a: iron catalyst + sixteen Chromic Dust -> one Chroma Alloy Ingot.
			save("chroma_ingot", Items.IRON_INGOT, ChromaCraftingItems.CHROMA_INGOT,
					count(ChromaTieredItems.CHROMA_DUST, 16));
			save("fiery_ingot", Items.GOLD_INGOT, ChromaCraftingItems.FIERY_INGOT,
					count(ChromaTieredItems.FIRAXITE, 16), count(Items.BLAZE_POWDER, 8),
					count(Items.COAL, 2));
			save("ender_ingot", Items.IRON_INGOT, ChromaCraftingItems.ENDER_INGOT,
					count(ChromaTieredItems.ENDER_DUST, 16), count(Items.ENDER_PEARL, 4));

			// V33a's early, overworld-obtainable pool alloys. Each count is transcribed exactly.
			save("water_ingot", Items.IRON_INGOT, ChromaCraftingItems.WATER_INGOT,
					count(ChromaTieredItems.WATER_DUST, 16), count(Items.GOLD_INGOT, 2));
			save("conductive_ingot", Items.GOLD_INGOT, ChromaCraftingItems.CONDUCTIVE_INGOT,
					count(Items.REDSTONE, 8), count(ChromaTieredItems.BEACON_DUST, 16));
			save("aura_ingot", Items.IRON_INGOT, ChromaCraftingItems.AURA_INGOT,
					count(ChromaTieredItems.AURA_DUST, 8), count(Items.GLOWSTONE_DUST, 8),
					count(Items.REDSTONE, 16), count(Items.QUARTZ, 4));

			// V33a's post-dragon alloy; Spatial Rifting Powder comes from Spacerift Stone in the End.
			save("space_ingot", Items.IRON_INGOT, ChromaCraftingItems.SPACE_INGOT,
					count(ChromaTieredItems.SPACE_DUST, 16), count(Items.GLOWSTONE_DUST, 32),
					count(Items.REDSTONE, 64), count(Items.QUARTZ, 16), count(Items.DIAMOND, 4));

			// The two V33a lines immediately following the alloy family. Their disallow-doubling calls
			// are commented out upstream, so both deliberately retain the normal doubling rule.
			save("experience_gem", ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get(),
					ChromaCraftingItems.EXPERIENCE_GEM,
					count(net.minecraft.world.level.block.Blocks.OBSIDIAN, 4), count(Items.EMERALD, 8));
			save("complex_ingot", ChromaItems.CRAFTING.get(ChromaCraftingItems.EXPERIENCE_GEM).get(),
					ChromaCraftingItems.COMPLEX_INGOT,
					count(ChromaCraftingItems.CHROMA_INGOT), count(ChromaCraftingItems.ENDER_INGOT),
					count(ChromaCraftingItems.WATER_INGOT), count(ChromaCraftingItems.SPACE_INGOT),
					count(ChromaCraftingItems.FIERY_INGOT), count(ChromaCraftingItems.AURA_INGOT),
					count(ChromaCraftingItems.CONDUCTIVE_INGOT));

			// V33a duplicates one Data Crystal from another plus eighteen Crystal Dust, but only after
			// the player has completed the Data Tower progression stage.
			save("data_crystal", new PoolAlloyingRecipe(Ingredient.of(ChromaItems.DATA_CRYSTAL.get()),
					List.of(count(ChromaCraftingItems.CRYSTAL_POWDER, 18)),
					new ItemStackTemplate(ChromaItems.DATA_CRYSTAL.get()), List.of(ProgressStage.TOWER),
					true, 0));

			// V33a STRUCTMAP: a map, alloyed with a compass, a Data Crystal, an Unknown Artefact, a Lumen Pod,
			// twelve Enhanced Ender Eyes and dusts. No doubling, at least two minutes, and the artefact/data
			// crystal alloying effect (shockwaves, the orange crystal, and an explosion when the map forms).
			save("structure_map", new PoolAlloyingRecipe(Ingredient.of(net.minecraft.world.item.Items.MAP),
					List.of(count(net.minecraft.world.item.Items.COMPASS, 1),
							count(ChromaItems.DATA_CRYSTAL.get(), 1),
							count(reika.chromaticraft.registry.ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get(), 1),
							count(reika.chromaticraft.registry.ChromaBlocks.META_ALLOY_LAMP.get(), 1),
							count(ChromaItems.ENDER_EYE.get(), 12),
							count(ChromaTieredItems.AURA_DUST, 64),
							count(ChromaTieredItems.SPACE_DUST, 48),
							count(net.minecraft.world.item.Items.GLOWSTONE_DUST, 32)),
					new ItemStackTemplate(ChromaItems.STRUCTURE_MAP.get()),
					List.of(ProgressStage.TOWER, ProgressStage.ARTEFACT), false, 20 * 60 * 2,
					List.of(reika.chromaticraft.auxiliary.recipemanagers.poolrecipes.AlloyingEffect.Registry.get(
							reika.chromaticraft.auxiliary.recipemanagers.poolrecipes.AlloyingEffect.Registry.ARTEFACT_DATA_CRYSTAL))));
		}

		private void save(String name, net.minecraft.world.level.ItemLike catalyst,
				ChromaCraftingItems result, CountedIngredient... ingredients) {
			save(name, new PoolAlloyingRecipe(Ingredient.of(catalyst), List.of(ingredients),
					new ItemStackTemplate(ChromaItems.CRAFTING.get(result).get()), List.of(), true, 0));
		}

		private static CountedIngredient count(net.minecraft.world.level.ItemLike item, int count) {
			return new CountedIngredient(Ingredient.of(item), count);
		}

		private static CountedIngredient count(ChromaTieredItems item, int count) {
			return count(ChromaItems.TIERED.get(item).get(), count);
		}

		private static CountedIngredient count(ChromaCraftingItems item) {
			return count(ChromaItems.CRAFTING.get(item).get(), 1);
		}

		private static CountedIngredient count(ChromaCraftingItems item, int count) {
			return count(ChromaItems.CRAFTING.get(item).get(), count);
		}

		private void save(String name, PoolAlloyingRecipe recipe) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pool_alloying/" + name));
			output.accept(key, recipe, null);
		}
	}
}
