package reika.chromaticraft.world.dimension.structure.laser;

import java.util.ArrayList;
import java.util.List;

import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/**
 * Source-exact, world-independent interaction rules for V33a's Chromatic Beams puzzle.
 *
 * <p>The result distinguishes a pulse that continues through the current effector from replacement
 * pulses emitted at that effector. V33a relies on that difference: mirrors, the direct splitter path
 * and prisms kill the incoming entity and spawn new ones, whereas a filter or successful refractor
 * changes the existing entity and lets it continue.
 */
public final class LaserPulseLogic {

	private LaserPulseLogic() {}

	public static Interaction interact(EffectType type, CubeDirections facing, BeamColor effectorColor,
			Pulse incoming) {
		boolean targetMatched = type.isTarget() && incoming.color().equals(effectorColor)
				&& (incoming.direction() == facing || type.isOmnidirectional());
		return switch (type) {
			case COLORIZER -> colorize(incoming, effectorColor, targetMatched);
			case EMITTER -> Interaction.absorbed(targetMatched);
			case MIRROR -> mirror(incoming, facing, false, targetMatched);
			case DOUBLEMIRROR -> mirror(incoming, facing, true, targetMatched);
			case SLITMIRROR -> slitMirror(incoming, facing, targetMatched);
			case ONEWAY -> incoming.direction() == facing
					? Interaction.continuing(incoming, targetMatched)
					: Interaction.absorbed(targetMatched);
			case POLARIZER -> incoming.direction() == facing
						|| incoming.direction() == facing.getOpposite()
					? Interaction.continuing(incoming, targetMatched)
					: Interaction.absorbed(targetMatched);
			case PRISM -> prism(incoming, facing, targetMatched);
			case REFRACTOR -> refract(incoming, facing, targetMatched);
			case SPLITTER -> split(incoming, facing, targetMatched);
			case TARGET -> Interaction.absorbed(targetMatched);
			case TARGET_THRU -> Interaction.continuing(incoming, targetMatched);
		};
	}

	private static Interaction colorize(Pulse incoming, BeamColor filter, boolean targetMatched) {
		BeamColor color = incoming.color().intersect(filter);
		return color.isBlack() ? Interaction.absorbed(targetMatched)
				: Interaction.continuing(incoming.withColor(color), targetMatched);
	}

	private static Interaction mirror(Pulse incoming, CubeDirections facing, boolean doubleMirror,
			boolean targetMatched) {
		if (doubleMirror) {
			CubeDirections perpendicular = incoming.direction().getRotation(true, 2);
			if (perpendicular == facing || perpendicular == facing.getOpposite())
				return Interaction.absorbed(targetMatched);
			CubeDirections surface = facing;
			int difference = Math.abs(incoming.direction().ordinal() - facing.ordinal());
			if (difference < 2 || difference > 6)
				surface = surface.getOpposite();
			return reflectedReplacement(incoming, surface, targetMatched);
		}
		int difference = Math.abs(incoming.direction().ordinal() - facing.ordinal());
		if (difference < 3 || difference > 5)
			return Interaction.absorbed(targetMatched);
		return reflectedReplacement(incoming, facing, targetMatched);
	}

	private static Interaction slitMirror(Pulse incoming, CubeDirections facing,
			boolean targetMatched) {
		CubeDirections perpendicular = incoming.direction().getRotation(true, 2);
		return perpendicular != facing && perpendicular != facing.getOpposite()
				? mirror(incoming, facing, true, targetMatched)
				: Interaction.continuing(incoming, targetMatched);
	}

	private static Interaction reflectedReplacement(Pulse incoming, CubeDirections surface,
			boolean targetMatched) {
		CubeDirections reflected = reflect(incoming.direction(), surface);
		return reflected == null ? Interaction.absorbed(targetMatched)
				: Interaction.replacing(List.of(incoming.withDirection(reflected)), targetMatched);
	}

	/** Exact vector construction used by V33a EntityLaserPulse#reflect. */
	public static CubeDirections reflect(CubeDirections incoming, CubeDirections surface) {
		int multiplier = surface.isCardinal() || surface == incoming.getOpposite() ? 2 : 1;
		int dx = incoming.directionX + multiplier * surface.directionX;
		int dz = incoming.directionZ + multiplier * surface.directionZ;
		return dx == 0 && dz == 0 ? null : CubeDirections.getFromVectors(dx, dz);
	}

	private static Interaction prism(Pulse incoming, CubeDirections facing, boolean targetMatched) {
		if (incoming.direction() != facing.getOpposite()) {
			boolean red = incoming.color().red()
					&& incoming.direction() == facing.getRotation(true, 2);
			boolean green = incoming.color().green() && incoming.direction() == facing;
			boolean blue = incoming.color().blue()
					&& incoming.direction() == facing.getRotation(false, 2);
			return Interaction.prismContribution(new BeamColor(red, green, blue), targetMatched);
		}
		List<Pulse> output = new ArrayList<>(3);
		if (incoming.color().red()) output.add(new Pulse(facing.getRotation(true, 2), BeamColor.RED));
		if (incoming.color().green()) output.add(new Pulse(facing, BeamColor.GREEN));
		if (incoming.color().blue()) output.add(new Pulse(facing.getRotation(false, 2), BeamColor.BLUE));
		return Interaction.replacing(output, targetMatched);
	}

