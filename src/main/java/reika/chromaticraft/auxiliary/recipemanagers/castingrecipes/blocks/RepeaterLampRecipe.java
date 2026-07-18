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

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.block.decoration.BlockRepeaterLight;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.libraries.ReikaRecipeHelper;


public class RepeaterLampRecipe extends CastingRecipe {

	public RepeaterLampRecipe(ChromaTiles c) {
		super(calcOutput(c), ReikaRecipeHelper.getShapedRecipeFor(calcOutput(c), getRecipe(c)));
	}

	private static Object[] getRecipe(ChromaTiles c) {
		return new Object[]{"ggg", "grg", "ggg", 'r', c.getCraftedProduct(), 'g', Items.glowstone_dust};
	}

	private static ItemStack calcOutput(ChromaTiles c) {
		return ChromaBlocks.REPEATERLAMP.getStackOfMetadata(BlockRepeaterLight.getMetadataFor(c));
	}

	@Override
	public int getNumberProduced() {
		return 12;
	}

	@Override
	public int getExperience() {
		return 1; //1/5 of normal
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
