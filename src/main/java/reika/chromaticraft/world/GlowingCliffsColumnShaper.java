package reika.chromaticraft.world;

import net.minecraft.util.Mth;
import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.instantiable.Interpolation;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V33a {@code GlowingCliffsColumnShaper}: the shape of the Luminous Cliffs, column by column.
 *
 * <p>One large landmass noise, clamped to [0, 1] and called {@code hval}, decides what a column is.
 * Below 0.3 it is sea, its floor sloping from 24-48 up to the shore. Above that the column is land, on
 * one of three shelves chosen against two noisy thresholds: shores (62-72, rising with hval), the middle
 * plateau (100-112) and the high plateau (144-160). Each plateau is undercut near its rim by a cave
 * hollow — a floor, a ceiling at 80-96 (or 128-140 under the high plateau), an elliptical profile — so
 * the shelves overhang and enclose lit caverns. Those cavern floors grow Glow Daisies (6% of columns)
 * and the odd oak sapling (0.8%), and their ceilings Rock Flowers (0.3%) or strands of one to four Glow
 * Roots (0.8%). Dirt is 1-4 deep by its own noise, and the column is finished with DragonAPI's
 * {@code cleanColumn}.
 *
 * <p>Near other biomes the landmass value is pulled toward the neighbour's: within 24 blocks of a
 * non-cliff biome, hval blends linearly toward sea level (a quarter of it beside an ocean), or toward the
 * shores biome's own value beside the shores. Outside the biome, with {@code BIOMEBLEND}, columns within
 * 16 blocks of the cliffs are pulled toward the height the cliffs intend there, along V33a's
 * {@code EDGE_BLENDING} curve. Every biome read goes through a {@link BiomeMask}, so this class is a pure
 * function of seed and biome layout: the world-generation pass and the island placer both ask it.
 *
 * <p>The ring searches are upstream's (one-block rings, 7.5° steps; four-block rings for the outward
 * blend), with the offsets precomputed. {@code hval} is memoised per sampler, since the shores recursion
 * asks for the same points repeatedly.
 *
 * <p>The blocks are vanilla stone, dirt and grass, as upstream's were: its cliff-stone biome blocks are
 * commented out. Cliff stone belongs to the floating islands.
 */
public final class GlowingCliffsColumnShaper {

	/** What a column's biome is, as far as the shaper cares. */
	public enum BiomeKind {
		/** {@code ChromatiCraft.glowingcliffs}. */
		MAIN,
		/** {@code ChromatiCraft.glowingcliffsEdge}, the Luminous Cliffs Shores. */
		EDGE,
		/** Any other biome in {@code BiomeManager.oceanBiomes}. */
		OCEAN,
		OTHER;

		public boolean isGlowingCliffs() {
			return this == MAIN || this == EDGE;
		}
	}

	@FunctionalInterface
	public interface BiomeMask {
		BiomeKind at(int x, int z);
	}

	public enum GlowCliffRegion {
		WATER,
		SHORES,
		PLATEAU,
		HIGH_PLATEAU
	}

	/** A column's shape. For water columns {@code top} is the sea floor's top block. */
	public record Column(GlowCliffRegion region, int top, int caveFloor, int caveCeil, int dirt, int oceanFloor) {

		public boolean hasCave() {
			return caveFloor != Integer.MAX_VALUE;
		}

		/** Whether y is solid ground in this column, which is what an island must not overlap. */
		public boolean isSolid(int y) {
			if (region == GlowCliffRegion.WATER)
				return y < oceanFloor;
			if (y > top)
				return false;
			return !(y > caveFloor && y < caveCeil);
		}
	}

	private static final double SHORELINE_THRESHOLD = 0.3;
	private static final double MIDDLE_MIN_THRESHOLD = 0.35;
	private static final double MIDDLE_MAX_THRESHOLD = 0.4;
	private static final double UPPER_MIN_THRESHOLD = 0.7;
	private static final double UPPER_MAX_THRESHOLD = 0.75;
	private static final double LOWER_CAVE_MIN_THRESHOLD = 0.4;
	private static final double LOWER_CAVE_MAX_THRESHOLD = 0.6;
	private static final double MIDDLE_CAVE_MIN_THRESHOLD = 0.8;
	private static final double MIDDLE_CAVE_MAX_THRESHOLD = 0.95;

