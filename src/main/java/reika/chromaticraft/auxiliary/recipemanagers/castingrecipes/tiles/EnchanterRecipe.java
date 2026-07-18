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

public class EnchanterRecipe extends TempleCastingRecipe {

	public EnchanterRecipe(ItemStack out, IRecipe in) {
		super(out, in);

		this.addRune(CrystalElement.BLACK, -4, 0, -3);
		this.addRune(CrystalElement.PURPLE, -3, 0, -4);
		this.addRune(CrystalElement.BLACK, 4, 0, 3);
		this.addRune(CrystalElement.PURPLE, 3, 0, 4);
	}

}
