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

import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

public class PotionCrystalRecipe extends PylonCastingRecipe {

	public PotionCrystalRecipe(CrystalElement e) {
		super(ChromaBlocks.SUPER.getStackOfMetadata(e.ordinal()), ChromaBlocks.CRYSTAL.getStackOfMetadata(e.ordinal()));

		this.addAuxItem(Blocks.obsidian, -2, 2);
		this.addAuxItem(Blocks.gold_block, 0, 2);
		this.addAuxItem(Blocks.obsidian, 2, 2);

		this.addAuxItem(Blocks.glowstone, 2, -2);
		this.addAuxItem(Blocks.redstone_block, -2, -2);

		this.addAuraRequirement(CrystalElement.WHITE, 10000);
		//this.addAuraRequirement(CrystalElement.BLUE, 1000);
		this.addAuraRequirement(CrystalElement.PURPLE, 5000);
		this.addAuraRequirement(e, 20000);
	}

	@Override
	public int getDuration() {
		return 4*super.getDuration();
	}

	@Override
	public float[] getHarmonics() {
		CrystalElement e = CrystalElement.elements[this.getOutput().getItemDamage()%16];
		float[] ret = new float[3];
		for (int i = 0; i <= 2; i++) {
			ret[i] = CrystalMusicManager.instance.getScaledDing(e, i);
		}
		return ret;
	}

}
