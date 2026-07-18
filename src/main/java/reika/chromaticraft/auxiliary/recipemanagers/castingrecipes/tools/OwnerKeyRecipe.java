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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;

import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;


public class OwnerKeyRecipe extends CastingRecipe {

	public OwnerKeyRecipe(ItemStack out, IRecipe recipe) {
		super(out, recipe);
	}

	@Override
	public int getExperience() {
		return 0;
	}

	@Override
	public NBTTagCompound handleNBTResult(TileEntityCastingTable te, EntityPlayer ep, NBTTagCompound originalCenter, NBTTagCompound tag) {
		EntityPlayer plc = te.getPlacer();
		if (plc != null && ep == plc) {
			if (tag == null)
				tag = new NBTTagCompound();
			tag.setString("owner", ep.getUniqueID().toString());
		}
		return tag;
	}

}
