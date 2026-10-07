package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.structure.RayBlendLayout.Cell;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

/**
 * V33a {@code RayBlendEntrance}: a stack of 3x3-cell maze floors climbing from the corridor to the surface, each
 * floor's exit hole above a different cell, then a plain shaft, a cobble ring and lit posts at the top, and up to
 * eight crystal deposits in the surrounding grass.
 *
 * <p>V33a generated it after the terrain existed and drew from {@code new Random()}, DragonAPI's shared random and
 * {@code world.rand}; here one stream seeded from the structure carries those draws in V33a's order, so every chunk
 * agrees. The deposits read the terrain, which a chunk cannot see beyond itself, so they are planned once when the
 * structure starts ({@link #deposits}) from the generator's height prediction and stored with the piece.
 */
public final class RayBlendEntrancePlan {

	private static final long LEVEL_SALT = 0x5261794265_6E7472L;
	private static final long DEPOSIT_SALT = 0x4372797374_616C73L;

	static final int HEIGHT = 5;
	static final int SEP = 3;
	static final int GRID_RADIUS = 1;
	static final int HOLE_RADIUS = 1;
	static final int TOTAL_RADIUS = 2;
	static final int CELL_SPACING = HOLE_RADIUS * 2 + 2;

	/** Half-width of every write around the entrance column, deposits included. */
	public static final int REACH = 40;

	private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	private RayBlendEntrancePlan() {}

	/** V33a {@code getTopSolidOrLiquidBlock} (the first free block above the top motion-blocking one) and grass. */
	public interface Terrain {
		int topSolid(int x, int z);

		boolean isGrass(int x, int y, int z);
	}

	/** One V33a {@code CrystalDeposit}: its crystal cells and their colours, in write order. */
	public record Deposit(BlockPos origin, List<BlockPos> crystals, byte[] colors) {

		/** V33a {@code calculate}'s edge: every crystal's six neighbours and the three above each, less the crystals. */
		public Set<BlockPos> edge() {
			Set<BlockPos> edge = new LinkedHashSet<>();
			for (BlockPos c : crystals)
				for (Direction d : Direction.values())
					for (int i = 0; i <= 3; i++)
						edge.add(c.relative(d).above(i));
			crystals.forEach(edge::remove);
			return edge;
		}
	}

	/** The column V33a's entrance is centred on: {@code x - (GRID_RADIUS*CELL_SPACING+2)} from its anchor. */
	public static BlockPos center(RayBlendLayout layout) {
		BlockPos a = layout.entranceAnchor();
		return new BlockPos(a.getX() - (GRID_RADIUS * CELL_SPACING + 2), a.getY(), a.getZ());
	}

