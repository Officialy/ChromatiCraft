/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.networking;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.VariableTexture;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.magic.network.PylonLinkNetwork;
import reika.chromaticraft.magic.network.PylonLinkNetwork.PylonNode;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;
import reika.dragonapi.interfaces.blockentity.LocationCached;

/** The owner-bound base tile that joins same-colour pylons into a persistent donation web. */
public class TileEntityPylonLink extends TileEntityChromaticBase
		implements LocationCached, VariableTexture, ChunkLoadingTile {

	private PylonNode connection;

	public TileEntityPylonLink(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PYLON_LINK.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PYLONLINK;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide())
			this.link();
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!world.isClientSide() && connection == null && this.getTicksExisted() % 40 == 35)
			this.link();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (connection != null)
			this.doFX(world, pos);
	}

	public void link() {
		if (this.getPlacerID() == null || this.getLevel() == null || this.getLevel().isClientSide())
			return;
		TileEntityCrystalPylon pylon = this.getPylon();
		if (pylon != null) {
			connection = PylonLinkNetwork.instance.addLocation(this, pylon);
			ChunkManager.instance.loadChunks(this);
			this.syncAllData(true);
		}
	}

	public Collection<WorldLocation> getLinkedPylons() {
		if (this.getLevel() == null || this.getPlacerID() == null || connection == null)
			return List.of();
		return PylonLinkNetwork.instance.getLinkedPylons(this.getLevel(), this.getPlacerID(), connection.color());
	}

	public TileEntityCrystalPylon getPylon() {
		Level world = this.getLevel();
		if (world == null)
			return null;
		for (int distance = 1; distance < 9; distance++) {
			BlockPos between = this.getBlockPos().above(distance);
			if (!world.getBlockState(between).isAir())
				return null;
		}
		BlockEntity tile = world.getBlockEntity(this.getBlockPos().above(9));
		return tile instanceof TileEntityCrystalPylon pylon ? pylon : null;
	}

	@Override
	public void breakBlock() {
		if (this.getLevel() != null && connection != null)
			PylonLinkNetwork.instance.removeLocation(this.getLevel(), connection);
		connection = null;
		ChunkManager.instance.unloadChunks(this);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		connection = tag.contains("link") ? PylonNode.fromTag(tag.getCompoundOrEmpty("link")) : null;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		if (connection != null)
			tag.put("link", connection.toTag());
	}

	public UUID getUUID() {
		return this.getPlacerID();
	}

	@Override
	public int getIconState(int side) {
		return side == 1 && connection != null ? 1 : 0;
	}

	@Override
	public Collection<ChunkPos> getChunksToLoad() {
		return ChunkManager.getChunkSquare(this.getBlockPos().getX(), this.getBlockPos().getZ(), 1);
	}

	private void doFX(Level world, BlockPos pos) {
		double time = this.getTicksExisted() * 0.075;
		for (int i = 0; i < 3; i++) {
			double angle = time + i * Math.PI * 2 / 3;
			world.addParticle(ParticleTypes.END_ROD,
					pos.getX() + 0.5 + 0.32 * Math.cos(angle), pos.getY() + 1.05,
					pos.getZ() + 0.5 + 0.32 * Math.sin(angle), 0, 0.015, 0);
		}
	}
}
