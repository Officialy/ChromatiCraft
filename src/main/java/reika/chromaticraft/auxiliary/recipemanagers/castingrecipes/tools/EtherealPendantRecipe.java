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

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

public class EtherealPendantRecipe extends MultiBlockCastingRecipe {

	public EtherealPendantRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromatiCraft.luma, -2, 0);
		this.addAuxItem(ChromatiCraft.luma, 2, 0);
		this.addAuxItem(ChromatiCraft.luma, 0, 2);
		this.addAuxItem(ChromatiCraft.luma, 0, -2);

		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLACK), -4, 0);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLACK), 4, 0);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLACK), 0, 4);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLACK), 0, -4);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLUE), 2, 2);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLUE), -2, 2);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLUE), 2, -2);
		this.addAuxItem(ChromaBlocks.GLASS.getStackOf(CrystalElement.BLUE), -2, -2);

		this.addAuxItem(Items.string, -2, -4);
		this.addAuxItem(Items.string, 2, -4);

		this.addAuxItem(ChromaStacks.spaceIngot, -2, 4);
		this.addAuxItem(ChromaStacks.spaceIngot, 2, 4);

		this.addAuxItem(ChromaStacks.auraDust, 4, -2);
		this.addAuxItem(ChromaStacks.auraDust, 4, 2);
		this.addAuxItem(ChromaStacks.auraDust, -4, -2);
		this.addAuxItem(ChromaStacks.auraDust, -4, 2);
	}

}