	/** V33a's eight deposit attempts around the entrance, each kept only where it lands on grass. */
	public static List<Deposit> deposits(RayBlendLayout layout, Terrain terrain) {
		Random rand = new Random(layout.seed() ^ DEPOSIT_SALT);
		BlockPos c = center(layout);
		List<Deposit> out = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			int r = 6 + rand.nextInt(11);
			double ang = rand.nextDouble() * 360;
			long depositSeed = rand.nextLong();
			double[] xyz = ReikaPhysicsHelper.polarToCartesian(r, 0, ang);
			int dx = c.getX() + Mth.floor(xyz[0]);
			int dz = c.getZ() + Mth.floor(xyz[2]);
			int dy = terrain.topSolid(dx, dz) - 1;
			if (!terrain.isGrass(dx, dy, dz))
				continue;
			out.add(deposit(new BlockPos(dx, dy, dz), new Random(depositSeed), terrain));
		}
		return out;
	}

	private static Deposit deposit(BlockPos origin, Random rand, Terrain terrain) {
		Set<BlockPos> crystals = new LinkedHashSet<>();
		int arms = 2 + rand.nextInt(4);
		for (int i = 0; i < arms; i++) {
			Direction dir = SIDES[rand.nextInt(4)];
			int maxl = 6 + rand.nextInt(15);
			int d = 0;
			BlockPos c = origin;
			while (d < maxl) {
				crystals.add(c);
				c = c.relative(dir);
				if (!terrain.isGrass(c.getX(), c.getY(), c.getZ()))
					break;
				if (rand.nextInt(3) == 0)
					dir = SIDES[rand.nextInt(4)];
				d++;
			}
		}
		List<BlockPos> list = List.copyOf(crystals);
		byte[] colors = new byte[list.size()];
		for (int i = 0; i < colors.length; i++)
			colors[i] = (byte)rand.nextInt(16);
		return new Deposit(origin, list, colors);
	}

	/** Every entrance write in V33a's order, for a surface at {@code y2}. */
	public static void run(RayBlendLayout.Sink sink, RayBlendLayout layout, int y2, List<Deposit> deposits) {
		Random rand = new Random(layout.seed() ^ LEVEL_SALT);
		BlockPos c = center(layout);
		int x = c.getX(), z = c.getZ();
		int y = layout.posY() + 2;
		Level last = null;
		int dx = 0, dz = 0;
		int floor = 0;
		while (y + HEIGHT + SEP < y2) {
			int sX = last != null ? last.shaftUp.x() : Integer.MIN_VALUE;
			int sZ = last != null ? last.shaftUp.y() : Integer.MIN_VALUE;
			Level el = new Level(rand, floor, x, z, sX, sZ);
			floor++;
			el.generate(sink, x, y, z, rand);
			y += HEIGHT + SEP;
			last = el;
			dx = x + last.shaftUp.x() * CELL_SPACING;
			dz = z + last.shaftUp.y() * CELL_SPACING;
		}

		// With no floor (the rooms already reach the surface) V33a never set dx/dz and wrote this shaft and its
		// markers at the world origin, nowhere near the structure; that stray patch is not reproduced.
		if (last != null) {
			while (y <= y2) {
				shaft(sink, dx, y, dz);
				y++;
			}
			for (int i = -TOTAL_RADIUS - 1; i <= TOTAL_RADIUS + 1; i++)
				for (int k = -TOTAL_RADIUS - 1; k <= TOTAL_RADIUS + 1; k++)
					if (Math.abs(i) == TOTAL_RADIUS + 1 || Math.abs(k) == TOTAL_RADIUS + 1)
						sink.shield(dx + i, y2 - 1, dz + k, ChromaShieldTypes.COBBLE);
			int p = TOTAL_RADIUS / 2 + 1;
			for (int i = 0; i <= 4; i++) {
				ChromaShieldTypes m = i == 4 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE;
				sink.shield(dx - p, y2 + i, dz + p, m);
				sink.shield(dx - p, y2 + i, dz - p, m);
				sink.shield(dx + p, y2 + i, dz + p, m);
				sink.shield(dx + p, y2 + i, dz - p, m);
			}
			int q = TOTAL_RADIUS * 2 + 1;
			for (int i = -1; i <= 2; i++) {
				ChromaShieldTypes m = i == 2 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.COBBLE;
				sink.shield(dx - q, y2 + i, dz + q, m);
				sink.shield(dx - q, y2 + i, dz - q, m);
				sink.shield(dx + q, y2 + i, dz + q, m);
				sink.shield(dx + q, y2 + i, dz - q, m);
				if (i == -1) {
					for (int a = -1; a <= 1; a++) {
						for (int b = -1; b <= 1; b++) {
							if (a == 0 || b == 0) {
								sink.shield(dx - q + a, y2 + i, dz + q + b, ChromaShieldTypes.CLOAK);
								sink.shield(dx - q + a, y2 + i, dz - q + b, ChromaShieldTypes.CLOAK);
								sink.shield(dx + q + a, y2 + i, dz + q + b, ChromaShieldTypes.CLOAK);
								sink.shield(dx + q + a, y2 + i, dz - q + b, ChromaShieldTypes.CLOAK);
							}
						}
					}
				}
			}
		}

		for (Deposit d : deposits) {
			for (BlockPos e : d.edge()) {
				if (e.getY() > d.origin().getY())
					sink.air(e.getX(), e.getY(), e.getZ());
				else
					sink.shield(e.getX(), e.getY(), e.getZ(), ChromaShieldTypes.CLOAK);
			}
			for (int i = 0; i < d.crystals().size(); i++) {
				BlockPos cr = d.crystals().get(i);
				sink.crystal(cr.getX(), cr.getY(), cr.getZ(), CrystalElement.elements[d.colors()[i] & 15]);
			}
		}
	}

	/** V33a {@code EntranceLevel.generateShaft}: a 3x3 hole in a stone ring. */
	private static void shaft(RayBlendLayout.Sink sink, int x, int y, int z) {
		int r = TOTAL_RADIUS / 2 + 1;
		for (int i = -r; i <= r; i++)
			for (int k = -r; k <= r; k++)
				if (Math.abs(i) == r || Math.abs(k) == r)
					sink.shield(x + i, y, z + k, ChromaShieldTypes.STONE);
				else
					sink.air(x + i, y, z + k);
	}

	/** V33a {@code EntranceLevel}. */
	private static final class Level {
		private final HashMap<Cell, LevelCell> cells = new HashMap<>();
		private final @Nullable Cell shaftDown;
		private final Cell shaftUp;
		private final int floorIndex;
		private final List<Cell> path = new ArrayList<>();

		Level(Random rand, int fn, int rx, int rz, int xd, int zd) {
			floorIndex = fn;
			shaftDown = xd != Integer.MIN_VALUE ? new Cell(xd, zd) : null;
			Cell p = new Cell(plusMinus(rand), plusMinus(rand));
			while (p.equals(shaftDown))
				p = new Cell(plusMinus(rand), plusMinus(rand));
			shaftUp = p;

			for (int i = -GRID_RADIUS; i <= GRID_RADIUS; i++)
				for (int k = -GRID_RADIUS; k <= GRID_RADIUS; k++)
					cells.put(new Cell(i, k), new LevelCell(i, k, rx + i * CELL_SPACING, rz + k * CELL_SPACING));

			calculatePath(rand);

			if (shaftDown != null)
				cells.get(shaftDown).openFloor = true;
			cells.get(shaftUp).openCeiling = true;

			int n1 = GRID_RADIUS * GRID_RADIUS / 2;
			int n2 = Math.max(1, GRID_RADIUS * GRID_RADIUS * 2 / 3);
			int n = n1 == n2 ? n1 : n1 + rand.nextInt(1 + n2 - n1);
			int amt = 0;
			for (LevelCell c : cells.values()) {
				if (c.canHaveLoot() && !(floorIndex == 0 && c.xPos == GRID_RADIUS && c.zPos == 0)) {
					c.hasLoot = true;
					amt++;
				}
				if (amt >= n)
					break;
			}
		}

		private static int plusMinus(Random rand) {
			return -GRID_RADIUS + rand.nextInt(GRID_RADIUS * 2 + 1);
		}

		private void calculatePath(Random rand) {
			Cell p = shaftUp;
			Cell end = shaftDown != null ? shaftDown : new Cell(GRID_RADIUS, 0);
			int dx = p.x() - end.x();
			int dz = p.y() - end.y();
			while (dx != 0 || dz != 0) {
				path.add(p);
				boolean moveX = dx != 0 && dz != 0 ? rand.nextBoolean() : dx != 0;
				int sx = moveX ? -(int)Math.signum(dx) : 0;
				int sz = moveX ? 0 : -(int)Math.signum(dz);
				p = new Cell(p.x() + sx, p.y() + sz);
				dx = p.x() - end.x();
				dz = p.y() - end.y();
			}
			path.add(end);
			for (int i = 1; i < path.size(); i++)
				connect(path.get(i - 1), path.get(i));

			HashSet<Cell> unconnected = new HashSet<>(cells.keySet());
			unconnected.removeAll(path);
			while (!unconnected.isEmpty()) {
				List<Cell> li = new ArrayList<>(unconnected);
				Cell pr = li.get(rand.nextInt(li.size()));
				List<Cell> subpath = new ArrayList<>();
				boolean pathed = false;
				while (!pathed) {
					Cell next = validRandomNeighbor(pr, rand);
					subpath.add(pr);
					if (!unconnected.contains(next)) {
						subpath.add(next);
						unconnected.removeAll(subpath);
						pathed = true;
						for (int i = 1; i < subpath.size(); i++)
							connect(subpath.get(i - 1), subpath.get(i));
					}
					else {
						pr = next;
					}
				}
			}
		}

		/** V33a picks from DragonAPI's shuffled horizontal list; the pick itself is what decides. */
		private Cell validRandomNeighbor(Cell pr, Random rand) {
			List<Direction> li = new ArrayList<>(List.of(SIDES));
			while (!li.isEmpty()) {
				Direction dir = li.remove(rand.nextInt(li.size()));
				Cell p2 = new Cell(pr.x() + dir.getStepX(), pr.y() + dir.getStepZ());
				if (Math.abs(p2.x()) <= GRID_RADIUS && Math.abs(p2.y()) <= GRID_RADIUS)
					return p2;
			}
			throw new IllegalStateException("No neighbour of " + pr + " in the entrance grid");
		}

		private void connect(Cell p1, Cell p2) {
			int side = sideIndex(p2.x() - p1.x(), p2.y() - p1.y());
			if (side < 0)
				throw new IllegalArgumentException("You cannot link two non-adjacent points " + p1 + ", " + p2);
			cells.get(p1).sides[side] = true;
			cells.get(p2).sides[side ^ 1] = true;
		}

		/** Index into {@link #SIDES} (north, south, west, east) of a unit step; pairs are opposites. */
		private static int sideIndex(int dx, int dz) {
			for (int i = 0; i < SIDES.length; i++)
				if (SIDES[i].getStepX() == Integer.signum(dx) && SIDES[i].getStepZ() == Integer.signum(dz))
					return i;
			return -1;
		}

		void generate(RayBlendLayout.Sink sink, int x, int y, int z, Random rand) {
			int r = CELL_SPACING * GRID_RADIUS + 3;
			for (int i = -r; i <= r; i++) {
				for (int k = -r; k <= r; k++) {
					sink.shield(x + i, y, z + k, ChromaShieldTypes.COBBLE);
					sink.shield(x + i, y + HEIGHT, z + k, ChromaShieldTypes.COBBLE);
					for (int h = 1; h < HEIGHT; h++)
						sink.air(x + i, y + h, z + k);
				}
			}
			for (LevelCell c : cells.values())
				c.generate(sink, y);
			for (int h = 1; h < HEIGHT; h++) {
				for (int d = -r; d <= r; d++) {
					sink.shield(x + d, y + h, z - r, wall(rand));
					sink.shield(x + d, y + h, z + r, wall(rand));
					sink.shield(x - r, y + h, z + d, wall(rand));
					sink.shield(x + r, y + h, z + d, wall(rand));
				}
				if (floorIndex == 0) {
					for (int i = -1; i <= 1; i++) {
						for (int d = 1; d <= 3; d++)
							sink.air(x + r, y + d, z + i);
						sink.shield(x + r - 1, y + 4, z + i, ChromaShieldTypes.COBBLE);
					}
				}
				for (int i = -GRID_RADIUS - 1; i <= GRID_RADIUS; i++)
					for (int k = -GRID_RADIUS - 1; k <= GRID_RADIUS; k++)
						sink.shield(x + i * CELL_SPACING + TOTAL_RADIUS, y + h, z + k * CELL_SPACING + TOTAL_RADIUS,
								ChromaShieldTypes.COBBLE);
			}
			int o = TOTAL_RADIUS / 2 + 1;
			sink.shield(x + o, y + HEIGHT / 2, z + o, ChromaShieldTypes.LIGHT);
			sink.shield(x + o, y + HEIGHT / 2, z - o, ChromaShieldTypes.LIGHT);
			sink.shield(x - o, y + HEIGHT / 2, z + o, ChromaShieldTypes.LIGHT);
			sink.shield(x - o, y + HEIGHT / 2, z - o, ChromaShieldTypes.LIGHT);
			for (int i = HEIGHT + 1; i <= HEIGHT + SEP; i++)
				shaft(sink, x + shaftUp.x() * CELL_SPACING, y + i, z + shaftUp.y() * CELL_SPACING);
		}

		/** V33a: {@code world.rand.nextInt(12) == 0} lights a wall block. */
		private static ChromaShieldTypes wall(Random rand) {
			return rand.nextInt(12) == 0 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE;
		}
	}

	/** V33a {@code RayBlendEntrance.Cell}. */
	private static final class LevelCell {
		final int xPos;
		final int zPos;
		final int rootX;
		final int rootZ;
		boolean openCeiling;
		boolean openFloor;
		boolean hasLoot;
		final boolean[] sides = new boolean[4];

		LevelCell(int x, int z, int rx, int rz) {
			xPos = x;
			zPos = z;
			rootX = rx;
			rootZ = rz;
		}

		boolean canHaveLoot() {
			int open = 0;
			for (boolean b : sides)
				if (b)
					open++;
			return !openCeiling && !openFloor && 4 - open == 3;
		}

		void generate(RayBlendLayout.Sink sink, int y) {
			int r = HOLE_RADIUS;
			for (int i = -r; i <= r; i++) {
				for (int k = -r; k <= r; k++) {
					int dx = rootX + i;
					int dz = rootZ + k;
					if (openFloor)
						sink.air(dx, y, dz);
					else
						sink.shield(dx, y, dz, ChromaShieldTypes.STONE);
					if (openCeiling)
						sink.air(dx, y + HEIGHT, dz);
					else
						sink.shield(dx, y + HEIGHT, dz, ChromaShieldTypes.STONE);
				}
			}
			for (int d = 0; d < 4; d++) {
				Direction dir = SIDES[d];
				int dx = rootX + dir.getStepX() * TOTAL_RADIUS;
				int dz = rootZ + dir.getStepZ() * TOTAL_RADIUS;
				for (int h = 1; h < HEIGHT; h++) {
					for (int a = -HOLE_RADIUS; a <= HOLE_RADIUS; a++) {
						boolean air = sides[d] || Math.abs(xPos + dir.getStepX()) > GRID_RADIUS
								|| Math.abs(zPos + dir.getStepZ()) > GRID_RADIUS;
						int bx = dir.getStepX() == 0 ? dx + a : dx;
						int bz = dir.getStepX() == 0 ? dz : dz + a;
						if (air)
							sink.air(bx, y + h, bz);
						else
							sink.shield(bx, y + h, bz, ChromaShieldTypes.GLASS);
					}
				}
			}
			if (hasLoot) {
				for (int i = -1; i <= 1; i++)
					for (int k = -1; k <= 1; k++)
						sink.shield(rootX + i, y, rootZ + k, i == 0 && k == 0 ? ChromaShieldTypes.LIGHT
								: ChromaShieldTypes.CLOAK);
				Direction dir = Direction.NORTH;
				for (int i = 0; i < sides.length; i++) {
					if (sides[i]) {
						dir = SIDES[i];
						break;
					}
				}
				sink.lootChest(rootX, y + 1, rootZ, dir);
			}
		}
	}
}
