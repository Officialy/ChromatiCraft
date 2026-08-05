package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code CaveIndicatorGenerator}: Piezo Crystals seeded through the stone under the Glowing
 * Cliffs, which this port registers as Luminous Cliffs.
 *
 * <p>Upstream sweeps every column of the chunk and makes two attempts per column, so this is a dense
 * pass rather than a scattered one; that whole 16x16x2 loop is kept here because expressing it as
 * vanilla count modifiers would change the distribution from "twice per column" to "N times
 * per chunk". Biome restriction is left to the biome modifier, which attaches this feature to the
 * two Luminous Cliffs biomes only.
 */
public final class CaveIndicatorFeature extends Feature<NoneFeatureConfiguration> {

	public CaveIndicatorFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		BlockState indicator = ChromaBlocks.CAVE_INDICATOR.get().defaultBlockState();
		boolean placed = false;
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int x = origin.getX() + i;
				int z = origin.getZ() + k;
				for (int n = 0; n < 2; n++) {
					// V33a: y = 4 + rand(60), an absolute band. It stays absolute for the same
					// reason the tiered ore band does -- the space below y 0 is deepslate, and this
					// replaces stone.
					BlockPos pos = new BlockPos(x, 4 + random.nextInt(60), z);
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
		return state.is(Blocks.STONE);
	}
}
