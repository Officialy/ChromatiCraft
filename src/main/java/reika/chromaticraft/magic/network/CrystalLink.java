/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.LinkWatchingRepeater;
import reika.dragonapi.instantiable.data.immutable.WorldChunk;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

import java.util.HashSet;

public final class CrystalLink {

	public final WorldLocation loc1;
	public final WorldLocation loc2;
	final HashSet<WorldChunk> chunks = new HashSet();
	private final HashSet<BlockPos> locations = new HashSet<>();

	boolean hasLOS = false;
	boolean isRainable = false;
	public final double length;
	private boolean activeEndpoint1 = false;
	private boolean activeEndpoint2 = false;

	boolean needsCalculation = true;

	CrystalLink(WorldLocation l1, WorldLocation l2) {
		loc1 = l1;
		loc2 = l2;
		double dd = l1.getDistanceTo(l2);
		Level world = l1.getWorld();
		for (int i = 0; i < dd; i++) {
			int x = Mth.floor(l1.pos.getX()+i*(l2.pos.getX()-l1.pos.getX())/dd);
			int z = Mth.floor(l1.pos.getZ()+i*(l2.pos.getZ()-l1.pos.getZ())/dd);
			WorldChunk ch = new WorldChunk(world, new ChunkPos(x >> 4, z >> 4));
            chunks.add(ch);
		}
		activeEndpoint1 = l1.getBlockEntity() instanceof LinkWatchingRepeater;
		activeEndpoint2 = l2.getBlockEntity() instanceof LinkWatchingRepeater;
		length = l1.getDistanceTo(l2);
	}

	private void recalculateLOS() {
		if (!needsCalculation)
			return;
		needsCalculation = false;
		LOSData los = PylonFinder.lineOfSight(loc1, loc2);
		hasLOS = los.hasLineOfSight;
		locations.clear();
		locations.addAll(los.blocks);
		isRainable = los.canRain;
		if (activeEndpoint1 || activeEndpoint2)
			this.updateEndpoints();
		//ReikaJavaLibrary.pConsole("Recalculating LOS for "+this+" (#"+System.identityHashCode(this)+"): "+hasLOS);
	}

	private void updateEndpoints() {
		if (activeEndpoint1) {
			BlockEntity te1 = loc1.getBlockEntity();
			if (te1 instanceof LinkWatchingRepeater) {
				((LinkWatchingRepeater)te1).onLinkRecalculated(this);
			}
		}
		if (activeEndpoint2) {
			BlockEntity te2 = loc2.getBlockEntity();
			if (te2 instanceof LinkWatchingRepeater) {
				((LinkWatchingRepeater)te2).onLinkRecalculated(this);
			}
		}
	}

	public boolean isChunkInPath(WorldChunk wc) {
		return chunks.contains(wc);
	}

	public boolean containsBlock(BlockPos pos) {
		return locations.contains(pos);
	}

	@Override
	public int hashCode() {
		return loc1.hashCode()^loc2.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CrystalLink l) {
            return (l.loc1.equals(loc1) && l.loc2.equals(loc2)) || (l.loc1.equals(loc2) && l.loc2.equals(loc1)); //order irrelevant
		}
		return false;
	}

	@Override
	public String toString() {
		return "["+loc1+" > "+loc2+"]";
	}

	boolean hasLineOfSight() {
		if (needsCalculation)
			this.recalculateLOS();
		//ReikaJavaLibrary.pConsole("Returning LOS for "+this+" (#"+System.identityHashCode(this)+"): "+hasLOS);
		return hasLOS;
	}

	public boolean isRainable() {
		return isRainable;
	}

	public boolean hasLOS() {
		return hasLOS;
	}

	/** Returns loc2 if the tile is on neither end */
	public CrystalNetworkTile getOtherEnd(CrystalNetworkTile te) {
		return PylonFinder.getNetTileAt(loc1.equals(te.getWorld(), new BlockPos(te.getX(), te.getY(), te.getZ())) ? loc2 : loc1, true);
	}

}
