package reika.chromaticraft.world.luminous;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;

/**
 * Bounded 26.2 terrain pass for V33a's stacked Luminous Cliffs plateaus. The original column
 * shaper's three characteristic elevations are retained without replacing the chunk generator.
 */
public final class LuminousCliffsTerrainFeature extends Feature<NoneFeatureConfiguration> {

	private static final int EDGE_BLEND = 24;
	private static final int DISTANCE_INFINITY = 1 << 20;
	private static final ConcurrentHashMap<Long, NoiseSet> NOISE = new ConcurrentHashMap<>();

    public LuminousCliffsTerrainFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        NoiseSet noise = NOISE.computeIfAbsent(level.getSeed(), NoiseSet::new);
        int baseX = context.origin().getX();
        int baseZ = context.origin().getZ();
		boolean placed = false;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		boolean[][] cliffColumns = sampleCliffBiomes(level, baseX, baseZ);
		int[][] edgeDistances = distanceFromNonCliff(cliffColumns);

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
				if (!cliffColumns[dx + EDGE_BLEND][dz + EDGE_BLEND])
					continue;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				cursor.set(x, surface, z);
				double edge = edgeFactor(edgeDistances[dx + EDGE_BLEND][dz + EDGE_BLEND]);

                double land = noise.land.getValue(x, z);
                int shoreTop = 67 + (int)Math.round(noise.shore.getValue(x, z) * 5);
                if (land < -0.18) {
					int target = Mth.lerpInt((float)edge, surface, shoreTop);
					placed |= shapeGroundColumn(level, cursor, surface, target);
                    continue;
                }

                int middleTop = 106 + (int)Math.round(noise.middleTop.getValue(x, z) * 6);
				int blendedMiddle = Mth.lerpInt((float)edge, surface, middleTop);
				placed |= shapeGroundColumn(level, cursor, surface, blendedMiddle);