	private static Interaction refract(Pulse incoming, CubeDirections facing, boolean targetMatched) {
		int difference = wrapDegrees(incoming.direction().angle - facing.angle);
		if (difference == 90)
			return Interaction.continuing(
					incoming.withDirection(incoming.direction().getRotation(false)), targetMatched);
		if (difference == -45)
			return Interaction.continuing(
					incoming.withDirection(incoming.direction().getRotation(true)), targetMatched);
		return Interaction.absorbed(targetMatched);
	}

	private static int wrapDegrees(int degrees) {
		int wrapped = Math.floorMod(degrees + 180, 360) - 180;
		return wrapped == -180 && degrees > 0 ? 180 : wrapped;
	}

	private static Interaction split(Pulse incoming, CubeDirections facing, boolean targetMatched) {
		if (incoming.direction() == facing) {
			return Interaction.replacing(List.of(
					incoming.withDirection(incoming.direction().getRotation(true)),
					incoming.withDirection(incoming.direction().getRotation(false))), targetMatched);
		}
		CubeDirections opposite = facing.getOpposite();
		if (incoming.direction() == opposite.getRotation(true)
				|| incoming.direction() == opposite.getRotation(false))
			return Interaction.continuing(incoming.withDirection(opposite), targetMatched);
		return Interaction.absorbed(targetMatched);
	}

	public record BeamColor(boolean red, boolean green, boolean blue) {
		public static final BeamColor BLACK = new BeamColor(false, false, false);
		public static final BeamColor RED = new BeamColor(true, false, false);
		public static final BeamColor GREEN = new BeamColor(false, true, false);
		public static final BeamColor BLUE = new BeamColor(false, false, true);
		public static final BeamColor WHITE = new BeamColor(true, true, true);

		public BeamColor intersect(BeamColor other) {
			return new BeamColor(red && other.red, green && other.green, blue && other.blue);
		}

		public BeamColor add(BeamColor other) {
			return new BeamColor(red || other.red, green || other.green, blue || other.blue);
		}

		public boolean isBlack() {
			return !red && !green && !blue;
		}

		public int renderColor() {
			return isBlack() ? 0x101010
					: (red ? 0xFF0000 : 0) | (green ? 0x00FF00 : 0) | (blue ? 0x0000FF : 0);
		}
	}

	public record Pulse(CubeDirections direction, BeamColor color) {
		public Pulse withDirection(CubeDirections replacement) {
			return new Pulse(replacement, color);
		}

		public Pulse withColor(BeamColor replacement) {
			return new Pulse(direction, replacement);
		}
	}

	public record Interaction(Pulse continuingPulse, List<Pulse> emittedPulses,
			BeamColor prismContribution, boolean targetMatched) {
		public Interaction {
			emittedPulses = List.copyOf(emittedPulses);
		}

		public boolean absorbed() {
			return continuingPulse == null;
		}

		private static Interaction absorbed(boolean targetMatched) {
			return new Interaction(null, List.of(), BeamColor.BLACK, targetMatched);
		}

		private static Interaction continuing(Pulse pulse, boolean targetMatched) {
			return new Interaction(pulse, List.of(), BeamColor.BLACK, targetMatched);
		}

		private static Interaction replacing(List<Pulse> pulses, boolean targetMatched) {
			return new Interaction(null, pulses, BeamColor.BLACK, targetMatched);
		}

		private static Interaction prismContribution(BeamColor color, boolean targetMatched) {
			return new Interaction(null, List.of(), color, targetMatched);
		}
	}

	/** V33a PrismTile's delayed additive recombination state. */
	public static final class PrismAccumulator {
		private final int timerLength;
		private BeamColor pending = BeamColor.BLACK;
		private int timer;

		public PrismAccumulator(int timerLength) {
			if (timerLength < 1)
				throw new IllegalArgumentException("Prism timer must be positive, got " + timerLength);
			this.timerLength = timerLength;
		}

		public void add(BeamColor contribution) {
			pending = pending.add(contribution);
			timer = timerLength;
		}

		/** Returns the recombined pulse exactly when V33a's countdown reaches zero. */
		public Pulse tick(CubeDirections facing) {
			if (timer > 0) timer--;
			if (timer == 0 && !pending.isBlack()) {
				Pulse output = new Pulse(facing, pending);
				pending = BeamColor.BLACK;
				return output;
			}
			return null;
		}

		public int timerLength() { return timerLength; }
		public int remainingTicks() { return timer; }
		public BeamColor pendingColor() { return pending; }
	}
}
