package reika.chromaticraft.auxiliary.recipemanagers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;

/** The two source fluids currently visible to a Coalescence Orchid. */
public record CobbleGeneratorInput(Fluid primary, Fluid secondary) implements RecipeInput {
	@Override public ItemStack getItem(int index) { return ItemStack.EMPTY; }
	@Override public int size() { return 0; }
}
