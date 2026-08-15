/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.StructureCalculator.StructurePlacement;
import reika.chromaticraft.world.dimension.biome.ProximaBiomeType;
import reika.chromaticraft.world.dimension.biome.ProximaBiomes;
import reika.chromaticraft.world.dimension.biome.ProximaSubBiomes;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.math.LobulatedCurve;

/**
 * V33a {@code BiomeDistributor}: Proxima's global biome map.
 *
 * <p>It is not a noise field. The dimension's biomes are painted once, into a square byte map that
 * tiles over the world, by a five-stage process:
 *
 * <ol>
 * <li><b>Distribute dots.</b> Each primary biome gets {@code spawnWeight} blobs dropped at random
 *     still-empty cells — literally the count, so Glowing Forest's 10 outnumber Lumen Skylands' 2.</li>
 * <li><b>Fill empty spaces.</b> Every remaining empty cell is visited in shuffled order and painted
 *     with a biome drawn from a weighted count of its 13x13 neighbourhood, so gaps close by growing
 *     inward from whatever surrounds them. Repeats until nothing is left empty.</li>
 * <li><b>Feather edges.</b> Any cell that shares its biome with none of its four orthogonal
 *     neighbours is absorbed into one of them, which removes single-cell speckle.</li>
 * <li><b>Add internal biomes.</b> For each parent blob, with the sub-biome's own probability, a
 *     smaller blob is stamped inside it — but only over cells still holding the parent.</li>
 * <li><b>Add structure biomes.</b> One irregular 96-384 block region per element and one for the
 *     monument. These are not painted into the map; they are evaluated per query.</li>
 * </ol>
 *
 * <p>{@link #getBiome} then answers in priority order: a structure or monument region if the query is
 * inside one, then Luminescent Sanctuary if it is inside the central region, and only otherwise the
 * painted map.
 *
 * <h2>Deliberate departures, all documented</h2>
 *
 * <ul>
 * <li><b>The spreader machinery is dropped as dead code.</b> V33a declares a {@code Spreader} inner
 *     class and a {@code spreadDots()} stage, but the only code that would ever add a spreader is
 *     commented out in {@code distributeDots}, so the collection is always empty and the stage is
 *     always a no-op. Porting it would be porting something upstream does not run.</li>
 * <li><b>Shuffling is seeded.</b> V33a calls {@code Collections.shuffle(li)}, which uses a shared
 *     unseeded Random, so the fill order — and therefore the whole map — differed on every run from
 *     the same seed. It now shuffles with the dimension-seeded Random. Same distribution, now
 *     reproducible; this is the same class of defect already corrected in the structure ring.</li>
 * <li><b>The wait loop is gone.</b> {@code addStructureBiomes} polls the structure generator in a
 *     {@code Thread.sleep(100)} loop because upstream runs generators on threads. The port orders
 *     them through {@link ProximaGenerators} and fails loudly on a mis-ordered call instead.</li>
 * <li><b>The map size is a parameter.</b> V33a's own {@code SIZE = 4096;//2048;//4096;} shows it was
 *     varied, and {@code placeBlob} already scales every radius by {@code SIZE/4096}. Making it a
 *     constructor argument is what lets a focused test exercise the real algorithm quickly.</li>
 * </ul>
 */
public final class BiomeDistributor {

	/** V33a SIZE: the map is this many cells square and tiles over the world. */
	public static final int DEFAULT_SIZE = 4096;

	/** V33a SCALE_FACTOR: two world blocks per map cell. */
	public static final int SCALE_FACTOR = 2;

	private static final double MIN_STRUCTURE_RADIUS = 96;
	private static final double MAX_STRUCTURE_RADIUS = 384;

	/** V33a: the structure and monument regions are twelve-lobe curves. */
	private static final int STRUCTURE_LOBES = 12;

	/** V33a placeBlob: six lobes at half-degree resolution, 384 +/- 48 cells before jitter. */
	private static final int BLOB_LOBES = 6;
	private static final double BLOB_ANGLE_STEP = 0.5;
	private static final double BLOB_RADIUS = 384;
	private static final double BLOB_VARIATION = 48;

