/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;

import reika.chromaticraft.modinterface.thaumcraft.NodeRecharger;
import reika.dragonapi.command.DragonCommandBase;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.modregistry.InterfaceCache;


public class NodeWrapperInspectionCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer ep = this.getCommandSenderAsPlayer(ics);
		MovingObjectPosition mov = ReikaPlayerAPI.getLookedAtBlock(ep, 5, false);
		if (mov != null) {
			TileEntity te = ep.worldObj.getTileEntity(mov.blockX, mov.blockY, mov.blockZ);
			if (InterfaceCache.NODE.instanceOf(te)) {
				WorldLocation loc = new WorldLocation(te);
				if (NodeRecharger.instance.hasLocation(loc)) {
					for (String s : NodeRecharger.instance.debug(loc)) {
						this.sendChatToSender(ics, s);
					}
				}
			}
		}
	}

	@Override
	public String getCommandString() {
		return "nodewrapper";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
