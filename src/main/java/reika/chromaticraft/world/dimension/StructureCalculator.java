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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.dimension.DimensionStructureType.ProximaStructureGenerator;
import reika.chromaticraft.world.dimension.DimensionStructureType.StructureTypeData;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code StructureCalculator}: decides which puzzle structure each crystal element gets and
 * where in Proxima it sits.
 *
 * <p>Placement is separable from puzzle contents because V33a's {@code startCalculate} sets
 * {@code entryX/entryZ} to the placement coordinate up front and only lets {@code calculate()} refine
 * them afterwards. Ported puzzle generators register through
 * {@link DimensionStructureType#registerGenerator}; types without one remain excluded by V33a's own
 * completeness gate.
 *
 * <p>Everything downstream in the dimension needs exactly this and nothing more:
 * {@code RegionMapper} blocks on {@link #arePositionsDetermined()} and sizes the central region from
 * {@link #getMaximumDistanceFromOrigin()}, and {@code BiomeDistributor} paints its Structure Field
 * and Monument Field biomes around the placements. That is why this lands before the biome layer.
 *
 * <h2>The two independent random sources, kept separate on purpose</h2>
 *
 * V33a uses two: {@code seededRand}, seeded from a per-installation file, chooses which structure
 * type each element gets; the inherited {@code rand}, seeded from the persistent dimension seed,
 * chooses the origin and the ring geometry. Both are preserved. The file-backed seed is what
 * {@link #assignSeed} ships to clients so a client can recompute the same colour-to-type map without
 * knowing the world; upstream's own comment says it wanted the world seed and could not reach one
 * from outside a world, which 26.2 no longer prevents — but changing it would change every existing
 * assignment, so it is left as a documented future option rather than silently altered.
 *
 * <h2>One deliberate, documented deviation</h2>
 *
 * V33a computes {@code structureOriginX/Z} and each structure's radius through
 * {@code ReikaRandomHelper.getRandomPlusMinus(base, range)}, whose two-argument form draws from
 * DragonAPI's <b>global unseeded</b> Random. The surrounding class carefully seeds two Randoms and
 * then does not use either for these three values, so every recomputation of the layout would move
 * the structures. This port passes the seeded {@code rand} to the three-argument overload instead, so
 * the layout is reproducible from the dimension seed. The distribution is identical; only its
 * determinism changes, and worldgen that is not deterministic from its seed is not portable.
 */
public class StructureCalculator {

	/** V33a: how far the whole structure ring's centre may wander from the world origin. */
	public static final int STRUCTURE_CENTER_VARIATION = 6000; //was 10K, then 4K

	/** V33a: the ring radius each structure is placed at, before variation. */
	public static final int BASE_RADIUS = 5000;
	public static final int RADIUS_VARIATION = 3000; //was +/- 4000, then 2000

	/** V33a: the sixteen elements are spread evenly around the ring. */
	private static final double ANGLE_PER_ELEMENT = 22.5;

	private static final String SEED_FILE = "ChromatiCraft_Data/DimensionGen.dat";
	private static final String SEED_PREFIX = "Seed:";
	/** V33a {@code maxAttempts}; attempt zero plus ten retries. */
	private static final int MAX_RETRIES = 10;

	private static boolean seedNeedsRecalc = false;
	private static long clientDimensionSeed;

	/** V33a allowUnfinishedStructures: lets a dev build assign generators that do not complete. */
	public static boolean allowUnfinishedStructures;

	private final Random rand;
	private final Random seededRand;

	private final List<StructurePlacement> placements = new ArrayList<>();
	private final EnumMap<CrystalElement, StructureTypeData> typeAssignment =
			new EnumMap<>(CrystalElement.class);

	private int structureOriginX;
	private int structureOriginZ;
	private float structureAngleOrigin;

	private volatile boolean positionsDetermined;

	public StructureCalculator(long seed) {
		rand = new Random(seed);
		seededRand = new Random(generateOrGetGenSeed());
	}

	/**
	 * V33a {@code StructurePair}, minus the live generator. It carries the element, the assigned
	 * structure type, the ring placement, and — once its generator is ported and has run — that
	 * generator, whose entry position may have moved away from {@link #placement()}.
	 */
	public static final class StructurePlacement {

		public final CrystalElement color;
		public final DimensionStructureType type;
		public final int generationIndex;
		private final BlockPos placement;
		private ProximaStructureGenerator generator;

		StructurePlacement(CrystalElement color, DimensionStructureType type, int generationIndex,
				int x, int z) {
			this.color = color;
			this.type = type;
			this.generationIndex = generationIndex;
			placement = new BlockPos(x, 0, z);
		}

		/** Where the ring put this structure. V33a passes exactly this into {@code startCalculate}. */
		public BlockPos placement() {
			return placement;
		}

		/**
		 * V33a {@code generator.getEntryPosX()/getEntryPosZ()}, which {@code startCalculate} seeds from
		 * the placement and only {@code calculate()} moves. Before a generator runs, the entry is the
		 * placement — which is the same answer upstream gives at that point.
		 */
		public int getEntryPosX() {
			return generator != null ? generator.getEntryPosX() : placement.getX();
		}

		public int getEntryPosZ() {
			return generator != null ? generator.getEntryPosZ() : placement.getZ();
		}

		public ProximaStructureGenerator getGenerator() {
			return generator;
		}

		public StructureTypeData typeData() {
			return new StructureTypeData(color, type, generationIndex);
		}

		@Override
		public String toString() {
			return color.name() + " " + type + " @ " + placement.getX() + ", " + placement.getZ();
		}
	}

	/**
	 * V33a run(): assign types, pick the ring, place every element, then site the monument at the ring
	 * centre. Positions are published before the per-structure work so {@code RegionMapper} — which
	 * polls {@link #arePositionsDetermined()} — is released as early as upstream releases it.
	 */
	public void generate() {
		this.initSeed();
		placements.clear();
		typeAssignment.clear();
		DimensionStructureType.resetCachedGenerators();

		this.assignTypes();

		structureOriginX = ReikaRandomHelper.getRandomPlusMinus(0, STRUCTURE_CENTER_VARIATION, rand);
		structureOriginZ = ReikaRandomHelper.getRandomPlusMinus(0, STRUCTURE_CENTER_VARIATION, rand);
		structureAngleOrigin = rand.nextFloat() * 360;

		for (CrystalElement color : CrystalElement.elements) {
			StructureTypeData data = typeAssignment.get(color);
			if (data == null)
				continue;
			double angle = Math.toRadians(structureAngleOrigin + color.ordinal() * ANGLE_PER_ELEMENT);
			int radius = ReikaRandomHelper.getRandomPlusMinus(BASE_RADIUS, RADIUS_VARIATION, rand);
			placements.add(new StructurePlacement(color, data.type(), data.generationIndex(),
					structureOriginX + (int)(radius * Math.cos(angle)),
					structureOriginZ + (int)(radius * Math.sin(angle))));
		}

		positionsDetermined = true;
		// V33a publishes positions before the per-structure work so RegionMapper, which polls
		// arePositionsDetermined() in a sleep loop, is released as early as possible.
		ProximaGenerators.finish(ProximaGenerators.Generator.STRUCTURE);

		for (StructurePlacement placement : List.copyOf(placements))
			this.layOutStructure(placement);
	}

	/**
	 * V33a's assignment loop: draw a usable type per element without replacement, and when the usable
	 * set runs out, refill it and bump the generation index so a reused type still produces a distinct
	 * structure. With no generator ported yet the usable set is empty and no element is assigned; the
	 * ring geometry above is unaffected, which is precisely why the biome layer can be built on this.
	 */
	private void assignTypes() {
		List<DimensionStructureType> usable = this.getUsableStructures();
		if (usable.isEmpty()) {
			ChromatiCraft.LOGGER.info("No Proxima structure generator is ported yet, so no structure "
					+ "types were assigned. Ring geometry and the monument are still placed.");
			return;
		}
		int generationIndex = 0;
		for (CrystalElement color : CrystalElement.elements) {
			DimensionStructureType type = usable.remove(seededRand.nextInt(usable.size()));
			typeAssignment.put(color, new StructureTypeData(color, type, generationIndex));
			if (usable.isEmpty()) {
				usable = this.getUsableStructures();
				generationIndex++;
			}
		}
	}

	/**
	 * V33a tryGenerate/doGenerate: hand the placement to its generator, retrying ten times after the
	 * initial failure. A type without a registered generator is not assigned in the first place; the
	 * null guard remains for development layouts assembled while registration changes.
	 */
	private void layOutStructure(StructurePlacement placement) {
		ProximaStructureGenerator generator = placement.type.createGenerator();
		if (generator == null)
			return;
		if (generator instanceof reika.chromaticraft.world.dimension.structure.StructureGeneratorBase base)
			base.setType(placement.type, placement.generationIndex);
		for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
			try {
				generator.startCalculate(placement.color, placement.placement().getX(),
						placement.placement().getZ(), rand);
				placement.generator = generator;
				return;
			}
			catch (OutOfMemoryError e) {
				throw e;
			}
			catch (Throwable e) {
				generator.clear();
				boolean retry = attempt < MAX_RETRIES;
				ChromatiCraft.LOGGER.error("Error calculating structure {} on attempt {}/{}; {}",
						placement, attempt + 1, MAX_RETRIES + 1,
						retry ? "retrying" : "giving up", e);
				if (!retry) {
					placements.remove(placement);
					placement.type.discardGenerator(generator.id());
				}
			}
		}
	}

	private void initSeed() {
		if (seedNeedsRecalc)
			seededRand.setSeed(generateOrGetGenSeed());
	}

	private List<DimensionStructureType> getUsableStructures() {
		List<DimensionStructureType> all = new ArrayList<>(List.of(DimensionStructureType.types));
		if (allowUnfinishedStructures)
			return all;
		return DimensionStructureType.usableStructures();
	}

	/** V33a arePositionsDetermined: what RegionMapper polls before it can size the central region. */
	public boolean arePositionsDetermined() {
		return positionsDetermined;
	}

	/**
	 * V33a getMaximumDistanceFromOrigin: the farthest a structure could possibly be from the world
	 * origin given this ring centre. Note it uses the maximum ring radius rather than the radii the
	 * structures actually got, so it is an upper bound rather than a fit.
	 */
	public double getMaximumDistanceFromOrigin() {
		double x = Math.max(Math.abs(structureOriginX + BASE_RADIUS + RADIUS_VARIATION),
				Math.abs(structureOriginX - BASE_RADIUS - RADIUS_VARIATION));
		double z = Math.max(Math.abs(structureOriginZ + BASE_RADIUS + RADIUS_VARIATION),
				Math.abs(structureOriginZ - BASE_RADIUS - RADIUS_VARIATION));
		return ReikaMathLibrary.py3d(x, 0, z);
	}

	/** V33a getMaximumPossibleDistance: the same bound over every possible ring centre. */
	public static double getMaximumPossibleDistance() {
		return STRUCTURE_CENTER_VARIATION + BASE_RADIUS + RADIUS_VARIATION;
	}

	/** V33a generateMonument: the monument sits at the ring's own centre. */
	public BlockPos getMonumentPosition() {
		return new BlockPos(structureOriginX, 0, structureOriginZ);
	}

	public int getStructureOriginX() {
		return structureOriginX;
	}

	public int getStructureOriginZ() {
		return structureOriginZ;
	}

	public float getStructureAngleOrigin() {
		return structureAngleOrigin;
	}

	public List<StructurePlacement> getPlacements() {
		return Collections.unmodifiableList(placements);
	}

	/** Exact layout-owned structure at a placement chunk, used by custom vanilla placements. */
	public StructurePlacement getPlacementInChunk(DimensionStructureType type, int chunkX, int chunkZ) {
		for (StructurePlacement placement : placements)
			if (placement.type == type && (placement.placement().getX() >> 4) == chunkX
					&& (placement.placement().getZ() >> 4) == chunkZ)
				return placement;
		return null;
	}

	/** V33a getNearestStructureWithinRange, used by the biome distributor's structure blobs. */
	public StructurePlacement getNearestStructureWithinRange(int x, int z, double r) {
		double best = Double.POSITIVE_INFINITY;
		StructurePlacement found = null;
		for (StructurePlacement placement : placements) {
			double dx = x - placement.placement().getX();
			double dz = z - placement.placement().getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (distance < best && distance <= r) {
				best = distance;
				found = placement;
			}
		}
		return found;
	}

	public String getStateMessage() {
		return placements.size() + " structures placed.";
	}

	/**
	 * V33a getStructureColorTypes: the client rebuilds the same colour-to-type map from the seed it was
	 * sent, without knowing anything about the world. Kept as a pure function of the seed so it stays
	 * identical to what {@link #assignTypes()} produces.
	 */
	public static EnumMap<CrystalElement, StructureTypeData> getStructureColorTypes(long seed) {
		EnumMap<CrystalElement, StructureTypeData> map = DimensionStructureType.emptyTypeMap();
		List<DimensionStructureType> usable = DimensionStructureType.usableStructures();
		if (usable.isEmpty())
			return map;
		Random client = new Random(seed);
		int generationIndex = 0;
		for (CrystalElement color : CrystalElement.elements) {
			DimensionStructureType type = usable.remove(client.nextInt(usable.size()));
			map.put(color, new StructureTypeData(color, type, generationIndex));
			if (usable.isEmpty()) {
				usable = DimensionStructureType.usableStructures();
				generationIndex++;
			}
		}
		return map;
	}

	/** V33a assignSeed: the client is told the generation seed and recomputes its own type map. */
	public static void assignSeed(long s) {
		clientDimensionSeed = s;
		seedNeedsRecalc = true;
	}

	public static long getClientDimensionSeed() {
		return clientDimensionSeed;
	}

	/**
	 * V33a generateOrGetGenSeed: a per-installation seed stored beside the game directory, created on
	 * first use. It is deliberately not the world seed — V33a's own comment explains it could not reach
	 * one from outside a world — so the structure-to-colour mapping is stable across every world on an
	 * installation and can be shipped to clients as a single number.
	 */
	public static long generateOrGetGenSeed() {
		seedNeedsRecalc = false;
		File f = new File(DragonAPI.getMinecraftDirectory(), SEED_FILE);
		try {
			if (f.exists()) {
				try {
					List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
					for (String line : lines) {
						int split = line.indexOf(':');
						if (split >= 0)
							return Long.parseLong(line.substring(split + 1).trim());
					}
				}
				catch (Exception e) {
					ChromatiCraft.LOGGER.error("Could not read the Proxima generation seed: " + e);
					f.delete();
				}
			}
			f.getParentFile().mkdirs();
			long seed = System.currentTimeMillis();
			Files.writeString(f.toPath(), SEED_PREFIX + seed + System.lineSeparator(),
					StandardCharsets.UTF_8);
			return seed;
		}
		catch (IOException e) {
			ChromatiCraft.LOGGER.error("Could not write the Proxima generation seed: " + e);
		}
		// V33a's fallback: something stable for this machine when the file cannot be used at all.
		return System.getProperty("os.name").hashCode();
	}
}
