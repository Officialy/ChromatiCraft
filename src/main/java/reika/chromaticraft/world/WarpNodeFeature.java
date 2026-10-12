package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.common.NeoForge;
import reika.chromaticraft.api.event.NexusGenEvent;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.biome.ChromaBiomes;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V33a {@code WarpNodeGenerator}: warp nodes floating high above the terrain, on their own grid.
 *
 * <p>The grid is much coarser than the pylon one — a 2048-chunk period with 55-chunk mean spacing and
 * up to 20 chunks of deviation, so roughly one candidate every 880 blocks with offsets up to 320m.
 * Whether a candidate becomes a node depends on the biome: an Ender Forest always succeeds, Rainbow
 * Forest four times in five, Luminous Cliffs two in three, and anywhere else only one in three.
 *
 * <p>Nodes sit in open sky well above the ground — at least 48 blocks over the highest block, and
 * above the modern middle plateau in Luminous Cliffs — because the whole point is that you reach them by flying and enter them at
 * speed. The node itself is inert until opened; see {@code BlockWarpNode}.
 */
public final class WarpNodeFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<WarpNodeFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(WarpNodeFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<WarpNodeFeature> codec() {
        return CODEC;
    }

	private static final int GRID_SIZE = 2048;
	private static final int GRID_DEVIATION = 20;
	private static final int GRID_SEPARATION = 55;

	private static final ConcurrentHashMap<Long, BitSet> GRIDS = new ConcurrentHashMap<>();

	public WarpNodeFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		if (!(world.getLevel() instanceof ServerLevel server))
			return false;
		if (server.dimension() != Level.OVERWORLD || server.isFlat()) return false;
		if (!isSelectedChunk(server.getSeed(), origin.getX() >> 4, origin.getZ() >> 4))
			return false;

		int x = origin.getX() + random.nextInt(16);
		int z = origin.getZ() + random.nextInt(16);
		Holder<Biome> biome = world.getBiome(new BlockPos(x, world.getSeaLevel(), z));
		if (random.nextDouble() >= this.getBiomeSuccessChance(biome))
			return false;

		int minY = minimumHeight(world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1,
				biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES));
        int maxY = world.getMaxY();
        if (minY > maxY)
			return false;
		int y = minY + random.nextInt(maxY - minY + 1);
		BlockPos pos = new BlockPos(x, y, z);
		if (!canGenerateAt(world, pos))
			return false;
		world.setBlock(pos, ChromaBlocks.WARP_NODE.get().defaultBlockState(), 2);
		NeoForge.EVENT_BUS.post(new NexusGenEvent(world, pos, random));
		return true;
	}

	public static boolean isSelectedChunk(long worldSeed, int chunkX, int chunkZ) {
		BitSet grid = GRIDS.computeIfAbsent(worldSeed, WarpNodeFeature::createGrid);
		return grid.get(Math.floorMod(chunkX, GRID_SIZE) * GRID_SIZE + Math.floorMod(chunkZ, GRID_SIZE));
	}

	private static BitSet createGrid(long worldSeed) {
		// Overworld dimension id was zero. Preserve ShuffledGrid's two discarded draws and
		// exact x/z traversal; ResourceKey identity hashes are not persistent seed material.
		var random = new java.util.Random(worldSeed * 7L);
		random.nextBoolean(); random.nextBoolean();
		BitSet grid = new BitSet(GRID_SIZE * GRID_SIZE);
		for (int x = GRID_DEVIATION; x < GRID_SIZE - GRID_DEVIATION; x += GRID_SEPARATION)
			for (int z = GRID_DEVIATION; z < GRID_SIZE - GRID_DEVIATION; z += GRID_SEPARATION) {
				int px = x - GRID_DEVIATION + random.nextInt(GRID_DEVIATION * 2 + 1);
				int pz = z - GRID_DEVIATION + random.nextInt(GRID_DEVIATION * 2 + 1);
				grid.set(px * GRID_SIZE + pz);
			}
		return grid;
	}

	/** V33a getBiomeSuccessChance. */
	private double getBiomeSuccessChance(Holder<Biome> biome) {
		// CHROMA-PORT: Satisforestry Pink Forest bridge: chance 0.75, trueTop+40 floor,
		// y=255 ceiling. Retain those rules when its biome API is available on 26.3.
		if (biome.is(ChromaBiomes.ENDER_FOREST))
			return 1;
		if (biome.is(ChromaBiomes.RAINBOW_FOREST))
			return 0.8;
		if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
			return 0.667;
		return 0.333;
	}

	/** V33a getMinY: 48 above the highest block in the column. */
	static int minimumHeight(int topNonAir, boolean cliffs) {
		int min = topNonAir + 48;
		return cliffs ? Math.max(min, GlowingCliffsColumnShaper.MAX_MIDDLE_TOP_Y + 16) : min;
	}

	/** V33a canGenerateAt: open air with a clear view of the sky. */
	public static boolean canGenerateAt(WorldGenLevel world, BlockPos pos) {
		return world.getBlockState(pos).isAir() && world.canSeeSky(pos);
	}
}
