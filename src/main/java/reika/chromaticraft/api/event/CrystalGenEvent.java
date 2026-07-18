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

import reika.chromaticraft.api.CrystalElementAccessor;
import reika.chromaticraft.api.crystalelementaccessor.CrystalElementProxy;
import reika.dragonapi.instantiable.event.WorldGenEvent;

/** Fired when a crystal is generated. */
public class CrystalGenEvent extends WorldGenEvent {

	/** Crystal color */
	public final CrystalElementProxy color;

	public CrystalGenEvent(World world, int x, int y, int z, Random random, int meta) {
		super(world, x, y, z, random);
		color = CrystalElementAccessor.getByIndex(meta%16);
	}

}
