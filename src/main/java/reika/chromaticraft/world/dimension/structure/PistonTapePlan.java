package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.dragonapi.instantiable.RGBColorData;

/**
 * Deterministic port of V33a's Filter Cycle (PISTONTAPE) generator: {@code PistonTapeGenerator.calculate} and
 * every piece it builds ({@code TapeStage}, {@code PistonTapeEntryArea}, {@code TapeArea}, {@code PistonTapeLoop},
 * {@code PistonTapeSlice}, {@code PistonTapeAccessHall}, {@code DoorSection}, {@code PistonTapeStaircase},
 * {@code PistonTapeLootRoom}, {@code PistonTapeEntrance}).
 *
 * <p>The generation sequence is written once, against a {@link Sink}. {@link #create} runs it against a sink that
 * writes nothing, which records every runtime position (pistons, emitters, read heads, door targets, door
 * panels, controllers) and the breakable cells; the structure piece runs the identical sequence again with a
 * sink that writes into its chunk. Both runs start from the same seed, so the random draws match exactly.
 *
 * <p>V33a used the generator's seeded random for the tape and the stairs, but {@code world.rand} for the
 * entrance and for the loot crystals' growth (whose callbacks also read the world). Those two use a
 * per-position seed here, so every chunk of the structure agrees on them.
 */
public final class PistonTapePlan {

	public static final Direction DIRECTION = Direction.EAST;
	/** V33a {@code PistonTapeEntryArea.DEPTH}. */
	static final int ENTRY_DEPTH = 3;
	/** V33a {@code PistonTapeAccessHall} dimensions. */
	static final int HALL_WIDTH = 9, HALL_HEIGHT = 4, HALL_DEPTH = 3;
	/** V33a {@code DoorSection} dimensions. */
	static final int DOOR_WIDTH = 10, DOOR_HEIGHT = 4;

	/** Everything the plan writes, in V33a's order. Positions are absolute. */
	public interface Sink {
		void block(int x, int y, int z, Kind kind);
		void shield(int x, int y, int z, ChromaShieldTypes type);
		void specialShield(int x, int y, int z, ChromaShieldTypes type);
		void piston(int x, int y, int z, Direction push);
		void bit(int x, int y, int z, int meta);
		void emitter(int x, int y, int z, Direction facing);
		void readHead(int x, int y, int z, Direction facing, int stage, int bus, int busWidth);
		void doorTarget(int x, int y, int z, Direction facing, int stage, int door, int bus, int busWidth,
				RGBColorData color, BlockPos doorPanel);
		void controller(int x, int y, int z, int type, int stage, Direction facing);
		void password(int x, int y, int z);
		void doorPanel(int x, int y, int z);
		void shiftLock(int x, int y, int z, BlockShiftLock.Passability passability);
		void lootChest(int x, int y, int z, Direction facing);
		void encrusted(int x, int y, int z);
		void core(int x, int y, int z);
		void hover(int x, int y, int z);
	}

	/** Plain vanilla blocks the plan places. */
	public enum Kind { AIR, STONE_BRICK_SLAB, STONE_BRICKS }


	private final int difficulty;
	private final long seed;
	private final int originX;
	private final int originZ;
	private int posY;
	private final List<Stage> stages = new ArrayList<>();
	private final Set<BlockPos> breakables = new HashSet<>();
	private BlockPos core;
	private BlockPos entranceStart;
	private boolean recording;
	private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

	private PistonTapePlan(int difficulty, long seed, int x, int z) {
		this.difficulty = difficulty;
		this.seed = seed;
		this.originX = x;
		this.originZ = z;
	}

	public static PistonTapePlan create(int difficulty, long seed, int x, int z) {
		if (difficulty < 1 || difficulty > 3)
			throw new IllegalArgumentException("Piston tape difficulty must be 1..3, got " + difficulty);
		PistonTapePlan plan = new PistonTapePlan(difficulty, seed, x, z);
		plan.recording = true;
		plan.run(plan.boundsSink(), Integer.MIN_VALUE);
		plan.recording = false;
		return plan;
	}

	public int difficulty() { return difficulty; }
	public long seed() { return seed; }
	public int originX() { return originX; }
	public int originZ() { return originZ; }
	public int posY() { return posY; }
	public List<Stage> stages() { return Collections.unmodifiableList(stages); }
	public Set<BlockPos> breakables() { return Collections.unmodifiableSet(breakables); }
	public BlockPos core() { return core; }
	/** {@code stages[0].getEntrance().offset(DIRECTION.getOpposite(), 2)}: the dynamic entrance's column. */
	public BlockPos entranceStart() { return entranceStart; }

	/** V33a {@code getTapes()}: (bus width, door count) per stage. */
	static int[][] tapes(int difficulty) {
		return switch (difficulty) {
			case 1 -> new int[][] {{2, 5}, {2, 8}, {3, 6}};
			case 2 -> new int[][] {{2, 6}, {3, 8}, {3, 10}};
			default -> new int[][] {{2, 8}, {3, 9}, {3, 12}, {4, 12}};
		};
	}