                double upperEdge = land + noise.upperEdge.getValue(x, z) * 0.18;
                if (upperEdge > 0.43) {
                    int upperBottom = 134 + (int)Math.round(noise.upperBottom.getValue(x, z) * 6);
                    int upperTop = 152 + (int)Math.round(noise.upperTop.getValue(x, z) * 8);
					double upperBlend = smoothStep(Mth.clamp((edge - 0.4) / 0.6, 0, 1));
					int thickness = (int)Math.round((upperTop - upperBottom + 1) * upperBlend);
					if (thickness >= 2)
						placed |= fillShelf(level, cursor, upperTop - thickness + 1, upperTop);
                }
            }
        }
        return placed;
    }

	private static boolean[][] sampleCliffBiomes(WorldGenLevel level, int baseX, int baseZ) {
		int size = 16 + EDGE_BLEND * 2;
		int minX = baseX - EDGE_BLEND;
		int minZ = baseZ - EDGE_BLEND;
		int minQuartX = QuartPos.fromBlock(minX);
		int minQuartZ = QuartPos.fromBlock(minZ);
		int maxQuartX = QuartPos.fromBlock(minX + size - 1);
		int maxQuartZ = QuartPos.fromBlock(minZ + size - 1);
		boolean[][] quartMask = new boolean[maxQuartX - minQuartX + 1][maxQuartZ - minQuartZ + 1];
		int quartY = QuartPos.fromBlock(level.getSeaLevel());
		for (int quartX = minQuartX; quartX <= maxQuartX; quartX++) {
			for (int quartZ = minQuartZ; quartZ <= maxQuartZ; quartZ++) {
				quartMask[quartX - minQuartX][quartZ - minQuartZ] = level
						.getUncachedNoiseBiome(quartX, quartY, quartZ)
						.is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS);
			}
		}
		boolean[][] result = new boolean[size][size];
		for (int x = 0; x < size; x++)
			for (int z = 0; z < size; z++)
				result[x][z] = quartMask[QuartPos.fromBlock(minX + x) - minQuartX]
						[QuartPos.fromBlock(minZ + z) - minQuartZ];
		return result;
	}

	private static int[][] distanceFromNonCliff(boolean[][] cliffColumns) {
		int width = cliffColumns.length;
		int height = cliffColumns[0].length;
		int[][] distance = new int[width][height];
		for (int x = 0; x < width; x++)
			for (int z = 0; z < height; z++)
				distance[x][z] = cliffColumns[x][z] ? DISTANCE_INFINITY : 0;

		// A 3/4 chamfer transform approximates Euclidean distance while visiting each sampled
		// column only twice. The previous radial search revisited roughly 600,000 cells per chunk.
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < height; z++) {
				if (x > 0) distance[x][z] = Math.min(distance[x][z], distance[x - 1][z] + 3);
				if (z > 0) distance[x][z] = Math.min(distance[x][z], distance[x][z - 1] + 3);
				if (x > 0 && z > 0)
					distance[x][z] = Math.min(distance[x][z], distance[x - 1][z - 1] + 4);
				if (x > 0 && z + 1 < height)
					distance[x][z] = Math.min(distance[x][z], distance[x - 1][z + 1] + 4);
			}
		}
		for (int x = width - 1; x >= 0; x--) {
			for (int z = height - 1; z >= 0; z--) {
				if (x + 1 < width)
					distance[x][z] = Math.min(distance[x][z], distance[x + 1][z] + 3);
				if (z + 1 < height)
					distance[x][z] = Math.min(distance[x][z], distance[x][z + 1] + 3);
				if (x + 1 < width && z + 1 < height)
					distance[x][z] = Math.min(distance[x][z], distance[x + 1][z + 1] + 4);
				if (x + 1 < width && z > 0)
					distance[x][z] = Math.min(distance[x][z], distance[x + 1][z - 1] + 4);
			}
		}
		return distance;
	}

	private static double edgeFactor(int chamferDistance) {
		if (chamferDistance >= DISTANCE_INFINITY)
			return 1;
		return smoothStep(chamferDistance / 3D / EDGE_BLEND);
	}

	private static double smoothStep(double value) {
		double clamped = Mth.clamp(value, 0, 1);
		return clamped * clamped * (3 - 2 * clamped);
	}

	private static boolean shapeGroundColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos,
			int surface, int target) {
		target = Mth.clamp(target, level.getMinY() + 4, level.getMaxY() - 1);
		boolean changed = false;
		if (target > surface) {
			for (int y = surface + 1; y <= target; y++) {
				pos.setY(y);
				changed |= level.setBlock(pos, cliffState(y, target), 2);
			}
		} else if (target < surface) {
			for (int y = surface; y > target; y--) {
				pos.setY(y);
				if (!level.getBlockState(pos).is(Blocks.BEDROCK))
					changed |= level.setBlock(pos, y <= level.getSeaLevel()
							? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
			}
		}
		pos.setY(target);
		return reskinSurface(level, pos, target) | changed;
	}

	private static BlockState cliffState(int y, int top) {
		return y == top ? ChromaBlocks.CLIFF_GRASS.get().defaultBlockState()
				: y >= top - 3 ? ChromaBlocks.CLIFF_DIRT.get().defaultBlockState()
				: ChromaBlocks.CLIFF_STONE.get().defaultBlockState();
	}

    private static boolean reskinSurface(WorldGenLevel level, BlockPos.MutableBlockPos pos, int top) {
        if (top <= level.getMinY())
            return false;
        boolean changed = false;
        for (int depth = 0; depth < 4; depth++) {
            pos.setY(top - depth);
            BlockState old = level.getBlockState(pos);
            if (old.isAir() || !old.getFluidState().isEmpty())
                continue;
            BlockState replacement = depth == 0 ? ChromaBlocks.CLIFF_GRASS.get().defaultBlockState()
                    : ChromaBlocks.CLIFF_DIRT.get().defaultBlockState();
            changed |= level.setBlock(pos, replacement, 2);
        }
        return changed;
    }

    private static boolean fillShelf(WorldGenLevel level, BlockPos.MutableBlockPos pos, int bottom, int top) {
        if (top >= level.getMaxY() || bottom >= top)
            return false;
        boolean changed = false;
        for (int y = bottom; y <= top; y++) {
            pos.setY(y);
            BlockState state = y == top ? ChromaBlocks.CLIFF_GRASS.get().defaultBlockState()
                    : y >= top - 3 ? ChromaBlocks.CLIFF_DIRT.get().defaultBlockState()
                    : ChromaBlocks.CLIFF_STONE.get().defaultBlockState();
            BlockState old = level.getBlockState(pos);
            if (old.is(Blocks.BEDROCK))
                continue;
            changed |= level.setBlock(pos, state, 2);
        }
        return changed;
    }

    /**
     * V33a {@code GlowingCliffsColumnShaper.GlowCliffRegion}: which shelf of the cliffs a column
     * belongs to. Upstream derived this from the shaper's own {@code hval} against its shoreline,
     * middle and top thresholds. That shaper was replaced wholesale by this feature, so the region is
     * read from the same {@code land} noise and the same thresholds this feature bands terrain with,
     * rather than reviving the abandoned class.
     */
    public enum GlowCliffRegion { WATER, SHORES, PLATEAU, HIGH_PLATEAU }

    /** The region of a column, for consumers that site things relative to the cliffs. */
    public static GlowCliffRegion getRegion(WorldGenLevel level, int x, int z) {
        NoiseSet noise = NOISE.computeIfAbsent(level.getSeed(), NoiseSet::new);
        double land = noise.land.getValue(x, z);
        if (land < -0.18)
            return GlowCliffRegion.WATER;
        double upperEdge = land + noise.upperEdge.getValue(x, z) * 0.18;
        if (upperEdge > 0.43)
            return GlowCliffRegion.HIGH_PLATEAU;
        return land < 0.05 ? GlowCliffRegion.SHORES : GlowCliffRegion.PLATEAU;
    }

    /** The top of the middle shelf at a column; V33a's MAX_MIDDLE_TOP_Y analogue. */
    public static int getMiddleTop(WorldGenLevel level, int x, int z) {
        NoiseSet noise = NOISE.computeIfAbsent(level.getSeed(), NoiseSet::new);
        return 106 + (int)Math.round(noise.middleTop.getValue(x, z) * 6);
    }

    private record NoiseSet(NoiseGeneratorBase land, NoiseGeneratorBase shore,
            NoiseGeneratorBase middleTop, NoiseGeneratorBase upperBottom,
            NoiseGeneratorBase upperTop, NoiseGeneratorBase upperEdge) {
        NoiseSet(long seed) {
            this(new SimplexNoiseGenerator(seed).setFrequency(1D / 144D).addOctave(2.5, 0.5).addOctave(6, 0.125),
                    new SimplexNoiseGenerator(~seed).setFrequency(1D / 32D),
                    new SimplexNoiseGenerator(-seed * 4 + 4096).setFrequency(1D / 32D),
                    new SimplexNoiseGenerator(seed * 8 + 4096).setFrequency(1D / 24D),
                    new SimplexNoiseGenerator(seed * 4 + 8192).setFrequency(1D / 24D),
                    new SimplexNoiseGenerator(seed * 2 + 65536).setFrequency(1D / 24D));
        }
    }
}
