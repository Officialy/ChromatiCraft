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


public class CobbleGenRecipe extends TempleCastingRecipe {

	public CobbleGenRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.ORANGE, 4, -1, -3);
		this.addRune(CrystalElement.CYAN, 4, -1, 3);
		this.addRune(CrystalElement.BROWN, -4, -1, -1);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 2;
	}

}
