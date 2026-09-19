package reika.chromaticraft.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.registry.ChromaTieredPlants;

/**
 * The plant half of V33a's {@code TieredWorldGenerator}, which the port had never carried — only its
 * ore half existed, so no tiered plant has ever generated.
 *
 * <p>Upstream is one generator looping every plant per chunk: a one-in-{@code getGenerationChance}
 * roll, then {@code getGenerationCount} attempts, each picking a random column in the chunk and
 * handing it to that plant's own {@code generate} branch. Here each plant is its own configured
 * feature so the chance and count live in the placed feature as vanilla {@code rarity_filter} and
 * {@code count} modifiers, and this class owns only the column search — which is the part that has
 * no vanilla equivalent, because each plant looks for a different thing at a different depth.
 */
public final class TieredPlantFeature extends Feature<NoneFeatureConfiguration> {

	private final ChromaTieredPlants plant;
	private final Supplier<BlockState> state;

	public TieredPlantFeature(ChromaTieredPlants plant, Supplier<BlockState> state) {
		super(NoneFeatureConfiguration.CODEC);
		this.plant = plant;
		this.state = state;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		BlockPos origin = context.origin();
		BlockPos target = this.findSite(world, origin.getX(), origin.getZ(), context.random());
		if (target == null)
			return false;
		// Worldgen writes take the no-neighbour-update flag, as the other ChromatiCraft features do.
		world.setBlock(target, state.get(), 2);
		return true;
	}

	/** V33a {@code TieredPlants.generate}, one branch per plant. Returns null when nothing fits. */
	private BlockPos findSite(WorldGenLevel world, int x, int z, RandomSource random) {
		return switch (plant.siting()) {
			case SURFACE -> this.scanFromSurface(world, x, z, state -> state.is(Blocks.DIRT)
					|| state.is(Blocks.GRASS_BLOCK));
			case SAND -> this.scanFromSurface(world, x, z, state -> state.is(Blocks.SAND));
			case CAVE -> this.scanCave(world, x, z);
			case LEAVES -> this.scanLeaves(world, x, z);
			case WATER -> this.scanWater(world, x, z);
			case TREE_POD -> this.scanTreePod(world, x, z, random);
			case TREE_ROOT -> this.scanTreeRoot(world, x, z, random);
		};
	}

	/**
	 * V33a POD: choose any log height from the discovered trunk, then attach to the first soft
	 * neighbour in a shuffled six-direction traversal.
	 */
	private BlockPos scanTreePod(WorldGenLevel world, int x, int z, RandomSource random) {
		x = bitRound(x, 4) + 7 + random.nextInt(2);
		z = bitRound(z, 4) + 7 + random.nextInt(2);
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		BlockPos tree = this.findTreeNear(world, x, surface, z, 7);
		if (tree == null)
			return null;

		BlockPos low = tree;
		while (this.inWorld(world, low) && this.isLog(world, low))
			low = low.below();
		low = low.above();
		BlockPos high = tree;
		while (this.inWorld(world, high) && this.isLog(world, high))
			high = high.above();
		high = high.below();
		if (high.getY() < low.getY())
			return null;
		int y = low.getY() + random.nextInt(high.getY() - low.getY() + 1);
		BlockPos trunk = new BlockPos(tree.getX(), y, tree.getZ());
		for (Direction direction : shuffledDirections(random)) {
			BlockPos target = trunk.relative(direction);
			if (this.inWorld(world, target) && this.isSoft(world, target))
				return target;
		}
		return null;
	}

	/**
	 * V33a ROOT: snap the random column to the centre of its 16x16 lattice cell, reproduce
	 * {@code ReikaWorldHelper.findTreeNear}, walk down to the lowest log, then try all six adjacent
	 * blocks in random order. Centreing the search also keeps its radius-seven reads inside the
	 * currently generating chunk, avoiding modern far-chunk worldgen access.
	 */
	private BlockPos scanTreeRoot(WorldGenLevel world, int x, int z, RandomSource random) {
		x = bitRound(x, 4) + 7 + random.nextInt(2);
		z = bitRound(z, 4) + 7 + random.nextInt(2);
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		BlockPos tree = this.findTreeNear(world, x, surface, z, 7);
		if (tree == null)
			return null;

		BlockPos base = tree;
		while (this.inWorld(world, base) && this.isLog(world, base))
			base = base.below();
		base = base.above();
		for (Direction direction : shuffledDirections(random)) {
			BlockPos target = base.relative(direction);
			if (this.inWorld(world, target) && this.isSoft(world, target))
				return target;
		}
		return null;
	}

