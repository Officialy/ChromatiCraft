package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;

/** V33a LumaGenerator: one or two low cave-floor ellipsoids per selected chunk. */
public final class LumaPatchFeature extends Feature<NoneFeatureConfiguration> {

    public LumaPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;
        int patches = 1 + random.nextInt(2);
        for (int n = 0; n < patches; n++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            int y = 10 + random.nextInt(39);
            int radius = 2 + random.nextInt(5);
            int radiusY = 1 + random.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -radiusY; dy <= radiusY; dy++) {
                        double distance = (dx * dx + dz * dz) / (double)(radius * radius)
                                + dy * dy / (double)(radiusY * radiusY);
                        if (distance > 1)
                            continue;
                        BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                        if (canReplace(level, pos))
                            placed |= level.setBlock(pos, ChromaBlocks.LUMA.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(ChromaBlocks.CLIFF_STONE.get()) || state.is(ChromaBlocks.CLIFF_DIRT.get())))
            return false;
        if (!level.getBlockState(pos.above()).isAir())
            return false;
        return level.getBlockState(pos.below()).isSolid()
                && level.getBlockState(pos.north()).isSolid()
                && level.getBlockState(pos.south()).isSolid()
                && level.getBlockState(pos.east()).isSolid()
                && level.getBlockState(pos.west()).isSolid();
    }
}
