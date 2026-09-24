package reika.chromaticraft.world.luminous;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import reika.dragonapi.instantiable.math.LobulatedCurve;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code GlowingCliffsAuxGenerator.Island}: one floating island of the Luminous Cliffs.
 *
 * <p>An island's outline is a lobulated curve of three to six lobes between an inner and outer radius
 * (8-12 and 12-18, times a size scale of 0.5-2 from a slow noise). It tapers from a thick middle (18-30
 * blocks, scaled) to two blocks at the rim, and its top follows a contour noise of up to six blocks. Its
 * body is transparent cliff stone under vanilla dirt (thicker toward the rim) and grass. Some islands
 * hold a lake sunk into the middle, and some a river: channels where the contour noise is nearly zero,
 * filled with water (or, for high islands over the sea, one time in twenty, Luma), which spill off the
 * edges as waterfalls. A river reaching the rim can spawn up to two smaller child islands lower down.
 *
 * <p>The shape is a pure function of the stored parameters and the world seed, computed in upstream's
 * order into a compact per-column form, so every chunk that paints part of the island paints the same
 * island.
 */
public final class LuminousIsland {

	public enum Kind {
		NONE, STONE, DIRT, GRASS, LIQUID;

		private static final Kind[] VALUES = values();
	}

	/** Where terrain is, which an island must not overlap (V33a {@code canGenerate}). */
	public interface Terrain {
		int topSolidOrLiquid(int x, int z);

		boolean isSolid(int x, int y, int z);
	}

	private static final int MAX_CONTOUR_HEIGHT = 6;

	private static final Map<Long, NoiseGeneratorBase> CONTOURS = new ConcurrentHashMap<>();

	int originX;
	int originY;
	int originZ;
	double scale = 1;
	double innerRadius = 10;
	double outerRadius = 18;
	double minThickness = 2;
	double maxThickness = 24;
	int degree = 6;
	boolean overWater;
	boolean hasLake;
	boolean hasRiver;
	boolean luma;
	double lakeDepth = 5;
	double lakeScale = 0.5;
	int allowableChildren;
	long curveSeed;

	LuminousIsland() {}

	/** V33a {@code contours}: {@code SimplexNoiseGenerator(seed >> 1)} at 1/16 with an octave. */
	static NoiseGeneratorBase contours(long worldSeed) {
		return CONTOURS.computeIfAbsent(worldSeed,
				seed -> new SimplexNoiseGenerator(seed >> 1).setFrequency(1 / 16D).addOctave(2, 0.5, 1000));
	}

	/** V33a {@code initializeIsland}. */
	static LuminousIsland initialize(Terrain terrain, int x, int z, Random rand, double sizeScale, boolean overWater) {
		LuminousIsland is = new LuminousIsland();
		is.degree = 3 + rand.nextInt(4);
		is.scale = sizeScale;
		is.overWater = overWater;
		is.innerRadius = (8 + 4 * rand.nextDouble()) * sizeScale;
		is.outerRadius = (12 + 6 * rand.nextDouble()) * sizeScale;
		is.maxThickness = (18 + 12 * rand.nextDouble()) * sizeScale;
		is.originX = x;
		is.originZ = z;
		is.originY = Math.min(250, Math.max(terrain.topSolidOrLiquid(x, z) + 10 + (int)is.maxThickness, 90) + rand.nextInt(80));
		is.hasLake = overWater ? rand.nextInt(3) > 0 : rand.nextInt(2) == 0;
		is.hasRiver = rand.nextInt(overWater ? 4 : 5) == 0;
		if (is.hasLake || is.hasRiver) {
			if (overWater && is.originY > 220 && rand.nextInt(20) == 0)
				is.luma = true;
		}
		if (rand.nextInt(20) == 0)
			is.hasLake = is.hasRiver = true;
		if (is.hasLake) {
			is.lakeDepth = 2 + rand.nextInt(5);
			is.lakeScale = 0.25 + rand.nextDouble() * 0.625;
		}
		if (is.hasRiver)
			is.allowableChildren = rand.nextInt(4) > 0 ? 0 : rand.nextInt(8) == 0 ? 2 : 1;
		return is;
	}

