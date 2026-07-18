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

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

public class EnhancedPendantRecipe extends PylonCastingRecipe {

	public EnhancedPendantRecipe(CrystalElement e) {
		super(ChromaItems.PENDANT3.getStackOf(e), ChromaBlocks.SUPER.getStackOfMetadata(e.ordinal()));

		this.addAuxItem(Items.diamond, -2, -2);
		this.addAuxItem(ChromaItems.PENDANT.getStackOf(e), 0, -2);
		this.addAuxItem(Items.diamond, 2, -2);

		this.addAuxItem(Items.gold_ingot, -2, 0);
		this.addAuxItem(Items.gold_ingot, 2, 0);

		this.addAuxItem(Items.ender_eye, -2, 2);
		this.addAuxItem(Items.ender_eye, 2, 2);

		this.addAuxItem(Items.ghast_tear, 0, 2);

		this.addAuraRequirement(CrystalElement.PURPLE, 8000);
		this.addAuraRequirement(CrystalElement.WHITE, 2000);
		this.addAuraRequirement(e, 16000);
	}

	@Override
	public int getDuration() {
		return 8*super.getDuration();
	}

}