	private static final int MIN_OCEAN_FLOOR_Y = 24;
	private static final int MAX_OCEAN_FLOOR_Y = 48;
	public static final int SEA_LEVEL = 62;
	private static final int MIN_SHORE_Y = SEA_LEVEL;
	private static final int MAX_SHORE_Y = 72;
	private static final int MIN_MIDDLE_BOTTOM_Y = 80;
	private static final int MAX_MIDDLE_BOTTOM_Y = 96;
	private static final int MIN_MIDDLE_TOP_Y = 100;
	public static final int MAX_MIDDLE_TOP_Y = 112;
	private static final int MIN_UPPER_BOTTOM_Y = 128;
	private static final int MAX_UPPER_BOTTOM_Y = 140;
	private static final int MIN_UPPER_TOP_Y = 144;
	public static final int MAX_UPPER_TOP_Y = 160;

	/** How far {@code calcHval} looks for another biome. */
	public static final int INWARD_SEARCH = 24;
	/** How far {@code blendEdge} looks for the cliffs. */
	public static final int OUTWARD_SEARCH = 16;
	/** Everything a column can read: the outward search, then the shores recursion's inward search. */
	public static final int MASK_MARGIN = INWARD_SEARCH * 2;

	private static final Interpolation EDGE_BLENDING = new Interpolation(false).addPoint(0, 1).addPoint(0.05, 0.925)
			.addPoint(0.1, 0.85).addPoint(0.3, 0.8).addPoint(0.33, 0.6).addPoint(0.55, 0.5).addPoint(0.7, 0.35)
			.addPoint(0.75, 0.2).addPoint(0.85, 0.075).addPoint(1, 0);

	private static final double ANGLE_SEARCH_STEP = 7.5;
	private static final int ANGLES = (int)(360 / ANGLE_SEARCH_STEP);
	/** {@code floor(d*cos(a))}, {@code floor(d*sin(a))} for d up to the inward search. */
	private static final int[][][] RING = new int[INWARD_SEARCH + 1][ANGLES][2];

	static {
		for (int d = 1; d <= INWARD_SEARCH; d++) {
			for (int i = 0; i < ANGLES; i++) {
				double a = Math.toRadians(i * ANGLE_SEARCH_STEP);
				// Upstream floors x + d*cos(a). At the axis angles the trig result is a rounding residue
				// (e.g. -1.8e-16) that vanishes when added to any real coordinate, so it counts as zero.
				RING[d][i][0] = Mth.floor(snap(d * Math.cos(a)));
				RING[d][i][1] = Mth.floor(snap(d * Math.sin(a)));
			}
		}
	}

	private static double snap(double v) {
		return Math.abs(v - Math.rint(v)) < 1e-9 ? Math.rint(v) : v;
	}

	private static final Map<Long, GlowingCliffsColumnShaper> CACHE = new ConcurrentHashMap<>();

	private final NoiseGeneratorBase landmassControl;
	private final NoiseGeneratorBase upperPlateauTop;
	private final NoiseGeneratorBase upperPlateauBottom;
	private final NoiseGeneratorBase upperPlateauEdge;
	private final NoiseGeneratorBase middlePlateauCaveDepth;
	private final NoiseGeneratorBase middlePlateauTop;
	private final NoiseGeneratorBase middlePlateauBottom;
	private final NoiseGeneratorBase middlePlateauEdge;
	private final NoiseGeneratorBase lowerPlateauCaveDepth;
	private final NoiseGeneratorBase shoreHeight;
	private final NoiseGeneratorBase dirtThickness;
	private final NoiseGeneratorBase oceanDepth;
	private final NoiseGeneratorBase caveCeilNoise;

	/** V33a {@code BiomeGlowingCliffs.initTerrain}: one shaper per world seed (plus the option's shift). */
	public static GlowingCliffsColumnShaper forSeed(long worldSeed) {
		long seed = worldSeed + ChromaOptions.LUMCLIFFSEEDSHIFT.getValue();
		return CACHE.computeIfAbsent(seed, GlowingCliffsColumnShaper::new);
	}