	/**
	 * Replays V33a {@code calculate} into {@code sink}. {@code surfaceY} is the entrance column's first free height,
	 * or {@link Integer#MIN_VALUE} for the dry run, which skips the post-generation entrance.
	 */
	public void run(Sink sink, int surfaceY) {
		Random rand = new Random(seed);
		boolean record = recording;
		if (record) {
			stages.clear();
			breakables.clear();
		}
		int x = originX;
		int z = originZ;
		int y = 60 + rand.nextInt(30);
		posY = y;
		int[][] arr = tapes(difficulty);
		List<Stage> built = new ArrayList<>();
		Direction dir = DIRECTION;
		for (int i = 0; i < arr.length; i++) {
			Stage s = new Stage(i, arr[i][0], arr[i][1], dir, rand);
			Direction dir2 = dir.getCounterClockWise();
			int dx = x + dir2.getStepX() * 12;
			int dz = z + dir2.getStepZ() * 12;
			s.generate(sink, dx, y, dz);
			built.add(s);
			x += dir.getStepX() * s.getDirectionLength();
			z += dir.getStepZ() * s.getDirectionLength();
			y -= Math.max(6, s.height);
			dir = dir.getOpposite();
		}
		for (int i = 0; i < built.size() - 1; i++)
			staircase(sink, built.get(i), built.get(i + 1), rand);
		Stage last = built.getLast();
		BlockPos end = last.exit;
		lootRoom(sink, dir.getOpposite(), end.getX(), end.getY(), end.getZ());
		if (record) {
			stages.addAll(built);
			entranceStart = built.getFirst().entrance.relative(DIRECTION.getOpposite(), 2);
		}
		if (surfaceY != Integer.MIN_VALUE)
			entrance(sink, entranceStart, DIRECTION, surfaceY);
	}

	// ============================================================================================ stage

	/** V33a {@code TapeStage} with its {@code TapeArea}, {@code PistonTapeLoop} and {@code DoorSection}s. */
	public final class Stage {
		public final int index;
		public final int doorCount;
		public final int bitsPerDoor;
		public final Direction mainDirection;
		/** The loop's facing: {@code getRightBy90(mainDirection)}. */
		public final Direction facing;
		public final int height;
		final LoopDimensions dimensions;
		private final Slice[] slices;
		private final List<DoorKey> doors = new ArrayList<>();
		private BlockPos entrance;
		private BlockPos exit;
		private BlockPos playback;

		Stage(int idx, int bus, int n, Direction dir, Random rand) {
			index = idx;
			doorCount = n;
			bitsPerDoor = bus;
			mainDirection = dir;
			facing = dir.getClockWise();
			dimensions = LoopDimensions.createFor(n);
			slices = new Slice[bus];
			for (int i = 0; i < bus; i++)
				slices[i] = new Slice(this, i);
			for (Slice s : slices)
				s.randomizeSolution(rand);
			height = dimensions.totalHeight + 3;
			for (int i = 0; i < n; i++) {
				RGBColorData[] colors = new RGBColorData[bus];
				for (int d = 0; d < bus; d++)
					colors[d] = slices[d].netColors[i];
				doors.add(new DoorKey(i, colors));
			}
		}

		public BlockPos entrance() { return entrance; }
		public BlockPos exit() { return exit; }
		/** The playback controller, which keeps the tape's runtime state for the whole stage. */
		public BlockPos playback() { return playback; }
		public List<DoorKey> doors() { return Collections.unmodifiableList(doors); }
		public Slice slice(int i) { return slices[i]; }
		public int sliceCount() { return slices.length; }
		/** V33a {@code getTotalLength}: the tape's bit length. */
		public int totalLength() { return dimensions.bitLength; }

		/** V33a {@code getDirectionLength}. */
		int getDirectionLength() {
			int base = 1 + 3 + ENTRY_DEPTH + HALL_DEPTH + (bitsPerDoor + 1);
			for (int i = 0; i < doorCount; i++)
				base += doorLength() + 1;
			return base;
		}

		int doorLength() {
			return Math.max(3, bitsPerDoor + 2);
		}

		/** V33a {@code getHallSplinePoints(i)}. */
		public List<BlockPos> hallSplinePoints(int bus) {
			List<BlockPos> li = new ArrayList<>(3);
			BlockPos c = slices[bus].target.relative(facing, -4);
			li.add(c);
			c = c.relative(mainDirection, 3);
			li.add(c);
			c = c.relative(facing, -5);
			li.add(c);
			return li;
		}

		void generate(Sink w, int x, int y, int z) {
			int dx = x;
			int dz = z;
			entrance = new BlockPos(x - facing.getStepX() * 3 - mainDirection.getStepX(), y + 2,
					z - facing.getStepZ() * 3 - mainDirection.getStepZ());
			entryArea(w, dx - facing.getStepX(), y, dz - facing.getStepZ());
			dx += (ENTRY_DEPTH + 1) * mainDirection.getStepX();
			dz += (ENTRY_DEPTH + 1) * mainDirection.getStepZ();
			tapeArea(w, dx - facing.getStepX() * 6, y, dz - facing.getStepZ() * 6);
			dx += (2 + bitsPerDoor) * mainDirection.getStepX();
			dz += (2 + bitsPerDoor) * mainDirection.getStepZ();
			accessHall(w, dx - facing.getStepX(), y, dz - facing.getStepZ());
			dx += (HALL_DEPTH + 2 + bitsPerDoor / 2) * mainDirection.getStepX();
			dz += (HALL_DEPTH + 2 + bitsPerDoor / 2) * mainDirection.getStepZ();
			for (DoorKey door : doors) {
				// V33a passes z, not dz; the main direction is always east or west, so they agree.
				doorSection(w, door, dx, y, z);
				dx += (doorLength() + 1) * mainDirection.getStepX();
				dz += (doorLength() + 1) * mainDirection.getStepZ();
			}
			exit = new BlockPos(dx - facing.getStepX() * 3, y + 2, dz - facing.getStepZ() * 3);
		}

		// ------------------------------------------------------------------- PistonTapeEntryArea

