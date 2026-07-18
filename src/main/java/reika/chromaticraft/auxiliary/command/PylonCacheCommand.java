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

import reika.chromaticraft.world.iwg.PylonGenerator;
import reika.dragonapi.command.DragonCommandBase;


public class PylonCacheCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		PylonGenerator.instance.printPylonCache(ics);
	}

	@Override
	public String getCommandString() {
		return "pylonlocs";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
