package reika.chromaticraft.world.luminous;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
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

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                cursor.set(x, surface, z);
                if (!level.getBiome(cursor).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS))
                    continue;

                double land = noise.land.getValue(x, z);
                int shoreTop = 67 + (int)Math.round(noise.shore.getValue(x, z) * 5);
                if (land < -0.18) {
                    placed |= reskinSurface(level, cursor, Math.min(surface, shoreTop));
                    continue;
                }

                int middleTop = 106 + (int)Math.round(noise.middleTop.getValue(x, z) * 6);
                placed |= fillShelf(level, cursor, 63, middleTop);

                double upperEdge = land + noise.upperEdge.getValue(x, z) * 0.18;
                if (upperEdge > 0.43) {
                    int upperBottom = 134 + (int)Math.round(noise.upperBottom.getValue(x, z) * 6);
                    int upperTop = 152 + (int)Math.round(noise.upperTop.getValue(x, z) * 8);
                    if (upperTop - upperBottom >= 8)
                        placed |= fillShelf(level, cursor, upperBottom, upperTop);
                }
            }
        }
        return placed;
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
