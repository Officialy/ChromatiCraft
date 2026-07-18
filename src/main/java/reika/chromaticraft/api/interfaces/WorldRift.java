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

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/** The world rift implements this; this is a hook for you to make your systems interact with the rift accordingly. For a sample implementation,
 * look to RotaryCraft shaft power distribution or ElectriCraft wire network pathfinding logic. For all directional functions, the return value
 * will be the appropriate object on that side of the world rift's other "end". So calling getBlockIDFrom(Direction.EAST) on a rift,
 * no matter its location, linked to another at 300, 50, 100 in the Nether returns the block at 301, 50, 100 in the Nether, the block east of the other rift.
 *
 *  Rifts are two-directional. */
public interface WorldRift {

	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	public Block getBlockIDFrom(Direction dir);
	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	public int getBlockMetadataFrom(Direction dir);
	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	public BlockEntity getTileEntityFrom(Direction dir);

	/** Returns the location of the other rift. */
	public WorldLocation getLinkTarget();

}
