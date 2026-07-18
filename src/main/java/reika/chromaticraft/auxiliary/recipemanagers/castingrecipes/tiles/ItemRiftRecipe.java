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

public class ItemRiftRecipe extends TempleCastingRecipe {

	public ItemRiftRecipe(ItemStack out, IRecipe main) {
		super(out, main);

		this.addRune(CrystalElement.LIME, -3, -1, 4);
		this.addRuneRingRune(CrystalElement.GRAY);
	}

	@Override
	public int getNumberProduced() {
		return 2;
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 18;
	}

}