	/**
	 * V33a {@code generateIsland} up to placement: calculate the shape (spawning children as the river
	 * reaches the rim), then accept it only if every block of it is air. Accepted islands, children
	 * included, are added to {@code out}; a child can be accepted even when its parent is not, as upstream
	 * spawned children before checking the parent.
	 */
	static boolean generate(Terrain terrain, Random rand, long worldSeed, LuminousIsland is, List<LuminousIsland> out) {
		is.curveSeed = rand.nextLong();
		Shape shape = is.calculate(worldSeed, terrain, rand, out);
		if (!shape.canGenerate(terrain))
			return false;
		out.add(is);
		return true;
	}

	/** V33a {@code generateChild}. */
	private void generateChild(Terrain terrain, int dx, int dz, Random rand, long worldSeed, List<LuminousIsland> out) {
		LuminousIsland is = initialize(terrain, dx, dz, rand, scale, overWater);
		is.originY = originY - (int)maxThickness - 10 - rand.nextInt(30);
		is.hasLake = true;
		is.lakeDepth = 2 + rand.nextInt(5);
		is.lakeScale = 0.375 + rand.nextDouble() * 0.5;
		is.hasRiver = false;
		is.innerRadius = 6 + 2 * rand.nextDouble();
		is.outerRadius = 10 + 2 * rand.nextDouble();
		is.maxThickness = 12 + 6 * rand.nextDouble();
		if (generate(terrain, rand, worldSeed, is, out))
			allowableChildren--;
	}

	/** The shape alone, for painting; children are their own pieces and are not respawned. */
	Shape shape(long worldSeed) {
		return this.calculate(worldSeed, null, null, null);
	}

	/**
	 * V33a {@code calculate}: one-degree steps round the curve, half-block steps out along each ray, each
	 * sample writing a tapered column, later writes replacing earlier ones in the order upstream made
	 * them.
	 */
	private Shape calculate(long worldSeed, Terrain terrain, Random rand, List<LuminousIsland> out) {
		NoiseGeneratorBase contours = contours(worldSeed);
		Shape shape = new Shape();
		LobulatedCurve lb = LobulatedCurve.fromMinMaxRadii(innerRadius, outerRadius, degree);
		lb.generate(new Random(curveSeed));
		int x = originX;
		int z = originZ;
		for (double d = 0; d < 360; d += 1) {
			double r = lb.getRadius(d);
			double cos = Math.cos(Math.toRadians(d));
			double sin = Math.sin(Math.toRadians(d));
			for (double dr = 0; dr <= r; dr += 0.5) {
				int dx = Mth.floor(x + dr * cos);
				int dz = Mth.floor(z + dr * sin);
				double t = ReikaMathLibrary.linterpolate(r - dr, 0, outerRadius, minThickness, maxThickness);
				double cont = Math.abs(contours.getValue(dx, dz));
				int oy = (int)(cont * MAX_CONTOUR_HEIGHT);
				double lr = r * lakeScale;
				if (hasLake && dr <= lr) {
					double ld = 2 * lakeDepth * ((lr - dr) / r / lakeScale);
					oy = (int)Math.max(0, oy - ld);
				}
				int ty = originY + oy;
				Column col = shape.column(dx, dz);
				for (int i = 0; i <= t; i++) {
					Kind bk = Kind.STONE;
					double dt = ReikaMathLibrary.linterpolate(dr, 0, outerRadius, 0.5, 3);
					if (i == 0)
						bk = Kind.GRASS;
					else if (i < dt)
						bk = Kind.DIRT;
					// Upstream guards with "bk.blockID != Blocks.stone || !blocks.containsKey(c)", but its
					// island stone is cliff stone, never vanilla stone, so the guard always passes and
					// every write lands. That is kept.
					col.set(ty - i, bk);
				}
				if ((hasRiver || (hasLake && dr < lr)) && cont < 0.125) {
					col.set(ty, Kind.LIQUID);
					col.set(ty - 1, Kind.GRASS);
					if (cont < 0.0625) {
						col.set(ty - 1, Kind.LIQUID);
						col.set(ty - 2, Kind.GRASS);
					}
					if (dr >= r - 1 && allowableChildren > 0 && terrain != null)
						this.generateChild(terrain, dx, dz, rand, worldSeed, out);
				}
				col.top = ty;
			}
		}
		return shape;
	}

