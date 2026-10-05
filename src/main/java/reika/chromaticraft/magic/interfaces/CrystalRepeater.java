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

import reika.chromaticraft.registry.CrystalElement;



public interface CrystalRepeater extends CrystalReceiver, CrystalTransmitter {

	int getSignalDegradation(boolean point);

	int getThoughputBonus(boolean point);

	int getThoughputInsurance();

	int getSignalDepth(CrystalElement e);

	void setSignalDepth(CrystalElement e, int d);

	boolean checkConnectivity();

	//public void onTransfer(CrystalSource src, CrystalReceiver r, CrystalElement element, int amt);

}