		private void entryArea(Sink w, int x, int y, int z) {
			Direction dir = mainDirection;
			Direction left = facing.getOpposite();
			int width = 7 + dimensions.totalDepth;
			int h = 2 + dimensions.totalHeight;
			for (int i = 0; i <= ENTRY_DEPTH; i++) {
				for (int k = 0; k <= width; k++) {
					for (int j = 0; j <= h; j++) {
						int dx = x + i * dir.getStepX() + k * left.getStepX();
						int dz = z + i * dir.getStepZ() + k * left.getStepZ();
						int dy = y + j;
						boolean air = k > 1 && k < width - 1 && i > 0 && i < ENTRY_DEPTH && j > 1 && j < h;
						air |= (i == 0 || i == ENTRY_DEPTH) && k >= 1 && k <= 3 && j >= 1 && j <= 3;
						air |= k >= 1 && k <= 3 && j >= 1 && j < h && i > 0 && i < ENTRY_DEPTH;
						air |= i == ENTRY_DEPTH && k >= 5 && k < width - 1 && j > 1 && j < h;
						air |= k > 0 && k <= 4 && j >= 2 && j < h && i == ENTRY_DEPTH;
						ChromaShieldTypes m = ChromaShieldTypes.STONE;
						if (i == 2) {
							if (k == 0 && j == Math.max(4, 4 + (h - 4) / 2))
								m = ChromaShieldTypes.LIGHT;
							else if (k == width - 1 && j == 3)
								m = ChromaShieldTypes.LIGHT;
							else if (k == 4 && j == h)
								m = ChromaShieldTypes.LIGHT;
						}
						Kind plain = air ? Kind.AIR : null;
						if (i > 0 && i <= ENTRY_DEPTH && j == 1) {
							if (k == 4)
								plain = Kind.STONE_BRICK_SLAB;
							else if (k == 5)
								plain = Kind.STONE_BRICKS;
						}
						boolean password = false;
						if (index == 0 && j == 3) {
							if (i == 2 && k == width - 1)
								password = true;
							else if (i == 0 && k == width - 3) {
								plain = null;
								m = ChromaShieldTypes.LIGHT;
							}
						}
						if (password)
							w.password(dx, dy, dz);
						else if (plain != null)
							w.block(dx, dy, dz, plain);
						else
							w.shield(dx, dy, dz, m);
					}
				}
			}
			for (int k = 0; k <= 4; k++) {
				for (int j = 4; j <= h; j++) {
					int i = ENTRY_DEPTH + 1;
					w.shield(x + i * dir.getStepX() + k * left.getStepX(), y + j,
							z + i * dir.getStepZ() + k * left.getStepZ(), ChromaShieldTypes.STONE);
				}
			}
			int dx = x + mainDirection.getStepX() * 3;
			int dz = z + mainDirection.getStepZ() * 3;
			int dx2 = dx - mainDirection.getStepX() * 2;
			int dz2 = dz - mainDirection.getStepZ() * 2;
			Direction controllerFacing = mainDirection.getCounterClockWise();
			playback = new BlockPos(dx, y + 3, dz);
			w.controller(dx, y + 3, dz, 0, index, controllerFacing);
			w.controller(dx, y + 2, dz, 1, index, controllerFacing);
			w.controller(dx, y + 1, dz, 2, index, controllerFacing);
			w.controller(dx2, y + 2, dz2, 3, index, controllerFacing);
		}

		// ------------------------------------------------------------------------------ TapeArea

		private void tapeArea(Sink w, int x, int y, int z) {
			int wd = bitsPerDoor + 1;
			int h = dimensions.totalHeight + 2;
			int l = dimensions.totalDepth + 1;
			Direction main = mainDirection;
			int fx = facing.getStepX(), fz = facing.getStepZ(), mx = main.getStepX(), mz = main.getStepZ();
			for (int i = 0; i < wd; i++) {
				w.shield(x - fx * 4 + mx * i, y + 5, z - fz * 4 + mz * i, ChromaShieldTypes.STONE);
				for (int d = 0; d <= 2; d++)
					w.shield(x - fx * 6 + mx * i, y + d, z - fz * 6 + mz * i, ChromaShieldTypes.STONE);
				for (int dh = 1; dh <= 3; dh++) {
					for (int d = 2; d <= 4; d++)
						w.block(x + fx * d + mx * i, y + dh, z + fz * d + mz * i, Kind.AIR);
					w.specialShield(x + fx + mx * i, y + dh, z + fz + mz * i, ChromaShieldTypes.GLASS);
					w.specialShield(x + fx * 5 + mx * i, y + dh, z + fz * 5 + mz * i, ChromaShieldTypes.GLASS);
					w.block(x + fx * 6 + mx * i, y + dh, z + fz * 6 + mz * i, Kind.AIR);
					w.shield(x + fx * 7 + mx * i, y + dh, z + fz * 7 + mz * i, ChromaShieldTypes.CLOAK);
				}
				for (int d = 1; d <= 4; d++) {
					w.shield(x + fx * d + mx * i, y, z + fz * d + mz * i, ChromaShieldTypes.COBBLE);
					w.shield(x + fx * d + mx * i, y + 4, z + fz * d + mz * i, ChromaShieldTypes.STONE);
				}
				w.shield(x + fx + mx * i, y, z + fz + mz * i, ChromaShieldTypes.STONE);
				w.shield(x + fx * 5 + mx * i, y, z + fz * 5 + mz * i, ChromaShieldTypes.STONE);
				w.shield(x + fx * 6 + mx * i, y, z + fz * 6 + mz * i, ChromaShieldTypes.CLOAK);
				w.shield(x + fx * 5 + mx * i, y + 4, z + fz * 5 + mz * i, ChromaShieldTypes.STONE);
				w.shield(x + fx * 6 + mx * i, y + 4, z + fz * 6 + mz * i, ChromaShieldTypes.CLOAK);
				w.shield(x + fx * 7 + mx * i, y + 4, z + fz * 7 + mz * i, ChromaShieldTypes.CLOAK);
			}
			for (int i = -l; i <= 7; i++) {
				int dh = i > 1 ? 4 : h;
				ChromaShieldTypes m = i > 5 ? ChromaShieldTypes.CLOAK : ChromaShieldTypes.STONE;
				for (int d = 0; d <= dh; d++) {
					w.shield(x + fx * i, y + d, z + fz * i, m);
					w.shield(x + fx * i + mx * wd, y + d, z + fz * i + mz * wd, m);
				}
			}
			for (int i = 2; i <= 4; i++) {
				for (int dh = 1; dh <= 3; dh++) {
					w.block(x + fx * i, y + dh, z + fz * i, Kind.AIR);
					w.block(x + fx * i + mx * wd, y + dh, z + fz * i + mz * wd, Kind.AIR);
				}
			}
			for (int i = -3; i <= 0; i++)
				for (int dh = 1; dh <= 3; dh++)
					w.specialShield(x + fx * i + mx * wd, y + dh, z + fz * i + mz * wd, ChromaShieldTypes.GLASS);
			int t = -l + 1;
			int th = dimensions.totalHeight + 1;
			for (int i = t; i <= 0; i++) {
				for (int dh = 1; dh <= th; dh++) {
					if (i == t || i == 0 || dh == 1 || dh == th)
						w.shield(x + fx * i, y + dh, z + fz * i, ChromaShieldTypes.COBBLE);
					else
						w.block(x + fx * i, y + dh, z + fz * i, Kind.AIR);
				}
			}
			for (int d = 2; d <= 4; d++)
				w.shield(x + fx * d + mx * wd, y, z + fz * d + mz * wd, ChromaShieldTypes.COBBLE);
			int h2 = dimensions.totalHeight + 1;
			for (int i = 1; i <= 3; i++)
				for (int dh = 4; dh <= h2; dh++)
					w.shield(x + fx * 2 + mx * i, y + dh, z + fz * 2 + mz * i, ChromaShieldTypes.STONE);
			// PistonTapeLoop.generate at (x + main, y + 1, z + main), slices stepping getLeftBy90(facing).
			Direction step = facing.getCounterClockWise();
			for (int i = 0; i < slices.length; i++)
				slices[i].generate(w, x + mx + step.getStepX() * i, y + 1, z + mz + step.getStepZ() * i);
		}

