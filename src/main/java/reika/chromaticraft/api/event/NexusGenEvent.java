/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.event;

import java.util.Random;

import net.minecraft.world.World;

import reika.dragonapi.instantiable.event.WorldGenEvent;

/** Fired when a Warp Nexus is generated. */
public class NexusGenEvent extends WorldGenEvent {

	public NexusGenEvent(World world, int x, int y, int z, Random random) {
		super(world, x, y, z, random);
	}

}
