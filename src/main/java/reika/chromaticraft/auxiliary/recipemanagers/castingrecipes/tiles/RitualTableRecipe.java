/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;

public class RitualTableRecipe extends CastingRecipe {

	public RitualTableRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);
	}

	@Override
	public void onCrafted(TileEntityCastingTable te, EntityPlayer ep, ItemStack output, int amount) {
		super.onCrafted(te, ep, output, amount);
		//achievement
	}

}
