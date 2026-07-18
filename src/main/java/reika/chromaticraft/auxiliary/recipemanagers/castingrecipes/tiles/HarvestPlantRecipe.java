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


public class HarvestPlantRecipe extends TempleCastingRecipe {

	public HarvestPlantRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.YELLOW, -5, 0, -2);
		this.addRune(CrystalElement.GREEN, -5, 0, 2);
	}

}