	/** Exact V33a sample order and three-log-column test from {@code findTreeNear}. */
	private BlockPos findTreeNear(WorldGenLevel world, int x, int y, int z, int radius) {
		int[] samples = radius > 2
				? new int[] {y - radius, y - radius / 2, y - 1, y, y + 1,
						y + radius / 2, y + radius}
				: new int[] {y - 2, y - 1, y, y + 1, y + 2};
		for (int sampleY : samples) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos pos = new BlockPos(x + dx, sampleY, z + dz);
					if (this.inWorld(world, pos.above()) && this.inWorld(world, pos.below())
							&& this.isLog(world, pos) && this.isLog(world, pos.above())
							&& this.isLog(world, pos.below()))
						return pos;
				}
			}
		}
		return null;
	}

	private boolean isLog(WorldGenLevel world, BlockPos pos) {
		return world.getBlockState(pos).is(BlockTags.LOGS);
	}

	/** V33a {@code Coordinate.softBlock}: air, fluid and other replaceable blocks are valid. */
	private boolean isSoft(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isAir() || state.canBeReplaced();
	}

	private static int bitRound(int value, int bits) {
		return value >> bits << bits;
	}

	private static List<Direction> shuffledDirections(RandomSource random) {
		List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
		for (int i = directions.size() - 1; i > 0; i--) {
			int swap = random.nextInt(i + 1);
			Direction old = directions.set(i, directions.get(swap));
			directions.set(swap, old);
		}
		return directions;
	}

	/**
	 * V33a FLOWER and DESERT: start one below the surface and sweep -8..+8 for the required ground
	 * with a free block directly above it.
	 *
	 * <p>V33a tests that block for {@code == Blocks.air} exactly. That is not portable as written:
	 * this runs after vegetation, where modern terrain is blanketed in tall grass and flowers, so a
	 * strict air test found a site in zero chunks out of 1,941 — Aura Bloom could never generate.
	 * Accepting a replaceable block as well is the same freedom vanilla's own flower placement takes,
	 * and it preserves the actual intent of the source test: nothing solid is standing there.
	 */
	private BlockPos scanFromSurface(WorldGenLevel world, int x, int z, java.util.function.Predicate<BlockState> ground) {
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
		for (int i = -8; i < 8; i++) {
			BlockPos pos = new BlockPos(x, surface + i, z);
			if (!this.inWorld(world, pos))
				continue;
			if (ground.test(world.getBlockState(pos)) && this.isFree(world, pos.above()))
				return pos.above();
		}
		return null;
	}

	/** Air, or vegetation the plant may stand in place of; see {@link #scanFromSurface}. */
	private boolean isFree(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
	}

	/**
	 * V33a CAVE: scanning upward, the first air block whose ceiling is stone or ore.
	 *
	 * <p>The source scans y 0..64, and those stay absolute for exactly the reason the tiered ore band
	 * does: only the overworld floor moved to -64, and that new space is deepslate, where a
	 * {@code Blocks.STONE} ceiling test can never match. Walking 64 blocks up from {@code getMinY()}
	 * put this whole scan in deepslate and was why Rock Flower generated in 1 chunk out of 1,764.
	 */
	private BlockPos scanCave(WorldGenLevel world, int x, int z) {
		for (int i = 0; i < 64; i++) {
			BlockPos pos = new BlockPos(x, i, z);
			if (!this.inWorld(world, pos.above()))
				continue;
			BlockState ceiling = world.getBlockState(pos.above());
			boolean supports = ceiling.is(Blocks.STONE) || ceiling.is(Tags.Blocks.ORES);
			if (supports && world.getBlockState(pos).isAir())
				return pos;
		}
		return null;
	}

	/**
	 * V33a BULB: from the dimension's average ground level upward, the first air block with leaves
	 * directly above it. {@code getAverageGroundLevel} has no modern equivalent, so the scan starts
	 * from the sea level the chunk generator actually reports.
	 */
	private BlockPos scanLeaves(WorldGenLevel world, int x, int z) {
		int start = world.getLevel().getSeaLevel();
		for (int i = 0; i < 32; i++) {
			BlockPos pos = new BlockPos(x, start + i, z);
			if (!this.inWorld(world, pos.above()))
				continue;
			if (this.isFree(world, pos) && world.getBlockState(pos.above()).is(BlockTags.LEAVES))
				return pos;
		}
		return null;
	}

	/**
	 * V33a LILY: still source water with air above it and open sky. The source checks metadata 0,
	 * which is a full water source block rather than any flowing level.
	 */
	private BlockPos scanWater(WorldGenLevel world, int x, int z) {
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		for (int i = -8; i < 38; i++) {
			BlockPos pos = new BlockPos(x, surface + i, z);
			if (!this.inWorld(world, pos.above()))
				continue;
			boolean source = world.getBlockState(pos).getFluidState().isSourceOfType(Fluids.WATER);
			if (source && world.getBlockState(pos.above()).isAir()
					&& world.canSeeSky(pos.above()))
				return pos.above();
		}
		return null;
	}

	private boolean inWorld(WorldGenLevel world, BlockPos pos) {
		return pos.getY() > world.getMinY() && pos.getY() < world.getMaxY();
	}
}
