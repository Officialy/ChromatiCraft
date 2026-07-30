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
	public int receiveElement(CrystalSource src, CrystalElement e, int amt);

	public void onPathBroken(CrystalFlow p, FlowFail f);

	public int getReceiveRange();

	public void onPathCompleted(CrystalFlow p);

	public boolean canReceiveFrom(CrystalTransmitter r);

	public boolean needsLineOfSightFromTransmitter(CrystalTransmitter r);

	public boolean canBeSuppliedBy(CrystalSource te, CrystalElement e);

	//public void markSource(WorldLocation loc);

}
