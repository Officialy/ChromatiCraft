/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;


public class FakeSkyRecipe extends TempleCastingRecipe {

	public FakeSkyRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.BLUE, -2, 0, -5);
	}

	@Override
	public int getNumberProduced() {
		return 4;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 4;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
