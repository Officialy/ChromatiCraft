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
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.dragonapi.libraries.ReikaRecipeHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;


public class RouterNodeRecipe extends CastingRecipe {

	public RouterNodeRecipe(ItemStack out, boolean in) {
		super(out, getRecipe(out, in));
	}

	private static IRecipe getRecipe(ItemStack out, boolean in) {
		return ReikaRecipeHelper.getShapedRecipeFor(out, " R ", "RIR", "AAA", 'R', in ? ReikaItemHelper.lapisDye : Items.redstone, 'I', Items.iron_ingot, 'A', in ? ChromaStacks.auraDust : ChromaStacks.beaconDust);
	}

	@Override
	public int getNumberProduced() {
		return 2;
	}

	@Override
	protected void filterMatchTags(ItemStack is) {
		super.filterMatchTags(is);
		is.stackTagCompound.removeTag("target");
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