		// -------------------------------------------------------------------- PistonTapeAccessHall

		private void accessHall(Sink w, int x, int y, int z) {
			Direction dir = mainDirection;
			Direction left = facing.getOpposite();
			int dX = dir.getStepX(), dZ = dir.getStepZ(), lx = left.getStepX(), lz = left.getStepZ();
			for (int i = 0; i <= HALL_DEPTH; i++) {
				for (int l = 0; l <= HALL_WIDTH; l++) {
					int dx = x + dX * i + lx * l;
					int dz = z + dZ * i + lz * l;
					for (int u = 0; u <= HALL_HEIGHT; u++)
						w.block(dx, y + u, dz, Kind.AIR);
					w.shield(dx, y, dz, l == 0 || l == HALL_WIDTH ? ChromaShieldTypes.STONE : ChromaShieldTypes.COBBLE);
					w.shield(dx, y + HALL_HEIGHT, dz, ChromaShieldTypes.STONE);
				}
				for (int u = 1; u <= HALL_HEIGHT; u++) {
					w.shield(x + dX * i, y + u, z + dZ * i, ChromaShieldTypes.STONE);
					w.shield(x + dX * i + lx * HALL_WIDTH, y + u, z + dZ * i + lz * HALL_WIDTH, ChromaShieldTypes.STONE);
				}
			}
			for (int u = 1; u <= HALL_HEIGHT; u++) {
				w.shield(x + dX * HALL_DEPTH + lx * 4, y + u, z + dZ * HALL_DEPTH + lz * 4, ChromaShieldTypes.STONE);
				w.shield(x + dX * HALL_DEPTH + lx * 5, y + u, z + dZ * HALL_DEPTH + lz * 5, ChromaShieldTypes.STONE);
			}
			w.shield(x + dX, y + 2, z + dZ, ChromaShieldTypes.LIGHT);
			w.shield(x + dX + lx * HALL_WIDTH, y + 2, z + dZ + lz * HALL_WIDTH, ChromaShieldTypes.LIGHT);
			int h = HALL_HEIGHT;
			int d = 1 + bitsPerDoor / 2 + 1;
			for (int i = 1; i <= d; i++) {
				for (int l = 0; l <= HALL_WIDTH; l++) {
					for (int dh = 0; dh <= h; dh++) {
						int ax = x + lx * l + dX * (i + HALL_DEPTH);
						int az = z + lz * l + dZ * (i + HALL_DEPTH);
						if (dh != 0 && dh != h && l != 0 && l != HALL_WIDTH && l != 4)
							w.block(ax, y + dh, az, Kind.AIR);
						else
							w.shield(ax, y + dh, az, ChromaShieldTypes.STONE);
					}
				}
			}
		}

		// ---------------------------------------------------------------------------- DoorSection

