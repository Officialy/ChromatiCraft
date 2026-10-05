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

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.neoforged.bus.api.Event;

/** Fired when a Warp Nexus is generated. */
public class NexusGenEvent extends Event {
	public final WorldGenLevel world;
	public final BlockPos position;
	public final RandomSource random;
	public final int x, y, z;

	public NexusGenEvent(WorldGenLevel world, BlockPos pos, RandomSource random) {
		this.world = world;
		position = pos.immutable();
		this.random = random;
		x = pos.getX(); y = pos.getY(); z = pos.getZ();
	}

	public NexusGenEvent(WorldGenLevel world, int x, int y, int z, RandomSource random) {
		this(world, new BlockPos(x, y, z), random);
	}

}
