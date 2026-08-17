package reika.chromaticraft.client.render;

import java.util.List;

import reika.chromaticraft.entity.AuroraData;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;
import reika.dragonapi.instantiable.math.Spline.BasicSplinePoint;
import reika.dragonapi.instantiable.math.Spline.SplineType;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

/**
 * V33a's {@code Aurora}: the drifting curtain one {@code EntityAurora} draws.
 *
 * <p>A chordal spline is laid between the ribbon's two endpoints with a control point every sixteen
 * blocks, and each interior point slides sideways along the ribbon's own perpendicular, picking a new
 * target whenever it arrives at the last one. That sideways drift is the whole animation — the ribbon
 * has no vertical motion at all, and its ends are pinned with zero variance and zero velocity so the
 * curtain waves in the middle while staying anchored.
 *
 * <p>The drift direction comes from the endpoints' own bearing: upstream takes the azimuth through
 * {@code cartesianToPolar}, negates it, and uses its cosine and sine as the offset factors, so every
 * point slides along the same line regardless of which way the ribbon runs.
 *
 * <p>This is geometry and animation only. It is a client-side class because nothing about a curtain's
 * shape is worth syncing — the server owns the endpoints and colours in {@link AuroraData}, and each
 * client wobbles its own copy.
 */
public final class Aurora {

	/** V33a's segment length: one spline control point every sixteen blocks. */
	private static final double SEGMENT_SIZE = 16;
	/** How high the curtain stands above its baseline. */
	public static final int HEIGHT = 24;
	/** V33a renders the spline at a fixed thirty-two steps regardless of length. */
	public static final int STEPS = 32;

	private final AuroraData data;
	private final Spline spline;

	public Aurora(AuroraData data) {
		this.data = data;
		this.spline = new Spline(SplineType.CHORDAL);

		DecimalPosition from = new DecimalPosition(data.from().x, data.from().y, data.from().z);
		DecimalPosition to = new DecimalPosition(data.to().x, data.to().y, data.to().z);
		double dx = to.xCoord - from.xCoord;
		double dy = to.yCoord - from.yCoord;
		double dz = to.zCoord - from.zCoord;
		// The perpendicular the whole ribbon drifts along, taken from its own bearing.
		double angle = Math.toRadians(-ReikaPhysicsHelper.cartesianToPolar(dx, dy, dz)[2]);
		double xFactor = Math.cos(angle);
		double zFactor = Math.sin(angle);

		int segments = Math.max(1, (int)Math.round(from.getDistanceTo(to) / SEGMENT_SIZE));
		// V33a's variance is a third of the segment length; its two ends get none of it.
		double variance = SEGMENT_SIZE / 3D;
		for (int i = 0; i <= segments; i++) {
			boolean end = i == 0 || i == segments;
			double fraction = i / (double)segments;
			spline.addPoint(new DriftingPoint(DecimalPosition.interpolate(from, to, fraction),
					end ? 0 : variance, end ? 0 : data.speed(), from, dx, dy, dz, fraction,
					xFactor, zFactor));
		}
	}

	public AuroraData data() {
		return data;
	}

	/** Advances every control point one tick of drift. */
	public void update() {
		spline.update();
	}

	/** The current curve, sampled at V33a's fixed step count. */
	public List<DecimalPosition> curve() {
		return spline.get(STEPS, false);
	}

	/**
	 * One control point of the curtain. It never moves vertically: {@code posY} is always its share of
	 * the straight line between the endpoints, and only {@code posX}/{@code posZ} take the drift offset.
	 */
	private static final class DriftingPoint extends BasicSplinePoint {

		private final double velocity;
		private final double variance;
		private final DecimalPosition origin;
		private final double totalX;
		private final double totalY;
		private final double totalZ;
		private final double fraction;
		private final double xFactor;
		private final double zFactor;

		private double offset;
		private double targetOffset;

		private DriftingPoint(DecimalPosition position, double variance, double velocity,
				DecimalPosition origin, double totalX, double totalY, double totalZ, double fraction,
				double xFactor, double zFactor) {
			super(position);
			this.variance = variance;
			this.velocity = velocity;
			this.origin = origin;
			this.totalX = totalX;
			this.totalY = totalY;
			this.totalZ = totalZ;
			this.fraction = fraction;
			this.xFactor = xFactor;
			this.zFactor = zFactor;
			this.offset = plusMinus(variance);
			this.pickNewTarget();
		}

		@Override
		public void update() {
			double delta = targetOffset - offset;
			// V33a assigns tolerance = 1 every tick, overriding the 0.25 the field is initialised with,
			// so the field's initial value is dead and one block is the real arrival threshold.
			double tolerance = 1;
			if (Math.abs(delta) < tolerance)
				this.pickNewTarget();
			posY = origin.yCoord + fraction * totalY;
			posX = origin.xCoord + fraction * totalX + offset * xFactor;
			posZ = origin.zCoord + fraction * totalZ + offset * zFactor;
			if (Math.abs(delta) >= tolerance)
				offset += velocity / 48D * Math.signum(delta);
		}

		private void pickNewTarget() {
			targetOffset = plusMinus(variance);
		}

		/** DragonAPI {@code getRandomPlusMinus(0, variance)}: anywhere in +/- variance. */
		private static double plusMinus(double variance) {
			return variance == 0 ? 0 : (Math.random() * 2 - 1) * variance;
		}
	}
}
