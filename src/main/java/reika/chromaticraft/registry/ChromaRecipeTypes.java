package reika.chromaticraft.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;

public final class ChromaRecipeTypes {
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, ChromatiCraft.MODID);
	public static final DeferredHolder<RecipeType<?>, RecipeType<CastingTableRecipe>> CASTING = RECIPE_TYPES.register("casting", () -> new RecipeType<CastingTableRecipe>() {});
	private ChromaRecipeTypes() {}
}
