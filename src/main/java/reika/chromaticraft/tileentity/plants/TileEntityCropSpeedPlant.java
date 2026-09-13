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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.ComplexAOE;
import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.block.worldgen26.BlockDecoFlower;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.libraries.registry.ReikaCropHelper;
import reika.dragonapi.modregistry.ModCropList;

/** V33a Crop Speed Plant: hydrates farmland and forces nearby crop growth ticks. */
public final class TileEntityCropSpeedPlant extends TileEntityMagicPlant implements ComplexAOE {

	private static final double[][] GROWTH_DISTRIBUTION = {
			{0, 0, 4, 0, 0},
			{0, 1, 8, 1, 0},
			{4, 8, 0, 8, 4},
			{0, 1, 8, 1, 0},
			{0, 0, 4, 0, 0}
	};
	private static final double[][] HYDRATE_DISTRIBUTION = {
			{0, 1, 4, 1, 0},
			{1, 2, 6, 2, 1},
			{4, 6, 4, 6, 4},
			{1, 2, 6, 2, 1},
			{0, 1, 4, 1, 0}
	};
	private static final WeightedRandom<BlockPos> GROWTH_RANDOM =
			WeightedRandom.fromArray(GROWTH_DISTRIBUTION);
	private static final WeightedRandom<BlockPos> HYDRATE_RANDOM =
			WeightedRandom.fromArray(HYDRATE_DISTRIBUTION);

	public TileEntityCropSpeedPlant(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CROP_SPEED_PLANT.get(), pos, state);
	}

	@Override public Direction getGrowthDirection() { return Direction.UP; }

	@Override public ChromaTiles getTile() { return ChromaTiles.CROPSPEED; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel server)) return;
		if (world.getRandom().nextInt(4) == 0) this.hydrateFarmland(server, pos);
		double operations = 0.5D + this.getAccelerationPlants() / 2D;
		while (operations >= 1) {
			this.growCrop(server, pos);
			operations--;
		}
		if (world.getRandom().nextDouble() < operations) this.growCrop(server, pos);
	}

	private void hydrateFarmland(ServerLevel world, BlockPos pos) {
		BlockPos relative = HYDRATE_RANDOM.getRandomEntry();
		if (relative == null) return;
		BlockPos target = pos.offset(relative.getX(), -1, relative.getZ());
		if (!world.hasChunkAt(target)) return;
		BlockState state = world.getBlockState(target);
		if (state.is(Blocks.FARMLAND) && state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE)
			world.setBlock(target, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), 2);
	}

	private void growCrop(ServerLevel world, BlockPos pos) {
		BlockPos relative = GROWTH_RANDOM.getRandomEntry();
		if (relative == null) return;
		BlockPos target = pos.offset(relative);
		if (!world.hasChunkAt(target)) return;
		if (world.getBlockEntity(target) instanceof TileEntityFunctionRelay relay) {
			Coordinate relayed = relay.getRandomCoordinate();
			if (relayed == null) return;
			target = relayed.asBlockPos();
			if (!world.hasChunkAt(target)) return;
		}
		BlockState state = world.getBlockState(target);
		Block block = state.getBlock();
		boolean growable = block instanceof SaplingBlock || block instanceof BlockDecoFlower
				|| block instanceof SugarCaneBlock || block instanceof CactusBlock
				|| block instanceof VineBlock || ReikaCropHelper.getCrop(block) != null
				|| ModCropList.getModCrop(world, target, state) != null;
		if (growable) state.randomTick(world, target, world.getRandom());
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override
	public boolean isPlantable(Level world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.below());
		return below.is(Blocks.FARMLAND) || below.is(ChromaBlocks.PLANT_ACCELERATOR.get())
				|| below.is(ChromaBlocks.CLIFF_FARMLAND.get());
	}

	@Override
	public Collection<Coordinate> getPossibleRelativePositions() {
		return GROWTH_RANDOM.getValues().stream().map(Coordinate::new).toList();
	}

	@Override
	public double getNormalizedWeight(Coordinate coordinate) {
		return GROWTH_RANDOM.getWeight(coordinate.asBlockPos()) / GROWTH_RANDOM.getMaxWeight();
	}
}
