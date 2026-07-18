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


public class CastingInjectorFocusRecipe extends TempleCastingRecipe {

	public CastingInjectorFocusRecipe(ItemStack out, IRecipe recipe, TempleCastingRecipe... groups) {
		super(out, recipe);

		for (TempleCastingRecipe cr : groups) {
			this.addRunes(cr.getRunes());
		}
	}

	@Override
	public int getNumberProduced() {
		return 1;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 8;
	}

}
