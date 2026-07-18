/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.interfaces.CoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;

public class CrystalLampRecipe extends CastingRecipe implements CoreRecipe {

	public CrystalLampRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 32;
	}

	@Override
	public int getPenaltyThreshold() {
		return super.getPenaltyThreshold()*4/5;
	}

	@Override
	public float getPenaltyMultiplier() {
		return 0.5F;
	}

}
