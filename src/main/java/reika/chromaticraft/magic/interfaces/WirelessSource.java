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

import reika.chromaticraft.base.tileentity.TileEntityWirelessPowered;
import reika.chromaticraft.registry.CrystalElement;


public interface WirelessSource extends CrystalReceiver, LumenTile {

	boolean canTransmitTo(TileEntityWirelessPowered te);

	int request(CrystalElement e, int amt, int x, int y, int z);

}