	/**
	 * V33a keeps the map, the blobs and the region curves in static fields, because Proxima is one
	 * level and every consumer asks statically. The state lives on the instance here so it is
	 * testable; this only tracks which instance the static accessors answer for.
	 */
	private static volatile BiomeDistributor active;

	private final int size;
	private final long seed;
	private final Random rand;

	/**
	 * The painted map. Index 0 means "empty"; a primary biome is {@code ordinal+1} and a sub-biome is
	 * {@code ordinal + primaryCount + 1}, exactly as V33a packs them into one byte.
	 */
	private final byte[][] biomes;
	private final ProximaBiomeType[] biomeList;

	private final MultiMap<ProximaBiomeType, Point> blobLocations = new MultiMap<>();
	private final EnumMap<CrystalElement, LobulatedCurve> structureBlobs = new EnumMap<>(CrystalElement.class);
	private LobulatedCurve monumentBlob;

	private StructureCalculator structures;

	public BiomeDistributor(long seed) {
		this(seed, DEFAULT_SIZE);
	}

	public BiomeDistributor(long seed, int size) {
		this.seed = seed;
		this.size = size;
		rand = new Random(seed);
		biomes = new byte[size][size];
		biomeList = buildBiomeList();
	}

	/** V33a buildBiomeList: primaries then sub-biomes, with slot 0 left null for "empty". */
	private static ProximaBiomeType[] buildBiomeList() {
		ProximaBiomeType[] arr =
				new ProximaBiomeType[ProximaBiomes.biomeList.length + ProximaSubBiomes.biomeList.length + 1];
		System.arraycopy(ProximaBiomes.biomeList, 0, arr, 1, ProximaBiomes.biomeList.length);
		System.arraycopy(ProximaSubBiomes.biomeList, 0, arr, 1 + ProximaBiomes.biomeList.length,
				ProximaSubBiomes.biomeList.length);
		return arr;
	}

	private ProximaBiomeType byIndex(byte index) {
		return biomeList[index];
	}

	private static byte indexOf(ProximaBiomeType b) {
		if (b == null)
			return 0;
		int offset = b instanceof ProximaSubBiomes ? ProximaBiomes.biomeList.length : 0;
		return (byte)(b.ordinal() + offset + 1);
	}

	/**
	 * V33a run(). {@code structures} must already have placed its ring and the central region must
	 * already exist, both of which {@link ProximaGenerators} orders.
	 */
	public BiomeDistributor generate(StructureCalculator structures) {
		if (!structures.arePositionsDetermined())
			throw new IllegalStateException("Proxima biomes cannot be distributed before the structure "
					+ "ring is placed; run ProximaGenerators.Generator.STRUCTURE first");
		this.structures = structures;
		blobLocations.clear();
		structureBlobs.clear();
		monumentBlob = null;

		this.distributeDots();
		// V33a's spreadDots() sits here and is a guaranteed no-op; see the class comment.
		boolean filled = this.fillEmptySpaces();
		while (filled)
			filled = this.fillEmptySpaces();
		this.featherEdges();
		this.addInternalBiomes();
		this.addStructureBiomes();

		active = this;
		ProximaGenerators.finish(ProximaGenerators.Generator.BIOME);
		return this;
	}

	/** V33a distributeDots: spawnWeight blobs per primary biome, each at a random still-empty cell. */
	private void distributeDots() {
		for (ProximaBiomes b : ProximaBiomes.biomeList) {
			for (int k = 0; k < b.spawnWeight; k++) {
				int dx = rand.nextInt(size);
				int dz = rand.nextInt(size);
				while (this.byIndex(biomes[dx][dz]) != null) {
					dx = rand.nextInt(size);
					dz = rand.nextInt(size);
				}
				this.placeBlob(b, dx, dz, 1, null);
			}
		}
	}

