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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.ComplexAOE;
import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

/** V33a Scissorweed: shears nearby matching foliage and can redirect attempts through relays. */
public final class TileEntityHarvesterPlant extends TileEntityMagicPlant implements ComplexAOE {

	private static final double[][] RANDOM_DISTRIBUTION = {
			{3, 2, 1, 1, 2, 3, 2, 1, 1, 2, 3},
			{1, 6, 4, 3, 4, 5, 4, 3, 4, 6, 2},
			{1, 4, 7, 5, 4, 6, 4, 5, 7, 4, 1},
			{1, 3, 5, 8, 6, 7, 6, 8, 5, 3, 1},
			{2, 4, 4, 6, 9, 0, 9, 6, 4, 4, 2},
			{3, 5, 6, 7, 0, 0, 0, 7, 6, 5, 3},
			{2, 4, 4, 6, 9, 0, 9, 6, 4, 4, 2},
			{1, 3, 5, 8, 6, 7, 6, 8, 5, 3, 1},
			{1, 4, 7, 5, 4, 6, 4, 5, 7, 4, 1},
			{2, 6, 4, 3, 3, 5, 3, 3, 4, 6, 2},
			{3, 2, 1, 1, 2, 3, 2, 1, 1, 2, 3}
	};
	private static final double[] HEIGHT_DISTRIBUTION = {10, 8, 5, 2};
	private static final WeightedRandom<BlockPos> COORDINATE_RANDOM = WeightedRandom.fromArray(RANDOM_DISTRIBUTION);
	private static final WeightedRandom<Integer> VERTICAL_RANDOM = new WeightedRandom<>();
	private static final Identifier BOTANIA_SPECIAL_FLOWER =
			Identifier.fromNamespaceAndPath("botania", "special_flower");

	private final Set<BlockKey> flowerCache = new HashSet<>();
	private final StepTimer cacheTimer = new StepTimer(20);

	static {
		for (int offset = 0; offset < HEIGHT_DISTRIBUTION.length; offset++)
			VERTICAL_RANDOM.addEntry(-offset, HEIGHT_DISTRIBUTION[offset]);
	}

	public TileEntityHarvesterPlant(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.HARVEST_PLANT.get(), pos, state);
	}

	@Override public Direction getGrowthDirection() { return Direction.UP; }

	@Override public ChromaTiles getTile() { return ChromaTiles.HARVESTPLANT; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel server)) return;
		cacheTimer.update();
		if (cacheTimer.checkCap()) this.loadCache(world, pos);
		if (!this.isActive(world, pos)) return;
		for (int attempt = 0; attempt < 1 + this.getAccelerationPlants(); attempt++) {
			BlockPos relative = COORDINATE_RANDOM.getRandomEntry();
			Integer vertical = VERTICAL_RANDOM.getRandomEntry();
			if (relative == null || vertical == null) continue;
			BlockPos target = pos.offset(relative.getX(), vertical, relative.getZ());
			if (!world.hasChunkAt(target)) continue;
			if (world.getBlockEntity(target) instanceof TileEntityFunctionRelay relay) {
				Coordinate relayed = relay.getRandomCoordinate();
				if (relayed == null) continue;
				target = relayed.asBlockPos();
				if (!world.hasChunkAt(target)) continue;
			}
			if (target.distManhattan(pos) > 1 && this.canHarvest(world, target))
				this.harvest(server, target);
		}
	}

	private boolean isActive(Level world, BlockPos pos) {
		return world.canSeeSky(pos.above()) && world.isBrightOutside()
				&& !world.hasNeighborSignal(pos.below());
	}

	private void harvest(ServerLevel world, BlockPos target) {
		BlockState state = world.getBlockState(target);
		List<ItemStack> drops = Block.getDrops(state, world, target, world.getBlockEntity(target),
				this.getPlacer(), new ItemStack(Items.SHEARS));
		for (ItemStack drop : drops) Block.popResource(world, target, drop);
		ReikaSoundHelper.playBreakSound(world, target, Blocks.GRASS_BLOCK);
		world.levelEvent(2001, target, Block.getId(state));
		world.removeBlock(target, false);
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		this.loadCache(world, pos);
	}

	private void loadCache(Level world, BlockPos pos) {
		flowerCache.clear();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos adjacent = pos.relative(direction);
			BlockState state = world.getBlockState(adjacent);
			if (isFoliage(state)) flowerCache.add(new BlockKey(state));
		}
	}

	private static boolean isFoliage(BlockState state) {
		return state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
				|| state.getBlock() instanceof VegetationBlock;
	}

	private boolean canHarvest(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (BOTANIA_SPECIAL_FLOWER.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
		if (!flowerCache.contains(new BlockKey(state))) return false;
		boolean hasMatchingNeighbour = false;
		for (Direction direction : Direction.values()) {
			BlockState adjacent = world.getBlockState(pos.relative(direction));
			if (adjacent == state) hasMatchingNeighbour = true;
			if (adjacent.is(ChromaBlocks.HARVEST_PLANT.get())) return false;
		}
		return hasMatchingNeighbour;
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override public boolean isPlantable(Level world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.below());
		return below.is(BlockTags.SUPPORTS_VEGETATION) || below.is(BlockTags.LEAVES)
				|| below.is(ChromaBlocks.PLANT_ACCELERATOR.get());
	}

	@Override public Collection<Coordinate> getPossibleRelativePositions() {
		return COORDINATE_RANDOM.getValues().stream().map(Coordinate::new).toList();
	}

	@Override public double getNormalizedWeight(Coordinate coordinate) {
		return COORDINATE_RANDOM.getWeight(coordinate.asBlockPos()) / COORDINATE_RANDOM.getMaxWeight();
	}
}
