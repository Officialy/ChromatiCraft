/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api;

import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.api.crystalelementaccessor.CrystalElementProxy;

public interface PlayerBufferAPI {

	/** Subtracts the specified amount of (or, if the player has less, all of their) energy of a given color from a player's buffer. */
	public void removeFromPlayer(EntityPlayer ep, CrystalElementProxy color, int amt);

}