		private void doorSection(Sink w, DoorKey door, int x, int y, int z) {
			Direction tunnel = mainDirection;
			Direction left = tunnel.getCounterClockWise();
			int tx = tunnel.getStepX(), tz = tunnel.getStepZ(), lx = left.getStepX(), lz = left.getStepZ();
			boolean closed = door.index == doorCount - 1;
			int len = doorLength();
			for (int d = 0; d <= len; d++) {
				for (int i = 0; i <= DOOR_WIDTH; i++) {
					for (int h = 0; h < DOOR_HEIGHT; h++) {
						if (closed && d == len && i > 4)
							w.shield(x + i * lx + d * tx, y + h, z + i * lz + d * tz, ChromaShieldTypes.STONE);
						else
							w.block(x + i * lx + d * tx, y + h, z + i * lz + d * tz, Kind.AIR);
					}
				}
				for (int h = 1; h < DOOR_HEIGHT; h++)
					w.shield(x + lx * DOOR_WIDTH + d * tx, y + h, z + lz * DOOR_WIDTH + d * tz, ChromaShieldTypes.STONE);
			}
			for (int d = 0; d <= len; d++) {
				for (int i = 0; i <= DOOR_WIDTH; i++) {
					w.shield(x + i * lx + d * tx, y, z + i * lz + d * tz, ChromaShieldTypes.STONE);
					w.shield(x + i * lx + d * tx, y + DOOR_HEIGHT, z + i * lz + d * tz, ChromaShieldTypes.STONE);
				}
				for (int h = 1; h < DOOR_HEIGHT; h++) {
					w.shield(x + d * tx, y + h, z + d * tz, ChromaShieldTypes.STONE);
					w.shield(x + d * tx + lx, y + h, z + d * tz + lz, ChromaShieldTypes.STONE);
					w.shield(x + d * tx + 5 * lx, y + h, z + d * tz + 5 * lz, ChromaShieldTypes.STONE);
				}
			}
			for (int hw = 2; hw <= 4; hw++) {
				for (int i = 1; i < DOOR_HEIGHT; i++) {
					w.doorPanel(x + lx * hw, y + i, z + lz * hw);
					door.panels.add(new BlockPos(x + lx * hw, y + i, z + lz * hw));
				}
			}
			BlockShiftLock.Passability escape = BlockShiftLock.Passability.values()[escapeMeta(tunnel)];
			for (int i = 1; i < DOOR_HEIGHT; i++) {
				int a = bitsPerDoor > 2 ? 1 : 0;
				ChromaShieldTypes m = i == 2 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE;
				w.shield(x + lx * 6 + (a + 1) * tx, y + i, z + lz * 6 + (a + 1) * tz, m);
				w.shield(x + lx * 6 + (a + 3) * tx, y + i, z + lz * 6 + (a + 3) * tz, m);
				w.shiftLock(x + lx * 5 + (a + 2) * tx, y + i, z + lz * 5 + (a + 2) * tz, escape);
			}
			w.shield(x + lx + 2 * tx, y, z + lz + 2 * tz, ChromaShieldTypes.LIGHT);
			w.lootChest(x + lx + 2 * tx, y + 1, z + lz + 2 * tz, left);
			w.shield(x + lx + 2 * tx, y + 2, z + lz + 2 * tz, ChromaShieldTypes.CRACK);
			BlockPos panel = new BlockPos(x + lx * 3, y + 1, z + lz * 3);
			door.panel = panel;
			Direction targetFacing = tunnel.getClockWise();
			for (int i = 0; i < bitsPerDoor; i++) {
				int ax = x + lx * 5 + (i - bitsPerDoor / 2) * tx;
				int az = z + lz * 5 + (i - bitsPerDoor / 2) * tz;
				door.targets[i] = new BlockPos(ax, y + 2, az);
				w.doorTarget(ax, y + 2, az, targetFacing, index, door.index, i, bitsPerDoor, door.colors[i], panel);
			}
		}

		/** V33a {@code getEscapeMeta}: the shift-lock passability for the escape slot. */
		private static int escapeMeta(Direction tunnel) {
			return switch (tunnel) {
				case WEST -> 9;
				case EAST -> 7;
				case NORTH -> 3;
				case SOUTH -> 5;
				default -> 1;
			};
		}
	}

	// ============================================================================================ slice

	/** V33a {@code PistonTapeSlice}: one bit-wide ring of tape blocks with its four pistons. */
	public final class Slice {
		private final Stage stage;
		public final int busIndex;
		public final int bitCount;
		private final LoopDimensions dimensions;
		private final RGBColorData[] bitColors;
		final RGBColorData[] netColors;
		private final Map<Direction, BlockPos> pistons = new EnumMap<>(Direction.class);
		private BlockPos emitter;
		private BlockPos target;
		private BlockPos[] positions;

		Slice(Stage stage, int idx) {
			this.stage = stage;
			busIndex = idx;
			dimensions = stage.dimensions;
			bitCount = dimensions.bitLength;
			bitColors = new RGBColorData[bitCount];
			netColors = new RGBColorData[bitCount];
		}

		public BlockPos emitter() { return emitter; }
		public BlockPos target() { return target; }
		/** The piston that pushes the given way. */
		public BlockPos piston(Direction push) { return pistons.get(push); }
		/** V33a {@code getNthBitBlock}: tape position {@code n} with the read head zeroed. */
		public BlockPos bitPosition(int n) { return positions[n]; }
		/** V33a {@code getColor(pos)}: the intersection of the two filters the read beam crosses. */
		public RGBColorData netColor(int pos) { return netColors[pos].copy(); }
		/** V33a {@code getSecondFilterOffset}: the read beam crosses tape cells {@code i} and {@code i + D + 1}. */
		public int secondFilterOffset() { return dimensions.totalDepth + 1; }
		/** The colour V33a's solution assigns to tape position {@code i}. */
		public RGBColorData bitColor(int i) { return bitColors[i] == null ? null : bitColors[i].copy(); }

		void randomizeSolution(Random rand) {
			for (int i = 0; i < bitCount; i++)
				genRandomColor(i, rand);
		}

