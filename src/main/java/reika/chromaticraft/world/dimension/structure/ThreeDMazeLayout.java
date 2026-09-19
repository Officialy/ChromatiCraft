package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.core.Direction;

/**
 * Seeded, level-independent topology for V33a's Three-Dimensional Maze.
 *
 * <p>The old generator mixed topology calculation with writes to a chunk-spliced cache. Modern
 * structures need the same topology to be reproducible after their pieces have been serialized, so
 * this class keeps the maze as compact masks and room records. Geometry remains in NBT components;
 * {@link ThreeDMazePiece} applies these masks only to cut openings and room interiors.
 */
public final class ThreeDMazeLayout {

	public static final int CELL_SIZE = 4;

	private final int width;
	private final int height;
	private final int[] connections;
	private final int[] windows;
	private final boolean[] lighted;
	private final ArrayList<Room> rooms = new ArrayList<>();

	private ThreeDMazeLayout(int difficulty) {
		width = widthFor(difficulty);
		height = heightFor(difficulty);
		connections = new int[width * height * width];
		windows = new int[connections.length];
		lighted = new boolean[connections.length];
	}

	public static ThreeDMazeLayout create(int difficulty, long seed) {
		ThreeDMazeLayout layout = new ThreeDMazeLayout(difficulty);
		layout.calculate(new Random(seed));
		return layout;
	}

	public static int widthFor(int difficulty) {
		return switch (difficulty) {
			case 1 -> 8;
			case 2 -> 16;
			case 3 -> 24;
			default -> throw new IllegalArgumentException("Three-Dimensional Maze difficulty " + difficulty);
		};
	}

	public static int heightFor(int difficulty) {
		return switch (difficulty) {
			case 1 -> 6;
			case 2 -> 8;
			case 3 -> 12;
			default -> throw new IllegalArgumentException("Three-Dimensional Maze difficulty " + difficulty);
		};
	}

	private void calculate(Random random) {
		generatePerfectMaze(random);
		connect(width / 2, height - 1, width / 2, Direction.UP, false);
		connect(width / 2, 0, width / 2, Direction.DOWN, false);
		cutExtras(random);
		addRooms(random);
		calculateCellDecoration(random);
		calculateRoomDecoration(random);
	}

	/** Iterative randomized depth-first search, equivalent to V33a's pathCache backtracker. */
	private void generatePerfectMaze(Random random) {
		boolean[] visited = new boolean[connections.length];
		ArrayDeque<Cell> stack = new ArrayDeque<>();
		Cell start = new Cell(width / 2, height - 1, width / 2);
		stack.addLast(start);
		visited[index(start.x, start.y, start.z)] = true;
		while (!stack.isEmpty()) {
			Cell cell = stack.peekLast();
			ArrayList<Direction> available = new ArrayList<>(6);
			for (Direction direction : Direction.values()) {
				int x = cell.x + direction.getStepX();
				int y = cell.y + direction.getStepY();
				int z = cell.z + direction.getStepZ();
				if (inside(x, y, z) && !visited[index(x, y, z)]) available.add(direction);
			}
			if (available.isEmpty()) {
				stack.removeLast();
				continue;
			}
			Direction direction = available.get(random.nextInt(available.size()));
			connect(cell.x, cell.y, cell.z, direction, true);
			Cell next = new Cell(cell.x + direction.getStepX(), cell.y + direction.getStepY(),
					cell.z + direction.getStepZ());
			visited[index(next.x, next.y, next.z)] = true;
			stack.addLast(next);
		}
	}

	/** V33a cutExtras: turns the perfect maze into the considerably harder braided maze. */
	private void cutExtras(Random random) {
		int rx = 4 + random.nextInt(width * 4 / 5);
		int ry = 2 + random.nextInt(height / 2);
		int rz = 4 + random.nextInt(width * 4 / 5);
		for (int i = 0; i < rx * ry * rz; i++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			int z = random.nextInt(width);
			Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
			if (inside(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()))
				connect(x, y, z, direction, true);
		}
	}

	private void addRooms(Random random) {
		boolean[] roomSpace = new boolean[connections.length];
		int count = (int)(Math.sqrt(width * height * width) / 2D);
		Cell start = new Cell(width / 2, height - 1, width / 2);
		for (int room = 0; room < count; room++) {
			int radius = random.nextInt(6) == 0 ? 2 : 1;
			Cell center = randomInteriorCell(random, radius);
			int minimumDistance = 4 + radius;
			int tries = 0;
			// V33a accidentally gives start and end the same top-centre coordinate. One distance
			// check therefore preserves both effective gates without disguising the source quirk.
			while (taxicab(center, start) < minimumDistance
					&& tries++ < 50)
				center = randomInteriorCell(random, radius);
			boolean overlaps;
			do {
				overlaps = overlapsRoom(roomSpace, center, radius + 1);
				if (overlaps) center = randomInteriorCell(random, radius);
				tries++;
			} while (overlaps && tries < 50);
			if (tries >= 50) continue;
			for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
				roomSpace[index(center.x + dx, center.y, center.z + dz)] = true;
			rooms.add(new Room(center.x, center.y, center.z, radius, false, false, false,
					Bonus.RAW_CRYSTAL, 0, 0, 0, 0));
		}
	}

