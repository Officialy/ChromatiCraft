/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import net.minecraft.core.BlockPos;

import java.util.Set;


public class LOSData {

	public final boolean hasLineOfSight;
	public final boolean canRain;
	final Set<BlockPos> blocks;

	LOSData(boolean los, boolean rain, Set<BlockPos> ray) {
		hasLineOfSight = los;
		canRain = rain;
		blocks = ray;
	}

}
