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

public class HeatLilyRecipe extends TempleCastingRecipe {

	public HeatLilyRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.ORANGE, -3, -1, 0);
		this.addRune(CrystalElement.ORANGE, 3, -1, 0);
	}

	@Override
	public int getNumberProduced() {
		return 3;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 16;
	}

}
