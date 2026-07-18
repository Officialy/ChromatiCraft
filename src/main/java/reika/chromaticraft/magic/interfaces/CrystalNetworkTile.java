/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.interfaces;

import java.util.UUID;

import net.minecraft.world.World;

import reika.chromaticraft.registry.CrystalElement;

public interface CrystalNetworkTile {

	public boolean isConductingElement(CrystalElement e);

	public void cachePosition();

	public void removeFromCache();

	public double getDistanceSqTo(double x, double y, double z);

	public World getWorld();

	public int getX();

	public int getY();

	public int getZ();

	/** Max per-tick flow. Called on connection for repeaters, called every tick for sources, to let them throttle as they drain. */
	public int maxThroughput();

	public boolean canConduct();

	public UUID getUniqueID();

	public UUID getPlacerUUID();

	public void triggerBottleneckDisplay(int duration);

	/** TileEntity isInvalid, ie has it been removed from the world */
	public boolean isRemoved();

	//public ResearchLevel getResearchTier();

	//public boolean canConductInterdimensionally();

}
