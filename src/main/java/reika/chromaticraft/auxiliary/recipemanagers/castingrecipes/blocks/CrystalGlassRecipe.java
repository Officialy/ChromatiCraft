/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.init.Blocks;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaRecipeHelper;


public class CrystalGlassRecipe extends DecoCastingRecipe {

	public CrystalGlassRecipe(CrystalElement e) {
		super(ChromaBlocks.GLASS.getStackOfMetadata(e.ordinal()), getRecipe(e));
	}

	private static IRecipe getRecipe(CrystalElement e) {
		return ReikaRecipeHelper.getShapedRecipeFor(ChromaBlocks.GLASS.getStackOfMetadata(e.ordinal()), "GSG", "SGS", "GSG", 'G', Blocks.glass, 'S', ChromaItems.SHARD.getStackOf(e));
	}

	@Override
	public int getNumberProduced() {
		return 16;
	}

	@Override
	public int getTypicalTotalAmount() {
		return 1024;
	}

}