	/**
	 * V33a placeBlob: ray-march a six-lobe curve at half-degree steps, adding a triangular jitter of
	 * up to three d9 rolls to each radius, and paint a radius-2 disc at every step along the ray.
	 *
	 * @param over only cells currently holding this biome are overwritten; null means only empty cells
	 */
	private void placeBlob(ProximaBiomeType b, int dx, int dz, double f, ProximaBiomeType over) {
		f *= size / (double)DEFAULT_SIZE;
		blobLocations.addValue(b, new Point(dx, dz));
		LobulatedCurve c = new LobulatedCurve(BLOB_RADIUS * f, BLOB_VARIATION * f, BLOB_LOBES, BLOB_ANGLE_STEP)
				.generate(rand);
		for (double d = 0; d < 360; d += BLOB_ANGLE_STEP) {
			double r = c.getRadius(d) + f * (rand.nextInt(9) + rand.nextInt(9) + rand.nextInt(9));
			for (double dr = 0; dr <= r; dr += 0.5) {
				double ax = dx + dr * Math.cos(Math.toRadians(d));
				double az = dz + dr * Math.sin(Math.toRadians(d));
				this.paint(Mth.floor(ax), Mth.floor(az), b, 2, over);
			}
		}
	}

	/**
	 * V33a fillEmptySpaces: collect every empty cell, shuffle, and grow a biome into each from its own
	 * neighbourhood. Returns whether anything was left to fill, so the caller can repeat.
	 */
	private boolean fillEmptySpaces() {
		List<Point> empty = new ArrayList<>();
		for (int i = 0; i < size; i++)
			for (int k = 0; k < size; k++)
				if (this.byIndex(biomes[i][k]) == null)
					empty.add(new Point(i, k));
		if (empty.isEmpty())
			return false;
		Collections.shuffle(empty, rand);
		for (Point p : empty)
			this.fillEmptySpace(p.x, p.y);
		return true;
	}

	/**
	 * V33a fillEmptySpace: a weighted draw over the 13x13 neighbourhood, painted as a radius-4 disc.
	 *
	 * <p>V33a builds a {@code CountMap} and calls {@code asWeightedRandom().getRandomEntry()}. That is
	 * doubly non-reproducible: {@code WeightedRandom} rolls its own unseeded {@code RandomSource}, and
	 * it walks a {@code HashMap} keyed by enums, whose iteration order follows identity hash codes and
	 * therefore changes between JVM runs. Since this decides the biome of every gap cell on the map,
	 * either alone would make Proxima regenerate differently from the same seed. The draw is done here
	 * instead, over the compact biome index in ascending order and off the dimension-seeded Random —
	 * the same distribution, made reproducible.
	 */
	private void fillEmptySpace(int x, int z) {
		int[] counts = new int[biomeList.length];
		int total = 0;
		int r = 6;
		for (int i = -r; i <= r; i++) {
			for (int k = -r; k <= r; k++) {
				byte b = this.getAdj(x, z, i, k);
				if (b != 0) {
					counts[b]++;
					total++;
				}
			}
		}
		if (total == 0)
			return;
		int roll = rand.nextInt(total);
		for (int i = 1; i < counts.length; i++) {
			roll -= counts[i];
			if (roll < 0) {
				this.paint(x, z, biomeList[i], 4, null);
				return;
			}
		}
	}

	/**
	 * V33a featherEdges: a cell matching none of its four orthogonal neighbours is absorbed into the
	 * last one checked, which erases single-cell speckle left by the fill pass.
	 */
	private void featherEdges() {
		for (int i = 0; i < size; i++) {
			for (int k = 0; k < size; k++) {
				byte b = biomes[i][k];
				byte replacement = 0;
				boolean isolated = true;
				for (Direction dir : Direction.Plane.HORIZONTAL) {
					byte neighbour = this.getAdj(i, k, dir.getStepX(), dir.getStepZ());
					if (b == neighbour) {
						isolated = false;
						break;
					}
					replacement = neighbour;
				}
				if (isolated)
					biomes[i][k] = replacement;
			}
		}
	}

