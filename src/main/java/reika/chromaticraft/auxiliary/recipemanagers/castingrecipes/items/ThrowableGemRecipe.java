/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaRecipeHelper;


public class ThrowableGemRecipe extends TempleCastingRecipe {

	public ThrowableGemRecipe(CrystalElement e) {
		super(getOutput(e), getRecipe(e));

		this.addRuneRingRune(e);
	}

	private static IRecipe getRecipe(CrystalElement e) {
		return ReikaRecipeHelper.getShapedRecipeFor(getOutput(e), "RRR", "RER", "RRR", 'R', Items.redstone, 'E', ChromaItems.ELEMENTAL.getStackOf(e));
	}

	private static ItemStack getOutput(CrystalElement e) {
		return ChromaItems.THROWGEM.getCraftedMetadataProduct(8, e.ordinal());
	}

}
