package reika.chromaticraft.world.dimension;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code WorldGenGlowingCracks}: the seam of light, and the ore crystal buried under it.
 *
 * <p>Two halves, and the second is the point. On the surface it lays a single Glowing Cracks block and
 * reinforces the nine-by-nine of ground under it — upstream sets metadata 1 on the grass and the stone
 * below, which is its "unbreakable" flag — so the light cannot be dug out from above. Underneath, eight
 * to sixteen blocks down, it grows a {@link Crystal}: a bipyramid eight to sixteen blocks tall on a
 * waist two to five across, tilted a few degrees off vertical and spun to any heading, then jittered
 * cell by cell so no two are alike. Every stone cell inside it becomes ore.
 *
 * <p>Which ore is half chance and half what the world offers. Upstream draws from a weighted table of
 * vanilla and modded ores and then, half the time regardless, uses a ChromatiCraft tiered ore instead.
 * The modded half of that table is {@code ModOreList}, which is 1.7.10 mod integration; what is here is
 * the vanilla weighting Reika gives — emerald 10, diamond 20, redstone 50, gold 40 — against the same
 * even split with the tiered ores, so the shape of the choice is hers even though the pool is shorter.
 */
public final class GlowingCracksFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a {@code r}: the ground it reinforces reaches four blocks out, so nine by nine. */
	private static final int GROUND_RADIUS = 4;

	public GlowingCracksFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		BlockPos origin = context.origin();

		// V33a: every cell of the nine-by-nine must be grass one below the placement, or nothing.
		for (int i = -GROUND_RADIUS; i <= GROUND_RADIUS; i++)
			for (int k = -GROUND_RADIUS; k <= GROUND_RADIUS; k++)
				if (!world.getBlockState(origin.offset(i, -1, k)).is(Blocks.GRASS_BLOCK))
					return false;

		world.setBlock(origin, ChromaBlocks.GLOWING_CRACKS.get().defaultBlockState(), 3);

		// V33a sets metadata 1 on the two layers under the whole square: its unbreakable flag. Grass has
		// no such state, so the ground becomes reinforced Stone Shielding, which is this port's identity
		// for exactly that idea -- the light cannot be undermined.
		BlockState reinforced = ChromaBlocks.shielding(
						reika.chromaticraft.registry.ChromaShieldTypes.STONE).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		for (int i = -GROUND_RADIUS; i <= GROUND_RADIUS; i++)
			for (int k = -GROUND_RADIUS; k <= GROUND_RADIUS; k++) {
				world.setBlock(origin.offset(i, -1, k), reinforced, 3);
				world.setBlock(origin.offset(i, -2, k), reinforced, 3);
			}

		int height = 8 + rand.nextInt(9);
		Crystal crystal = new Crystal(origin.getX(), origin.getY() - 4 - height / 2, origin.getZ(),
				height, 2 + rand.nextInt(4),
				rand.nextDouble() * 10 - 5, rand.nextDouble() * 360, rand.nextDouble() * 10 - 5);
		BlockState ore = randomOre(rand);
		boolean placed = false;
		for (BlockPos cell : crystal.cells(rand)) {
			BlockState at = world.getBlockState(cell);
			if (!at.is(Blocks.STONE) && !(at.getBlock()
					instanceof reika.chromaticraft.block.worldgen26.BlockTieredOre))
				continue;
			world.setBlock(cell, ore, 2);
			placed = true;
		}
		return placed;
	}

	/**
	 * V33a's ore choice: a weighted draw, then a coin flip that replaces it with a tiered ore anyway.
	 * The tiered ores eligible are those whose host is stone, which upstream expresses as its
	 * {@code while (ore.genBlock != Blocks.stone)} loop.
	 */
	private static BlockState randomOre(RandomSource rand) {
		if (rand.nextInt(2) == 0)
			return (rand.nextBoolean() ? ChromaBlocks.ENERGIZED_ROCK : ChromaBlocks.ELEMENTAL_STONES)
					.get().defaultBlockState();
		// Reika's own weights: emerald 10, diamond 20, redstone 50, gold 40.
		int roll = rand.nextInt(120);
		if (roll < 10)
			return Blocks.EMERALD_ORE.defaultBlockState();
		if (roll < 30)
			return Blocks.DIAMOND_ORE.defaultBlockState();
		if (roll < 80)
			return Blocks.REDSTONE_ORE.defaultBlockState();
		return Blocks.GOLD_ORE.defaultBlockState();
	}

	/**
	 * V33a's {@code Crystal}: a bipyramid with an apex above and below a four-point waist, rotated as a
	 * whole and then jittered point by point.
	 *
	 * <p>Its cells are found a layer at a time. At each y the four waist points are pulled toward
	 * whichever apex that layer is on — linearly, so the taper is straight-sided — and the quadrilateral
	 * they make is filled. Upstream tests three points per cell (its centre and both diagonal corners)
	 * rather than one, which is what keeps the sloping faces from coming out ragged.
	 */
	private static final class Crystal {

		private final double centerY;
		private final double[] lower = new double[3];
		private final double[] upper = new double[3];
		private final double[][] edges = new double[4][3];

		Crystal(int x, int y, int z, double rootHeight, double rootSize,
				double rotX, double rotY, double rotZ) {
			this.centerY = y;
			rotate(new double[] {0, -rootHeight, 0}, rotX, rotY, rotZ, lower);
			rotate(new double[] {0, rootHeight, 0}, rotX, rotY, rotZ, upper);
			rotate(new double[] {rootSize, 0, 0}, rotX, rotY, rotZ, edges[0]);
			rotate(new double[] {0, 0, rootSize}, rotX, rotY, rotZ, edges[1]);
			rotate(new double[] {-rootSize, 0, 0}, rotX, rotY, rotZ, edges[2]);
			rotate(new double[] {0, 0, -rootSize}, rotX, rotY, rotZ, edges[3]);
			offset(lower, x, y, z);
			offset(upper, x, y, z);
			for (double[] edge : edges)
				offset(edge, x, y, z);
		}

		/** V33a randomize: every point drifts up to half a block, so no two crystals are the same. */
		private void jitter(RandomSource rand) {
			jitter(lower, rand);
			jitter(upper, rand);
			for (double[] edge : edges)
				jitter(edge, rand);
		}

		Collection<BlockPos> cells(RandomSource rand) {
			jitter(rand);
			Set<BlockPos> set = new HashSet<>();
			int y0 = (int)Math.floor(Math.min(lower[1], upper[1]));
			int y1 = (int)Math.ceil(Math.max(lower[1], upper[1]));
			for (int y = y0; y < y1; y++) {
				double dy = y + 0.5;
				boolean below = dy < centerY;
				double[] apex = below ? lower : upper;
				// How far along the taper this layer is: 1 at the waist, 0 at the apex.
				double span = below ? centerY - lower[1] : upper[1] - centerY;
				double f = span == 0 ? 0 : (below ? 1 - (centerY - dy) / span : 1 - (dy - centerY) / span);
				double[] xs = new double[4];
				double[] zs = new double[4];
				for (int i = 0; i < 4; i++) {
					xs[i] = apex[0] + (edges[i][0] - apex[0]) * f;
					zs[i] = apex[2] + (edges[i][2] - apex[2]) * f;
				}
				double minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
				double maxX = Math.max(Math.max(xs[0], xs[1]), Math.max(xs[2], xs[3]));
				double minZ = Math.min(Math.min(zs[0], zs[1]), Math.min(zs[2], zs[3]));
				double maxZ = Math.max(Math.max(zs[0], zs[1]), Math.max(zs[2], zs[3]));
				for (int x = (int)Math.floor(minX); x <= (int)Math.ceil(maxX); x++)
					for (int z = (int)Math.floor(minZ); z <= (int)Math.ceil(maxZ); z++) {
						double cx = x + 0.5;
						double cz = z + 0.5;
						if (contains(xs, zs, cx, cz) || contains(xs, zs, cx - 0.5, cz - 0.5)
								|| contains(xs, zs, cx + 0.5, cz + 0.5))
							set.add(new BlockPos(x, y, z));
					}
			}
			return set;
		}

		/** {@code DoublePolygon.contains}, as the even-odd ray cast a four-point polygon needs. */
		private static boolean contains(double[] xs, double[] zs, double x, double z) {
			boolean inside = false;
			for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
				if (zs[i] > z != zs[j] > z
						&& x < (xs[j] - xs[i]) * (z - zs[i]) / (zs[j] - zs[i]) + xs[i])
					inside = !inside;
			}
			return inside;
		}

		/** {@code ReikaVectorHelper.rotateVector}, in its X-then-Y-then-Z order. */
		private static void rotate(double[] v, double rx, double ry, double rz, double[] into) {
			double x = v[0];
			double y = v[1];
			double z = v[2];
			double a = Math.toRadians(rx);
			double y1 = y * Math.cos(a) - z * Math.sin(a);
			double z1 = y * Math.sin(a) + z * Math.cos(a);
			a = Math.toRadians(ry);
			double x2 = x * Math.cos(a) + z1 * Math.sin(a);
			double z2 = -x * Math.sin(a) + z1 * Math.cos(a);
			a = Math.toRadians(rz);
			into[0] = x2 * Math.cos(a) - y1 * Math.sin(a);
			into[1] = x2 * Math.sin(a) + y1 * Math.cos(a);
			into[2] = z2;
		}

		private static void offset(double[] v, double x, double y, double z) {
			v[0] += x;
			v[1] += y;
			v[2] += z;
		}

		private static void jitter(double[] v, RandomSource rand) {
			v[0] += rand.nextDouble() - 0.5;
			v[1] += rand.nextDouble() - 0.5;
			v[2] += rand.nextDouble() - 0.5;
		}
	}
}
