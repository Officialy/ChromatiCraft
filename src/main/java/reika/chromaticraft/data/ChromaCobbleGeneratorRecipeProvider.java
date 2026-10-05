package reika.chromaticraft.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CobbleGeneratorRecipe;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.CrystalElement;

import java.util.List;

/** Exact V33a fluid-mix table for the Coalescence Orchid. */
public final class ChromaCobbleGeneratorRecipeProvider {

    private ChromaCobbleGeneratorRecipeProvider() {}

    public static void bootstrap(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        new Recipes(recipes, advancements).buildRecipes();
    }

	private static final class Recipes extends RecipeProvider {

		private Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
		}

		@Override
		protected void buildRecipes() {
			save("cobblestone", net.minecraft.world.level.material.Fluids.WATER,
					net.minecraft.world.level.material.Fluids.LAVA, net.minecraft.world.level.block.Blocks.COBBLESTONE,
					10, 20, 0, 0, List.of());
			save("crystal_stone", ChromaFluids.CHROMA.get(),
					net.minecraft.world.level.material.Fluids.LAVA,
					ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get(),
					50, 25, 50, 1, List.of());
			save("cliff_stone", ChromaFluids.LUMA.get(),
					net.minecraft.world.level.material.Fluids.LAVA, ChromaBlocks.CLIFF_STONE.get(),
					120, 0, 10, 2, List.of());
			save("end_stone", ChromaFluids.ENDER.get(),
					net.minecraft.world.level.material.Fluids.LAVA, net.minecraft.world.level.block.Blocks.END_STONE,
					80, 1, 20, 3, List.of(CrystalElement.BLACK, CrystalElement.BROWN));
		}

		private void save(String name, Fluid primary, Fluid secondary, ItemLike result,
				int duration, float primaryChance, float secondaryChance, int order,
				List<CrystalElement> effectElements) {
			CobbleGeneratorRecipe recipe = new CobbleGeneratorRecipe(
					primary.builtInRegistryHolder(), secondary.builtInRegistryHolder(),
					new ItemStackTemplate(result.asItem()), duration, primaryChance,
					secondaryChance, order, effectElements);
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "cobble_generator/" + name));
			output.accept(key, recipe, null);
		}
	}
}
