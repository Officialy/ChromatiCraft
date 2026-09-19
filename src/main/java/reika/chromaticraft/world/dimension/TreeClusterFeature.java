package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code WorldGenTreeCluster}: Proxima's ordinary forests, as opposed to the glowing trees.
 *
 * <p>A cluster is three to ten trees scattered about a point, each rolling its own wood and its own
 * shape from two separate weighted tables. That double roll is the character of it — a stand comes out
 * mixed rather than uniform, and the rare shapes are rare per tree rather than per cluster.
 *
 * <h2>The woods that survive</h2>
 *
 * <p>Upstream's table is ten entries, seven of which are other mods' woods reached through
 * {@code ModWoodList} — Thaumcraft's silverwood, Twilight Forest's sakura, silverbell and maple, and
 * so on — and it filters them with {@code gen.type.exists()} at class-init. With none of those mods
 * present the table Reika actually rolls is oak, birch and her own lighted wood, at their upstream
 * weights of 10, 8 and 3. That is what is here. The absent seven are not dropped behaviour; they are
 * a filter upstream applies for itself, and they will return with those mods if they are ever ported.
 *
 * <h2>Why the cluster is anchored at the chunk's centre</h2>
 *
 * <p>Upstream scatters each tree up to sixteen blocks from the cluster point, and a giant can reach
 * five more. A decoration feature may only write within a forty-eight block window, and a placement
 * that also randomised the anchor inside the chunk would push the far side of a cluster outside it —
 * where the writes are dropped in silence, which is the failure that once left structures half
 * generated. Anchoring at the chunk centre keeps the whole spread inside the window while leaving
 * upstream's own sixteen-block scatter untouched.
 */
