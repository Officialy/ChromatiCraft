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


public interface CrystalFuse extends CrystalNetworkTile {

	/** Non-fuse tiles have a failure weight of 1. */
	public float getFailureWeight(CrystalElement e);

	public void overload(CrystalElement e);

}
