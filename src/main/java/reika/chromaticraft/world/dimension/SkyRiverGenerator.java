package reika.chromaticraft.world.dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;
import reika.dragonapi.instantiable.math.Spline.BasicSplinePoint;
import reika.dragonapi.instantiable.math.Spline.SplineType;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * V33a {@code SkyRiverGenerator}: the sky rivers, Proxima's long-distance transport.
 *
 * <p>They are rays radiating from the world origin at y 384 to 512, drawn as splines and then
 * resampled so no two points are more than eighteen blocks apart. A player who comes within
 * {@link #RIVER_TUNNEL_RADIUS} of one is caught and carried along it — which is what makes a dimension
 * fifteen thousand blocks wide crossable at all.
 *
 * <h2>The two layers</h2>
 *
 * <p>Eight rays start close in, between 64 and 256 blocks from the origin, one every 45 degrees. A
 * second set fills the gaps every 11.25 degrees but only begins at 1024 to 3072 blocks out, so the sky
 * near the middle is not a thicket of rivers while the outer world still has a dense network. Both
 * layers run out past the structure ring, to the maximum possible structure distance plus the region
 * buffer plus 512, and up to 2048 further.
 *
 * <p>A ray wanders as it goes: its bearing is re-rolled at every node within a variation that widens
 * from 10 degrees near the centre to 5 further out, and successive bearings may differ by at most
 * {@link #MAX_ANGLE_STEP}, so a river curves rather than zigzagging. Node spacing grows with distance —
 * {@code max(128, 2*sqrt(d))} — because a fixed spacing would put absurdly many points in the far ring.
 *
 * <p>Points are indexed by chunk, which is the whole reason the lookup is affordable: finding the river
 * near a player is a handful of map lookups rather than a scan of every point in the world.
 */
public final class SkyRiverGenerator {

	/** V33a RIVER_TUNNEL_RADIUS: how close a player must be to be caught by a river. */
	public static final double RIVER_TUNNEL_RADIUS = 12;

	private static final double INNER_RADIUS_MIN = 64;
	private static final double INNER_RADIUS_MAX = 256;
	private static final double LAYER2_RADIUS_MIN = 1024;
	private static final double LAYER2_RADIUS_MAX = 3072;

	private static final double FULL_RAY_ANGLE = 45;
	private static final double LAYER2_RAY_ANGLE = 22.5 / 2;
	private static final double NODE_LENGTH = 64 * 2;
	private static final double INNER_ANGLE_VARIATION = 10;
	private static final double ANGLE_VARIATION = 10 / 2D;
	private static final double MAX_ANGLE_STEP = ANGLE_VARIATION / 2;
	private static final double VERTICAL_POSITION_MIN = 384;
	private static final double VERTICAL_POSITION_MAX = 512;
	private static final double ANGLE_VARIATION_FADE_RANGE = 384;
	/** V33a Ray.MAX_POINT_DST: the spline is resampled so no gap exceeds this. */
	private static final double MAX_POINT_DISTANCE = 18;

	private static volatile SkyRiverGenerator active;

	private final List<Ray> rays = new ArrayList<>();
	private final Map<Long, List<RiverPoint>> pointsByChunk = new HashMap<>();

	private SkyRiverGenerator() {}

	/** The rivers for the loaded world, or null before they have been generated. */
	public static SkyRiverGenerator getActive() {
		return active;
	}

	public static void clear() {
		active = null;
	}

	private static double outerRadiusMin() {
		return StructureCalculator.getMaximumPossibleDistance() + RegionMapper.MAX_BUFFER + 512;
	}

	/**
	 * The same rivers, built for a client without touching the server-side {@code active} instance or
	 * the generator gate. On an integrated server both copies live in one process, so keeping them
	 * separate is what stops single-player working by accident while multiplayer draws nothing.
	 */
	public static SkyRiverGenerator generateForClient(long seed) {
		SkyRiverGenerator generator = build(seed);
		generator.index();
		return generator;
	}

	private static SkyRiverGenerator build(long seed) {
		SkyRiverGenerator generator = new SkyRiverGenerator();
		Random random = new Random(seed);
		double outerMin = outerRadiusMin();
		double outerMax = outerMin + 2048;
		for (double angle = 0; angle < 360; angle += FULL_RAY_ANGLE)
			generator.generateRay(random, angle,
					INNER_RADIUS_MIN + random.nextDouble() * (INNER_RADIUS_MAX - INNER_RADIUS_MIN),
					outerMin + random.nextDouble() * (outerMax - outerMin));
		for (double angle = 0; angle < 360; angle += LAYER2_RAY_ANGLE) {
			if (angle % FULL_RAY_ANGLE == 0)
				continue;
			generator.generateRay(random, angle,
					LAYER2_RADIUS_MIN + random.nextDouble() * (LAYER2_RADIUS_MAX - LAYER2_RADIUS_MIN),
					outerMin + random.nextDouble() * (outerMax - outerMin));
		}
		return generator;
	}

	public static SkyRiverGenerator generate(long seed) {
		SkyRiverGenerator generator = build(seed);
		generator.index();
		active = generator;
		ProximaGenerators.finish(ProximaGenerators.Generator.SKYRIVER);
		return generator;
	}

	/**
	 * V33a's per-ray walk. The bearing is re-rolled each node and rejected while it differs from the
	 * last by more than {@link #MAX_ANGLE_STEP}, which is what keeps a river smooth.
	 */
	private void generateRay(Random random, double bearing, double from, double to) {
		Ray ray = new Ray();
		double lastAngle = bearing;
		for (double d = from; d <= to; d += Math.max(NODE_LENGTH, 2 * Math.sqrt(d))) {
			double variation = ANGLE_VARIATION;
			if (d < LAYER2_RADIUS_MIN)
				variation = d < LAYER2_RADIUS_MIN - ANGLE_VARIATION_FADE_RANGE ? INNER_ANGLE_VARIATION
						: ReikaMathLibrary.linterpolate(d, LAYER2_RADIUS_MIN - ANGLE_VARIATION_FADE_RANGE,
								LAYER2_RADIUS_MIN, INNER_ANGLE_VARIATION, ANGLE_VARIATION);
			double angle = bearing + random.nextDouble() * variation * 2 - variation;
			// Upstream rejects and re-rolls with no bound. The variation is always wider than the step,
			// so it terminates with probability one, but a bounded retry avoids betting a worldgen
			// thread on that; after the cap the nearest legal bearing is taken instead.
			for (int attempt = 0; attempt < 64 && Math.abs(angle - lastAngle) > MAX_ANGLE_STEP; attempt++)
				angle = bearing + random.nextDouble() * variation * 2 - variation;
			if (Math.abs(angle - lastAngle) > MAX_ANGLE_STEP)
				angle = lastAngle + Math.signum(angle - lastAngle) * MAX_ANGLE_STEP;
			lastAngle = angle;
			double radians = Math.toRadians(angle);
			ray.points.add(new DecimalPosition(d * Math.cos(radians),
					VERTICAL_POSITION_MIN + random.nextDouble()
							* (VERTICAL_POSITION_MAX - VERTICAL_POSITION_MIN),
					d * Math.sin(radians)));
		}
		if (ray.points.size() <= 2)
			return;
		ray.spline();
		ray.resample();
		rays.add(ray);
	}

	/**
	 * V33a indexes every interior point by the chunk it falls in. The first and last point of a ray are
	 * skipped because a river point needs both a previous and a next to define its segment.
	 */
	private void index() {
		for (Ray ray : rays) {
			int count = ray.points.size();
			for (int i = 1; i < count - 1; i++) {
				DecimalPosition position = ray.points.get(i);
				ChunkPos chunk = new ChunkPos(Mth.floor(position.xCoord) >> 4, Mth.floor(position.zCoord) >> 4);
				pointsByChunk.computeIfAbsent(ChunkPos.pack(chunk.x(), chunk.z()), key -> new ArrayList<>())
						.add(new RiverPoint(i, count, position, ray.points.get(i - 1), ray.points.get(i + 1)));
			}
		}
	}

	public List<Ray> getRays() {
		return Collections.unmodifiableList(rays);
	}

	public Collection<RiverPoint> getPointsForChunk(int chunkX, int chunkZ) {
		List<RiverPoint> points = pointsByChunk.get(ChunkPos.pack(chunkX, chunkZ));
		return points == null ? List.of() : Collections.unmodifiableList(points);
	}

	/** Every point within the given block range of an entity, gathered chunk by chunk. */
	public Collection<RiverPoint> getPointsWithin(Entity entity, double range) {
		int chunkX = Mth.floor(entity.getX()) >> 4;
		int chunkZ = Mth.floor(entity.getZ()) >> 4;
		int chunkRange = Mth.floor(range) >> 4;
		List<RiverPoint> found = new ArrayList<>();
		for (int dx = -chunkRange - 1; dx <= chunkRange; dx++)
			for (int dz = -chunkRange - 1; dz <= chunkRange; dz++)
				found.addAll(this.getPointsForChunk(chunkX + dx, chunkZ + dz));
		return found;
	}

	public RiverPoint getClosestPoint(Entity entity, double range) {
		RiverPoint closest = null;
		double best = Double.POSITIVE_INFINITY;
		for (RiverPoint point : this.getPointsWithin(entity, range)) {
			double distance = entity.distanceToSqr(point.position().xCoord, point.position().yCoord,
					point.position().zCoord);
			if (distance < best && distance <= range * range) {
				best = distance;
				closest = point;
			}
		}
		return closest;
	}

	/** True if the entity is inside the tunnel of either segment meeting at this point. */
	public static boolean isWithinRiver(Entity entity, RiverPoint point) {
		return point != null && (isBetween(point.previous(), point.position(), entity)
				|| isBetween(point.position(), point.next(), entity));
	}

	public static boolean isBetween(DecimalPosition from, DecimalPosition to, Entity entity) {
		return ReikaVectorHelper.getDistFromPointToLine(from.xCoord, from.yCoord, from.zCoord,
				to.xCoord, to.yCoord, to.zCoord, entity.getX(), entity.getY(), entity.getZ())
				< RIVER_TUNNEL_RADIUS;
	}

	/** One river: an ordered run of points from near the origin out past the structure ring. */
	public static final class Ray {

		private List<DecimalPosition> points = new ArrayList<>();

		public List<DecimalPosition> getPoints() {
			return Collections.unmodifiableList(points);
		}

		private void spline() {
			Spline spline = new Spline(SplineType.CHORDAL);
			for (DecimalPosition point : points)
				spline.addPoint(new BasicSplinePoint(point));
			points = spline.get(8, false);
		}

		/**
		 * V33a {@code rebuildWithMaxDst}: the spline's own sampling leaves long gaps where a ray runs
		 * straight, and a gap wider than the tunnel lets a player fall between two points without ever
		 * being near one. Long segments are subdivided so that cannot happen.
		 *
		 * <p>One deliberate correction. Upstream computes the number of pieces as
		 * {@code floor(distance / maxDistance)}, which is 1 for any gap shorter than <em>twice</em> the
		 * target — so the segment is re-added unchanged and gaps up to 36 blocks survive a method whose
		 * whole purpose is to bound them at 18. That is not academic: a rider is found by searching
		 * within 16 blocks of a point, so at the midpoint of a 36-block gap they are 18 from each
		 * neighbour, out of reach of both, and drop out of the sky at seven blocks a tick. Using
		 * {@code ceil} honours the method's own contract and is what stops that happening.
		 */
		private void resample() {
			List<DecimalPosition> resampled = new ArrayList<>();
			for (int i = 0; i < points.size() - 1; i++) {
				DecimalPosition from = points.get(i);
				DecimalPosition to = points.get(i + 1);
				double distance = from.getDistanceTo(to);
				if (distance < MAX_POINT_DISTANCE) {
					resampled.add(from);
					continue;
				}
				double steps = Math.ceil(distance / MAX_POINT_DISTANCE);
				double stepX = (to.xCoord - from.xCoord) / steps;
				double stepY = (to.yCoord - from.yCoord) / steps;
				double stepZ = (to.zCoord - from.zCoord) / steps;
				for (int step = 0; step < Math.round(steps); step++)
					resampled.add(new DecimalPosition(from.xCoord + stepX * step,
							from.yCoord + stepY * step, from.zCoord + stepZ * step));
			}
			resampled.add(points.get(points.size() - 1));
			points = resampled;
		}
	}

	/**
	 * One indexed point of a river, carrying the two neighbours that define its segments and where it
	 * sits along the ray — {@link #fractionalPosition()} is what the renderer fades a river in and out
	 * by, and what tells a rider how far along they are.
	 */
	public record RiverPoint(int index, int pathLength, DecimalPosition position,
			DecimalPosition previous, DecimalPosition next) {

		public float fractionalPosition() {
			return index / (float)pathLength;
		}
	}
}
