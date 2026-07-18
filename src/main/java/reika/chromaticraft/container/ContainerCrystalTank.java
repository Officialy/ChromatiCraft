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

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.storage.TileEntityCrystalTank;
import reika.dragonapi.base.CoreContainer;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

public class ContainerCrystalTank extends CoreContainer {

	public ContainerCrystalTank(EntityPlayer player, TileEntityCrystalTank te) {
		super(player, te);
		this.setAlwaysInteractable();
	}

	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();
		ReikaPacketHelper.sendTankSyncPacket(ChromatiCraft.packetChannel, tile, "tank");
	}

}
