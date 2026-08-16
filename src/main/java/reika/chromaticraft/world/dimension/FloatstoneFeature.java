package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenFloatstone}: the drifts of Floatstone hanging above Proxima's Skylands.
 *
 * <p>Upstream picks a cluster height twelve to twenty-four blocks above the ground, then scatters two
 * to eight veins around it, each offset up to eight blocks horizontally and three vertically. Every
 * vein is a {@code WorldGenMinable} of twenty-four to forty blocks replacing <em>air</em> rather than
 * stone, which is what leaves them hanging unsupported. Once a cluster has four or more veins each is
 * shrunk by {@code 1 - n/16}, so a busy cluster is many small drifts rather than one mass.
 *
 * <h2>Why this does not call vanilla's ore feature</h2>
 *
 * <p>{@code OreFeature} is {@code WorldGenMinable}'s descendant and was the obvious thing to hand this
 * to, but it refuses outright: before placing anything it requires the vein to start at or below the
 * {@code OCEAN_FLOOR_WG} heightmap, a guard that makes sense for buried ore and none at all for a
 * drift twelve blocks up. Every call would return false. So the 1.7.10 blob algorithm is ported
 * directly — a line between two endpoints with an ellipsoid swept along it, radii widest at the middle
 * — which is the same shape without the height guard.
 *
 * <p>The eight-block shift is upstream's and deliberate here: {@code WorldGenMinable} adds it inside
 * itself, from the days generators were handed chunk corners, and V33a passes block coordinates
 * straight in. It biases every cluster by eight blocks in x and z, and reproducing that is what makes
 * these land where V33a puts them.
 *
 * <p>The size upstream passes is a vein block count, not a width; forty blocks is a drift about nine
 * across, so with the scatter a whole cluster spans roughly twenty-seven blocks and sits inside the
 * chunks this generation step may write to.
 */
public final class FloatstoneFeature extends Feature<NoneFeatureConfiguration> {

	public FloatstoneFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		// V33a: the cluster floats twelve to twenty-four blocks above whatever it was anchored to.
		int clusterY = origin.getY() + 12 + random.nextInt(12);
		int veins = 2 + random.nextInt(6);
		BlockState floatstone = ChromaBlocks.deco(ProximaDecoTypes.FLOATSTONE).get().defaultBlockState();
		boolean placed = false;
		for (int i = 0; i < veins; i++) {
			int size = 24 + random.nextInt(16);
			// V33a's crowding rule: a cluster of four or more veins makes each of them smaller.
			if (veins >= 4)
				size = (int)(size * (1 - veins / 16D));
			placed |= vein(world, random, floatstone, size,
					plusMinus(random, origin.getX(), 8),
					plusMinus(random, clusterY, 3),
					plusMinus(random, origin.getZ(), 8));
		}
		return placed;
	}

	/**
	 * 1.7.10 {@code WorldGenMinable.generate}: two endpoints either side of the centre along a random
	 * horizontal bearing, with an ellipsoid swept between them whose radius peaks in the middle.
	 */
	private static boolean vein(WorldGenLevel world, RandomSource random, BlockState state, int size,
			int x, int y, int z) {
		if (size <= 0)
			return false;
		float bearing = random.nextFloat() * (float)Math.PI;
		// The +8 is upstream's own chunk-corner offset; see the class documentation.
		double x0 = x + 8 + Math.sin(bearing) * size / 8F;
		double x1 = x + 8 - Math.sin(bearing) * size / 8F;
		double z0 = z + 8 + Math.cos(bearing) * size / 8F;
		double z1 = z + 8 - Math.cos(bearing) * size / 8F;
		double y0 = y + random.nextInt(3) - 2;
		double y1 = y + random.nextInt(3) - 2;
		boolean placed = false;
		for (int step = 0; step <= size; step++) {
			double cx = x0 + (x1 - x0) * step / size;
			double cy = y0 + (y1 - y0) * step / size;
			double cz = z0 + (z1 - z0) * step / size;
			double scale = random.nextDouble() * size / 16D;
			double width = (Math.sin(step * Math.PI / size) + 1) * scale + 1;
			double height = (Math.sin(step * Math.PI / size) + 1) * scale + 1;
			int minX = Mth.floor(cx - width / 2);
			int minY = Mth.floor(cy - height / 2);
			int minZ = Mth.floor(cz - width / 2);
			int maxX = Mth.floor(cx + width / 2);
			int maxY = Mth.floor(cy + height / 2);
			int maxZ = Mth.floor(cz + width / 2);
			for (int bx = minX; bx <= maxX; bx++) {
				double dx = (bx + 0.5 - cx) / (width / 2);
				if (dx * dx >= 1)
					continue;
				for (int by = minY; by <= maxY; by++) {
					double dy = (by + 0.5 - cy) / (height / 2);
					if (dx * dx + dy * dy >= 1)
						continue;
					for (int bz = minZ; bz <= maxZ; bz++) {
						double dz = (bz + 0.5 - cz) / (width / 2);
						if (dx * dx + dy * dy + dz * dz >= 1)
							continue;
						BlockPos pos = new BlockPos(bx, by, bz);
						// V33a's target block is air, which is why a drift can form in open sky.
						if (world.isOutsideBuildHeight(by) || !world.getBlockState(pos).isAir())
							continue;
						world.setBlock(pos, state, 2);
						placed = true;
					}
				}
			}
		}
		return placed;
	}

	/** DragonAPI {@code ReikaRandomHelper.getRandomPlusMinus}: centre plus or minus the given spread. */
	private static int plusMinus(RandomSource random, int centre, int spread) {
		return centre - spread + random.nextInt(spread * 2 + 1);
	}
}