		private void genRandomColor(int i, Random rand) {
			int i2 = (i + dimensions.totalDepth + 1) % bitColors.length;
			Collection<RGBColorData> optionsA = RGBColorData.getAllPossibilities();
			Collection<RGBColorData> optionsB = RGBColorData.getAllPossibilities();
			if (bitColors[i] == null && bitColors[i2] == null) {
				// both free
			}
			else if (bitColors[i] == null) {
				optionsA = bitColors[i2].getReductiveChildren(true, false);
				optionsB = new ArrayList<>(List.of(bitColors[i2]));
			}
			else if (bitColors[i2] == null) {
				optionsA = new ArrayList<>(List.of(bitColors[i]));
				optionsB = bitColors[i].getReductiveChildren(true, false);
			}
			else {
				optionsA = new ArrayList<>(List.of(bitColors[i]));
				optionsB = new ArrayList<>(List.of(bitColors[i2]));
			}
			strip(optionsA);
			strip(optionsB);
			bitColors[i] = pick(rand, optionsA);
			bitColors[i2] = pick(rand, optionsB);
			RGBColorData net = bitColors[i].copy();
			net.intersect(bitColors[i2]);
			netColors[i] = net;
		}

		private static void strip(Collection<RGBColorData> options) {
			Iterator<RGBColorData> it = options.iterator();
			while (it.hasNext()) {
				RGBColorData c = it.next();
				if (c.isBlack() || c.isPrimary())
					it.remove();
			}
		}

		/** DragonAPI {@code getRandomCollectionEntry}. */
		private static RGBColorData pick(Random rand, Collection<RGBColorData> c) {
			List<RGBColorData> list = c instanceof List<RGBColorData> l ? l : new ArrayList<>(c);
			return list.isEmpty() ? null : list.get(rand.nextInt(list.size()));
		}

		void generate(Sink w, int x, int y, int z) {
			Direction facing = stage.facing;
			int fx = facing.getStepX(), fz = facing.getStepZ();
			int D = dimensions.totalDepth, H = dimensions.totalHeight;
			for (int d = -1; d <= D + 2; d++) {
				for (int h = -2; h <= H + 2; h++) {
					if (d == -1 && h < H)
						continue;
					ChromaShieldTypes m = null;
					boolean special = false;
					if (h >= H)
						m = ChromaShieldTypes.STONE;
					if (h < 0)
						m = ChromaShieldTypes.CLOAK;
					if (d == -2 || d > D)
						m = ChromaShieldTypes.STONE;
					if (d == -2 && h >= 0 && h <= H - 1) {
						m = ChromaShieldTypes.GLASS;
						special = true;
					}
					if (d == 0 && h == H)
						m = null;
					if (m != null && !special && d == D / 2)
						m = ChromaShieldTypes.LIGHT;
					int ax = x - d * fx, az = z - d * fz;
					if (m == null)
						w.block(ax, y + h, az, Kind.AIR);
					else if (special)
						w.specialShield(ax, y + h, az, m);
					else
						w.shield(ax, y + h, az, m);
				}
			}
			generateTape(w, x, y, z);
			placePiston(w, x, y - 1, z, Direction.UP);
			placePiston(w, x + fx, y + H, z + fz, facing.getOpposite());
			placePiston(w, x - fx * D, y + H + 1, z - fz * D, Direction.DOWN);
			placePiston(w, x - fx * (D + 1), y, z - fz * (D + 1), facing);
			emitter = new BlockPos(x - fx * (D + 1), y + 1, z - fz * (D + 1));
			w.emitter(emitter.getX(), emitter.getY(), emitter.getZ(), facing);
			target = new BlockPos(x + fx * 7, y + 1, z + fz * 7);
			w.readHead(target.getX(), target.getY(), target.getZ(), facing, stage.index, busIndex, stage.bitsPerDoor);
		}

		private void generateTape(Sink w, int x, int y, int z) {
			Direction facing = stage.facing;
			positions = dimensions.calculatePositions(x, y, z, 1, facing, true);
			int D = dimensions.totalDepth, H = dimensions.totalHeight;
			for (int d = 0; d <= D; d++) {
				for (int h = 0; h <= H; h++) {
					if (d == 0 || d == D || h == 0 || h == H) {
						if (d == 0 && h == H)
							continue;
						if (d == D && h == 0)
							continue;
						w.bit(x - d * facing.getStepX(), y + h, z - d * facing.getStepZ(), 0);
					}
				}
			}
		}

		private void placePiston(Sink w, int x, int y, int z, Direction push) {
			pistons.put(push, new BlockPos(x, y, z));
			w.piston(x, y, z, push);
		}
	}

	/** V33a {@code PistonTapeLoop.LoopDimensions} and its per-slice position table. */
	static final class LoopDimensions {
		private static final int MIN_HEIGHT = 4;
		private static final int MAX_HEIGHT = 6;
		final int totalHeight;
		final int totalDepth;
		final int bitLength;

		LoopDimensions(int d, int h) {
			totalHeight = h;
			totalDepth = d;
			bitLength = (totalHeight - 1) * 2 + (totalDepth - 1) * 2 + 2;
		}

		static LoopDimensions createFor(int stages) {
			int l = stages * 2;
			int bestH = -1;
			int bestD = -1;
			int best = l + 5;
			for (int h = MIN_HEIGHT; h <= MAX_HEIGHT; h++) {
				int d = Mth.ceil(l / (float)h);
				int excess = d * h - l;
				if (excess < best) {
					bestH = h;
					bestD = d;
					best = excess;
				}
			}
			return new LoopDimensions(bestD, bestH);
		}

