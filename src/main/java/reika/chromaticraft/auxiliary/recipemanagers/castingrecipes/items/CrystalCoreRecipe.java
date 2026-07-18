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

import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.ShardGroupingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;

public class CrystalCoreRecipe extends MultiBlockCastingRecipe implements ShardGroupingRecipe {

	public CrystalCoreRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.primaryCluster, -2, 0);
		this.addAuxItem(ChromaStacks.primaryCluster, 2, 0);

		this.addAuxItem(ChromaStacks.secondaryCluster, 0, -2);
		this.addAuxItem(ChromaStacks.secondaryCluster, 0, 2);

		//this.addRune(color, rx, ry, rz);
	}

	@Override
	public final boolean canBeSimpleAutomated() {
		return true;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

	@Override
	public int getDuration() {
		return 2*super.getDuration();
	}

	@Override
	public int getNumberProduced() {
		return 4;
	}

}
