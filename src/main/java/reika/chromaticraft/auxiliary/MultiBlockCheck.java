/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.dragonapi.instantiable.data.maps.timermap.TimerCallback;


public class MultiBlockCheck implements TimerCallback {

	private final MultiBlockChromaTile tile;

	public MultiBlockCheck(MultiBlockChromaTile te) {
		tile = te;
	}

	@Override
	public void call() {
		tile.validateStructure();
	}

}
