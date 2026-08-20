package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code WorldGenLightedShrub}: the glowing bushes that carpet Proxima's ground.
 *
 * <p>This is the dimension's ground cover, and there is a lot of it — upstream's chance is <b>four per
 * chunk</b> in the Glowing Forest and two nearly everywhere else (only the Sparkling Sands, at a
 * quarter, is sparse). Without it the ground reads as bare between the trees, which is exactly what its
 * absence looked like.
 *
 * <p>Three sizes, and the smallest is the common one. One attempt in forty is the large form: a
 * five-by-five ring two blocks tall — the {@code |i| < 2 || |k| < 2} test leaves its four corners open —
 * capped by three diamond layers of radii 2, 2 and 1. One in fifteen of the rest is the middle form,
 * which is those same three layers starting at the ground. <b>Everything else is a single Lighted Log
 * with a glow leaf on each of its five non-downward faces</b>, and since that is the overwhelming
 * majority of attempts, it is what the ground cover actually is.
 *
 * <p>Leaves are only ever placed into air, so a shrub never eats terrain it grows against.
 */
public final class LightedShrubFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a's three crown layers, shared by both the large and middle forms. */
	private static final int[] CROWN_RADII = {2, 2, 1};

	public LightedShrubFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		BlockPos origin = context.origin();
		// V33a: grass directly beneath, and nothing else will do.
		if (!world.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK))
			return false;

		// Upstream rolls the two chances in sequence, so the middle form's one-in-fifteen is taken from
		// what the one-in-forty left -- not from every attempt.
		int size = 0;
		if (rand.nextInt(40) == 0)
			size = 2;
		else if (rand.nextInt(15) == 0)
			size = 1;

		BlockState log = ChromaBlocks.GLOW_LOG.get().defaultBlockState();
		BlockState leaves = ChromaBlocks.GLOWING_LEAVES.get().defaultBlockState();

		switch (size) {
			case 2 -> {
				// The body: a five-by-five ring two tall, its corners left open.
				for (int j = 0; j < 2; j++)
					for (int i = -2; i <= 2; i++)
						for (int k = -2; k <= 2; k++)
							if (Math.abs(i) < 2 || Math.abs(k) < 2)
								leaf(world, origin.offset(i, j, k), leaves);
				crown(world, origin.above(2), leaves);
				world.setBlock(origin, log, 3);
			}
			case 1 -> {
				crown(world, origin, leaves);
				world.setBlock(origin, log, 3);
			}
			default -> {
				world.setBlock(origin, log, 3);
				// Every face but the one it stands on.
				for (Direction dir : Direction.values())
					if (dir != Direction.DOWN)
						leaf(world, origin.relative(dir), leaves);
			}
		}
		return true;
	}

	/** V33a's {@code r = {2, 2, 1}} diamonds, tested as {@code |i|+|k| <= r+1}. */
	private static void crown(WorldGenLevel world, BlockPos base, BlockState leaves) {
		for (int j = 0; j < CROWN_RADII.length; j++)
			for (int i = -2; i <= 2; i++)
				for (int k = -2; k <= 2; k++)
					if (Math.abs(i) + Math.abs(k) <= CROWN_RADII[j] + 1)
						leaf(world, base.offset(i, j, k), leaves);
	}

	private static void leaf(WorldGenLevel world, BlockPos pos, BlockState leaves) {
		if (world.getBlockState(pos).isAir())
			world.setBlock(pos, leaves, 3);
	}
}
