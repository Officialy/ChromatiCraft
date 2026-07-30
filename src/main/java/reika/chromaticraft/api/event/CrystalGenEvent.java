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

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.registry.CrystalElement;

/** Fired when a crystal is generated. */
public class CrystalGenEvent extends Event {

	/** Crystal color */
	public final CrystalElementProxy color;
	public final WorldGenLevel world;
	public final BlockPos position;
	public final RandomSource random;
	/** Source-compatible coordinate mirrors for consumers that previously read WorldGenEvent fields. */
	public final int x;
	public final int y;
	public final int z;

	public CrystalGenEvent(WorldGenLevel world, BlockPos pos, RandomSource random, int colorIndex) {
		this.world = world;
		this.position = pos.immutable();
		this.random = random;
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
		color = CrystalElement.elements[Math.floorMod(colorIndex, CrystalElement.elements.length)];
	}

}
