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

import net.minecraft.world.level.Level;
import reika.chromaticraft.registry.CrystalElement;

import java.util.UUID;

public interface CrystalNetworkTile {

	boolean isConductingElement(CrystalElement e);

	void cachePosition();

	void removeFromCache();

	double getDistanceSqTo(double x, double y, double z);

	Level getWorld();

	int getX();

	int getY();

	int getZ();

	/** Max per-tick flow. Called on connection for repeaters, called every tick for sources, to let them throttle as they drain. */
    int maxThroughput();

	boolean canConduct();

	UUID getUniqueID();

	UUID getPlacerUUID();

	void triggerBottleneckDisplay(int duration);

	/** TileEntity isInvalid, ie has it been removed from the world */
    boolean isRemoved();

	//public ResearchLevel getResearchTier();

	//public boolean canConductInterdimensionally();

}
