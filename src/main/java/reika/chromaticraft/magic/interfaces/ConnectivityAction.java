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

import reika.chromaticraft.magic.network.CrystalPath;


public interface ConnectivityAction extends LinkWatchingRepeater {

	public void notifySendingTo(CrystalPath p, CrystalReceiver r);
	public void notifyReceivingFrom(CrystalPath p, CrystalTransmitter t);

}
