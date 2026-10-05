package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.Direction;

/**
 * Deterministic model of V33a's Channeled Flow (WATER) puzzle: {@code WaterPuzzleGenerator.calculate},
 * {@code WaterPath} and the lock/checkpoint construction of {@code WaterFloor}, minus the world writes.
 *
 * <p>Floor {@code i} is a square grid of rotating locks of radius {@code 2+i}. Its {@link Path} wanders from
 * the previous floor's exit to a new exit; every lock the path crosses gets the two channel arms the
 * path uses, and is then turned a random number of quarter-turns, so the player has to turn it back. A
 * few path locks become checkpoints, which must hold fluid for the floor to count as solved.
 *
 * <p>V33a drew its start/end points from DragonAPI's unseeded static random and its channel moss from
 * another; the port draws everything from the generator's seeded random, so one seed always produces
 * one puzzle (the structure piece rebuilds this layout in every chunk it writes).
 */
public final class WaterPuzzleLayout {

	/** V33a {@code WaterFloor.HEIGHT}. */
	public static final int FLOOR_HEIGHT = 7;
	/** V33a {@code Lock.SIZE}: the half-width of one lock's channel arms. */
	public static final int LOCK_SIZE = 2;
	/** Distance between neighbouring lock centres: {@code Lock.SIZE*2+1+1}. */
	public static final int LOCK_SPACING = LOCK_SIZE * 2 + 2;
	/** Vertical distance between floors: {@code WaterFloor.HEIGHT+4}. */
	public static final int FLOOR_SPACING = FLOOR_HEIGHT + 4;

	private final int difficulty;
	private final int baseY;
	private final List<Floor> floors;
	private final int coreCorner;

	private WaterPuzzleLayout(int difficulty, int baseY, List<Floor> floors, int coreCorner) {
		this.difficulty = difficulty;
		this.baseY = baseY;
		this.floors = Collections.unmodifiableList(floors);
		this.coreCorner = coreCorner;
	}

	/** V33a {@code getSize()}: 3, 4 or 6 floors. */
	public static int floorCount(int difficulty) {
		return switch (difficulty) {
			case 1 -> 3;
			case 2 -> 4;
			default -> 6;
		};
	}

	/** V33a {@code getRadius(i)}. */
	public static int radius(int floor) {
		return 2 + floor;
	}

	/** V33a {@code ReikaRandomHelper.getRandomPlusMinus(0, r)}: uniform in [-r, r]. */
	private static int plusMinus(Random random, int range) {
		return -range + random.nextInt(range * 2 + 1);
	}

