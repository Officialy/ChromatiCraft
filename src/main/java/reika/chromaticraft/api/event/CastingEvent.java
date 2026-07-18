/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.instantiable.event.TileEntityEvent;


public class CastingEvent extends TileEntityEvent {

	public final EntityPlayer player;
	private final ItemStack result;
	public final CastingRecipe recipe;

	public CastingEvent(TileEntityCastingTable te, CastingRecipe cr, EntityPlayer ep, ItemStack is) {
		super(te);
		player = ep;
		result = is;
		recipe = cr;
	}

	public ItemStack getCraftedItem() {
		return result.copy();
	}

}