public final class TreeClusterFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a canGenerateTree: fourteen blocks of clear air, which is what a giant needs. */
	private static final int REQUIRED_CLEARANCE = 14;
	/** V33a's scatter about the cluster point. */
	private static final int SPREAD = 16;

	public TreeClusterFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/** V33a's wood table, less the entries whose mods are absent; weights are upstream's. */
	private enum Wood {
		OAK(10), BIRCH(8), LIGHTED(3);

		private final int weight;

		Wood(int weight) {
			this.weight = weight;
		}

		BlockState log() {
			return switch (this) {
				case OAK -> Blocks.OAK_LOG.defaultBlockState();
				case BIRCH -> Blocks.BIRCH_LOG.defaultBlockState();
				case LIGHTED -> ChromaBlocks.GLOW_LOG.get().defaultBlockState();
			};
		}

		BlockState leaf() {
			return switch (this) {
				case OAK -> Blocks.OAK_LEAVES.defaultBlockState();
				case BIRCH -> Blocks.BIRCH_LEAVES.defaultBlockState();
				case LIGHTED -> ChromaBlocks.GLOWING_LEAVES.get().defaultBlockState();
			};
		}
	}

	/**
	 * V33a's shape table. The four numbers are the bare trunk before the canopy and its variation, then
	 * the canopy's own height and variation; the weights make an oak stand mostly oaks with the
	 * occasional giant.
	 */
	private enum Shape {
		OAK(3, 2, 3, 1, 20), HANG(5, 3, 4, 1, 15), TALL(4, 4, 12, 3, 15),
		WIDE(3, 2, 3, 2, 10), NEEDLE(4, 2, 8, 4, 8), GIANT(5, 3, 20, 10, 1);

		private final int baseLog;
		private final int logVariation;
		private final int baseHeight;
		private final int heightVariation;
		private final int weight;

		Shape(int baseLog, int logVariation, int baseHeight, int heightVariation, int weight) {
			this.baseLog = baseLog;
			this.logVariation = logVariation;
			this.baseHeight = baseHeight;
			this.heightVariation = heightVariation;
			this.weight = weight;
		}
	}

	private static final int WOOD_WEIGHT;
	private static final int SHAPE_WEIGHT;

	static {
		int wood = 0;
		for (Wood w : Wood.values())
			wood += w.weight;
		WOOD_WEIGHT = wood;
		int shape = 0;
		for (Shape s : Shape.values())
			shape += s.weight;
		SHAPE_WEIGHT = shape;
	}

	private static Wood rollWood(RandomSource random) {
		int roll = random.nextInt(WOOD_WEIGHT);
		for (Wood w : Wood.values()) {
			roll -= w.weight;
			if (roll < 0)
				return w;
		}
		return Wood.OAK;
	}

	private static Shape rollShape(RandomSource random) {
		int roll = random.nextInt(SHAPE_WEIGHT);
		for (Shape s : Shape.values()) {
			roll -= s.weight;
			if (roll < 0)
				return s;
		}
		return Shape.OAK;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		// A placed feature receives the chunk's minimum corner, not its centre. Omitting this offset
		// let the +/-16 scatter plus a giant canopy enter a chunk two steps away; 26.2 correctly
		// rejected those far-chunk writes and produced the perfectly flat half-trees seen in-world.
		BlockPos centre = context.origin().offset(8, 0, 8);
		int count = 3 + random.nextInt(8);
		boolean any = false;
		for (int i = 0; i < count; i++) {
			int x = centre.getX() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
			int z = centre.getZ() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
			int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos at = new BlockPos(x, y, z);
			if (!canGenerate(world, at))
				continue;
			grow(world, at, random, rollWood(random), rollShape(random));
			any = true;
		}
		return any;
	}

	private static boolean canGenerate(WorldGenLevel world, BlockPos pos) {
		if (!world.getBlockState(pos.below()).is(BlockTags.SUPPORTS_VEGETATION))
			return false;
		for (int i = 0; i < REQUIRED_CLEARANCE; i++)
			if (!world.getBlockState(pos.above(i)).isAir())
				return false;
		return true;
	}

	/**
	 * V33a TreeShape.generate: the trunk runs the whole height, the canopy starts where the bare log
	 * ends, and the shape decides the rest.
	 */
	private static void grow(WorldGenLevel world, BlockPos base, RandomSource random, Wood wood,
			Shape shape) {
		BlockState log = wood.log();
		BlockState leaf = wood.leaf();
		int trunk = shape.baseLog + random.nextInt(shape.logVariation);
		int height = trunk + shape.baseHeight + random.nextInt(shape.heightVariation);
		int canopy = height - trunk;
		for (int i = 0; i < height; i++)
			world.setBlock(base.above(i), log, 3);

		BlockPos top = base.above(trunk);
		switch (shape) {
			case OAK -> oak(world, top, leaf);
			case HANG -> hang(world, top, leaf);
			case WIDE -> wide(world, top, leaf);
			case NEEDLE -> needle(world, top, leaf, log, canopy);
			case TALL -> tall(world, top, leaf, log, canopy);
			case GIANT -> giant(world, top, leaf, random, canopy);
		}
	}

	/** V33a generateOak: two layers of radius two, one of one, and a cross on top. */
	private static void oak(WorldGenLevel world, BlockPos top, BlockState leaf) {
		for (int j = 0; j < 3; j++) {
			int r = j <= 1 ? 2 : 1;
			for (int i = -r; i <= r; i++)
				for (int k = -r; k <= r; k++)
					if (i != 0 || k != 0)
						leaf(world, top.offset(i, j, k), leaf);
		}
		cross(world, top.above(3), leaf);
	}

	/** V33a generateHanging: an oak crown with four curtains of leaves falling from its edge. */
	private static void hang(WorldGenLevel world, BlockPos top, BlockState leaf) {
		oak(world, top, leaf);
		for (int dy = 0; dy >= -3; dy--)
			for (int k = -2; k <= 2; k++)
				if (dy > -3 || (k >= -1 && k <= 1)) {
					leaf(world, top.offset(-2, dy, k), leaf);
					leaf(world, top.offset(2, dy, k), leaf);
					leaf(world, top.offset(k, dy, 2), leaf);
					leaf(world, top.offset(k, dy, -2), leaf);
				}
	}

	/** V33a generateWide: four layers tapering three, three, two, one. */
	private static void wide(WorldGenLevel world, BlockPos top, BlockState leaf) {
		for (int j = 0; j < 4; j++) {
			int r = j <= 1 ? 3 : j < 3 ? 2 : 1;
			for (int i = -r; i <= r; i++)
				for (int k = -r; k <= r; k++)
					if (i != 0 || k != 0 || j == 3)
						leaf(world, top.offset(i, j, k), leaf);
		}
	}

	/** V33a generateNeedle: a narrow column of arms, widest in its middle, capped by a cross. */
	private static void needle(WorldGenLevel world, BlockPos top, BlockState leaf, BlockState log,
			int canopy) {
		for (int dy = 0; dy < canopy; dy++) {
			int r = dy == 0 || dy == canopy ? 1 : 2;
			for (Direction dir : Direction.Plane.HORIZONTAL)
				for (int k = 1; k <= r; k++)
					leaf(world, top.offset(dir.getStepX() * k, dy, dir.getStepZ() * k), leaf);
			if (r > 1)
				for (int i = -1; i <= 1; i += 2)
					for (int k = -1; k <= 1; k += 2)
						leaf(world, top.offset(i, dy, k), leaf);
		}
		world.setBlock(top.above(canopy), log, 3);
		cross(world, top.above(canopy), leaf);
		leaf(world, top.above(canopy + 1), leaf);
	}

	/**
	 * V33a generateTall: a needle wrapped in wide rings, with two of those rings carrying log spokes —
	 * the branches that make it read as a tree rather than a column.
	 */
	private static void tall(WorldGenLevel world, BlockPos top, BlockState leaf, BlockState log,
			int canopy) {
		needle(world, top, leaf, log, canopy);
		for (int j = 1; j < canopy - 1; j++) {
			int r = j >= 2 && j < canopy - 2 ? 3 : 2;
			for (int i = -r; i <= r; i++)
				for (int k = -r; k <= r; k++) {
					if (i == 0 && k == 0)
						continue;
					boolean spoke = (i == 0 && Math.abs(k) <= 2) || (k == 0 && Math.abs(i) <= 2);
					if (spoke && (j == 3 || j == canopy - 3)) {
						world.setBlock(top.offset(i, j, k), log, 3);
						continue;
					}
					// The clipped corners of the two branch rings, which upstream leaves open.
					if ((j == 2 || j == canopy - 3)
							&& ((Math.abs(k) == 2 && Math.abs(i) == 3)
									|| (Math.abs(i) == 2 && Math.abs(k) == 3)))
						continue;
					if (Math.abs(i) != r || Math.abs(k) != r
							|| (j >= canopy / 2 - 1 && j <= canopy / 2 + 1))
						leaf(world, top.offset(i, j, k), leaf);
				}
		}
		cross(world, top.above(canopy), leaf);
	}

	/** V33a generateGiant: a bulging column whose radius grows and shrinks with height, twice at random. */
	private static void giant(WorldGenLevel world, BlockPos top, BlockState leaf, RandomSource random,
			int canopy) {
		for (int j = 0; j < canopy; j++) {
			int r = 1;
			if (j > 0 && j < canopy - 2)
				r++;
			if (j > 2 && j < canopy - 4)
				r++;
			if (j > 4 && j < canopy - 7)
				r++;
			if (random.nextBoolean() && j > 6 && j < canopy - 10)
				r++;
			if (random.nextBoolean() && j > 9 && j < canopy - 12)
				r++;
			for (int i = -r; i <= r; i++)
				for (int k = -r; k <= r; k++)
					if ((i != 0 || k != 0) && i * i + k * k <= r * r)
						leaf(world, top.offset(i, j, k), leaf);
		}
		for (int i = 0; i < 2; i++)
			leaf(world, top.above(canopy + i), leaf);
	}

	/** The five-cell cap several shapes share. */
	private static void cross(WorldGenLevel world, BlockPos at, BlockState leaf) {
		leaf(world, at, leaf);
		for (Direction dir : Direction.Plane.HORIZONTAL)
			leaf(world, at.relative(dir), leaf);
	}

	/** Leaves never replace what is already standing, so overlapping canopies do not eat each other. */
	private static void leaf(WorldGenLevel world, BlockPos pos, BlockState leaf) {
		if (world.getBlockState(pos).canBeReplaced())
			world.setBlock(pos, leaf, 3);
	}
}
