/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class OreSilkerRecipe extends TempleCastingRecipe {

	public OreSilkerRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.PURPLE, 0, -1, 1);
		this.addRune(CrystalElement.PURPLE, 0, -1, -1);

		this.addRune(CrystalElement.LIME, 1, -1, 0);
		this.addRune(CrystalElement.LIME, -1, -1, 0);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 4;
	}

}
