package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.magic.ElementMixer;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * Deterministic plan of V33a's Crystal Interference ({@code RayBlendGenerator} + {@code RayBlendPuzzle}).
 *
 * <p>Every room is a jigsaw sudoku: an {@code edge = gridSize^2} square floor split into {@code edge} connected
 * regions of {@code edge} cells, each region, row and column holding each crystal colour at most once. Regions are
 * grown cell by cell with V33a's constraints (weighted start, frontier expansion that refuses to cut
 * the remaining cells into pieces it cannot absorb, a stringiness cap), a whole room is rejected when its regions are
 * all congruent, and then colours, mix cages and the initial crystals are drawn in V33a's order.
 *
 * <p>Version 0 replays the previous port for unversioned saves, including its point-hash iteration and
 * exhausted-room decisions. Version 1 returns rejected region cells before retrying, measures the growing shape
 * for compactness weights, and uses insertion order for point collections too: heavily colliding point hashes
 * otherwise cause hash-map tree bins to use object identity. The version is persisted on both structure pieces
 * and floor tiles, so new deterministic plans do not reinterpret already generated rooms.
 */
public final class RayBlendLayout {

	/** Unversioned 26.3 saves replay the original port, including its exhausted-room decisions. */
	public static final int LEGACY_VERSION = 0;
	public static final int CURRENT_VERSION = 1;
	private static final int MAX_ROOM_ATTEMPTS = 40;
	private static final int MAX_REGION_ATTEMPTS = 80;
	private static final int MAX_START_ATTEMPTS = 80;
	public static final int PADDING_LOWER = 3;
	public static final int STEP_HEIGHT = 1;
	public static final int PADDING_UPPER = 4;
	/** V33a {@code calculate}: the spacing added between rooms besides their floors. */
	public static final int EXTRA = 5 + PADDING_LOWER * 2 + PADDING_UPPER * 2;
	/** V33a room height (air box and outer wall). */
	public static final int ROOM_HEIGHT = 8;
	/** Corridor interior height and half width. */
	public static final int HALL_HEIGHT = 4, HALL_HALF_WIDTH = 3;

