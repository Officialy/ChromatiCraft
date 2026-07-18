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


public class EssentiaRelayRecipe extends TempleCastingRecipe {

	public EssentiaRelayRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);

		this.addRune(CrystalElement.BLACK, 1, -1, 3);
		this.addRune(CrystalElement.LIME, 1, -1, -3);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 4;
	}

	@Override
	public int getNumberProduced() {
		return 2;
	}
}
