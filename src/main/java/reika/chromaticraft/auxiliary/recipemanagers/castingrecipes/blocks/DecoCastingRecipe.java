package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;


public class DecoCastingRecipe extends CastingRecipe {

	public DecoCastingRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);
	}

	@Override
	public final boolean canGiveDoubleOutput() {
		return true;
	}

	@Override
	public final int getTypicalCraftedAmount() {
		return this.getTypicalTotalAmount()/this.getNumberProduced();
	}

	protected int getTypicalTotalAmount() {
		return this.getNumberProduced();
	}

}