	private GlowingCliffsColumnShaper(long seed) {
		landmassControl = new SimplexNoiseGenerator(seed).setFrequency(1D / 144D).addOctave(2.5, 0.5).addOctave(6, 0.125);
		landmassControl.clampEdge = true;

		shoreHeight = new SimplexNoiseGenerator(~seed).setFrequency(1 / 32D);
		dirtThickness = new SimplexNoiseGenerator(-seed).setFrequency(1 / 8D);
		oceanDepth = new SimplexNoiseGenerator(ReikaMathLibrary.cycleBitsRight(seed, 16)).setFrequency(1 / 32D);
		caveCeilNoise = new SimplexNoiseGenerator(ReikaMathLibrary.cycleBitsLeft(seed, 16)).setFrequency(1 / 16D);

		upperPlateauTop = new SimplexNoiseGenerator(seed * 4 + 8192).setFrequency(1 / 24D);
		upperPlateauBottom = new SimplexNoiseGenerator(seed * 8 + 4096).setFrequency(1 / 24D);
		upperPlateauEdge = new SimplexNoiseGenerator(seed * 2 + 65536).setFrequency(1 / 24D);
		middlePlateauCaveDepth = new SimplexNoiseGenerator(seed * 16 + 32).setFrequency(1 / 24D);

		middlePlateauTop = new SimplexNoiseGenerator(-seed * 4 + 4096).setFrequency(1 / 32D);
		middlePlateauBottom = new SimplexNoiseGenerator(-seed * 8 + 65536).setFrequency(1 / 32D);
		middlePlateauEdge = new SimplexNoiseGenerator(-seed * 2 + 16384).setFrequency(1 / 32D);
		lowerPlateauCaveDepth = new SimplexNoiseGenerator(-seed * 16 + 32).setFrequency(1 / 32D);
	}

	/** A biome mask with {@code hval} memoised; one per chunk or island. */
	public final class Sampler {

		private final BiomeMask mask;
		private final Map<Long, Double> hvals = new HashMap<>();

		public Sampler(BiomeMask mask) {
			this.mask = mask;
		}

		public BiomeKind biome(int x, int z) {
			return mask.at(x, z);
		}

		double hval(int x, int z, BiomeKind b) {
			long key = (((long)x & 0x3FFFFFF) << 28) ^ (((long)z & 0x3FFFFFF) << 2) ^ b.ordinal();
			Double v = hvals.get(key);
			if (v == null) {
				v = calcHval(this, x, z, b);
				hvals.put(key, v);
			}
			return v;
		}

		public Column column(int x, int z) {
			return GlowingCliffsColumnShaper.this.column(this, x, z, mask.at(x, z));
		}

		public GlowCliffRegion region(int x, int z) {
			return GlowingCliffsColumnShaper.this.getRegion(this, x, z, mask.at(x, z));
		}

		/** V33a {@code blendEdge} for a non-cliff column: the new top, or -1 for none. */
		public int blendedTop(int x, int z, int existingTop) {
			return GlowingCliffsColumnShaper.this.blendedTop(this, x, z, existingTop);
		}

		public GlowingCliffsColumnShaper shaper() {
			return GlowingCliffsColumnShaper.this;
		}
	}

