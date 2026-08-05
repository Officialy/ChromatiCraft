package reika.chromaticraft.world;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
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
		};
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
