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

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.CrystalElement;

public interface LumenTile {

	int getEnergy(CrystalElement e);

	ElementTagCompound getEnergy();

	int getMaxStorage(CrystalElement e);

	//public ElementTagCompound getRequiredEnergy();

	int getTicksExisted();

}
