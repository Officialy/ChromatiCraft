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
import reika.chromaticraft.items.itemblock.ItemBlockCrystalGlow;

public class CrystalGlowRecipe extends CastingRecipe implements CoreRecipe {

	public CrystalGlowRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 8;
	}

	@Override
	public int getPenaltyThreshold() {
		return super.getPenaltyThreshold()/2;
	}

	@Override
	public float getPenaltyMultiplier() {
		return 0.5F;
	}

	@Override
	public int getNumberProduced() {
		return 16;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

	@Override
	public boolean shouldGroupAsRecipe(ItemStack is1, ItemStack is2) {
		return ItemBlockCrystalGlow.getBase(is1) == ItemBlockCrystalGlow.getBase(is2);
	}
}
