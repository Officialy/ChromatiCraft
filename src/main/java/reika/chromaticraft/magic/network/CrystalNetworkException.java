/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.dragonapi.exception.DragonAPIException;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;


public abstract class CrystalNetworkException extends DragonAPIException {

	protected CrystalNetworkException() {

	}

	public static class InvalidLocationException extends CrystalNetworkException {

		public InvalidLocationException(CrystalNetworkTile te, WorldLocation loc, WorldLocation correct) {
			message.append("Network tile #"+te.getUniqueID()+" moved to invalid location "+loc+"! It is supposed to be at "+correct+"!\n");
			message.append("Moving pylons with TileEntity moving devices has a VERY high risk of world corruption, ");
			message.append("and the game has been terminated to prevent damage to your world save.");
			this.crash();
		}
	}

}
