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

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;


public class RecipeAreaBreaker extends MultiBlockCastingRecipe {

	public RecipeAreaBreaker(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.energyPowder, -2, 0);
		this.addAuxItem(ChromaStacks.energyPowder, 2, 0);

		this.addAuxItem(ChromaStacks.grayShard, 2, 2);
		this.addAuxItem(ChromaStacks.grayShard, -2, 2);
		this.addAuxItem(ChromaStacks.grayShard, 2, -2);
		this.addAuxItem(ChromaStacks.grayShard, -2, -2);

		this.addAuxItem(ChromaStacks.teleDust, 0, -2);
		this.addAuxItem(ChromaStacks.teleDust, 0, 2);
	}

}
