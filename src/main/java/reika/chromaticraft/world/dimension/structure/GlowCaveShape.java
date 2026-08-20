package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import reika.dragonapi.instantiable.Interpolation;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;
import reika.dragonapi.instantiable.math.Spline.BasicSplinePoint;
import reika.dragonapi.instantiable.math.Spline.SplineType;

/**
 * V33a {@code WorldGenGlowCave}'s shape half: the set of cells a glowing cave occupies.
 *
 * <p>The cave is grown, not carved. A chain of points wanders down from the surface — the first step
 * drops eight to twelve blocks and moves at most six sideways, every step after can move thirty-two —
 * and those points become a chordal spline, which is sampled twelve times per segment and swept with a
 * sphere whose radius drifts between three and four and a half along the way. Every quarter-block of
 * that sweep contributes its cells, so the result is a smooth tunnel rather than a chain of beads.
 *
 * <p>It forks. At any segment past a one-in-{@code max(24, y)} roll — rarer the higher up it is, so
 * branching happens deep — a whole new chain grows from a point along the current one, up to three
 * levels deep. A fork that reconnects with its parent stops rather than tangling further, which is
 * upstream's {@code break} on {@code getLine} reporting an intersection.
 *
 * <p>Separating the shape from the generator is what makes the cave testable and what lets the piece
 * write it in chunk-sized bites: the whole set is computed once, in structure-start coordinates, and
 * the piece writes only the part of it inside whatever box it is handed.
 */
public final class GlowCaveShape {

	/** V33a MIN_RADIUS / MAX_RADIUS. */
	private static final double MIN_RADIUS = 3;
	private static final double MAX_RADIUS = 4.5;

	private GlowCaveShape() {}

	/** The cells of one cave, in world coordinates, grown down from the given surface point. */
	public static Set<BlockPos> grow(RandomSource rand, double x, double y, double z) {
		Set<BlockPos> all = new HashSet<>();
		growFrom(rand, x, y, z, MAX_RADIUS, all, new HashSet<>(), 0, 0);
		return all;
	}

	/**
	 * V33a growFrom. {@code parent} is everything grown so far — a fork reads it to know when it has
	 * met its own trunk — and {@code path} is this chain's own cells, which are folded into the parent
	 * when it finishes.
	 */
	private static void growFrom(RandomSource rand, double x0, double y0, double z0, double maxr,
			Set<BlockPos> parent, Set<BlockPos> path, int forkDepth, double minY) {
		minY = Math.max(0, Math.min(minY, y0 - 1));
		List<CavePoint> points = new ArrayList<>();
		double r0 = between(rand, MIN_RADIUS, maxr);
		points.add(new CavePoint(x0, y0, z0, r0));
		boolean first = true;
		while (y0 > minY) {
			// The first step is deliberately tighter than the rest: a cave has to start near enough to
			// its mouth to read as belonging to it, and only then wanders.
			double x2 = plusMinus(rand, x0, first ? 6 : 32);
			double z2 = plusMinus(rand, z0, first ? 6 : 32);
			double y2 = first ? y0 - 8 - rand.nextInt(5) : y0 - rand.nextInt(16);
			y2 = Math.max(y2, minY);
			x0 = x2;
			y0 = y2;
			z0 = z2;
			points.add(new CavePoint(x0, y0, z0, r0));
			first = false;
		}

		Spline spline = new Spline(SplineType.CHORDAL);
		for (int i = 0; i < points.size(); i++) {
			CavePoint pos = points.get(i);
			spline.addPoint(pos);
			// Every interior point gets a second anchor a little below it, which is what stops the
			// spline bulging upward between two widely separated points.
			if (i != 0 && i != points.size() - 1)
				spline.addPoint(pos.offset(0, -1 - rand.nextInt(6), 0));
		}
		List<DecimalPosition> line = spline.get(12, false);

		// The radius is its own interpolation over the sampled line, re-anchored at random intervals, so
		// the tunnel pinches and opens out along its length instead of being a uniform tube.
		Interpolation radius = new Interpolation(false);
		int d = 0;
		while (d < line.size()) {
			radius.addPoint(d, between(rand, MIN_RADIUS, maxr));
			int remaining = line.size() - d;
			d += 1 + rand.nextInt(Math.max(4, remaining / 6));
		}
		radius.addPoint(line.size() - 1, between(rand, MIN_RADIUS, maxr));

		for (int i = 0; i < line.size() - 1; i++) {
			DecimalPosition p1 = line.get(i);
			DecimalPosition p2 = line.get(i + 1);
			boolean met = sweep(p1, p2, radius.getValue(i), radius.getValue(i + 1), parent, path);
			// A fork that has run back into its own trunk stops there rather than tangling.
			if (met && forkDepth > 0 && i > 10)
				break;
			if (forkDepth <= 3 && rand.nextInt(Math.max(24, (int)p1.yCoord)) == 0) {
				double f = rand.nextDouble();
				Set<BlockPos> branch = new HashSet<>();
				growFrom(rand, p1.xCoord + (p2.xCoord - p1.xCoord) * f,
						p1.yCoord + (p2.yCoord - p1.yCoord) * f,
						p1.zCoord + (p2.zCoord - p1.zCoord) * f, radius.getValue(i + f), path, branch,
						forkDepth + 1, rand.nextInt(3) == 0 ? 0 : between(rand, 6, 20));
				path.addAll(branch);
			}
		}
		parent.addAll(path);
	}

