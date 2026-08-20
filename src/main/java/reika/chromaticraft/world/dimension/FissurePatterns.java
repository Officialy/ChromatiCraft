package reika.chromaticraft.world.dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;

/**
 * V33a {@code FissurePatternCalculator}: the five hundred fissure shapes Proxima chooses from.
 *
 * <p>A pattern is grown by the same recursive walk the generator itself uses — a wandering cut that
 * turns left or right at random and forks — and what comes out of it is a footprint: the set of XZ
 * columns the fissure occupies. Upstream computes all five hundred on a background thread as the world
 * loads; here they are built on first use from a seed, which is deterministic in the same way and does
 * not need a thread framework behind it.
 *
 * <h2>Why this stores a footprint and not blocks</h2>
 *
 * <p>V33a's pattern also records a full block map and can stamp itself into the world through
 * {@code FissurePattern.generate}. Nothing calls that: {@code WorldGenFissure} asks the pattern only for
 * {@code getDepthMap()} and then runs its <em>own</em> cut, against the real world with its own
 * protection checks, at every column the footprint names. So the footprint is the whole of what a
 * pattern is asked for, and it is what a pattern is.
 *
 * <p>The two are not merely similar — the footprint is exactly the set of XZ pairs the block map's keys
 * project onto, so deriving it from the same walk gives the identical answer with none of the machinery
 * that would only ever be thrown away.
 */
public final class FissurePatterns {

	/** V33a PATTERN_COUNT. */
	private static final int PATTERN_COUNT = 500;
	/** V33a's per-direction walk length. */
	private static final int WALK_LENGTH = 12;

	private static List<Pattern> patterns;
	private static long patternSeed;

	private FissurePatterns() {}

	/**
	 * The pattern table for a world, built once. Upstream seeds its calculator from the world seed, so
	 * two worlds get different fissures and one world gets the same set every time it loads.
	 */
	public static synchronized List<Pattern> patterns(long seed) {
		if (patterns == null || patternSeed != seed) {
			RandomSource rand = RandomSource.create(seed);
			List<Pattern> built = new ArrayList<>(PATTERN_COUNT);
			for (int i = 0; i < PATTERN_COUNT; i++)
				built.add(Pattern.calculate(rand));
			patterns = Collections.unmodifiableList(built);
			patternSeed = seed;
		}
		return patterns;
	}

	/** V33a getRandomFissure. */
	public static Pattern random(long seed, RandomSource rand) {
		List<Pattern> table = patterns(seed);
		return table.get(rand.nextInt(table.size()));
	}

	/** One fissure's footprint: the XZ columns it opens, relative to its origin. */
	public static final class Pattern {

		private final Set<Vec3i> footprint;

		private Pattern(Set<Vec3i> footprint) {
			this.footprint = Collections.unmodifiableSet(footprint);
		}

		/** V33a getDepthMap, as the set of columns it keys. */
		public Collection<Vec3i> columns() {
			return footprint;
		}

		/**
		 * V33a {@code FissurePattern.calculate}: pick a depth and a width, choose which of the four
		 * horizontal directions to walk (each is taken two times in three), and cut along each.
		 */
		private static Pattern calculate(RandomSource rand) {
			int my = 8 + rand.nextInt(16);
			double w = rand.nextDouble();

			List<Direction> dirs = new ArrayList<>();
			for (Direction dir : Direction.Plane.HORIZONTAL)
				if (rand.nextInt(3) > 0)
					dirs.add(dir);

			Set<Vec3i> footprint = new HashSet<>();
			for (Direction dir : dirs) {
				List<Direction> follow = new ArrayList<>();
				follow.add(dir);
				cut(rand, 0, 0, w, my, 0, WALK_LENGTH, follow, footprint);
			}
			return new Pattern(footprint);
		}

		/**
		 * V33a's cut, recording columns rather than blocks. The radius grows with height —
		 * {@code w*sqrt(1+(dy-my)/4)} — so a fissure is a narrow seam at its floor and wider at the top,
		 * and the walk turns ninety degrees at random and forks along whichever direction it has taken.
		 */
		private static void cut(RandomSource rand, int x, int z, double w, int my, int dist, int len,
				List<Direction> follow, Set<Vec3i> footprint) {
			// The cut runs from `my` up to twelve above the pattern's own origin, which is zero here.
			for (int dy = my; dy <= 12; dy++) {
				int r = (int)(w * Math.sqrt(1 + (dy - my) / 4D));
				for (int i = -r; i <= r; i++)
					for (int k = -r; k <= r; k++) {
						// Every cell the cut touches, plus the shell it shields, is part of the column.
						footprint.add(new Vec3i(x + i, 0, z + k));
						for (Direction dir : Direction.values())
							footprint.add(new Vec3i(x + i + dir.getStepX(), 0, z + k + dir.getStepZ()));
					}
			}

			if (dist > 1 && rand.nextInt(2) == 0) {
				Direction dir = follow.get(follow.size() - 1).getCounterClockWise();
				if (rand.nextBoolean())
					dir = dir.getOpposite();
				follow.add(dir);
				cut(rand, x + dir.getStepX(), z + dir.getStepZ(), w, my, 0, len - 1, follow, footprint);
				follow.remove(follow.size() - 1);
			}

			if (len > 0 && rand.nextInt(6 + len) > 0) {
				Direction dir = follow.get(rand.nextInt(follow.size()));
				cut(rand, x + dir.getStepX(), z + dir.getStepZ(), w, my, dist + 1, len - 1, follow,
						footprint);
			}
		}
	}
}
