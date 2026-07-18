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

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class TrapFloorRecipe extends TempleCastingRecipe {

	public TrapFloorRecipe(ItemStack out, int amt, IRecipe recipe) {
		super(ReikaItemHelper.getSizedItemStack(out, amt), recipe);

		this.addRune(CrystalElement.BROWN, 5, 0, -1);
		this.addRune(CrystalElement.LIGHTGRAY, -5, 0, 1);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 6;
	}

	@Override
	public int getNumberProduced() {
		return 6;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
