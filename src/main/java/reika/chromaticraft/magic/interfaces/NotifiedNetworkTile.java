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


public interface NotifiedNetworkTile extends CrystalNetworkTile {

	public void onPathConnected(CrystalPath p);

	public void onTileNetworkTopologyChange(CrystalNetworkTile te, boolean remove);

}
