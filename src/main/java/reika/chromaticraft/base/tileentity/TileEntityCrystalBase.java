/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.CrystalNetworkLogger.FlowFail;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.network.CrystalFlow;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.network.CrystalPath;

/**
 * Base for every crystal-network BlockEntity — registers itself with the {@link CrystalNetworker} on
 * first tick and carries the network UUID + bottleneck-display state. Re-based onto DragonAPI's 26.2
 * {@link TileEntityChromaticBase} (tick hooks {@code (Level, BlockPos)}, NBT via
 * {@code saveAdditional(ValueOutput)} / {@code loadAdditional(ValueInput)}).
 */
public abstract class TileEntityCrystalBase extends TileEntityChromaticBase implements CrystalNetworkTile {

	public static final double DEFAULT_BEAM_RADIUS = 0.35;

	private UUID uniqueID = CrystalNetworker.instance.getNewUniqueID();

	private int bottleNeckDisplayTick = 0;

	private boolean networkCached;

	protected TileEntityCrystalBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (bottleNeckDisplayTick > 0) {
			if (world.isClientSide())
				this.doBottleneckDisplay();
			bottleNeckDisplayTick--;
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		this.cachePosition();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (this.getLevel() != null && !this.getLevel().isClientSide())
			this.cachePosition();
	}

	@Override
	public void onChunkUnloaded() {
		if (networkCached && this.getLevel() != null && !this.getLevel().isClientSide()) {
			CrystalNetworker.instance.unloadTile(this);
			networkCached = false;
		}
		super.onChunkUnloaded();
	}

	@Override
	public void setRemoved() {
		if (this.getLevel() != null && !this.getLevel().isClientSide())
			this.removeFromCache();
		super.setRemoved();
	}
	@Override
	public final void cachePosition() {
		if (!networkCached) {
			CrystalNetworker.instance.addTile(this);
			networkCached = true;
		}
	}

	@Override
	public final void removeFromCache() {
		if (networkCached) {
			CrystalNetworker.instance.removeTile(this);
			networkCached = false;
		}
	}

	@Override
	public final double getDistanceSqTo(double x, double y, double z) {
		double dx = x - this.getX();
		double dy = y - this.getY();
		double dz = z - this.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	public final Level getWorld() {
		return this.getLevel();
	}

	@Override
	public final int getX() {
		return this.getBlockPos().getX();
	}

	@Override
	public final int getY() {
		return this.getBlockPos().getY();
	}

	@Override
	public final int getZ() {
		return this.getBlockPos().getZ();
	}

	@Override
	public int getUpdatePacketRadius() {
		return 512;
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);
		NBT.putString("netuid", uniqueID.toString());
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);
		String s = NBT.getStringOr("netuid", "");
		uniqueID = s.isEmpty() ? CrystalNetworker.instance.getNewUniqueID() : UUID.fromString(s);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putInt("bottleneck", bottleNeckDisplayTick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		bottleNeckDisplayTick = NBT.getIntOr("bottleneck", 0);
	}

	@Override
	public final UUID getUniqueID() {
		return uniqueID;
	}

	@Override
	public final UUID getPlacerUUID() {
		Player ep = this.getPlacer();
		return ep != null ? ep.getUUID() : null;
	}

	public double getIncomingBeamRadius() {
		return DEFAULT_BEAM_RADIUS;
	}

	public double getOutgoingBeamRadius() {
		return DEFAULT_BEAM_RADIUS;
	}

	public void onPathCompleted(CrystalFlow p) {

	}

	public void onPathBroken(CrystalFlow p, FlowFail f) {

	}

	public void onPathConnected(CrystalPath p) {

	}

	public boolean canConductInterdimensionally() {
		return false;
	}

	@Override
	public final void triggerBottleneckDisplay(int duration) {
		bottleNeckDisplayTick = duration;
		this.syncAllData(false);
	}

	public final boolean isDoingBottleneckDisplay() {
		return bottleNeckDisplayTick > 0;
	}

	protected void doBottleneckDisplay() {

	}
}
