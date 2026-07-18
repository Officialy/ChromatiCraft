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

import java.util.Collection;

import reika.chromaticraft.auxiliary.interfaces.CoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

public class StoneRuneRecipe extends TempleCastingRecipe implements CoreRecipe {

	public StoneRuneRecipe(CrystalElement e) {
		super(RuneRecipe.genOutput(e), RuneRecipe.getRecipe(e, true));
		this.addRuneRingRune(e);
	}

	@Override
	public int getExperience() {
		return super.getExperience()*5/2;
	}

	@Override
	public void getRequiredProgress(Collection<ProgressStage> c) {
		super.getRequiredProgress(c);
		c.add(ProgressStage.ALLCOLORS);
	}

	@Override
	public int getNumberProduced() {
		return 4;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