	public static WaterPuzzleLayout create(int difficulty, long seed) {
		if (difficulty < 1 || difficulty > 3)
			throw new IllegalArgumentException("Water puzzle difficulty must be 1..3, got " + difficulty);
		Random random = new Random(seed);
		int baseY = 10 + random.nextInt(10);
		int n = floorCount(difficulty);
		int r = radius(0);
		int startX = plusMinus(random, r);
		int startZ = plusMinus(random, r);
		while (startX == 0 && startZ == 0) {
			startX = plusMinus(random, r);
			startZ = plusMinus(random, r);
		}
		List<Floor> floors = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			r = radius(i);
			int endX = plusMinus(random, r);
			int endZ = plusMinus(random, r);
			while (endX == startX && endZ == startZ) {
				endX = plusMinus(random, r);
				endZ = plusMinus(random, r);
			}
			Path path = new Path(new Cell(startX, startZ), new Cell(endX, endZ), r);
			path.generate(random);
			floors.add(new Floor(i, r, path, random));
			startX = endX;
			startZ = endZ;
		}
		// V33a's loot loop: the core sits in the first corner that rolls (the last corner always does).
		int coreCorner = -1;
		for (int i = 0; i < 4; i++) {
			boolean roll = i == 3 || random.nextInt(4) == 0;
			if (roll && coreCorner < 0)
				coreCorner = i;
		}
		return new WaterPuzzleLayout(difficulty, baseY, floors, coreCorner);
	}

	public int difficulty() { return difficulty; }
	public List<Floor> floors() { return floors; }
	/** Corner index 0..3 ({@code (i%2, i/2)} of the bottom floor's ring) holding the core. */
	public int coreCorner() { return coreCorner; }

	/** V33a's initial {@code posY}, before it is moved to the top floor. */
	public int baseY() { return baseY; }

	/** The top floor's base Y, which V33a stores back into {@code posY}. */
	public int topY() { return baseY + floors.size() * FLOOR_SPACING; }

	/** Base Y of floor {@code i}; floors are generated top down. */
	public int floorY(int i) { return topY() - i * FLOOR_SPACING; }

	public Floor bottom() { return floors.getLast(); }

	/** Offset of loot corner {@code i} from the structure centre, at the bottom floor's base. */
	public int[] lootCorner(int i) {
		int r = bottom().width() - 2;
		return new int[] {(i % 2) * r * 2 - r, (i / 2) * r * 2 - r};
	}

	// ----------------------------------------------------------------------------------------------

	public record Cell(int x, int z) {
		Cell step(Direction dir, int dist) {
			return new Cell(x + dir.getStepX() * dist, z + dir.getStepZ() * dist);
		}
	}

	/** One rotating lock: its path arms before rotation and how many clockwise quarter-turns it was given. */
	public static final class Lock {
		private final Cell location;
		private final Set<Direction> openEndsAtZero = EnumSet.noneOf(Direction.class);
		private int turns;

		Lock(Cell location) {
			this.location = location;
		}

		/** A free-standing lock, for building a single floor cell outside a generated layout. */
		public static Lock of(int x, int z, Set<Direction> openEndsAtZero, int turns) {
			Lock lock = new Lock(new Cell(x, z));
			lock.openEndsAtZero.addAll(openEndsAtZero);
			lock.turns = Math.floorMod(turns, 4);
			return lock;
		}

		public Cell location() { return location; }
		public Set<Direction> openEndsAtZero() { return Collections.unmodifiableSet(openEndsAtZero); }
		/** Clockwise quarter-turns away from the solved orientation, 0..3. */
		public int turns() { return turns; }

		/** V33a {@code Lock.facing}: EAST turned right {@link #turns()} times. */
		public Direction facing() {
			Direction d = Direction.EAST;
			for (int i = 0; i < turns; i++)
				d = d.getClockWise();
			return d;
		}

		/** V33a {@code Lock.isDirectionOpen}: the arm is open if the direction, turned back, was a path arm. */
		public boolean isOpen(Direction dir) {
			return isOpen(openEndsAtZero, facing(), dir);
		}

		/** Shared with the lock tile: {@code facing} encodes the turns from EAST. */
		public static boolean isOpen(Set<Direction> openEndsAtZero, Direction facing, Direction dir) {
			Direction d = dir;
			for (Direction ref = facing; ref != Direction.EAST; ref = ref.getCounterClockWise())
				d = d.getCounterClockWise();
			return openEndsAtZero.contains(d);
		}
	}

	/** One floor: the lock grid built from its path, plus the checkpoints that must carry fluid. */
	public static final class Floor {
		private final int index;
		private final int radius;
		private final Lock[][] grid;
		private final Path path;
		private final Set<Cell> checkpoints = new HashSet<>();

		Floor(int index, int radius, Path path, Random random) {
			this.index = index;
			this.radius = radius;
			this.path = path;
			int size = radius * 2 + 1;
			grid = new Lock[size][size];
			for (int i = 0; i < size; i++) {
				for (int k = 0; k < size; k++) {
					Lock lock = new Lock(new Cell(i - radius, k - radius));
					grid[i][k] = lock;
					for (Direction dir : path.lockSides[i][k]) {
						lock.openEndsAtZero.add(dir);
						while (random.nextInt(4) > 0)
							lock.turns = (lock.turns + 1) % 4;
					}
				}
			}
			int c = 0;
			for (Cell p : path.solution) {
				c++;
				if (c > 2 && random.nextInt(Math.max(2, 6 - c)) == 0 && !p.equals(path.start) && !p.equals(path.end)) {
					c = 0;
					checkpoints.add(p);
				}
			}
		}

		public int index() { return index; }
		public int radius() { return radius; }
		public int gridSize() { return radius * 2 + 1; }
		public Path path() { return path; }
		public Set<Cell> checkpoints() { return Collections.unmodifiableSet(checkpoints); }

		public Lock lock(int x, int z) {
			return grid[x + radius][z + radius];
		}

		/** V33a {@code getWidth()}: locks + gaps + centre space + outer wall space. */
		public int width() {
			return radius * (2 + LOCK_SIZE) + (radius - 1) * 2 + 6 + 4;
		}
	}

	/** V33a {@code WaterPath}: a shortest path that is then widened sideways until it fills the grid. */
	public static final class Path {
		final Cell start;
		final Cell end;
		private final int gridSize;
		private final Set<Cell> visited = new HashSet<>();
		final List<Cell> solution = new ArrayList<>();
		@SuppressWarnings("unchecked")
		final Set<Direction>[][] lockSides;

		@SuppressWarnings("unchecked")
		Path(Cell start, Cell end, int size) {
			this.start = start;
			this.end = end;
			gridSize = size;
			int s = size * 2 + 1;
			lockSides = new Set[s][s];
			for (int i = 0; i < s; i++)
				for (int k = 0; k < s; k++)
					lockSides[i][k] = EnumSet.noneOf(Direction.class);
			visited.add(start);
			visited.add(end);
		}

		public Cell start() { return start; }
		public Cell end() { return end; }
		public List<Cell> solution() { return Collections.unmodifiableList(solution); }

		void generate(Random random) {
			List<Cell> li = shortestPath();
			boolean extended = true;
			while (extended) {
				extended = false;
				List<Integer> indices = new ArrayList<>();
				for (int i = 0; i < li.size() - 1; i++)
					indices.add(i);
				while (!indices.isEmpty() && !extended) {
					int idx = indices.remove(random.nextInt(indices.size()));
					Cell p1 = li.get(idx);
					Cell p2 = li.get(idx + 1);
					Direction dir = between(p1, p2).getCounterClockWise();
					extended = tryExtend(p1, p2, idx, li, dir);
					if (!extended)
						extended = tryExtend(p1, p2, idx, li, dir.getOpposite());
				}
			}
			for (int i = 0; i < li.size() - 1; i++)
				link(li.get(i), li.get(i + 1));
			solution.addAll(li);
		}

		private boolean tryExtend(Cell p1, Cell p2, int idx, List<Cell> li, Direction dir) {
			if (!canStep(p1, dir, 1) || !canStep(p2, dir, 1))
				return false;
			int dist = 0;
			boolean more = true;
			while (more) {
				more = false;
				dist++;
				if (canStep(p1, dir, dist) && canStep(p2, dir, dist))
					more = true;
			}
			List<Cell> inject = new ArrayList<>();
			for (int i = dist - 1; i > 0; i--) {
				inject.add(0, p1.step(dir, i));
				inject.add(p2.step(dir, i));
			}
			visited.addAll(inject);
			li.addAll(idx + 1, inject);
			return true;
		}

		private List<Cell> shortestPath() {
			int dx = end.x() - start.x();
			int dz = end.z() - start.z();
			Cell p = start;
			List<Cell> li = new ArrayList<>();
			li.add(p);
			while (dx != 0) {
				p = visit(p.step(dx > 0 ? Direction.EAST : Direction.WEST, 1));
				li.add(p);
				dx = Integer.signum(dx) * (Math.abs(dx) - 1);
			}
			while (dz != 0) {
				p = visit(p.step(dz > 0 ? Direction.SOUTH : Direction.NORTH, 1));
				li.add(p);
				dz = Integer.signum(dz) * (Math.abs(dz) - 1);
			}
			return li;
		}

		private boolean canStep(Cell from, Direction dir, int dist) {
			Cell to = from.step(dir, dist);
			return Math.abs(to.x()) <= gridSize && Math.abs(to.z()) <= gridSize && !visited.contains(to);
		}

		private Cell visit(Cell to) {
			visited.add(to);
			return to;
		}

		private void link(Cell from, Cell to) {
			Direction dir = between(from, to);
			lockSides[from.x() + gridSize][from.z() + gridSize].add(dir);
			lockSides[to.x() + gridSize][to.z() + gridSize].add(dir.getOpposite());
		}

		private static Direction between(Cell a, Cell b) {
			return Direction.getNearest(b.x() - a.x(), 0, b.z() - a.z(), null);
		}
	}
}
