package reika.chromaticraft.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;

public final class ChromaRecipeSerializers {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ChromatiCraft.MODID);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CastingTableRecipe>> CASTING = RECIPE_SERIALIZERS.register("casting", () -> new RecipeSerializer<>(CastingTableRecipe.CODEC, CastingTableRecipe.STREAM_CODEC));
	private ChromaRecipeSerializers() {}
}
