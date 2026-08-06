package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.instantiable.data.ShuffledGrid;

/**
 * V33a {@code WarpNodeGenerator}: warp nodes floating high above the terrain, on their own grid.
 *
 * <p>The grid is much coarser than the pylon one — a 2048-chunk period with 55-chunk mean spacing and
 * up to 20 chunks of deviation, so roughly one candidate every 880 blocks with offsets up to 320m.
 * Whether a candidate becomes a node depends on the biome: an Ender Forest always succeeds, Rainbow
 * Forest four times in five, Luminous Cliffs two in three, and anywhere else only one in three.
 *
 * <p>Nodes sit in open sky well above the ground — at least 48 blocks over the highest block, and
 * never below y 128 — because the whole point is that you reach them by flying and enter them at
 * speed. The node itself is inert until opened; see {@code BlockWarpNode}.
 */
public final class WarpNodeFeature extends Feature<NoneFeatureConfiguration> {

	private static final int GRID_SIZE = 2048;
	private static final int GRID_DEVIATION = 20;
	private static final int GRID_SEPARATION = 55;

	private ShuffledGrid grid;
	private long gridSeed;
	private boolean gridReady;

	public WarpNodeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		if (!(world.getLevel() instanceof ServerLevel server))
			return false;
		if (!this.isGennableChunk(server, origin.getX() >> 4, origin.getZ() >> 4))
			return false;

		int x = origin.getX() + random.nextInt(16);
		int z = origin.getZ() + random.nextInt(16);
		Holder<Biome> biome = world.getBiome(new BlockPos(x, world.getSeaLevel(), z));
		if (random.nextDouble() >= this.getBiomeSuccessChance(biome))
			return false;

		int minY = this.getMinY(world, x, z);
		int maxY = this.getMaxY(biome);
		if (minY >= maxY)
			return false;
		int y = minY + random.nextInt(maxY - minY + 1);
		BlockPos pos = new BlockPos(x, y, z);
		if (!canGenerateAt(world, pos))
			return false;
		world.setBlock(pos, ChromaBlocks.WARP_NODE.get().defaultBlockState(), 2);
		// CHROMA-PORT: V33a posts NexusGenEvent here for API consumers; the event is not registered yet.
		return true;
	}

	private boolean isGennableChunk(ServerLevel world, int chunkX, int chunkZ) {
		long seed = -(world.dimension().hashCode() * 11L - world.getSeed() * 7L);
		if (!gridReady || gridSeed != seed) {
			// V33a: ShuffledGrid(2048, 20, 55), seeded from the dimension id and world seed.
			grid = new ShuffledGrid(GRID_SIZE, GRID_DEVIATION, GRID_SEPARATION);
			grid.calculate(new java.util.Random(seed));
			gridSeed = seed;
			gridReady = true;
		}
		return grid.isValid(chunkX, chunkZ);
	}

	/** V33a getBiomeSuccessChance. */
	private double getBiomeSuccessChance(Holder<Biome> biome) {
		if (biome.is(ChromaBiomes.ENDER_FOREST))
			return 1;
		if (biome.is(ChromaBiomes.RAINBOW_FOREST))
			return 0.8;
		if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
			return 0.667;
		return 0.333;
	}

	/** V33a getMinY: 48 above the highest block in the column. */
	private int getMinY(WorldGenLevel world, int x, int z) {
		return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 48;
	}

	/**
	 * V33a getMaxY: {@code min(255, 192 * max(1, rootHeight))}, floored at 128, and unrestricted in
	 * the cliff biomes. Modern biomes have no rootHeight, so the plain 192 case applies and the two
	 * Luminous Cliffs biomes keep their raised ceiling.
	 */
	private int getMaxY(Holder<Biome> biome) {
		if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
			return 255;
		return Math.max(128, 192);
	}

	/** V33a canGenerateAt: open air with a clear view of the sky. */
	public static boolean canGenerateAt(WorldGenLevel world, BlockPos pos) {
		return world.getBlockState(pos).isAir() && world.canSeeSky(pos);
	}
}