	/**
	 * V33a getLine: spheres every block along the segment, reporting whether any of them met a cell the
	 * parent chain already claimed.
	 */
	private static boolean sweep(DecimalPosition p1, DecimalPosition p2, double r1, double r2,
			Set<BlockPos> parent, Set<BlockPos> path) {
		double dx = p2.xCoord - p1.xCoord;
		double dy = p2.yCoord - p1.yCoord;
		double dz = p2.zCoord - p1.zCoord;
		double dd = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dd <= 0)
			return false;
		boolean met = false;
		for (double d = 0.25; d < dd; d += 1) {
			double f = d / dd;
			met |= sphere(p1.xCoord + f * dx, p1.yCoord + f * dy, p1.zCoord + f * dz,
					r1 + (r2 - r1) * f, parent, path);
		}
		return met;
	}

	/** V33a getSphere: a 0.75-step lattice, which oversamples enough that no cell is missed. */
	private static boolean sphere(double x, double y, double z, double r, Set<BlockPos> parent,
			Set<BlockPos> path) {
		boolean met = false;
		for (double dx = -r; dx <= r; dx += 0.75)
			for (double dy = -r; dy <= r; dy += 0.75)
				for (double dz = -r; dz <= r; dz += 0.75) {
					if (dx * dx + dy * dy + dz * dz > r * r)
						continue;
					BlockPos cell = new BlockPos((int)Math.floor(x + dx), (int)Math.floor(y + dy),
							(int)Math.floor(z + dz));
					path.add(cell);
					met |= parent.contains(cell);
				}
		return met;
	}

	/** {@code ReikaRandomHelper.getRandomBetween}. */
	private static double between(RandomSource rand, double min, double max) {
		return min + rand.nextDouble() * (max - min);
	}

	/** {@code ReikaRandomHelper.getRandomPlusMinus}. */
	private static double plusMinus(RandomSource rand, double base, double spread) {
		return base - spread + rand.nextDouble() * spread * 2;
	}

	/** V33a CavePoint: a spline anchor that also remembers the radius it was chosen with. */
	private static final class CavePoint extends BasicSplinePoint {

		private final double radius;

		CavePoint(double x, double y, double z, double r) {
			super(x, y, z);
			this.radius = r;
		}

		CavePoint offset(int x, int y, int z) {
			return new CavePoint(posX + x, posY + y, posZ + z, radius);
		}
	}
}
