package reika.chromaticraft.auxiliary.recipemanagers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** Snapshot of the catalyst and every dropped stack sharing one Liquid Chroma source cell. */
public record PoolAlloyingInput(ItemStack catalyst, List<ItemStack> items) implements RecipeInput {
	public PoolAlloyingInput {
		catalyst = catalyst.copy();
		items = items.stream().map(ItemStack::copy).toList();
	}

	@Override public ItemStack getItem(int index) { return index == 0 ? catalyst : items.get(index - 1); }
	@Override public int size() { return items.size() + 1; }
}
