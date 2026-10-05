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

import reika.chromaticraft.auxiliary.CrystalNetworkLogger.FlowFail;
import reika.chromaticraft.magic.network.CrystalFlow;
import reika.chromaticraft.registry.CrystalElement;

public interface CrystalReceiver extends CrystalNetworkTile, EnergyBeamReceiver {

	/** Returns the amount successfully added. */
    int receiveElement(CrystalSource src, CrystalElement e, int amt);

	void onPathBroken(CrystalFlow p, FlowFail f);

	int getReceiveRange();

	void onPathCompleted(CrystalFlow p);

	boolean canReceiveFrom(CrystalTransmitter r);

	boolean needsLineOfSightFromTransmitter(CrystalTransmitter r);

	boolean canBeSuppliedBy(CrystalSource te, CrystalElement e);

	//public void markSource(WorldLocation loc);

}
