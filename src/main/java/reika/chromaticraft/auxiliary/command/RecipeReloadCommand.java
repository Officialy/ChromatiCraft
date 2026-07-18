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

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.RecipesCastingTable;
import reika.dragonapi.command.DragonCommandBase;


public class RecipeReloadCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		RecipesCastingTable.instance.reload();
		ChromatiCraft.logger.log("Casting Recipes reloaded.");
		this.sendChatToSender(ics, "Recipes reloaded.");
	}

	@Override
	public String getCommandString() {
		return "reloadchromacasting";
	}

	@Override
	protected boolean isAdminOnly() {
		return true;
	}

}
