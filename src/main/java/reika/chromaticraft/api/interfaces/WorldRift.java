/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

/** The world rift implements this; this is a hook for you to make your systems interact with the rift accordingly. For a sample implementation,
 * look to RotaryCraft shaft power distribution or ElectriCraft wire network pathfinding logic. For all directional functions, the return value
 * will be the appropriate object on that side of the world rift's other "end". So calling getBlockIDFrom(Direction.EAST) on a rift,
 * no matter its location, linked to another at 300, 50, 100 in the Nether returns the block at 301, 50, 100 in the Nether, the block east of the other rift.
 *
 *  Rifts are two-directional.
 *
 *  <p>26.x: the methods are declared on DragonAPI's {@link reika.dragonapi.interfaces.blockentity.WorldRift}, which
 *  RotaryCraft and ElectriCraft (ChromatiCraft's own dependencies) test for; this name stays for API users. */
public interface WorldRift extends reika.dragonapi.interfaces.blockentity.WorldRift {

}
