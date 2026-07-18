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

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;

import reika.chromaticraft.auxiliary.interfaces.CoreRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaRecipeHelper;

public class RuneRecipe extends CastingRecipe implements CoreRecipe {

	public RuneRecipe(CrystalElement e) {
		super(genOutput(e), getRecipe(e, false));
	}

	static ShapedRecipes getRecipe(CrystalElement e, boolean enhanced) {
		ItemStack shard = ChromaItems.SHARD.getStackOfMetadata(enhanced ? 16+e.ordinal() : e.ordinal());
		return ReikaRecipeHelper.getShapedRecipeFor(genOutput(e), "SSS", "SCS", "SSS", 'C', shard, 'S', ChromaBlocks.PYLONSTRUCT.getStackOf());
	}

	static ItemStack genOutput(CrystalElement e) {
		return ChromaBlocks.RUNE.getStackOf(e);
	}

	@Override
	public int getExperience() {
		return 2*super.getExperience();
	}

	@Override
	public void getRequiredProgress(Collection<ProgressStage> c) {
		super.getRequiredProgress(c);
		c.add(ProgressStage.ALLCOLORS);
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
