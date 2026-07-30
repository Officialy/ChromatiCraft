package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.registry.ChromaBlocks;

/** V33a cave-column decoration: Glow Daisies on shelf floors and Glow Roots below upper shelves. */
public final class LuminousFloraFeature extends Feature<NoneFeatureConfiguration> {

    public LuminousFloraFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int baseX = context.origin().getX();
        int baseZ = context.origin().getZ();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                BlockPos biomePos = new BlockPos(x, 120, z);
                if (!level.getBiome(biomePos).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS))
                    continue;

                BlockPos floor = findCaveFloor(level, x, z);
                if (floor != null && random.nextDouble() < 0.06) {
                    BlockPos plant = floor.above();
                    BlockState daisy = ChromaBlocks.GLOW_DAISY.get().defaultBlockState();
                    if (level.getBlockState(plant).isAir() && daisy.canSurvive(level, plant))
                        placed |= level.setBlock(plant, daisy, 2);
                }

                BlockPos ceiling = findCaveCeiling(level, x, z);
                if (ceiling != null && random.nextDouble() < 0.008) {
                    int length = 1 + random.nextInt(4);
                    for (int i = 1; i <= length; i++) {
                        BlockPos rootPos = ceiling.below(i);
                        BlockState root = ChromaBlocks.GLOW_ROOT.get().defaultBlockState();
                        if (!level.getBlockState(rootPos).isAir() || !root.canSurvive(level, rootPos))
                            break;
                        placed |= level.setBlock(rootPos, root, 2);
                    }
                }
            }
        }
        return placed;
    }

    private static BlockPos findCaveFloor(WorldGenLevel level, int x, int z) {
        for (int y = 112; y >= 80; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).is(ChromaBlocks.CLIFF_GRASS.get())
                    && level.getBlockState(pos.above()).isAir()
                    && hasCeiling(level, pos.above(), 40))
                return pos;
        }
        return null;
    }

    private static BlockPos findCaveCeiling(WorldGenLevel level, int x, int z) {
        for (int y = 120; y <= 160; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isAir())
                return pos;
        }
        return null;
    }

    private static boolean hasCeiling(WorldGenLevel level, BlockPos start, int distance) {
        for (int dy = 1; dy <= distance; dy++) {
            if (!level.getBlockState(start.above(dy)).isAir())
                return true;
        }
        return false;
    }
}
