package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code CaveIndicatorGenerator}: Piezo Crystals seeded through the stone under the Glowing
 * Cliffs, which this port registers as Luminous Cliffs.
 *
 * <p>Upstream sweeps every column of the chunk and makes two attempts per column, so this is a dense
 * pass rather than a scattered one; that whole 16x16x2 loop is kept here because expressing it as
 * vanilla count modifiers would change the distribution from "twice per column" to "N times
 * per chunk". Each actual column is restricted to the two Luminous Cliffs biomes.
 */
public final class CaveIndicatorFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<CaveIndicatorFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(CaveIndicatorFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<CaveIndicatorFeature> codec() {
        return CODEC;
    }

	public CaveIndicatorFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		if (world.getLevel().isFlat()) return false;
		BlockState indicator = ChromaBlocks.CAVE_INDICATOR.get().defaultBlockState();
		boolean placed = false;
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int x = origin.getX() + i;
				int z = origin.getZ() + k;
				if (!reika.chromaticraft.world.luminous.LuminousCliffsBiomeMask.kindOf(
						world.getBiome(new BlockPos(x, world.getSeaLevel(), z))).isGlowingCliffs()) continue;
				for (int n = 0; n < 2; n++) {
                            // Include deepslate caves beneath the old world floor.
                            BlockPos pos = new BlockPos(x, reika.dragonapi.libraries.level.WorldgenHeight.randomBetween(
                                    world, random, reika.dragonapi.libraries.level.WorldgenHeight.caveFloor(world), 63), z);
					if (canGenerateAt(world, pos)) {
						world.setBlock(pos, indicator, 2);
						placed = true;
					}
				}
			}
		}
		return placed;
	}

	/**
	 * V33a canGenerateAt: replaceable stone, with a free block above it that cannot see the sky, in
	 * the dark. The light test is what keeps these to real caves rather than solid rock.
	 */
	public static boolean canGenerateAt(WorldGenLevel world, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState ceiling = world.getBlockState(above);
		if (!ceiling.isAir() && !ceiling.canBeReplaced())
			return false;
		if (!canGenerateIn(world, pos))
			return false;
		return !world.canSeeSky(above) && world.getMaxLocalRawBrightness(pos) < 8;
	}

	/** V33a canGenerateIn: stone, or anything ore generation would treat as stone. */
	public static boolean canGenerateIn(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.is(reika.chromaticraft.registry.ChromaBlockTags.NATURAL_STONE);
	}
}