	private static final Map<String, RayBlendLayout> CACHE = Collections.synchronizedMap(
			new LinkedHashMap<>(16, 0.75F, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, RayBlendLayout> eldest) {
					return size() > 16;
				}
			});

	/** V33a {@code PuzzleProfile}. */
	public record Profile(int gridSize, float initialFill, boolean allowCaging, boolean markIntersections) {
		Profile(int gridSize, float initialFill) {
			this(gridSize, initialFill, false, false);
		}
	}

	/** V33a {@code getProfileList}, per structure difficulty. */
	public static Profile[] profiles(int difficulty) {
		return switch (difficulty) {
			case 1 -> new Profile[] {new Profile(2, 0.5F), new Profile(2, 0.4F, true, false), new Profile(3, 0.4F),
					new Profile(3, 0.3F, true, true), new Profile(4, 0.3F, true, false)};
			case 2 -> new Profile[] {new Profile(2, 0.5F), new Profile(2, 0.4F, true, false),
					new Profile(2, 0.25F, true, true), new Profile(3, 0.25F), new Profile(3, 0.2F, true, false),
					new Profile(4, 0.2F, true, true)};
			default -> new Profile[] {new Profile(2, 0.5F), new Profile(2, 0.4F, true, false),
					new Profile(2, 0.3F, true, true), new Profile(2, 0.2F, true, true), new Profile(3, 0.2F),
					new Profile(3, 0.15F, true, false), new Profile(3, 0.1F, true, true), new Profile(4, 0.2F, true, false),
					new Profile(4, 0.1F, true, true)};
		};
	}

	/** Receives the plan's writes in V33a's order. */
	public interface Sink {
		void air(int x, int y, int z);
		void shield(int x, int y, int z, ChromaShieldTypes type);
		void specialShield(int x, int y, int z, ChromaShieldTypes type);
		/** V33a {@code DIMGEN} Lifewater, under a blocked cell. */
		void lifewater(int x, int y, int z);
		void floor(int x, int y, int z, Room room, int cellX, int cellZ);
		void crystal(int x, int y, int z, CrystalElement color);
		/** V33a's fallback for a cell with neither colour nor block; {@code pickRandomColorForSlot} never leaves one. */
		void brick(int x, int y, int z);
		void door(int x, int y, int z);
		void password(int x, int y, int z);
		/** V33a {@code RayBlendEntrance.Cell.generateLootChest}: a stronghold-crossing chest in an entrance dead end. */
		void lootChest(int x, int y, int z, Direction facing);
	}

	private final int version;
	private final int difficulty;
	private final long seed;
	private final int originX;
	private final int originZ;
	private final int posY;
	private final int length;
	private final List<Room> rooms;
	private final Set<Integer> corridorFlips;
	private final Map<Integer, Room> doorColumns;

	private RayBlendLayout(int version, int difficulty, long seed, int x, int z, int y, int length, List<Room> rooms,
			Set<Integer> corridorFlips, Map<Integer, Room> doorColumns) {
		this.version = version;
		this.difficulty = difficulty;
		this.seed = seed;
		originX = x;
		originZ = z;
		posY = y;
		this.length = length;
		this.rooms = rooms;
		this.corridorFlips = corridorFlips;
		this.doorColumns = doorColumns;
	}

	public static RayBlendLayout cached(int difficulty, long seed, int x, int z) {
		return cached(CURRENT_VERSION, difficulty, seed, x, z);
	}

	public static RayBlendLayout cached(int version, int difficulty, long seed, int x, int z) {
		String key = version + ":" + difficulty + ":" + seed + ":" + x + ":" + z;
		RayBlendLayout hit = CACHE.get(key);
		if (hit != null)
			return hit;
		RayBlendLayout made = create(version, difficulty, seed, x, z);
		CACHE.put(key, made);
		return made;
	}

	/** V33a {@code RayBlendGenerator.calculate} on its own random stream. */
	public static RayBlendLayout create(int difficulty, long seed, int x, int z) {
		return create(CURRENT_VERSION, difficulty, seed, x, z);
	}

	/** Version is part of the persisted plan key; absent version fields mean {@link #LEGACY_VERSION}. */
	public static RayBlendLayout create(int version, int difficulty, long seed, int x, int z) {
		if (version != LEGACY_VERSION && version != CURRENT_VERSION)
			throw new IllegalArgumentException("Unsupported RayBlend layout version " + version);
		Random rand = new Random(seed);
		int y = 10 + rand.nextInt(70);
		Profile[] sz = profiles(difficulty);
		int len = 0;
		for (Profile p : sz)
			len += p.gridSize() * p.gridSize() + EXTRA;

		List<Room> rooms = new ArrayList<>();
		Set<Integer> flips = new HashSet<>();
		Map<Integer, Room> doors = new HashMap<>();
		int dx = x;
		for (int i = 0; i < sz.length; i++) {
			Profile p = sz[i];
			Builder rb = createPuzzle(rand, p, version);
			if (rb == null) // Only legacy save replay can reproduce an exhausted, omitted room.
				continue;
			int sizePre = i == 0 ? 0 : sz[i - 1].gridSize();
			dx += EXTRA + sizePre * sizePre;
			int dz = z - p.gridSize() * p.gridSize() / 2;
			Room room = rb.bake(i, rooms.size(), dx, y, dz, z);
			rooms.add(room);
			// V33a GappedRange.addGap(minX+1, maxX-1): both ends are flips, so the open span is [minX+1, maxX-1).
			flips.add(room.minX() + 1);
			flips.add(room.maxX() - 1);
			doors.put(room.maxX() + 2, room);
		}
		len += 7;
		return new RayBlendLayout(version, difficulty, seed, x, z, y, len, List.copyOf(rooms), Set.copyOf(flips),
				Map.copyOf(doors));
	}

	/** Forty bounded fresh-room attempts. Only old saves replay V33a's skipped-room behavior. */
	private static @Nullable Builder createPuzzle(Random rand, Profile p, int version) {
		int attempts = 1;
		Builder ret = new Builder(p, version);
		while (!ret.prepare(rand)) {
			ret = new Builder(p, version);
			attempts++;
			if (attempts > MAX_ROOM_ATTEMPTS) {
				if (version != LEGACY_VERSION)
					throw new IllegalStateException("RayBlend could not complete authored " + p.gridSize() + "x"
							+ p.gridSize() + " room within forty attempts");
				reika.chromaticraft.ChromatiCraft.LOGGER.error("Failed to generate a {}x{} puzzle in a reasonable time!",
						p.gridSize(), p.gridSize());
				return null;
			}
		}
		return ret;
	}

	public int version() { return version; }
	public int difficulty() { return difficulty; }
	public long seed() { return seed; }
	public int originX() { return originX; }
	public int originZ() { return originZ; }
	public int posY() { return posY; }
	public int length() { return length; }
	public List<Room> rooms() { return rooms; }

	/** V33a {@code new RayBlendLoot(this).generate(world, x+len+3, y+2, z-7)}. */
	public BlockPos lootOrigin() {
		return new BlockPos(originX + length + 3, posY + 2, originZ - 7);
	}

	/** V33a {@code RayBlendLoot.placeCore(x+6, y+2, z+7)}. */
	public BlockPos core() {
		return lootOrigin().offset(6, 2, 7);
	}

	/** V33a's two reward chests: (2, 7, 5) facing south and (2, 7, 9) facing north. */
	public List<BlockPos> rewardChests() {
		return List.of(lootOrigin().offset(2, 7, 5), lootOrigin().offset(2, 7, 9));
	}

	/** V33a {@code addBreakable}: the two 3x3 plates at the reward room's z ends. */
	public List<BlockPos> breakables() {
		List<BlockPos> out = new ArrayList<>();
		BlockPos o = lootOrigin();
		for (int i = 0; i < 3; i++)
			for (int h = 0; h < 3; h++) {
				out.add(o.offset(1 + i, 1 + h, 0));
				out.add(o.offset(1 + i, 1 + h, 14));
			}
		return out;
	}

	/** V33a {@code addDynamicStructure(new RayBlendEntrance(this), x-4, z)}. */
	public BlockPos entranceAnchor() {
		return new BlockPos(originX - 4, posY, originZ);
	}

	/** The x of the V33a corridor columns, {@code GappedRange.isInGap}: inside a room's open span. */
	public boolean isInRoomGap(int x) {
		int n = 0;
		for (int flip : corridorFlips)
			if (flip <= x)
				n++;
		return (n & 1) == 1;
	}

	public @Nullable Room roomForDoorColumn(int x) {
		return doorColumns.get(x);
	}

	/** Every write of the rooms, the corridor and the password bypass, in V33a's order. */
	public void run(Sink sink) {
		for (Room room : rooms)
			room.generate(sink);

		int x = originX, y = posY, z = originZ;
		for (int d = -2; d < length + 4; d++) {
			int odx = x + d;
			if (isInRoomGap(odx))
				continue;
			for (int i = -HALL_HALF_WIDTH; i <= HALL_HALF_WIDTH; i++) {
				for (int dh = -1; dh <= HALL_HEIGHT; dh++) {
					if (Math.abs(i) == HALL_HALF_WIDTH || dh == -1 || dh == HALL_HEIGHT)
						sink.shield(odx, y + 3 + dh, z + i, ChromaShieldTypes.STONE);
					else if (doorColumns.containsKey(odx))
						sink.door(odx, y + 3 + dh, z + i);
					else
						sink.air(odx, y + 3 + dh, z + i);
				}
			}
		}

		for (Room room : rooms)
			room.generateBypass(sink);
	}

	/** Every write's bounds, as {minX, minY, minZ, maxX, maxY, maxZ}, over {@link #run} and the reward room. */
	public int[] bounds() {
		int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
				Integer.MIN_VALUE};
		Sink probe = new Sink() {
			void add(int x, int y, int z) {
				b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
				b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
			}
			@Override public void air(int x, int y, int z) { add(x, y, z); }
			@Override public void shield(int x, int y, int z, ChromaShieldTypes type) { add(x, y, z); }
			@Override public void specialShield(int x, int y, int z, ChromaShieldTypes type) { add(x, y, z); }
			@Override public void lifewater(int x, int y, int z) { add(x, y, z); }
			@Override public void floor(int x, int y, int z, Room room, int cellX, int cellZ) { add(x, y, z); }
			@Override public void crystal(int x, int y, int z, CrystalElement color) { add(x, y, z); }
			@Override public void brick(int x, int y, int z) { add(x, y, z); }
			@Override public void door(int x, int y, int z) { add(x, y, z); }
			@Override public void password(int x, int y, int z) { add(x, y, z); }
			@Override public void lootChest(int x, int y, int z, Direction facing) { add(x, y, z); }
		};
		run(probe);
		BlockPos loot = lootOrigin();
		b[0] = Math.min(b[0], loot.getX()); b[1] = Math.min(b[1], loot.getY()); b[2] = Math.min(b[2], loot.getZ());
		b[3] = Math.max(b[3], loot.getX() + 12); b[4] = Math.max(b[4], loot.getY() + 9);
		b[5] = Math.max(b[5], loot.getZ() + 14);
		return b;
	}

	// ------------------------------------------------------------------------------------------------ Rooms

	/**
	 * One placed V33a {@code RayBlendPuzzle}. Cell arrays are indexed {@code cellX * edge + cellZ}; {@code region}
	 * is the creation index of the cell's subgrid, {@code cage} its mix cage colour or null.
	 */
	public record Room(int profileIndex, int index, int gridSize, int edge, int x, int y, int z, int[] region,
			boolean[] blocked, @Nullable CrystalElement[] color, boolean[] start, @Nullable CrystalElement[] cage,
			List<BlockPos> doors) {

		public int cell(int cx, int cz) {
			return cx * edge + cz;
		}

		public boolean contains(int cx, int cz) {
			return cx >= 0 && cz >= 0 && cx < edge && cz < edge;
		}

		/** V33a {@code generationBounds}: (x+min2, y-1, z+min2) to (x+max2, y+h, z+max2). */
		public int minX() { return x - PADDING_LOWER - PADDING_UPPER - 1; }
		public int maxX() { return x + edge + PADDING_LOWER + PADDING_UPPER; }
		public int minZ() { return z - PADDING_LOWER - PADDING_UPPER - 1; }
		public int maxZ() { return z + edge + PADDING_LOWER + PADDING_UPPER; }

		/** The floor tile of a cell (V33a {@code y+1}); its crystal sits on top. */
		public BlockPos floor(int cx, int cz) {
			return new BlockPos(x + cx, y + 1, z + cz);
		}

		/** V33a {@code bypassCoord}: room 0 only, at (minX+1, y+2, z+2). */
		public @Nullable BlockPos bypass() {
			return profileIndex == 0 ? new BlockPos(minX() + 1, y + 2, z + 2) : null;
		}

		/** V33a {@code RayBlendPuzzle.generate}. */
		void generate(Sink sink) {
			int h = ROOM_HEIGHT;
			int min1 = -PADDING_LOWER - 1;
			int max1 = edge + PADDING_LOWER;
			int min2 = -PADDING_LOWER - PADDING_UPPER - 1;
			int max2 = edge + PADDING_LOWER + PADDING_UPPER;
			float midX = x + edge / 2F;
			float midZ = z + edge / 2F;
			for (int i = min2; i < max2; i++)
				for (int k = min2; k < max2; k++)
					for (int d = 0; d < h; d++)
						sink.air(x + i, y + d, z + k);
			for (int i = min1; i < max1; i++) {
				for (int k = min1; k < max1; k++) {
					ChromaShieldTypes m = ChromaShieldTypes.CLOAK;
					if (i >= -1 && i <= edge && k >= -1 && k <= edge)
						m = ChromaShieldTypes.STONE;
					sink.shield(x + i, y - 1, z + k, ChromaShieldTypes.STONE);
					sink.shield(x + i, y, z + k, ChromaShieldTypes.STONE);
					sink.shield(x + i, y + 1, z + k, m);
				}
				for (int d = 0; d < STEP_HEIGHT; d++) {
					sink.shield(x + min1, y + 1 + d, z + i, ChromaShieldTypes.STONE);
					sink.shield(x + max1, y + 1 + d, z + i, ChromaShieldTypes.STONE);
					sink.shield(x + i, y + 1 + d, z + min1, ChromaShieldTypes.STONE);
					sink.shield(x + i, y + 1 + d, z + max1, ChromaShieldTypes.STONE);
				}
			}
			for (int i = min2; i < max2; i++) {
				for (int k = min2; k < max2; k++) {
					int dx = x + i;
					int dz = z + k;
					if (i <= min1 || i >= max1 || k <= min1 || k >= max1) {
						ChromaShieldTypes m = ChromaShieldTypes.STONE;
						if (i > min2 + 1 && i < max2 - 1 && k > min2 + 1 && k < max2 - 1)
							if (i > max1 || i < min1 || k > max1 || k < min1)
								m = ChromaShieldTypes.CLOAK;
						boolean edgeI = i == min1 || i == max1 || i == max2 - 1 || i == min2 + 1;
						boolean edgeK = k == min1 || k == max1 || k == max2 - 1 || k == min2 + 1;
						// V33a's precedence: the STONE test binds only to the last alternative.
						if ((edgeI && edgeK) || (dx == Mth.floor(midX) && edgeK)
								|| (dz == Mth.floor(midZ) && edgeI) && m == ChromaShieldTypes.STONE)
							m = ChromaShieldTypes.LIGHT;
						sink.shield(dx, y + 1 + STEP_HEIGHT, dz, m);
					}
					sink.shield(dx, y + h - 1, dz, ChromaShieldTypes.STONE);
				}
				for (int d = 0; d < h; d++) {
					sink.shield(x + min2, y - 1 + d, z + i, ChromaShieldTypes.STONE);
					sink.shield(x + max2, y - 1 + d, z + i, ChromaShieldTypes.STONE);
					sink.shield(x + i, y - 1 + d, z + min2, ChromaShieldTypes.STONE);
					// V33a writes this wall one block east of the other three.
					sink.shield(x + i + 1, y - 1 + d, z + max2, ChromaShieldTypes.STONE);
				}
			}
			for (int i = 0; i < edge; i++) {
				for (int k = 0; k < edge; k++) {
					int dx = x + i;
					int dz = z + k;
					boolean light = (i / gridSize + k / gridSize) % 2 == 0;
					sink.specialShield(dx, y, dz, light ? ChromaShieldTypes.STONE : ChromaShieldTypes.CLOAK);
					int c = cell(i, k);
					if (blocked[c]) {
						sink.lifewater(dx, y, dz);
						sink.floor(dx, y + 1, dz, this, i, k);
					}
					else if (color[c] != null) {
						if (start[c])
							sink.crystal(dx, y + 2, dz, color[c]);
						else
							sink.air(dx, y + 2, dz);
						sink.floor(dx, y + 1, dz, this, i, k);
					}
					else {
						sink.brick(dx, y + 1, dz);
					}
				}
			}
		}

		/** V33a {@code generateBypass}: the password column and its little alcove, room 0 only. */
		void generateBypass(Sink sink) {
			BlockPos b = bypass();
			if (b == null)
				return;
			int bx = b.getX(), by = b.getY(), bz = b.getZ();
			for (int i = 1; i <= 4; i++) {
				sink.shield(bx, by + i, bz - 1, ChromaShieldTypes.STONE);
				sink.shield(bx, by + i, bz + 1, ChromaShieldTypes.STONE);
				if (i == 2)
					sink.password(bx, by + i, bz);
				else
					sink.shield(bx, by + i, bz, i == 1 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE);
			}
			for (int i = 1; i <= 4; i++)
				for (int d = -3; d <= 3; d++)
					if (i == 4)
						sink.shield(bx - 1, by + i, bz + d, ChromaShieldTypes.STONE);
					else
						sink.air(bx - 1, by + i, bz + d);
		}
	}

	// ----------------------------------------------------------------------------------- V33a generation

	/** {@code java.awt.Point} with Point's own hash, so hash-ordered collections iterate as V33a's did. */
	record Cell(int x, int y) {
		@Override
		public int hashCode() {
			long bits = Double.doubleToLongBits(x);
			bits ^= Double.doubleToLongBits(y) * 31;
			return (int)bits ^ (int)(bits >> 32);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Cell c && c.x == x && c.y == y;
		}
	}

	private static final class Slot {
		final Subgrid parent;
		final int xPos;
		final int zPos;
		boolean appearsAtStart;
		boolean isBlocked;
		@Nullable CrystalElement color;

		Slot(Subgrid parent, int x, int z) {
			this.parent = parent;
			xPos = x;
			zPos = z;
		}

		Cell key() {
			return new Cell(xPos, zPos);
		}

		@Nullable Slot neighbor(Builder b, Direction dir) {
			return b.getAt(xPos + dir.getStepX(), zPos + dir.getStepZ());
		}

		/** V33a {@code GridSlot.getNeighbors}: east, west, north, south. */
		Set<Slot> neighbors(Builder b) {
			Set<Slot> set = new LinkedHashSet<>();
			for (Direction dir : new Direction[] {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
				Slot s = neighbor(b, dir);
				if (s != null)
					set.add(s);
			}
			return set;
		}
	}

	private static final class Subgrid {
		final int index;
		final Map<Cell, Slot> slots;
		final Set<Slot> unpopulated = new LinkedHashSet<>();
		private final Builder parent;

		Subgrid(Builder parent) {
			this.parent = parent;
			slots = parent.cellMap();
			parent.unfinished.add(this);
			index = parent.subIndex++;
		}

		void createSlot(int x, int z) {
			Slot gs = new Slot(this, x, z);
			slots.put(gs.key(), gs);
			unpopulated.add(gs);
			parent.createNewSlot(gs, this);
		}

		int size() {
			return slots.size();
		}

		void clear() {
			if (parent.version != LEGACY_VERSION) {
				for (Slot slot : slots.values()) {
					parent.grids.remove(slot.key(), this);
					parent.unGridded.add(slot.key());
					parent.uncaged.remove(slot);
				}
			}
			slots.clear();
			unpopulated.clear();
		}

		double stringiness() {
			return parent.calcStringiness(slots.keySet());
		}

		/** V33a {@code isCongruent}: equal after normalising, under any of the four 90-degree rotations. */
		boolean isCongruent(Subgrid g) {
			if (g == this)
				return true;
			if (g.slots.size() != slots.size())
				return false;
			List<int[]> set1 = copy(slots.keySet());
			List<int[]> set2 = copy(g.slots.keySet());
			normalize(set1);
			normalize(set2);
			if (same(set1, set2))
				return true;
			for (int i = 0; i < 3; i++) {
				rotate(set2);
				normalize(set2);
				if (same(set1, set2))
					return true;
			}
			return false;
		}

		private static List<int[]> copy(Collection<Cell> keys) {
			List<int[]> out = new ArrayList<>();
			for (Cell c : keys)
				out.add(new int[] {c.x(), c.y()});
			return out;
		}

		/** V33a {@code ReikaVectorHelper.rotateVector(.., 0, 90, 0)} then floor, rounding error included. */
		private static void rotate(List<int[]> set) {
			for (int[] p : set) {
				Vec3 r = ReikaVectorHelper.rotateVector(new Vec3(p[0], 0, p[1]), 0, 90, 0);
				p[0] = Mth.floor(r.x);
				p[1] = Mth.floor(r.z);
			}
		}

		private static void normalize(List<int[]> set) {
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			for (int[] p : set) {
				minX = Math.min(minX, p[0]);
				minY = Math.min(minY, p[1]);
			}
			for (int[] p : set) {
				p[0] -= minX;
				p[1] -= minY;
			}
		}

		private static boolean same(List<int[]> a, List<int[]> b) {
			Set<Cell> sa = new HashSet<>();
			for (int[] p : a)
				sa.add(new Cell(p[0], p[1]));
			Set<Cell> sb = new HashSet<>();
			for (int[] p : b)
				sb.add(new Cell(p[0], p[1]));
			return sa.equals(sb);
		}
	}

	private static final class Cage {
		final List<Slot> slots = new ArrayList<>();
		@Nullable CrystalElement blendedColor;

		Cage(@Nullable CrystalElement e) {
			blendedColor = e;
		}

		Set<Slot> neighbors(Builder b) {
			Set<Slot> set = new LinkedHashSet<>();
			for (Slot gs : slots)
				set.addAll(gs.neighbors(b));
			slots.forEach(set::remove);
			return set;
		}

		boolean canAbsorb(Slot gs) {
			return gs.color != null && !gs.isBlocked && ElementMixer.instance.isMixable(blendedColor, gs.color);
		}

		void addSlot(Slot gs) {
			blendedColor = ElementMixer.instance.getMix(blendedColor, gs.color);
			slots.add(gs);
		}
	}

	/** V33a {@code RayBlendPuzzle} during generation. */
	private static final class Builder {
		final int version;
		final Profile profile;
		final int gridSize;
		final int edge;
		final int total;
		final int cellsPerSubgrid;
		final float fill;

		final Map<Cell, Subgrid> grids;
		final Map<Cell, Cage> cages;
		final Set<Cell> unGridded;
		final Set<Subgrid> unfinished = new LinkedHashSet<>();
		final Set<Slot> uncaged = new LinkedHashSet<>();
		final Set<Slot> candidateStarts = new LinkedHashSet<>();
		int subIndex;

		Builder(Profile p, int version) {
			this.version = version;
			grids = cellMap();
			cages = cellMap();
			unGridded = cellSet(List.of());
			profile = p;
			gridSize = p.gridSize();
			edge = gridSize * gridSize;
			total = edge * edge;
			cellsPerSubgrid = edge;
			fill = p.initialFill();
		}

		/** Cell hashes collide heavily; HashMap tree bins otherwise use identity as their tie breaker. */
		private <T> Map<Cell, T> cellMap() {
			return version == LEGACY_VERSION ? new HashMap<>() : new LinkedHashMap<>();
		}

		private Set<Cell> cellSet(Collection<Cell> cells) {
			return version == LEGACY_VERSION ? new HashSet<>(cells) : new LinkedHashSet<>(cells);
		}

		boolean prepare(Random rand) {
			subIndex = 0;
			for (int i = 0; i < edge; i++)
				for (int k = 0; k < edge; k++)
					unGridded.add(new Cell(i, k));
			boolean flag = generateGrids(rand);
			if (flag)
				randomize(rand);
			return flag;
		}

		private boolean generateGrids(Random rand) {
			while (!unGridded.isEmpty()) {
				Subgrid sg = new Subgrid(this);
				if (!populate(sg, rand))
					return false;
				uncaged.addAll(sg.slots.values());
				unfinished.add(sg);
				for (Slot gs : sg.slots.values())
					grids.put(gs.key(), sg);
			}
			return isNotUniform(rand);
		}

		private boolean isNotUniform(Random rand) {
			Subgrid g1 = randomEntry(rand, grids.values());
			for (Subgrid g : grids.values())
				if (!g1.isCongruent(g))
					return true;
			return false;
		}

		private boolean populate(Subgrid sg, Random rand) {
			if (version == LEGACY_VERSION)
				return populateLegacy(sg, rand);
			for (int attempt = 0; attempt < MAX_REGION_ATTEMPTS; attempt++) {
				sg.clear();
				Set<Cell> starts = cellSet(unGridded);
				boolean started = false;
				for (int startAttempt = 0; startAttempt < MAX_START_ATTEMPTS && !starts.isEmpty(); startAttempt++) {
					Cell start = weightedStart(rand, starts);
					starts.remove(start);
					if (unGridded.size() == total || canExpandInto(sg, start)) {
						sg.createSlot(start.x(), start.y());
						started = true;
						break;
					}
				}
				if (!started)
					return false;
				while (sg.size() < cellsPerSubgrid) {
					Cell next = candidateNextPoint(sg, rand);
					if (next == null)
						break;
					sg.createSlot(next.x(), next.y());
				}
				if (sg.size() == cellsPerSubgrid && sg.stringiness() <= maxAllowedStringiness())
					return true;
			}
			sg.clear();
			return false;
		}

		/** Kept intact for previously generated floors and chunk-clipped pieces. */
		private boolean populateLegacy(Subgrid sg, Random rand) {
			int attempts0 = 0;
			Set<Cell> starts = cellSet(unGridded);
			while (attempts0 < MAX_START_ATTEMPTS) {
				attempts0++;
				Cell start = weightedStart(rand, starts);
				starts.remove(start);
				if (start == null)
					return false;
				boolean valid = unGridded.size() == total || canExpandInto(sg, start);
				if (valid) {
					sg.createSlot(start.x(), start.y());
					break;
				}
				else if (starts.isEmpty()) {
					return false;
				}
			}
			int attempts = 0;
			while (attempts < MAX_REGION_ATTEMPTS) {
				attempts++;
				while (sg.size() < cellsPerSubgrid) {
					Cell p = candidateNextPoint(sg, rand);
					if (p == null) {
						sg.clear();
						break;
					}
					sg.createSlot(p.x(), p.y());
				}
				if (sg.size() == cellsPerSubgrid && sg.stringiness() <= maxAllowedStringiness())
					return true;
			}
			return false;
		}

		private double maxAllowedStringiness() {
			return switch (gridSize) {
				case 1, 2 -> 1;
				case 3 -> 0.9;
				case 4 -> 0.7;
				case 5 -> 0.55;
				case 6 -> 0.4;
				default -> Math.max(0.1, 0.4 - 0.05 * (gridSize - 6));
			};
		}

		private @Nullable Cell weightedStart(Random rand, Set<Cell> starts) {
			if (starts.isEmpty())
				return null;
			Map<Cell, Double> wr = cellMap();
			for (Cell p : starts)
				wr.put(p, (double)filledNeighbors(p));
			return weighted(rand, wr);
		}

		private int filledNeighbors(Cell p) {
			Collection<Cell> c = neighbors(p);
			int ret = c.size();
			for (Cell p2 : c)
				if (unGridded.contains(p2))
					ret--;
			if (p.x() == 0)
				ret++;
			if (p.y() == 0)
				ret++;
			if (p.x() == edge - 1)
				ret++;
			if (p.y() == edge - 1)
				ret++;
			return ret;
		}

		private List<Cell> neighbors(Cell p) {
			List<Cell> c = new ArrayList<>(4);
			if (p.x() > 0)
				c.add(new Cell(p.x() - 1, p.y()));
			if (p.x() < edge - 1)
				c.add(new Cell(p.x() + 1, p.y()));
			if (p.y() > 0)
				c.add(new Cell(p.x(), p.y() - 1));
			if (p.y() < edge - 1)
				c.add(new Cell(p.x(), p.y() + 1));
			return c;
		}

		private @Nullable Cell candidateNextPoint(Subgrid sg, Random rand) {
			Set<Cell> set = cellSet(List.of());
			for (Cell p : sg.slots.keySet())
				set.addAll(neighbors(p));
			set.retainAll(unGridded);
			if (set.isEmpty())
				return null;
			Map<Cell, Double> wr = cellMap();
			for (Cell p : set) {
				// Legacy measures the frontier itself. Current plans measure the growing region plus the candidate.
				Set<Cell> points = cellSet(version == LEGACY_VERSION ? set : sg.slots.keySet());
				points.add(p);
				wr.put(p, 100 - 100 * calcStringiness(points));
			}
			while (!set.isEmpty()) {
				Cell p = weighted(rand, wr);
				if (canExpandInto(sg, p))
					return p;
				set.remove(p);
				wr.remove(p);
			}
			return null;
		}

		/** V33a: whether taking the cell would cut the remaining cells into pieces the region cannot absorb. */
		private boolean canExpandInto(Subgrid sg, Cell p) {
			List<Cell> c = neighbors(p);
			c.retainAll(unGridded);
			if (c.isEmpty())
				return true;
			Set<Cell> visited = cellSet(List.of());
			visited.add(p);
			List<LinkedList<Cell>> groups = new ArrayList<>();
			for (Cell p2 : unGridded) {
				if (visited.contains(p2))
					continue;
				LinkedList<Cell> li = new LinkedList<>();
				LinkedList<Cell> li2 = new LinkedList<>();
				groups.add(li);
				li2.add(p2);
				while (!li2.isEmpty()) {
					Cell p3 = li2.removeLast();
					if (visited.contains(p3))
						continue;
					visited.add(p3);
					li.add(p3);
					for (Cell np3 : neighbors(p3))
						if (!visited.contains(np3) && unGridded.contains(np3))
							li2.add(np3);
				}
			}
			if (groups.size() == 1)
				return true;
			groups.sort((o1, o2) -> Integer.compare(o1.size(), o2.size()));
			groups.remove(groups.size() - 1);
			List<Cell> merge = new ArrayList<>();
			for (LinkedList<Cell> li : groups)
				merge.addAll(li);
			if (cellsPerSubgrid - sg.size() > merge.size()) {
				for (Cell in : merge)
					sg.createSlot(in.x(), in.y());
				return true;
			}
			return false;
		}

		double calcStringiness(Set<Cell> points) {
			int stringy = 0;
			for (Cell p : points) {
				List<Cell> c = neighbors(p);
				c.retainAll(points);
				if (c.size() <= 2)
					stringy++;
			}
			return stringy / (double)points.size();
		}

		void createNewSlot(Slot gs, Subgrid g) {
			uncaged.add(gs);
			grids.put(gs.key(), g);
			unGridded.remove(gs.key());
		}

		private void randomize(Random rand) {
			while (!unfinished.isEmpty()) {
				Subgrid g = randomEntry(rand, unfinished);
				if (g.unpopulated.isEmpty()) {
					unfinished.remove(g);
				}
				else {
					Slot slot = randomEntry(rand, g.unpopulated);
					if (pickRandomColorForSlot(rand, slot))
						g.unpopulated.remove(slot);
				}
			}
			if (profile.allowCaging()) {
				while (!uncaged.isEmpty()) {
					Slot slot = randomEntry(rand, uncaged);
					if (slot.isBlocked || slot.color == CrystalElement.BROWN) {
						uncaged.remove(slot);
						continue;
					}
					Set<Direction> dirs = new LinkedHashSet<>(List.of(Direction.NORTH, Direction.SOUTH,
							Direction.WEST, Direction.EAST));
					boolean flag = false;
					while (!dirs.isEmpty()) {
						Direction dir = randomEntry(rand, dirs);
						dirs.remove(dir);
						Slot slot2 = slot.neighbor(this, dir);
						if (slot2 != null && uncaged.contains(slot2) && canCage(slot, slot2)) {
							uncaged.remove(slot);
							uncaged.remove(slot2);
							cage(slot, slot2);
							flag = true;
							break;
						}
					}
					if (dirs.isEmpty() && !flag)
						uncaged.remove(slot);
				}
				// V33a walks the value list, so every cage comes up once per cell it holds.
				for (Cage g : new ArrayList<>(cages.values())) {
					Set<Slot> set = g.neighbors(this);
					while (!set.isEmpty()) {
						Slot gs = randomEntry(rand, set);
						set.remove(gs);
						if (g.canAbsorb(gs) && cages.get(gs.key()) == null) {
							g.addSlot(gs);
							cages.put(gs.key(), g);
							break;
						}
					}
				}
				if (!cages.isEmpty()) {
					Cage cage = randomEntry(rand, cages.values());
					Slot gs = randomEntry(rand, cage.slots);
					gs.appearsAtStart = true; // V33a: at least one crystal in some cage per puzzle
				}
			}
			if (profile.markIntersections()) {
				// V33a getValidMix begins with "if (true) return null", so no mix point is ever kept; the draws
				// that pick each candidate still happen.
				Set<Cell> set = cellSet(grids.keySet());
				int mixPoints = 0;
				while (!set.isEmpty() && mixPoints < gridSize) {
					Cell mix = randomEntry(rand, set);
					set.remove(mix);
				}
			}
			int n = (int)(fill * total);
			while (n > 0 && !candidateStarts.isEmpty()) {
				Slot gs = randomEntry(rand, candidateStarts);
				candidateStarts.remove(gs);
				gs.appearsAtStart = true;
				n--;
			}
		}

		private void cage(Slot slot, Slot slot2) {
			Cage g = new Cage(ElementMixer.instance.getMix(slot.color, slot2.color));
			g.slots.add(slot);
			g.slots.add(slot2);
			cages.put(slot.key(), g);
			cages.put(slot2.key(), g);
		}

		private boolean canCage(Slot slot, Slot slot2) {
			return slot.color != null && slot2.color != null && ElementMixer.instance.isMixable(slot.color, slot2.color);
		}

		private boolean pickRandomColorForSlot(Random rand, Slot gs) {
			Set<CrystalElement> set = EnumSet.allOf(CrystalElement.class);
			for (Slot gs2 : gs.parent.slots.values())
				set.remove(gs2.color);
			for (int p = 0; p < edge; p++) {
				Slot g1 = getAt(gs.xPos, p);
				Slot g2 = getAt(p, gs.zPos);
				if (g1 != null && g1 != gs)
					set.remove(g1.color);
				if (g2 != null && g2 != gs)
					set.remove(g2.color);
			}
			if (set.isEmpty()) {
				gs.isBlocked = true;
			}
			else {
				gs.color = randomEntry(rand, set);
				candidateStarts.add(gs);
			}
			return true;
		}

		@Nullable Slot getAt(int x, int z) {
			if (x < 0 || z < 0 || x >= edge || z >= edge)
				return null;
			Cell key = new Cell(x, z);
			return grids.get(key).slots.get(key);
		}

		Room bake(int profileIndex, int index, int x, int y, int z, int corridorZ) {
			int[] region = new int[total];
			boolean[] blocked = new boolean[total];
			CrystalElement[] color = new CrystalElement[total];
			boolean[] start = new boolean[total];
			CrystalElement[] cage = new CrystalElement[total];
			for (int i = 0; i < edge; i++) {
				for (int k = 0; k < edge; k++) {
					Slot gs = getAt(i, k);
					int c = i * edge + k;
					region[c] = gs.parent.index;
					blocked[c] = gs.isBlocked;
					color[c] = gs.color;
					start[c] = gs.appearsAtStart;
					Cage g = cages.get(new Cell(i, k));
					cage[c] = g != null ? g.blendedColor : null;
				}
			}
			// V33a doors.put(maxX+2, ID): the corridor's interior at that column, in its write order.
			int doorX = x + edge + PADDING_LOWER + PADDING_UPPER + 2;
			List<BlockPos> doors = new ArrayList<>();
			for (int i = -HALL_HALF_WIDTH + 1; i < HALL_HALF_WIDTH; i++)
				for (int dh = 0; dh < HALL_HEIGHT; dh++)
					doors.add(new BlockPos(doorX, y + 3 + dh, corridorZ + i));
			return new Room(profileIndex, index, gridSize, edge, x, y, z, region, blocked, color, start, cage,
					List.copyOf(doors));
		}
	}

	private static <E> E randomEntry(Random rand, Collection<E> c) {
		List<E> li = c instanceof List<E> list ? list : new ArrayList<>(c);
		return li.get(rand.nextInt(li.size()));
	}

	/** DragonAPI's weighted draw, with the iteration order chosen by the saved layout version. */
	private static <V> V weighted(Random rand, Map<V, Double> wr) {
		double sum = 0;
		for (double w : wr.values())
			sum += w;
		double d = rand.nextDouble() * sum;
		double p = 0;
		V last = null;
		for (Map.Entry<V, Double> e : wr.entrySet()) {
			p += e.getValue();
			last = e.getKey();
			if (d <= p)
				return e.getKey();
		}
		// Only reachable through rounding; V33a returned null here and then threw.
		return last;
	}
}
