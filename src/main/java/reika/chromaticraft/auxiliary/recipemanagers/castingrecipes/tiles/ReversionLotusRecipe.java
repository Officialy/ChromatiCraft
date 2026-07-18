/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class ReversionLotusRecipe extends TempleCastingRecipe {

	public ReversionLotusRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.GREEN, 2, -1, -5);
		this.addRune(CrystalElement.MAGENTA, -2, -1, 5);
	}

	@Override
	public int getNumberProduced() {
		return 6;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 32;
	}

}