		/** V33a {@code LoopSliceDimensions.calculatePositions}, then rotated by {@code initialBitIndex+1}. */
		BlockPos[] calculatePositions(int x, int y, int z, int initialBitIndex, Direction facing, boolean skipFirstAndThirdCorners) {
			BlockPos[] positions = new BlockPos[bitLength];
			BlockPos c = new BlockPos(x, y, z);
			Direction dir = facing.getOpposite();
			int n = 0;
			for (int i = 0; i < bitLength; i++) {
				n++;
				c = c.relative(dir, 1);
				if (n == totalDepth) {
					dir = Direction.UP;
					if (skipFirstAndThirdCorners) {
						i--;
						continue;
					}
				}
				else if (n == totalDepth + totalHeight) {
					dir = facing;
					if (!skipFirstAndThirdCorners) {
						i--;
						continue;
					}
				}
				else if (n == totalDepth + totalHeight + totalDepth) {
					dir = Direction.DOWN;
					if (skipFirstAndThirdCorners) {
						i--;
						continue;
					}
				}
				else if (n == totalDepth + totalHeight + totalDepth + totalHeight) {
					dir = facing.getOpposite();
					if (!skipFirstAndThirdCorners) {
						i--;
						continue;
					}
				}
				positions[i] = c;
			}
			for (int i = 0; i <= initialBitIndex; i++) {
				BlockPos last = positions[positions.length - 1];
				System.arraycopy(positions, 0, positions, 1, positions.length - 1);
				positions[0] = last;
			}
			return positions;
		}
	}

	/** V33a {@code DoorKey}: the colours one door expects, one per bus line, and where they must land. */
	public static final class DoorKey {
		public final int index;
		private final RGBColorData[] colors;
		/** V33a {@code value}: three bits per colour, red 4 / green 2 / blue 1. */
		public final int value;
		final BlockPos[] targets;
		final List<BlockPos> panels = new ArrayList<>();
		BlockPos panel;

		DoorKey(int idx, RGBColorData[] clr) {
			index = idx;
			colors = clr;
			targets = new BlockPos[clr.length];
			int v = 0;
			for (int i = 0; i < clr.length; i++)
				v |= numberValue(clr[i]) << (i * 3);
			value = v;
		}

		public static int numberValue(RGBColorData c) {
			return (c.red ? 4 : 0) | (c.green ? 2 : 0) | (c.blue ? 1 : 0);
		}

		public int colorCount() { return colors.length; }
		public RGBColorData color(int i) { return colors[i].copy(); }
		public BlockPos target(int i) { return targets[i]; }
		/** The door panel V33a opens ({@code x+left*3, y+1}). */
		public BlockPos panel() { return panel; }
		public List<BlockPos> panels() { return Collections.unmodifiableList(panels); }
	}

	// ========================================================================== staircase, loot, entry

