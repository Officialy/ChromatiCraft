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

/** The world rift implements this hook for systems that interact with a linked pair. For examples,
 * look to RotaryCraft shaft power distribution or ElectriCraft wire network pathfinding logic.
 * Block and complete-state reads use the opposite face of the far rift; tile-entity reads use the requested face.
 * Thus EAST reads a block/state west of the far rift, but a tile entity east of it, matching the V33a implementation.
 *
 *  Rifts are two-directional.
 *
 *  <p>26.3: the methods are declared on DragonAPI's {@link reika.dragonapi.interfaces.blockentity.WorldRift}, which
 *  RotaryCraft and ElectriCraft (ChromatiCraft's own dependencies) test for; this name stays for API users. */
public interface WorldRift extends reika.dragonapi.interfaces.blockentity.WorldRift {

}
