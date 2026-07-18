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

import reika.chromaticraft.auxiliary.interfaces.CoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class StandRecipe extends TempleCastingRecipe implements CoreRecipe {

	public StandRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.PURPLE, -2, 0, 3);
		this.addRune(CrystalElement.PURPLE, 2, 0, -3);
		this.addRune(CrystalElement.BLACK, -2, 0, -3);
		this.addRune(CrystalElement.BLACK, 2, 0, 3);
	}

	@Override
	public int getExperience() {
		return 2*super.getExperience();
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 25;
	}

}