	/** V33a {@code PistonTapeStaircase}: a radius-2 tube from one stage's exit to the next stage's entrance. */
	private void staircase(Sink w, Stage s1, Stage s2, Random rand) {
		int radius = 2;
		Direction main = s1.mainDirection;
		BlockPos c1 = s1.exit.relative(main, radius);
		BlockPos c2 = s2.entrance.relative(main, radius);
		Set<BlockPos> set = new LinkedHashSet<>();
		double lx = c2.getX() - c1.getX();
		double ly = c2.getY() - c1.getY();
		double lz = c2.getZ() - c1.getZ();
		for (double d = 0; d <= 1; d += 0.03125 / 4) {
			double dx = c1.getX() + lx * d;
			double dy = c1.getY() + ly * d;
			double dz = c1.getZ() + lz * d;
			int mx = Mth.floor(dx - radius), my = Mth.floor(dy - radius), mz = Mth.floor(dz - radius);
			int px = Mth.floor(dx + radius), py = Mth.floor(dy + radius), pz = Mth.floor(dz + radius);
			for (int i = mx; i <= px; i++)
				for (int j = my; j <= py; j++)
					for (int k = mz; k <= pz; k++)
						set.add(new BlockPos(i, j, k));
		}
		for (BlockPos c : set) {
			ChromaShieldTypes m = null;
			for (Direction dir : Direction.values()) {
				if (!set.contains(c.relative(dir))) {
					m = rand.nextInt(12) == 0 && set.contains(c.above()) && set.contains(c.below())
							? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE;
					break;
				}
			}
			if (m == null)
				w.block(c.getX(), c.getY(), c.getZ(), Kind.AIR);
			else
				w.shield(c.getX(), c.getY(), c.getZ(), m);
		}
		Direction left = main.getCounterClockWise();
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				w.block(c1.getX() + left.getStepX() * i - main.getStepX() * 2, c1.getY() + k,
						c1.getZ() + left.getStepZ() * i - main.getStepZ() * 2, Kind.AIR);
				w.block(c2.getX() + left.getStepX() * i - main.getStepX() * 2, c2.getY() + k,
						c2.getZ() + left.getStepZ() * i - main.getStepZ() * 2, Kind.AIR);
			}
		}
	}

	/** V33a {@code PistonTapeLootRoom}: a crystal-encrusted vault holding the core. */
	private void lootRoom(Sink w, Direction facing, int x, int y, int z) {
		int d = 7;
		int h = 2;
		Direction left = facing.getCounterClockWise();
		int l = 4;
		if (recording) {
			for (int i = -1; i <= 1; i++) {
				for (int k = -1; k <= 1; k++) {
					int dx = x + facing.getStepX() * (i + l) + left.getStepX() * k;
					int dz = z + facing.getStepZ() * (i + l) + left.getStepZ() * k;
					breakables.add(new BlockPos(dx, y - h, dz));
					breakables.add(new BlockPos(dx, y + h, dz));
				}
			}
			for (int i = -1; i <= 1; i++) {
				for (int k = 4; k <= 6; k++) {
					int dx = x + facing.getStepX() * -1 + left.getStepX() * k;
					int dz = z + facing.getStepZ() * -1 + left.getStepZ() * k;
					breakables.add(new BlockPos(dx, y + i, dz));
				}
			}
			core = new BlockPos(x + facing.getStepX() * l, y, z + facing.getStepZ() * l);
		}
		for (int i = 0; i <= d; i++) {
			for (int j = -h; j <= h; j++) {
				int width = i > 2 && i < d - 1 ? 3 : 2;
				for (int k = -width; k <= width; k++) {
					int dx = x + i * facing.getStepX() + k * left.getStepX();
					int dy = y + j;
					int dz = z + i * facing.getStepZ() + k * left.getStepZ();
					boolean wall = j == -h || j == h || k == width || k == -width || i == d;
					ChromaShieldTypes m = null;
					if (wall) {
						m = ChromaShieldTypes.STONE;
						if (j == 0 && (i == 1 || i == 4))
							m = ChromaShieldTypes.LIGHT;
					}
					else if (i == 1) {
						m = j == 0 && k == 0 ? ChromaShieldTypes.CRACKS : ChromaShieldTypes.GLASS;
					}
					if (wall && i > 2 && (j == -h || j == h) && (i == 2 || i == d - 1 || k == width - 1 || k == -width + 1))
						m = ChromaShieldTypes.COBBLE;
					if (m == null && i > 1)
						w.encrusted(dx, dy, dz);
					else if (m == null)
						w.block(dx, dy, dz, Kind.AIR);
					else
						w.shield(dx, dy, dz, m);
				}
			}
		}
		w.core(x + facing.getStepX() * l, y, z + facing.getStepZ() * l);
	}

	/** V33a {@code PistonTapeEntrance}: a breathing shaft from the surface down to the first stage. */
	private void entrance(Sink w, BlockPos start, Direction dir, int top) {
		int x = start.getX();
		int z = start.getZ();
		int y1 = start.getY() - 2;
		for (int y = y1; y <= top; y++) {
			int r = entranceRadius(y, y1);
			for (int i = -r; i <= r; i++) {
				for (int k = -r; k <= r; k++) {
					int dx = x + i;
					int dz = z + k;
					boolean wall = i == r || i == -r || k == -r || k == r || y == y1;
					if (wall) {
						Random rand = new Random(seed ^ BlockPos.asLong(dx, y, dz));
						ChromaShieldTypes m = rand.nextInt(20) == 0 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE;
						if (isEntranceEdge(y - 1, y1))
							m = ChromaShieldTypes.COBBLE;
						w.shield(dx, y, dz, m);
					}
					else if (isEntranceEdge(y, y1)) {
						w.hover(dx, y, dz);
					}
					else {
						w.block(dx, y, dz, Kind.AIR);
					}
				}
			}
		}
		Direction left = dir.getCounterClockWise();
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				w.block(x + 3 * dir.getStepX() + left.getStepX() * i, start.getY() + k,
						z + 3 * dir.getStepZ() + left.getStepZ() * i, Kind.AIR);
	}

	private static boolean isEntranceEdge(int y, int y1) {
		return y > y1 + 1 && entranceRadius(y, y1) != entranceRadius(y + 1, y1);
	}

	private static int entranceRadius(int y, int y1) {
		return 4 - (int)(2 * Math.cos((y - y1) / 4D));
	}

	/** Every cell the structure body writes, entrance excluded: {minX, minY, minZ, maxX, maxY, maxZ}. */
	public int[] bounds() {
		return new int[] {minX, minY, minZ, maxX, maxY, maxZ};
	}

	private void include(int x, int y, int z) {
		minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
		maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
	}

	/** The dry-run sink: writes nothing, records the extent of every write. */
	private Sink boundsSink() {
		return new Sink() {
			@Override public void block(int x, int y, int z, Kind kind) { include(x, y, z); }
			@Override public void shield(int x, int y, int z, ChromaShieldTypes type) { include(x, y, z); }
			@Override public void specialShield(int x, int y, int z, ChromaShieldTypes type) { include(x, y, z); }
			@Override public void piston(int x, int y, int z, Direction push) { include(x, y, z); }
			@Override public void bit(int x, int y, int z, int meta) { include(x, y, z); }
			@Override public void emitter(int x, int y, int z, Direction facing) { include(x, y, z); }
			@Override public void readHead(int x, int y, int z, Direction facing, int stage, int bus, int busWidth) { include(x, y, z); }
			@Override public void doorTarget(int x, int y, int z, Direction facing, int stage, int door, int bus,
					int busWidth, RGBColorData color, BlockPos doorPanel) { include(x, y, z); }
			@Override public void controller(int x, int y, int z, int type, int stage, Direction facing) { include(x, y, z); }
			@Override public void password(int x, int y, int z) { include(x, y, z); }
			@Override public void doorPanel(int x, int y, int z) { include(x, y, z); }
			@Override public void shiftLock(int x, int y, int z, BlockShiftLock.Passability passability) { include(x, y, z); }
			@Override public void lootChest(int x, int y, int z, Direction facing) { include(x, y, z); }
			@Override public void encrusted(int x, int y, int z) { include(x, y, z); }
			@Override public void core(int x, int y, int z) { include(x, y, z); }
			@Override public void hover(int x, int y, int z) { include(x, y, z); }
		};
	}

	/** Cells V33a {@code addBreakable}d: the vault's floor, ceiling and back-wall plates. */
	public boolean isBreakable(int x, int y, int z) {
		return breakables.contains(new BlockPos(x, y, z));
	}
}
