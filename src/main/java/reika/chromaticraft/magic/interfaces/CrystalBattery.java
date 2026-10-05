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

public interface CrystalBattery extends CrystalReceiver, CrystalSource, ChargingPoint {

	int getMaxStorage(CrystalElement e);

	float getFillFraction(CrystalElement e);

}