	/** V33a {@code generateColumn}'s shape half, for a Luminous Cliffs column. */
	private Column column(Sampler s, int x, int z, BiomeKind b) {
		int dirtt = (int)ReikaMathLibrary.normalizeToBounds(dirtThickness.getValue(x, z), 1, 4);
		double hval = s.hval(x, z, b);
		GlowCliffRegion r = this.region(hval, x, z);
		double middlethresh = r == GlowCliffRegion.WATER ? 0 : this.calcMiddleThresh(x, z);
		double topthresh = r == GlowCliffRegion.WATER ? 0 : this.calcTopThresh(x, z);
		int none = Integer.MAX_VALUE;
		int noneCeil = Integer.MIN_VALUE;
		switch (r) {
			case WATER -> {
				int floor = this.getOceanFloor(x, z, hval);
				return new Column(r, floor - 1, none, noneCeil, dirtt, floor);
			}
			case SHORES -> {
				double cave = ReikaMathLibrary.normalizeToBounds(lowerPlateauCaveDepth.getValue(x, z),
						LOWER_CAVE_MIN_THRESHOLD, LOWER_CAVE_MAX_THRESHOLD);
				// Upstream passes the lower cave threshold, not the middle one, as the shore's upper bound.
				int top = this.getLowPlateauHeight(hval, cave);
				return new Column(r, top, none, noneCeil, dirtt, none);
			}
			case PLATEAU -> {
				double cave = ReikaMathLibrary.normalizeToBounds(lowerPlateauCaveDepth.getValue(x, z),
						LOWER_CAVE_MIN_THRESHOLD, LOWER_CAVE_MAX_THRESHOLD);
				int top = this.getMidPlateauHeight(x, z);
				if (hval < cave) {
					int caveFloor = (int)ReikaMathLibrary.linterpolate(hval, SHORELINE_THRESHOLD, cave, MIN_SHORE_Y, MAX_SHORE_Y);
					int baseCeil = (int)ReikaMathLibrary.normalizeToBounds(middlePlateauBottom.getValue(x, z),
							MIN_MIDDLE_BOTTOM_Y, MAX_MIDDLE_BOTTOM_Y);
					return this.undercut(r, x, z, dirtt, hval, middlethresh, cave, top, caveFloor, baseCeil);
				}
				return new Column(r, top, none, noneCeil, dirtt, none);
			}
			default -> {
				double cave = ReikaMathLibrary.normalizeToBounds(middlePlateauCaveDepth.getValue(x, z),
						MIDDLE_CAVE_MIN_THRESHOLD, MIDDLE_CAVE_MAX_THRESHOLD);
				int top = this.getTopPlateauHeight(x, z);
				if (hval < cave) {
					int caveFloor = (int)ReikaMathLibrary.normalizeToBounds(middlePlateauTop.getValue(x, z),
							MIN_MIDDLE_TOP_Y, MAX_MIDDLE_TOP_Y);
					int baseCeil = (int)ReikaMathLibrary.normalizeToBounds(upperPlateauBottom.getValue(x, z),
							MIN_UPPER_BOTTOM_Y, MAX_UPPER_BOTTOM_Y);
					return this.undercut(r, x, z, dirtt, hval, topthresh, cave, top, caveFloor, baseCeil);
				}
				return new Column(r, top, none, noneCeil, dirtt, none);
			}
		}
	}

	/** The shared cave-hollow arithmetic of {@code generateMidPlateau} and {@code generateUpperPlateau}. */
	private Column undercut(GlowCliffRegion r, int x, int z, int dirtt, double hval, double thresh, double caveThresh,
			int top, int caveFloor, int baseCeil) {
		double dh = baseCeil - caveFloor;
		dh *= ReikaMathLibrary.normalizeToBounds(caveCeilNoise.getValue(x, z), 0.8, 1.2);
		int caveCeil = caveFloor + 4 + (int)ReikaMathLibrary.ellipticalInterpolation(Math.abs(hval - thresh), 0,
				caveThresh - thresh, 0, dh);
		dh = top - baseCeil;
		int newTop = baseCeil + 2 + (int)ReikaMathLibrary.ellipticalInterpolation(hval - caveThresh, 0,
				caveThresh - thresh, 0, dh);
		return new Column(r, newTop, caveFloor, caveCeil, dirtt, Integer.MAX_VALUE);
	}

	private GlowCliffRegion getRegion(Sampler s, int x, int z, BiomeKind b) {
		return this.region(s.hval(x, z, b), x, z);
	}

	private GlowCliffRegion region(double hval, int x, int z) {
		if (hval < SHORELINE_THRESHOLD)
			return GlowCliffRegion.WATER;
		if (hval < this.calcMiddleThresh(x, z))
			return GlowCliffRegion.SHORES;
		if (hval < this.calcTopThresh(x, z))
			return GlowCliffRegion.PLATEAU;
		return GlowCliffRegion.HIGH_PLATEAU;
	}

	private double calcTopThresh(int x, int z) {
		return ReikaMathLibrary.normalizeToBounds(upperPlateauEdge.getValue(x, z), UPPER_MIN_THRESHOLD, UPPER_MAX_THRESHOLD);
	}

	private double calcMiddleThresh(int x, int z) {
		return ReikaMathLibrary.normalizeToBounds(middlePlateauEdge.getValue(x, z), MIDDLE_MIN_THRESHOLD, MIDDLE_MAX_THRESHOLD);
	}

	/** V33a {@code calcHval}. */
	private double calcHval(Sampler s, int x, int z, BiomeKind b) {
		double hval = ReikaMathLibrary.normalizeToBounds(landmassControl.getValue(x, z), 0, 1);
		int[] point = new int[3];
		int d = this.getDistanceFactor(s, x, z, b, point);
		if (d > 0) {
			BiomeKind other = BiomeKind.values()[point[2]];
			double dm = other == BiomeKind.EDGE ? s.hval(point[0], point[1], BiomeKind.EDGE) : getBlendedBiomeHeight(other);
			double dh = hval - dm;
			hval = dm + dh * d / (double)INWARD_SEARCH;
		}
		return hval;
	}