	private void calculateCellDecoration(Random random) {
		for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < width; z++) {
			int index = index(x, y, z);
			lighted[index] = x % 3 == 0 && y % (3 / 2) == 0 && z % 3 == 0;
			if (x == 0 || y == 0 || z == 0 || x == width - 1 || y == height - 1 || z == width - 1)
				continue;
			if (random.nextInt(10) == 0) {
				windows[index] |= bit(Direction.values()[random.nextInt(6)]);
				while (random.nextInt(10) == 0)
					windows[index] |= bit(Direction.values()[random.nextInt(6)]);
			}
		}
	}

	/** Consume the shared Random in the same phase in which V33a generated its rooms. */
	private void calculateRoomDecoration(Random random) {
		for (int i = 0; i < rooms.size(); i++) {
			Room room = rooms.get(i);
			boolean chest = random.nextInt(Math.max(1, 3 - room.radius)) == 0;
			boolean desert = false;
			Bonus bonus = Bonus.RAW_CRYSTAL;
			int bonusColor = 0;
			int bonusCount = 0;
			int facing = 0;
			long chestSeed = 0;
			if (chest) {
				desert = !random.nextBoolean();
				if (random.nextBoolean()) {
					bonus = Bonus.BOOSTED_SHARD;
					bonusColor = random.nextInt(16);
				}
				int multiplier = 2;
				if (room.radius == 2 && random.nextInt(4) == 0) {
					bonus = Bonus.COMPLEX_INGOT;
					multiplier = 1;
				}
				bonusCount = 1 + multiplier * random.nextInt(2 * room.radius);
				facing = random.nextInt(4);
				// V33a supplied an extra-loot-roll count before its guaranteed bonus stack.
				random.nextInt(1 + room.radius);
				chestSeed = random.nextLong();
			}
			// The Chroma ring closes the centre floor. V33a only selects it when that floor was not
			// already cut open by a downward maze connection; otherwise the glowstone form is used.
			boolean chromaRing = room.radius == 2 && random.nextBoolean()
					&& (connections[index(room.x, room.y, room.z)] & bit(Direction.DOWN)) == 0;
			rooms.set(i, new Room(room.x, room.y, room.z, room.radius, chest, desert,
					chromaRing, bonus, bonusColor, bonusCount, facing, chestSeed));
		}
	}

	private Cell randomInteriorCell(Random random, int radius) {
		return new Cell(radius + random.nextInt(width - radius * 2 - 1),
				radius + random.nextInt(height - radius * 2 - 1),
				radius + random.nextInt(width - radius * 2 - 1));
	}

	private boolean overlapsRoom(boolean[] occupied, Cell center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
			int x = center.x + dx;
			int z = center.z + dz;
			if (inside(x, center.y, z) && occupied[index(x, center.y, z)]) return true;
		}
		return false;
	}

	private static int taxicab(Cell a, Cell b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
	}

	private void connect(int x, int y, int z, Direction direction, boolean reciprocal) {
		connections[index(x, y, z)] |= bit(direction);
		if (reciprocal) {
			int dx = x + direction.getStepX();
			int dy = y + direction.getStepY();
			int dz = z + direction.getStepZ();
			if (inside(dx, dy, dz)) connections[index(dx, dy, dz)] |= bit(direction.getOpposite());
		}
	}

	private boolean inside(int x, int y, int z) {
		return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < width;
	}

	private int index(int x, int y, int z) {
		return (x * height + y) * width + z;
	}

	private static int bit(Direction direction) {
		return 1 << direction.ordinal();
	}

	public int width() { return width; }
	public int height() { return height; }
	public int connections(int x, int y, int z) { return connections[index(x, y, z)]; }
	public int windows(int x, int y, int z) { return windows[index(x, y, z)]; }
	public boolean lighted(int x, int y, int z) { return lighted[index(x, y, z)]; }
	public List<Room> rooms() { return Collections.unmodifiableList(rooms); }

	public enum Bonus { BOOSTED_SHARD, RAW_CRYSTAL, COMPLEX_INGOT }

	public record Room(int x, int y, int z, int radius, boolean chest, boolean desertLoot,
			boolean chromaRing, Bonus bonus, int bonusColor, int bonusCount, int facing,
			long chestSeed) {}

	private record Cell(int x, int y, int z) {}
}
