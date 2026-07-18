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

import reika.chromaticraft.api.crystalelementaccessor.CrystalElementProxy;
import reika.dragonapi.instantiable.event.WorldGenEvent;

/** Fired when a pylon is successfully generated. */
public class PylonGenerationEvent extends WorldGenEvent {

	/** Whether or not the structure is damaged and thus the pylon is inactive and must be repaired. */
	public final boolean isBroken;
	/** Pylon color */
	public final CrystalElementProxy color;

	public PylonGenerationEvent(World world, int x, int y, int z, Random r, boolean br, CrystalElementProxy e) {
		super(world, x, y, z, r);
		isBroken = br;
		color = e;
	}

}
