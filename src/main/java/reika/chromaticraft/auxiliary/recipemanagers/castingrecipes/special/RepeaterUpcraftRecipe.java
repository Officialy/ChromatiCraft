/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special;

import java.util.Collection;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.RepeaterRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.RecipeCrystalRepeater;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;

public class RepeaterUpcraftRecipe extends RepeaterRecipe {

	public RepeaterUpcraftRecipe(RecipeCrystalRepeater r) {
		super(ChromaTiles.REPEATER, ChromaStacks.elementUnit);

		this.addAuxItems(r);
		this.addAuxItem(ChromaStacks.crystalPowder, -2, -2);
		this.addAuxItem(ChromaStacks.crystalPowder, 2, -2);
		this.addAuxItem(ChromaStacks.crystalPowder, -2, 2);
		this.addAuxItem(ChromaStacks.crystalPowder, 2, 2);

		this.addAuxItem(ChromaTiles.WEAKREPEATER.getCraftedProduct(), -4, -4);
		this.addAuxItem(ChromaTiles.WEAKREPEATER.getCraftedProduct(), 4, -4);
		this.addAuxItem(ChromaTiles.WEAKREPEATER.getCraftedProduct(), -4, 4);
		this.addAuxItem(ChromaTiles.WEAKREPEATER.getCraftedProduct(), 4, 4);
	}

	@Override
	public int getNumberProduced() {
		return 4;
	}

	@Override
	public boolean canBeStacked() {
		return true;
	}

	@Override
	public float getConsecutiveStackingTimeFactor(TileEntityCastingTable te) {
		return 0.75F;
	}

	@Override
	public int getExperience() {
		return 0;
	}

	@Override
	public int getDuration() {
		return super.getDuration()*3;
	}

	@Override
	public void getRequiredProgress(Collection<ProgressStage> c) {
		super.getRequiredProgress(c);

		c.addAll(ProgressionManager.instance.getPrereqs(ProgressStage.REPEATER));
	}

}
