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

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.ComplexAOE;
import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

/** V33a Reversion Lotus: restores altered biome cells to the chunk generator's natural biome. */
public final class TileEntityBiomeReverter extends TileEntityMagicPlant implements ComplexAOE {

	private static final double[][] RANDOM_DISTRIBUTION = {
			{0, 0, 0, 1, 2, 1, 0, 0, 0},
			{0, 0, 1, 2, 5, 2, 1, 0, 0},
			{0, 1, 3, 5, 7, 5, 3, 1, 0},
			{1, 2, 5, 8, 10, 8, 5, 2, 1},
			{2, 5, 7, 10, 10, 10, 7, 5, 2},
			{1, 2, 5, 8, 10, 8, 5, 2, 1},
			{0, 1, 3, 5, 7, 5, 3, 1, 0},
			{0, 0, 1, 2, 5, 2, 1, 0, 0},
			{0, 0, 0, 1, 2, 1, 0, 0, 0}
	};
	private static final WeightedRandom<BlockPos> COORDINATE_RANDOM =
			WeightedRandom.fromArray(RANDOM_DISTRIBUTION);

	private final StepTimer timer = new StepTimer(40);

	public TileEntityBiomeReverter(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.BIOME_REVERTER.get(), pos, state);
	}

	@Override public Direction getGrowthDirection() { return Direction.UP; }

	@Override public ChromaTiles getTile() { return ChromaTiles.REVERTER; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel server)) return;
		int acceleration = 1 + 2 * this.getAccelerationPlants();
		timer.update(acceleration);
		if (timer.checkCap()) this.revertBiome(server, pos, acceleration > 5);
	}

	public static boolean stopConversion(Level world, int x, int z) {
		return false;
	}

	private void revertBiome(ServerLevel world, BlockPos pos, boolean large) {
		BlockPos relative = COORDINATE_RANDOM.getRandomEntry();
		if (relative == null) return;
		if (large && world.getRandom().nextInt(4) == 0)
			relative = new BlockPos(relative.getX() * 2, 0, relative.getZ() * 2);
		BlockPos target = pos.offset(relative);
		if (!world.hasChunkAt(target)) return;
		if (world.getBlockEntity(target) instanceof TileEntityFunctionRelay relay) {
			Coordinate relayed = relay.getRandomCoordinate();
			if (relayed == null) return;
			target = relayed.asBlockPos();
			if (!world.hasChunkAt(target)) return;
		}
		Holder<Biome> natural = world.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
				QuartPos.fromBlock(target.getX()), QuartPos.fromBlock(target.getY()),
				QuartPos.fromBlock(target.getZ()), world.getChunkSource().randomState().sampler());
		if (world.getBiome(target).equals(natural)) return;
		FillBiomeCommand.fill(world,
				new BlockPos(target.getX(), world.getMinY(), target.getZ()),
				new BlockPos(target.getX(), world.getMaxY(), target.getZ()), natural);
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override
	public boolean isPlantable(Level world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.below());
		return below.is(net.minecraft.tags.BlockTags.SUPPORTS_VEGETATION)
				|| below.is(ChromaBlocks.PLANT_ACCELERATOR.get());
	}

	@Override
	public Collection<Coordinate> getPossibleRelativePositions() {
		return COORDINATE_RANDOM.getValues().stream().map(Coordinate::new).toList();
	}

	@Override
	public double getNormalizedWeight(Coordinate coordinate) {
		return COORDINATE_RANDOM.getWeight(coordinate.asBlockPos()) / COORDINATE_RANDOM.getMaxWeight();
	}
}
