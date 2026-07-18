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

public class RedstonePodRecipe extends TempleCastingRecipe {

	public RedstonePodRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.LIME, -1, 0, -3);
		this.addRune(CrystalElement.YELLOW, 1, 0, 3);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 32;
	}

	@Override
	public int getNumberProduced() {
		return 2;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