	/**
	 * V33a addInternalBiomes: for every blob of a parent biome, roll the sub-biome's spawn chance and,
	 * if it hits, look for a cell within 16 that is still the parent (up to 200 tries) and stamp a
	 * quarter-to-three-quarter sized sub-blob there, overwriting only the parent.
	 */
	private void addInternalBiomes() {
		for (ProximaBiomes b : ProximaBiomes.biomeList) {
			ProximaSubBiomes s = b.getSubBiome();
			if (s == null)
				continue;
			Collection<Point> blobs = blobLocations.get(b);
			if (blobs == null)
				continue;
			for (Point p : List.copyOf(blobs)) {
				if (rand.nextDouble() >= s.spawnWeight)
					continue;
				int dx = this.getAdj(p.x, rand.nextInt(33) - 16);
				int dz = this.getAdj(p.y, rand.nextInt(33) - 16);
				int tries = 0;
				while (this.byIndex(biomes[dx][dz]) != b && tries < 200) {
					dx = this.getAdj(p.x, rand.nextInt(33) - 16);
					dz = this.getAdj(p.y, rand.nextInt(33) - 16);
					tries++;
				}
				if (this.byIndex(biomes[dx][dz]) == b)
					this.placeBlob(s, dx, dz, 0.25 + rand.nextDouble() * 0.5, b);
			}
		}
	}

	/**
	 * V33a addStructureBiomes: one irregular region per element and one for the monument. These are
	 * never painted into the map — they are evaluated per query in {@link #getBiome}, which is why
	 * moving a structure does not require repainting anything.
	 */
	private void addStructureBiomes() {
		for (StructurePlacement p : structures.getPlacements())
			structureBlobs.put(p.color, LobulatedCurve
					.fromMinMaxRadii(MIN_STRUCTURE_RADIUS, MAX_STRUCTURE_RADIUS, STRUCTURE_LOBES).generate(rand));
		monumentBlob = LobulatedCurve
				.fromMinMaxRadii(MIN_STRUCTURE_RADIUS, MAX_STRUCTURE_RADIUS, STRUCTURE_LOBES).generate(rand);
	}

	/**
	 * V33a getBiome(x, z), in its exact priority order: a structure or monument region wins, then the
	 * central region, then the painted map.
	 */
	public ProximaBiomeType biomeAt(int x, int z) {
		double d = this.getDistanceToNearestStructure(x, z, MAX_STRUCTURE_RADIUS);
		if (d <= MAX_STRUCTURE_RADIUS) {
			StructurePlacement p = structures.getNearestStructureWithinRange(x, z, d);
			LobulatedCurve map;
			int dx;
			int dz;
			if (p == null) {
				// Nothing but the monument is within range, so this is the monument's own region.
				map = monumentBlob;
				dx = x - structures.getMonumentPosition().getX();
				dz = z - structures.getMonumentPosition().getZ();
			}
			else {
				map = structureBlobs.get(p.color);
				dx = x - p.getEntryPosX();
				dz = z - p.getEntryPosZ();
			}
			if (map != null && map.isPointInsideCurve(dx, dz))
				return map == monumentBlob ? ProximaBiomes.MONUMENT : ProximaBiomes.STRUCTURE;
		}
		if (RegionMapper.isPointInCentralRegion(x, z))
			return ProximaBiomes.CENTER;
		return this.byIndex(this.getAdj(x / SCALE_FACTOR, z / SCALE_FACTOR, 0, 0));
	}

	/**
	 * V33a getDistanceToNearestStructureBlockCoordsWithinRange. The monument is always considered, at
	 * any distance; structures only count if they are already within {@code r}. That asymmetry is what
	 * makes a null result in {@link #biomeAt} mean "the monument is nearest".
	 */
	private double getDistanceToNearestStructure(int x, int z, double r) {
		double dx = structures.getMonumentPosition().getX() - x;
		double dz = structures.getMonumentPosition().getZ() - z;
		double d = Math.sqrt(dx * dx + dz * dz);
		for (StructurePlacement s : structures.getPlacements()) {
			// V33a measures from the structure's central location, which before its generator runs is
			// the placement itself.
			dx = x - s.placement().getX();
			dz = z - s.placement().getZ();
			double dd = Math.sqrt(dx * dx + dz * dz);
			if (dd <= r)
				d = Math.min(d, dd);
		}
		return d;
	}

