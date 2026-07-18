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


/** Fired when a rainbow tree is generated. */
public class RainbowTreeEvent extends WorldGenEvent {

	public RainbowTreeEvent(World world, int x, int y, int z, Random r) {
		super(world, x, y, z, r);
	}

}
