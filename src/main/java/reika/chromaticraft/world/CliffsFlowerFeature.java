package reika.chromaticraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.world.luminous.LuminousCliffsBiomeMask;

/** V33a DecoFlowerGenerator's separate Glow Daisy and Lumen Root post-population patches. */
public final class CliffsFlowerFeature implements Feature {
    public static final MapCodec<CliffsFlowerFeature> CODEC = Codec.BOOL.fieldOf("root")
            .xmap(CliffsFlowerFeature::new, feature -> feature.root);
    private final boolean root;

    public CliffsFlowerFeature(boolean root) {
        this.root = root;
    }

    @Override
    public MapCodec<CliffsFlowerFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!world.dimensionType().hasSkyLight()
                || world.getLevel().isFlat() && !ChromaOptions.FLATGEN.getState())
            return false;
        return placePatch(world, random, origin);
    }

    boolean placePatch(WorldGenLevel world, RandomSource random, BlockPos origin) {
        BlockState state = (root ? ChromaBlocks.GLOW_ROOT : ChromaBlocks.GLOW_DAISY).get().defaultBlockState();
        int wanted = random.nextInt(5) == 0 ? 1 + random.nextInt(4) + random.nextInt(6) : 1;
        int done = 0;
        for (int tries = 0; done < wanted && tries < 40; tries++) {
            int x = origin.getX() + random.nextInt(16), z = origin.getZ() + random.nextInt(16);
            if (!LuminousCliffsBiomeMask.kindOf(world.getBiome(new BlockPos(x, world.getSeaLevel(), z))).isGlowingCliffs())
                continue;
            // getTopSolidOrLiquidBlock ignores leaves. Keep its initial +/-25 draw even for
            // roots, which subsequently replace the height with the source's inclusive 4..80.
            int surface = world.getHeight(world instanceof net.minecraft.server.level.WorldGenRegion
                    ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE, x, z);
            while (surface > world.getMinY()) {
                BlockState below = world.getBlockState(new BlockPos(x, surface - 1, z));
                if (!below.isAir() && !below.is(BlockTags.LEAVES)) break;
                surface--;
            }
            int y = Math.clamp(surface + random.nextInt(51) - 25, world.getMinY() + 1, world.getMaxY() - 1);
            if (root) {
                y = 4 + random.nextInt(77);
            } else {
                while (y > world.getMinY() + 1 && world.getBlockState(new BlockPos(x, y - 1, z)).isAir()) y--;
                if (y <= world.getMinY() + 1) continue;
            }
            while (y < world.getMaxY() - 1 && !world.getBlockState(new BlockPos(x, y, z)).isAir()) y++;
            BlockPos pos = new BlockPos(x, y, z);
            if (root) {
                while (pos.getY() < world.getMaxY() - 1 && !state.canSurvive(world, pos)
                        && world.getBlockState(pos.above()).isAir()) pos = pos.above();
            }
            if (world.getBlockState(pos).isAir() && state.canSurvive(world, pos)) {
                world.setBlock(pos, state, 2);
                done++;
            }
        }
        return done > 0;
    }
}