	public int minX() {
		return Mth.floor(originX + 0.5 - outerRadius) - 1;
	}

	public int maxX() {
		return Mth.floor(originX + 0.5 + outerRadius) + 1;
	}

	public int minZ() {
		return Mth.floor(originZ + 0.5 - outerRadius) - 1;
	}

	public int maxZ() {
		return Mth.floor(originZ + 0.5 + outerRadius) + 1;
	}

	public int minY() {
		return (int)(originY - MAX_CONTOUR_HEIGHT - maxThickness) - 2;
	}

	public int maxY() {
		return originY + MAX_CONTOUR_HEIGHT + 1;
	}

	/** V33a {@code getRandomX}. */
	int getRandomX(Random rand) {
		int min = Mth.floor(originX + 0.5 - outerRadius);
		int max = Mth.floor(originX + 0.5 + outerRadius);
		return min + rand.nextInt(max - min + 1);
	}

	/** V33a {@code getRandomY}. */
	int getRandomY(Random rand) {
		int max = originY + MAX_CONTOUR_HEIGHT;
		int min = (int)(originY - MAX_CONTOUR_HEIGHT - maxThickness);
		return min + rand.nextInt(max - min + 1);
	}

	/** V33a {@code getRandomZ}. */
	int getRandomZ(Random rand) {
		int min = Mth.floor(originZ + 0.5 - outerRadius);
		int max = Mth.floor(originZ + 0.5 + outerRadius);
		return min + rand.nextInt(max - min + 1);
	}

	void save(CompoundTag tag) {
		tag.putInt("OX", originX);
		tag.putInt("OY", originY);
		tag.putInt("OZ", originZ);
		tag.putDouble("Scale", scale);
		tag.putDouble("Inner", innerRadius);
		tag.putDouble("Outer", outerRadius);
		tag.putDouble("MinT", minThickness);
		tag.putDouble("MaxT", maxThickness);
		tag.putInt("Degree", degree);
		tag.putBoolean("OverWater", overWater);
		tag.putBoolean("Lake", hasLake);
		tag.putBoolean("River", hasRiver);
		tag.putBoolean("Luma", luma);
		tag.putDouble("LakeDepth", lakeDepth);
		tag.putDouble("LakeScale", lakeScale);
		tag.putLong("CurveSeed", curveSeed);
	}

	static LuminousIsland load(CompoundTag tag) {
		LuminousIsland is = new LuminousIsland();
		is.originX = tag.getIntOr("OX", 0);
		is.originY = tag.getIntOr("OY", 0);
		is.originZ = tag.getIntOr("OZ", 0);
		is.scale = tag.getDoubleOr("Scale", 1);
		is.innerRadius = tag.getDoubleOr("Inner", 10);
		is.outerRadius = tag.getDoubleOr("Outer", 18);
		is.minThickness = tag.getDoubleOr("MinT", 2);
		is.maxThickness = tag.getDoubleOr("MaxT", 24);
		is.degree = tag.getIntOr("Degree", 6);
		is.overWater = tag.getBooleanOr("OverWater", false);
		is.hasLake = tag.getBooleanOr("Lake", false);
		is.hasRiver = tag.getBooleanOr("River", false);
		is.luma = tag.getBooleanOr("Luma", false);
		is.lakeDepth = tag.getDoubleOr("LakeDepth", 5);
		is.lakeScale = tag.getDoubleOr("LakeScale", 0.5);
		is.curveSeed = tag.getLongOr("CurveSeed", 0);
		return is;
	}

	/** Identifies an island for the shape cache. */
	long key() {
		return curveSeed ^ BlockPos.asLong(originX, originY, originZ);
	}

	/** The island's cells, one short vertical run per column. */
	static final class Shape {

		private final Map<Long, Column> columns = new HashMap<>();

		Column column(int x, int z) {
			return columns.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> new Column());
		}

