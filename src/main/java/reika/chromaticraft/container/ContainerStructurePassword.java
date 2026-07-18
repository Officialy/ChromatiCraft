/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.block.dimension.structure.blockstructuredatastorage.TileEntityStructurePassword;
import reika.dragonapi.base.CoreContainer;

public class ContainerStructurePassword extends CoreContainer {

	private final TileEntityStructurePassword tile;

	public ContainerStructurePassword(EntityPlayer player, TileEntityStructurePassword te) {
		super(player, te);
		tile = te;

		for (int i = 0; i < 8; i++)
			this.addSlot(i, 17+i*18, 25);

		this.addPlayerInventoryWithOffset(player, 0, -29);
	}

	@Override
	public ItemStack slotClick(int ID, int par2, int par3, EntityPlayer ep) {
		ItemStack ret = super.slotClick(ID, par2, par3, ep);
		tile.checkPassword(ep);
		return ret;
	}


}