	/**
	 * V33a {@code getDistanceFactor}: the nearest ring holding a biome that is neither the main cliffs
	 * nor the column's own, and the first such point in angle order on it. Returns the distance (0 for
	 * none) and writes the point and its biome into {@code out}.
	 */
	private int getDistanceFactor(Sampler s, int x, int z, BiomeKind biome, int[] out) {
		for (int d = 1; d <= INWARD_SEARCH; d++) {
			for (int i = 0; i < ANGLES; i++) {
				int dx = x + RING[d][i][0];
				int dz = z + RING[d][i][1];
				BiomeKind b = s.biome(dx, dz);
				if (b != BiomeKind.MAIN && b != biome) {
					out[0] = dx;
					out[1] = dz;
					out[2] = b.ordinal();
					return d;
				}
			}
		}
		return 0;
	}

	private static double getBlendedBiomeHeight(BiomeKind b) {
		if (ChromaOptions.BIOMEBLEND.getState()) {
			if (b == BiomeKind.OCEAN)
				return SHORELINE_THRESHOLD * 0.25;
			// CHROMA-PORT: Satisforestry's Pink Forest blended to MIDDLE_MAX_THRESHOLD + 0.1.
			return SHORELINE_THRESHOLD;
		}
		return SHORELINE_THRESHOLD * 0.75;
	}

	/** V33a {@code blendEdge}: -1 when the column is out of reach of the cliffs or blending is off. */
	private int blendedTop(Sampler s, int x, int z, int y) {
		if (!ChromaOptions.BIOMEBLEND.getState())
			return -1;
		int mind = Integer.MAX_VALUE;
		int look = OUTWARD_SEARCH;
		int lx = 0;
		int lz = 0;
		for (int d = 1; d <= look; d += 4) {
			for (int i = 0; i < ANGLES; i++) {
				int dx = x + RING[d][i][0];
				int dz = z + RING[d][i][1];
				if (s.biome(dx, dz).isGlowingCliffs()) {
					if (d < mind) {
						mind = d;
						lx = dx;
						lz = dz;
						look = mind - 1;
					}
					break;
				}
			}
		}
		if (mind == Integer.MAX_VALUE)
			return -1;
		double f2 = EDGE_BLENDING.getValue(mind / (double)OUTWARD_SEARCH);
		return (int)(f2 * this.calcIntendedHeight(s, lx, lz) + (1 - f2) * y);
	}

	/** V33a {@code calcIntendedHeight}, which always asks as the shores biome. */
	private double calcIntendedHeight(Sampler s, int x, int z) {
		double hval = s.hval(x, z, BiomeKind.EDGE);
		double middle = this.calcMiddleThresh(x, z);
		double top = this.calcTopThresh(x, z);
		if (hval < SHORELINE_THRESHOLD)
			return this.getOceanFloor(x, z, hval) - 1;
		else if (hval == SHORELINE_THRESHOLD)
			return SEA_LEVEL;
		else if (hval < middle)
			return this.getLowPlateauHeight(hval, middle);
		else if (hval < top)
			return this.getMidPlateauHeight(x, z);
		return this.getTopPlateauHeight(x, z);
	}

	private int getLowPlateauHeight(double hval, double max) {
		return (int)ReikaMathLibrary.linterpolate(hval, SHORELINE_THRESHOLD, max, MIN_SHORE_Y, MAX_SHORE_Y);
	}

	private int getMidPlateauHeight(int x, int z) {
		return (int)ReikaMathLibrary.normalizeToBounds(middlePlateauTop.getValue(x, z), MIN_MIDDLE_TOP_Y, MAX_MIDDLE_TOP_Y);
	}

	private int getTopPlateauHeight(int x, int z) {
		return (int)ReikaMathLibrary.normalizeToBounds(upperPlateauTop.getValue(x, z), MIN_UPPER_TOP_Y, MAX_UPPER_TOP_Y);
	}

	/** V33a {@code getOceanFloor}: the lowest water block. */
	private int getOceanFloor(int x, int z, double hval) {
		int min = (int)ReikaMathLibrary.normalizeToBounds(oceanDepth.getValue(x, z), MIN_OCEAN_FLOOR_Y, MAX_OCEAN_FLOOR_Y);
		return (int)ReikaMathLibrary.linterpolate(hval, 0, SHORELINE_THRESHOLD, min, SEA_LEVEL + 1);
	}

