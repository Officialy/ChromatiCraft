package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.registry.ChromaBlocks;

/** Floating, tapered Luminous Cliffs auxiliary islands with occasional water or Luma crowns. */
public final class LuminousIslandFeature extends Feature<NoneFeatureConfiguration> {

    public LuminousIslandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int x = context.origin().getX() + 8;
        int z = context.origin().getZ() + 8;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int top = Math.max(surface + 28, 104) + random.nextInt(38);
        int radius = 8 + random.nextInt(10);
        int depth = 10 + random.nextInt(14);
        BlockPos center = new BlockPos(x, top, z);
        if (!level.getBiome(center).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS)
                || top + 4 >= level.getMaxY() || top - depth <= surface + 10)
            return false;
        if (!isClear(level, center, radius))
            return false;

        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double horizontal = Math.sqrt(dx * dx + dz * dz) / radius;
                if (horizontal > 1)
                    continue;
                int columnDepth = Math.max(1, (int)Math.round(depth * (1 - horizontal * horizontal)));
                for (int dy = 0; dy >= -columnDepth; dy--) {
                    BlockState state = dy == 0 ? ChromaBlocks.CLIFF_GRASS.get().defaultBlockState()
                            : dy >= -3 ? ChromaBlocks.CLIFF_DIRT.get().defaultBlockState()
                            : ChromaBlocks.CLIFF_STONE.get().defaultBlockState();
                    placed |= level.setBlock(center.offset(dx, dy, dz), state, 2);
                }
            }
        }

        if (radius >= 11 && random.nextInt(3) == 0) {
            BlockState liquid = random.nextInt(8) == 0 ? ChromaBlocks.LUMA.get().defaultBlockState()
                    : Blocks.WATER.defaultBlockState();
            int pool = Math.max(2, radius / 4);
            for (int dx = -pool; dx <= pool; dx++) {
                for (int dz = -pool; dz <= pool; dz++) {
                    if (dx * dx + dz * dz <= pool * pool)
                        level.setBlock(center.offset(dx, 0, dz), liquid, 2);
                }
            }
        }
        return placed;
    }

    private static boolean isClear(WorldGenLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx += Math.max(2, radius / 3)) {
            for (int dz = -radius; dz <= radius; dz += Math.max(2, radius / 3)) {
                for (int dy = 0; dy <= 4; dy += 2) {
                    if (!level.getBlockState(center.offset(dx, dy, dz)).isAir())
                        return false;
                }
            }
        }
        return true;
    }
}
