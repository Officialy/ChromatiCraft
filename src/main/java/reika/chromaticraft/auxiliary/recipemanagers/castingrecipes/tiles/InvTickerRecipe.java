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

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

public class InvTickerRecipe extends MultiBlockCastingRecipe {

	public InvTickerRecipe(ItemStack out, ItemStack ctr) {
		super(out, ctr);

		this.addRune(CrystalElement.BLACK, -2, -1, -3);
		this.addRune(CrystalElement.LIGHTBLUE, -3, -1, -2);
		this.addRune(CrystalElement.LIGHTBLUE, 2, -1, 3);
		this.addRune(CrystalElement.BLACK, 3, -1, 2);

		this.addAuxItem(Items.clock, -2, 0);
		this.addAuxItem(Items.clock, 2, 0);
		this.addAuxItem(ChromaStacks.auraDust, 0, 2);
		this.addAuxItem(ChromaStacks.auraDust, 0, -2);

		this.addAuxItem(ChromaStacks.chromaIngot, -2, -2);
		this.addAuxItem(ChromaStacks.chromaIngot, -2, 2);
		this.addAuxItem(ChromaStacks.chromaIngot, 2, -2);
		this.addAuxItem(ChromaStacks.chromaIngot, 2, 2);

		this.addAuxItem(ChromaBlocks.PYLONSTRUCT.getStackOf(), -2, -2);
		this.addAuxItem(ChromaBlocks.PYLONSTRUCT.getStackOf(), -2, 2);
		this.addAuxItem(ChromaBlocks.PYLONSTRUCT.getStackOf(), 2, -2);
		this.addAuxItem(ChromaBlocks.PYLONSTRUCT.getStackOf(), 2, 2);
	}

}