		/** The cell at a position, NONE if the island has nothing there. */
		Kind get(int x, int y, int z) {
			Column col = columns.get(BlockPos.asLong(x, 0, z));
			return col == null ? Kind.NONE : col.get(y);
		}

		boolean contains(int x, int y, int z) {
			return this.get(x, y, z) != Kind.NONE;
		}

		/** The column's top as V33a's {@code topMap} recorded it, or Integer.MIN_VALUE. */
		int topMap(int x, int z) {
			Column col = columns.get(BlockPos.asLong(x, 0, z));
			return col == null ? Integer.MIN_VALUE : col.top;
		}

		Map<Long, Column> columns() {
			return columns;
		}

		/** V33a {@code canGenerate}: every cell of the island is air in the world. */
		boolean canGenerate(Terrain terrain) {
			for (Map.Entry<Long, Column> e : columns.entrySet()) {
				int x = BlockPos.getX(e.getKey());
				int z = BlockPos.getZ(e.getKey());
				Column col = e.getValue();
				for (int i = 0; i < col.kinds.length; i++)
					if (col.kinds[i] != 0 && terrain.isSolid(x, col.minY + i, z))
						return false;
			}
			return true;
		}

		/**
		 * V33a {@code placeBlocks}' choice of block for one cell. Water on the rim becomes dirt or grass
		 * unless both cells along the rim are also island (and not themselves rim water), which is what
		 * lets rivers break out as waterfalls only where they meet the edge squarely; dirt or grass with
		 * nothing under it becomes stone; grass with something over it becomes dirt.
		 */
		Kind resolve(int x, int y, int z, boolean waterLiquid) {
			Kind bk = this.get(x, y, z);
			if (bk == Kind.LIQUID && waterLiquid) {
				Direction dir = this.getEdgeSide(x, y, z);
				if (dir != null) {
					dir = dir.getCounterClockWise();
					int x2 = x + dir.getStepX();
					int z2 = z + dir.getStepZ();
					int x3 = x - dir.getStepX();
					int z3 = z - dir.getStepZ();
					Kind e1 = this.get(x2, y, z2);
					Kind e2 = this.get(x3, y, z3);
					if (e1 == Kind.NONE || e2 == Kind.NONE
							|| (e1 == Kind.LIQUID && this.getEdgeSide(x2, y, z2) != null)
							|| (e2 == Kind.LIQUID && this.getEdgeSide(x3, y, z3) != null))
						bk = this.contains(x, y + 1, z) ? Kind.DIRT : Kind.GRASS;
				}
			}
			if ((bk == Kind.DIRT || bk == Kind.GRASS) && !this.contains(x, y - 1, z))
				bk = Kind.STONE;
			if (bk == Kind.GRASS && this.contains(x, y + 1, z))
				bk = Kind.DIRT;
			return bk;
		}

		/** V33a {@code getEdgeSide}: the first horizontal side (N, S, W, E) the island does not continue. */
		private Direction getEdgeSide(int x, int y, int z) {
			for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST})
				if (!this.contains(x + dir.getStepX(), y, z + dir.getStepZ()))
					return dir;
			return null;
		}
	}

	static final class Column {

		private int minY;
		private byte[] kinds = new byte[0];
		int top = Integer.MIN_VALUE;

		Kind get(int y) {
			int i = y - minY;
			return i < 0 || i >= kinds.length ? Kind.NONE : Kind.VALUES[kinds[i]];
		}

		void set(int y, Kind k) {
			if (kinds.length == 0) {
				minY = y;
				kinds = new byte[1];
			}
			else if (y < minY) {
				byte[] grown = new byte[kinds.length + (minY - y)];
				System.arraycopy(kinds, 0, grown, minY - y, kinds.length);
				kinds = grown;
				minY = y;
			}
			else if (y >= minY + kinds.length) {
				kinds = java.util.Arrays.copyOf(kinds, y - minY + 1);
			}
			kinds[y - minY] = (byte)k.ordinal();
		}

		int minY() {
			return minY;
		}

		int height() {
			return kinds.length;
		}
	}
}
