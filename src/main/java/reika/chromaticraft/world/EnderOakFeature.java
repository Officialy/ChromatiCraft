package reika.chromaticraft.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.dragonapi.instantiable.math.LobulatedCurve;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

/**
 * V33a {@code EnderOakGenerator}: the Ender Forest's own tree shape.
 *
 * <p>It builds vanilla oak logs and leaves — the Ender Oak is a silhouette, not a new wood type — but
 * the silhouette is nothing vanilla can express. A trunk of random height carries a crown whose radius
 * random-walks up the tree and whose cross-section is a {@link LobulatedCurve}, so each layer is a
 * lobed blob rather than a disc, clipped by a per-tree height ratio so the whole crown tapers.
 * Optional branches fire off at evenly spaced azimuths with jittered polar angles, laying logs along a
 * polar vector and scattering 8-20 leaves per half-step around it. Finally, leaves on the lowest crown
 * layer that are not fully enclosed can drip downward in columns until something stops them, which is
 * what gives the forest its hanging curtains.
 *
 * <p>Three variants exist, differing only in constructor parameters; see {@link Variant}.
 */
public class EnderOakFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a BiomeEnderForest's three configured Ender Oaks, verbatim. */
	public enum Variant {
		/** {@code enderOakSmall}: squat, no branches, light dripping. */
		SMALL(2, 4, 3, 5, 2, 3, 0F, 0, 0.1F),
		/** {@code enderOakLarge}: the broad canopy tree. */
		LARGE(3, 7, 5, 12, 3, 5, 0.15F, 6, 0.15F),
		/** {@code enderOakNarrow}: tall and thin, branch-heavy, no dripping. */
		NARROW(6, 12, 6, 15, 1, 2, 0.35F, 4, 0F);

		final int minTrunk;
		final int maxTrunk;
		final int minFoliage;
		final int maxFoliage;
		final int minRadius;
		final int maxRadius;
		final int maxBranchLength;
		final float branchChancePerLevel;
		final float columnChancePerLeaf;

		Variant(int h0, int h1, int f0, int f1, int fr0, int fr1, float b, int bl, float c) {
			minTrunk = h0;
			maxTrunk = h1;
			minFoliage = f0;
			maxFoliage = f1;
			minRadius = fr0;
			maxRadius = fr1;
			branchChancePerLevel = b;
			maxBranchLength = bl;
			columnChancePerLeaf = c;
		}
	}

	private final Variant variant;

	public EnderOakFeature(Variant variant) {
		super(NoneFeatureConfiguration.CODEC);
		this.variant = variant;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		// V33a gates on ReikaPlantHelper.SAPLING.canPlantAt: ordinary sapling ground.
		BlockState soil = world.getBlockState(origin.below());
		if (!soil.is(Blocks.GRASS_BLOCK) && !soil.is(Blocks.DIRT) && !soil.is(Blocks.PODZOL)
				&& !soil.is(Blocks.COARSE_DIRT) && !soil.is(Blocks.ROOTED_DIRT))
			return false;
		int trunk = variant.minTrunk + random.nextInt(variant.maxTrunk - variant.minTrunk + 1);
		int foliage = variant.minFoliage + random.nextInt(variant.maxFoliage - variant.minFoliage + 1);
		Tree tree = new Tree(trunk, foliage);
		tree.calculate(world, random, origin);
		if (!tree.canPlace(world))
			return false;
		tree.place(world);
		return true;
	}

	private final class Tree {

		private final Set<BlockPos> logs = new HashSet<>();
		private final Map<Integer, Set<BlockPos>> leaves = new HashMap<>();
		private final Map<BlockPos, Branch> branches = new HashMap<>();

		private final int trunkHeight;
		private final int leafHeight;
		private final int totalHeight;

		private int lowestLeafY = Integer.MAX_VALUE;
		private int currentRadius;
		private float currentRadiusExponent = 0.5F;

		private Tree(int trunk, int leaf) {
			trunkHeight = trunk;
			leafHeight = leaf;
			totalHeight = trunk + leaf;
			currentRadius = Math.min(3, (int)(variant.maxRadius * 0.8));
		}

		private void addLeaf(BlockPos pos) {
			leaves.computeIfAbsent(pos.getY(), k -> new HashSet<>()).add(pos);
			lowestLeafY = Math.min(lowestLeafY, pos.getY());
		}

		private Collection<BlockPos> allLeaves() {
			List<BlockPos> all = new ArrayList<>();
			leaves.values().forEach(all::addAll);
			return all;
		}

		private void calculate(WorldGenLevel world, RandomSource random, BlockPos base) {
			LobulatedCurve curve = new LobulatedCurve(1, 0.2F, 3, 1);
			// LobulatedCurve predates RandomSource; seed a java.util.Random from the feature's own.
			curve.generate(new java.util.Random(random.nextLong()));
			double heightRatio = 1 + 1.2 * random.nextDouble();
			for (int h = 0; h <= totalHeight; h++) {
				BlockPos core = base.above(h);
				if (h >= totalHeight - 1)
					this.addLeaf(core);
				else
					logs.add(core);
				if (h > trunkHeight && h < totalHeight)
					this.generateLayer(random, base, h, core, curve, heightRatio);
			}
			this.generateBranches(random, base);
			this.dripColumns(world, random, base);
		}

		/** V33a: evenly spaced azimuths, jittered, with the polar angle clamped by remaining height. */
		private void generateBranches(RandomSource random, BlockPos base) {
			if (variant.branchChancePerLevel <= 0)
				return;
			Set<Integer> levels = new HashSet<>(leaves.keySet());
			for (int i = 0; i < 3; i++)
				levels.remove(base.getY() + totalHeight - i);
			if (levels.isEmpty())
				return;
			// V33a rerolls while exactly one branch was chosen, so a tree never has a lone branch.
			int n = 1;
			while (n == 1) {
				n = 0;
				for (int i = 0; i < 3; i++)
					if (random.nextFloat() < variant.branchChancePerLevel)
						n++;
			}
			if (n <= 0)
				return;
			List<Integer> pool = new ArrayList<>(levels);
			float phi0 = random.nextFloat() * 360;
			float spacing = 360F / n;
			for (int i = 0; i < n; i++) {
				int levelY = pool.get(random.nextInt(pool.size()));
				int dh = base.getY() + totalHeight - levelY;
				float phi = phi0 + spacing * i + (random.nextFloat() * 48F - 24F);
				float theta = random.nextFloat() * 60F - 30F;
				theta = Math.min(theta, Math.max(0, 10 * (dh - 3)));
				BlockPos root = new BlockPos(base.getX(), levelY, base.getZ());
				branches.put(root, new Branch(root, phi, theta));
			}
			for (Branch branch : branches.values()) {
				branch.calculate(random);
				logs.addAll(branch.logs);
				List<BlockPos> spare = new ArrayList<>(branch.leaves);
				spare.removeAll(logs);
				spare.forEach(this::addLeaf);
			}
		}

		/** V33a: edge leaves on the lowest crown layer hang down until something stops them. */
		private void dripColumns(WorldGenLevel world, RandomSource random, BlockPos base) {
			if (variant.columnChancePerLeaf <= 0 || lowestLeafY == Integer.MAX_VALUE)
				return;
			Set<BlockPos> lowest = new HashSet<>(leaves.getOrDefault(lowestLeafY, Set.of()));
			Set<BlockPos> add = new HashSet<>();
			for (BlockPos pos : lowest) {
				if (pos.getX() == base.getX() && pos.getZ() == base.getZ())
					continue;
				int neighbours = 0;
				for (Direction dir : Direction.Plane.HORIZONTAL)
					if (lowest.contains(pos.relative(dir)))
						neighbours++;
				if (neighbours == 4)
					continue; // fully enclosed, so not an edge
				if (random.nextFloat() >= variant.columnChancePerLeaf)
					continue;
				for (int y = lowestLeafY; y > world.getMinY(); y--) {
					BlockPos below = new BlockPos(pos.getX(), y, pos.getZ());
					if (!this.canReplace(world, below))
						break;
					add.add(below);
				}
			}
			add.forEach(this::addLeaf);
		}

		private void generateLayer(RandomSource random, BlockPos base, int h, BlockPos core,
				LobulatedCurve curve, double heightRatio) {
			this.permuteRadius(random, h);
			int r = currentRadius + 2;
			int dh = totalHeight - h;
			for (int i = -r; i <= r; i++) {
				for (int k = -r; k <= r; k++) {
					double ia = Math.pow(Math.abs(i), 1D / currentRadiusExponent);
					double ka = Math.pow(Math.abs(k), 1D / currentRadiusExponent);
					double d = Math.pow(Math.abs(ia + ka), currentRadiusExponent);
					double ang = Math.toDegrees(Math.atan2(k, i));
					double dr = (currentRadius + 0.5) * curve.getRadius(ang);
					double maxr = Math.min(variant.maxRadius, dh * heightRatio);
					dr = Mth.clamp(dr, variant.minRadius, maxr);
					dr = Math.min(maxr, dr);
					if (d <= dr)
						this.addLeaf(new BlockPos(base.getX() + i, core.getY(), base.getZ() + k));
				}
			}
		}

		/** V33a's random walk on both the crown radius and the superellipse exponent. */
		private void permuteRadius(RandomSource random, int h) {
			if (currentRadius == 0 || random.nextInt(3) > 0) {
				boolean incr = currentRadius < variant.maxRadius;
				boolean decr = currentRadius > variant.minRadius;
				if (incr && decr)
					currentRadius += random.nextBoolean() ? 1 : -1;
				else if (incr)
					currentRadius++;
				else if (decr)
					currentRadius--;
			}
			if (random.nextInt(2) == 0) {
				boolean incr = currentRadiusExponent < 0.65F;
				boolean decr = currentRadiusExponent > 0.35F;
				int sign = incr && decr ? (random.nextBoolean() ? 1 : -1) : incr ? 1 : decr ? -1 : 0;
				currentRadiusExponent += sign * (0.025F + random.nextFloat() * 0.075F);
			}
			int r = Math.min(variant.maxRadius, totalHeight - h);
			currentRadius = Math.min(Mth.clamp(currentRadius, variant.minRadius, r), r);
			currentRadiusExponent = Mth.clamp(currentRadiusExponent, 0.35F, 0.65F);
		}

		/** V33a: every log must fit, but only three quarters of the leaves need to. */
		private boolean canPlace(WorldGenLevel world) {
			for (BlockPos pos : logs)
				if (!this.canReplace(world, pos))
					return false;
			Collection<BlockPos> all = this.allLeaves();
			int required = (int)(all.size() * 0.75);
			int placeable = 0;
			for (BlockPos pos : all)
				if (this.canReplace(world, pos))
					placeable++;
			return placeable >= required;
		}

		private boolean canReplace(WorldGenLevel world, BlockPos pos) {
			BlockState state = world.getBlockState(pos);
			if (state.isAir() || state.canBeReplaced())
				return true;
			if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))
				return false;
			return state.is(BlockTags.LEAVES);
		}

		private void place(WorldGenLevel world) {
			BlockState log = Blocks.OAK_LOG.defaultBlockState();
			BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState();
			for (BlockPos pos : logs)
				world.setBlock(pos, log, 2);
			for (BlockPos pos : this.allLeaves())
				if (!logs.contains(pos))
					world.setBlock(pos, leaf, 2);
		}

		/** V33a Branch: logs along a polar vector, with leaves scattered around each half-step. */
		private final class Branch {

			private final BlockPos root;
			private final Set<BlockPos> logs = new HashSet<>();
			private final Set<BlockPos> leaves = new HashSet<>();
			private final double xStep;
			private final double yStep;
			private final double zStep;

			private Branch(BlockPos root, float phi, float theta) {
				this.root = root;
				double[] xyz = ReikaPhysicsHelper.polarToCartesian(1, theta, phi);
				xStep = xyz[0];
				yStep = xyz[1];
				zStep = xyz[2];
			}

			private void calculate(RandomSource random) {
				int max = Math.min((int)(leafHeight * 0.67),
						2 + random.nextInt(Math.max(1, variant.maxBranchLength - 1)));
				for (double d = 0.5; d <= max; d += 0.5) {
					double dx = root.getX() + 0.5 + xStep * d;
					double dy = root.getY() + 0.5 + yStep * d;
					double dz = root.getZ() + 0.5 + zStep * d;
					logs.add(BlockPos.containing(dx, dy, dz));
					int n = 8 + random.nextInt(13);
					for (int i = 0; i < n; i++) {
						BlockPos leaf = BlockPos.containing(
								dx + (random.nextDouble() * 3 - 1.5),
								dy + (random.nextDouble() * 3 - 1.5),
								dz + (random.nextDouble() * 3 - 1.5));
						if (!logs.contains(leaf))
							leaves.add(leaf);
					}
				}
			}
		}
	}
}
