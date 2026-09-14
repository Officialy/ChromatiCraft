/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.plants;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.blockentity.LocationCached;

/** V33a Heat Lily: melts nearby ice and snow and suppresses freezing within seven blocks. */
public final class TileEntityHeatLily extends TileEntityMagicPlant implements LocationCached {
	private static final Set<WorldLocation> CACHE = ConcurrentHashMap.newKeySet();
	private final StepTimer timer = new StepTimer(40);

	public TileEntityHeatLily(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.HEAT_LILY.get(), pos, state);
	}

	@Override public Direction getGrowthDirection() { return Direction.UP; }
	@Override public ChromaTiles getTile() { return ChromaTiles.HEATLILY; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) return;
		timer.update();
		if (timer.checkCap()) this.meltIce(world, pos);
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		CACHE.add(new WorldLocation(world, pos));
	}

	@Override
	public void breakBlock() {
		if (level != null) CACHE.remove(new WorldLocation(level, worldPosition));
	}

	@Override
	public void setRemoved() {
		this.breakBlock();
		super.setRemoved();
	}

	public static boolean stopFreeze(Level world, BlockPos pos) {
		for (WorldLocation location : CACHE) {
			if (!location.getDimension().equals(world.dimension())) continue;
			BlockPos lily = location.pos;
			if (Math.abs(pos.getX() - lily.getX()) + Math.abs(pos.getY() - lily.getY())
					+ Math.abs(pos.getZ() - lily.getZ()) <= 7) return true;
		}
		return false;
	}

	private void meltIce(Level world, BlockPos pos) {
		BlockPos target = pos.offset(world.getRandom().nextInt(7) - 3, -1,
				world.getRandom().nextInt(7) - 3);
		if (!world.hasChunkAt(target)) return;
		BlockState state = world.getBlockState(target);
		if (state.is(Blocks.ICE)) world.setBlock(target, Blocks.WATER.defaultBlockState(), 3);
		else if (state.is(Blocks.SNOW)) world.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override
	public boolean isPlantable(Level world, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState state = world.getBlockState(below);
		return (world.getFluidState(below).is(FluidTags.SUPPORTS_LILY_PAD)
				|| state.is(BlockTags.SUPPORTS_LILY_PAD))
				&& world.getFluidState(pos).isEmpty();
	}
}
