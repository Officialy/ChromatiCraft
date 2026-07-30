package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
            // V33a biases half of all patches into the lower half of that band.
            if (random.nextBoolean())
                y = 10 + (y - 10) / 2;
            int radius = 2 + random.nextInt(5);
            int radiusY = 1 + random.nextInt(3);
            double maxDistance = Math.sqrt(radius * radius + radiusY * radiusY + radius * radius);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -radiusY; dy <= radiusY; dy++) {
                        double normalized = (dx * dx + dz * dz) / (double)(radius * radius)
                                + dy * dy / (double)(radiusY * radiusY);
                        if (normalized > 1)
                            continue;
                        // V33a density falloff: 0.8 at the centre down to 0.3 at the rim, so the
                        // patch is a ragged pool rather than a solid ellipsoid.
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (random.nextDouble() >= 0.8 - 0.5 * distance / maxDistance)
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

    /**
     * V33a {@code LumaGenerator.isValidLocation}: an ore-gen-replaceable stone/dirt block with air
     * directly above and every other face backed by something solid.
     *
     * <p>The "or already Luma" clause is load-bearing, not decoration. Luma is a fluid, so it is not
     * solid; without the exemption the first placed block makes all of its neighbours invalid and a
     * patch can never grow past isolated single blocks.
     */
    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(ChromaBlocks.CLIFF_STONE.get()) || state.is(ChromaBlocks.CLIFF_DIRT.get())))
            return false;
        if (!level.getBlockState(pos.above()).isAir())
            return false;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP)
                continue;
            if (!isSupporting(level, pos.relative(dir)))
                return false;
        }
        return true;
    }

    private static boolean isSupporting(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ChromaBlocks.LUMA.get()) || state.isSolid();
    }
}