	/** One column's blocks, bottom 0 to 255, in upstream's vocabulary. */
	public enum Cell {
		AIR, STONE, DIRT, GRASS, WATER, GLOW_DAISY, SAPLING, ROCK_FLOWER, GLOW_ROOT;

		public boolean isPlant() {
			return this == GLOW_DAISY || this == SAPLING || this == ROCK_FLOWER || this == GLOW_ROOT;
		}
	}

	public static final int HEIGHT = 256;

	/**
	 * V33a {@code generateColumn} in full for a Luminous Cliffs column: the blocks, the cavern plants,
	 * and {@code cleanColumn}. The plant rolls use {@code TerrainShaper}'s per-column seed, so they are
	 * stable; upstream drew them from DragonAPI's shared unseeded random.
	 */
	public Cell[] cells(Column c, int x, int z) {
		Cell[] col = new Cell[HEIGHT];
		if (c.region() == GlowCliffRegion.WATER) {
			int floor = c.oceanFloor();
			for (int i = 0; i < HEIGHT; i++)
				col[i] = i < floor ? Cell.STONE : i <= SEA_LEVEL ? Cell.WATER : Cell.AIR;
			for (int h = 1; h <= c.dirt(); h++)
				if (floor - h >= 0 && floor - h < HEIGHT)
					col[floor - h] = Cell.DIRT;
		}
		else {
			int top = c.top();
			int caveFloor = c.caveFloor();
			int caveCeil = c.caveCeil();
			int dirt = c.dirt();
			for (int i = 0; i < HEIGHT; i++) {
				Cell b = Cell.STONE;
				if (i > top || (i > caveFloor && i < caveCeil))
					b = Cell.AIR;
				else if (i == top || (i == caveFloor && i < caveCeil))
					b = Cell.GRASS;
				else if (i >= top - dirt || (i >= caveFloor - dirt && i < caveCeil))
					b = Cell.DIRT;
				col[i] = b;
			}
			if (caveFloor > 0 && caveFloor < HEIGHT) {
				Random rand = columnRandom(x, z);
				if (ReikaRandomHelper.doWithChance(6, rand))
					set(col, caveFloor + 1, Cell.GLOW_DAISY);
				else if (ReikaRandomHelper.doWithChance(0.008, rand))
					set(col, caveFloor + 1, Cell.SAPLING);
				if (ReikaRandomHelper.doWithChance(0.003, rand)) {
					set(col, caveCeil - 1, Cell.ROCK_FLOWER);
				}
				else if (ReikaRandomHelper.doWithChance(0.008, rand)) {
					int nt = Math.min(ReikaRandomHelper.getRandomBetween(1, 4, rand), caveCeil - caveFloor - 3);
					for (int i = 1; i <= nt; i++)
						set(col, caveCeil - i, Cell.GLOW_ROOT);
				}
			}
		}
		cleanColumn(col);
		return col;
	}

	private static void set(Cell[] col, int y, Cell c) {
		if (y >= 0 && y < HEIGHT)
			col[y] = c;
	}

	/** DragonAPI {@code TerrainShaper}'s per-column seed. */
	private static Random columnRandom(int x, int z) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		long seed = chunkX * 341873128712L + chunkZ * 132897987541L;
		seed ^= (x % 16) * 287238347L + (z % 16) * 8258735447L;
		Random rand = new Random(seed);
		rand.nextBoolean();
		rand.nextBoolean();
		return rand;
	}

	/**
	 * DragonAPI {@code TerrainShaper.cleanColumn}: dirt with nothing (or a plant) under it becomes stone,
	 * and grass is buried to dirt when something is on top and the block under it is a plant — upstream
	 * tests the block below there, not above, and its third branch (uncovered dirt to grass) can never run
	 * because the first branch already claims every dirt block. Both are kept as written.
	 */
	private static void cleanColumn(Cell[] col) {
		for (int i = 1; i < HEIGHT; i++) {
			if (col[i] == Cell.DIRT) {
				Cell below = col[i - 1];
				if (below == Cell.AIR || below.isPlant())
					col[i] = Cell.STONE;
			}
			else if (i < HEIGHT - 1 && col[i] == Cell.GRASS) {
				if (col[i + 1] != Cell.AIR && col[i - 1].isPlant())
					col[i] = Cell.DIRT;
			}
		}
	}
}
