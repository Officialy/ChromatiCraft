/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.interfaces;

public interface CrystalTransmitter extends CrystalNetworkTile, EnergyBeamRenderer {

	int getSendRange();

	boolean needsLineOfSightToReceiver(CrystalReceiver r);

	boolean canTransmitTo(CrystalReceiver r);

	/** Larger is higher priority. */
    int getPathPriority();
}
