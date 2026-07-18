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

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class ExcavatorRecipe extends MultiBlockCastingRecipe {

	public ExcavatorRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		//for now
		this.addAuxItem(Items.stick, -2, -2);
		this.addAuxItem(Items.stick, 2, 2);
		this.addAuxItem(Items.stick, -2, 2);
		this.addAuxItem(Items.stick, 2, -2);

		this.addAuxItem(ChromaStacks.chromaIngot, -2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 0, 2);
		this.addAuxItem(ChromaStacks.chromaIngot, 0, -2);

		this.addAuxItem(this.getChargedShard(CrystalElement.BROWN), -4, 0);
		this.addAuxItem(this.getChargedShard(CrystalElement.BROWN), 4, 0);
		this.addAuxItem(this.getChargedShard(CrystalElement.BROWN), 0, 4);
		this.addAuxItem(this.getChargedShard(CrystalElement.BROWN), 0, -4);
	}

}