	/** The static face every V33a consumer uses. */
	public static ProximaBiomeType getBiome(int x, int z) {
		BiomeDistributor distributor = active;
		return distributor == null ? null : distributor.biomeAt(x, z);
	}

	public static BiomeDistributor getActive() {
		return active;
	}

	public static void clear() {
		active = null;
	}

	private byte getAdj(int x, int z, int dx, int dz) {
		return biomes[this.getAdj(x, dx)][this.getAdj(z, dz)];
	}

	private void setAdj(int x, int z, int dx, int dz, ProximaBiomeType b) {
		biomes[this.getAdj(x, dx)][this.getAdj(z, dz)] = indexOf(b);
	}

	/** V33a getAdj: the map wraps, so every coordinate is valid. */
	private int getAdj(int p, int d) {
		return (size + (p + d) % size) % size;
	}

	/** V33a paint: a filled disc, writing only where the current value is {@code over}. */
	private void paint(int x, int z, ProximaBiomeType b, int r, ProximaBiomeType over) {
		for (int i = -r; i <= r; i++)
			for (int k = -r; k <= r; k++)
				if (i * i + k * k <= r * r && this.byIndex(this.getAdj(x, z, i, k)) == over)
					this.setAdj(x, z, i, k, b);
	}

	/**
	 * V33a getDataForPacket: the client is sent every eighth cell in both axes, so a 4096 map ships as
	 * 512x512 bytes. The coarse copy is enough for the map overlay it drives.
	 */
	public byte[] getDataForPacket() {
		int n = 8;
		int span = size / n;
		byte[] out = new byte[span * span];
		for (int i = 0; i < span; i++)
			for (int k = 0; k < span; k++)
				out[i * span + k] = biomes[i * n][k * n];
		return out;
	}

	public void fillFromPacket(byte[] data) {
		int n = 8;
		int span = size / n;
		for (int i = 0; i < span; i++)
			for (int k = 0; k < span; k++)
				biomes[i * n][k * n] = data[i * span + k];
	}

	public int getSize() {
		return size;
	}

	/** Where each biome's blobs were seeded, which is what the sub-biome pass walks. */
	public Collection<Point> getBlobLocations(ProximaBiomeType b) {
		Collection<Point> c = blobLocations.get(b);
		return c == null ? List.of() : Collections.unmodifiableCollection(c);
	}

	public Collection<ProximaBiomeType> getBlobbedBiomes() {
		return Collections.unmodifiableCollection(blobLocations.keySet());
	}

	/**
	 * V33a createImage: the development visualiser for the finished map, kept because verifying a
	 * painted biome layout by eye is the only practical check on the next slices. Each primary biome
	 * gets an evenly spaced hue, sub-biomes are drawn at two thirds brightness, and Structure Field is
	 * flat grey.
	 */
	public File createImage(String phase) throws IOException {
		File f = new File(reika.dragonapi.DragonAPI.getMinecraftDirectory(),
				"DimensionMap/" + seed + "L/" + System.nanoTime() + "_" + phase + ".png");
		f.getParentFile().mkdirs();
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < size; i++) {
			for (int k = 0; k < size; k++) {
				ProximaBiomeType actual = this.byIndex(biomes[i][k]);
				ProximaBiomeType b = actual instanceof ProximaSubBiomes sub ? sub.getParent() : actual;
				int color = b != null
						? 0xff000000 | Color.HSBtoRGB(b.ordinal() / (float)ProximaBiomes.biomeList.length, 1, 1)
						: 0xffffffff;
				if (actual instanceof ProximaSubBiomes)
					color = reika.dragonapi.libraries.rendering.ReikaColorAPI
							.getColorWithBrightnessMultiplier(color, 0.67F);
				if (actual == ProximaBiomes.STRUCTURE)
					color = 0xff606060;
				img.setRGB(i, k, color);
			}
		}
		ImageIO.write(img, "png", f);
		return f;
	}

	/** V33a getStateMessage. */
	public String getStateMessage() {
		return "Biome array populated, " + size + "x" + size + " with " + blobLocations.totalSize()
				+ " biome patches of " + blobLocations.keySet().size() + " types.";
	}
}
