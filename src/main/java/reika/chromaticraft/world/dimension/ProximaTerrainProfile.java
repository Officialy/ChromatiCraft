package reika.chromaticraft.world.dimension;

import reika.chromaticraft.world.dimension.StructureCalculator.StructurePlacement;

/**
 * V33a's terrain shape for Proxima: the base height and height variation that
 * {@code ChunkProviderChroma.applyNoiseLayers} feeds into the noise interpolation.
 *
 * <p>This is the one place where V33a genuinely departs from vanilla's generator. Everything else in
 * {@code applyNoiseLayers} is 1.7.10's own {@code ChunkProviderGenerate} pipeline verbatim; what
 * upstream replaces is the pair of values vanilla reads from the biome — {@code f3}, the base height,
 * and {@code f4}, the height variation — with a purely radial profile:
 *
 * <pre>
 *   f0 = sqrt((qx*qx + qz*qz) / (65536 * 32)) * 0.03125
 *   if (distanceToNearestStructure &lt;= 8) f0 *= distanceToNearestStructure / 8
 *   f3 = max(-0.25, 0.125 - f0 * 0.125)
 *   f4 = 0.5 * f0
 * </pre>
 *
 * <p>The consequence is the shape of the whole dimension. Near the origin {@code f0} is nearly zero,
 * so the base height sits at its maximum 0.125 and the variation at nothing: the centre is a flat
 * plain. As you travel out, the base height falls towards its {@code -0.25} floor while the variation
 * climbs without limit, so the land drops away and becomes progressively more mountainous — which is
 * what makes the outer regions feel like the edge of the world rather than more of the same.
 *
 * <p>Within eight chunks of any structure or the monument, {@code f0} is scaled down linearly to zero
 * at the structure itself. That is what flattens the ground each puzzle stands on, and it is why the
 * terrain profile has to consult the structure ring rather than being a pure function of position.
 *
 * <p>The commented-out block beside it in the source — a weighted average of nearby biomes'
 * {@code getBaseHeightDelta} over a 7x7 chunk window — is upstream's abandoned attempt at making the
 * profile biome-driven. It is not ported, because upstream does not run it; the height deltas on
 * {@link reika.chromaticraft.world.dimension.biome.ProximaBiomeType} exist for the per-biome terrain
 * shapers instead.
 *
 * <p>Note the coordinates are the <em>quart</em> coordinates the noise layer works in — V33a calls
 * {@code applyNoiseLayers(chunkX * 4, chunkZ * 4)} and then names the parameters {@code chunkX} and
 * {@code chunkZ} — while the structure distance is measured in real chunks. Both are preserved here,
 * with distinct parameter names so the mismatch cannot be "tidied" into a bug.
 */
public final class ProximaTerrainProfile {

	/** V33a: the radial term is normalised against this before its 1/32 scaling. */
	private static final double RADIAL_DIVISOR = 65536D * 32D;
	private static final double RADIAL_SCALE = 0.03125;

	/** V33a: the base height floor, and the value it starts from at the origin. */
	private static final float BASE_HEIGHT_MAX = 0.125F;
	private static final float BASE_HEIGHT_FLOOR = -0.25F;

	/** V33a: within this many chunks of a structure, the terrain is flattened towards it. */
	public static final double STRUCTURE_FLATTEN_CHUNKS = 8;

	private final StructureCalculator structures;

	public ProximaTerrainProfile(StructureCalculator structures) {
		this.structures = structures;
	}

	/**
	 * V33a's {@code f0}: the radial roughness term, damped near structures.
	 *
	 * @param quartX the noise layer's X coordinate, which is the chunk X times four
	 */
	public double roughness(int quartX, int quartZ) {
		double f0 = Math.sqrt((quartX * (double)quartX + quartZ * (double)quartZ) / RADIAL_DIVISOR)
				* RADIAL_SCALE;
		double toStructure = this.distanceToNearestStructureInChunks(quartX / 4, quartZ / 4);
		if (toStructure <= STRUCTURE_FLATTEN_CHUNKS)
			f0 *= toStructure / STRUCTURE_FLATTEN_CHUNKS;
		return f0;
	}

	/** V33a's {@code f3}: flat and high at the centre, falling to a floor of -0.25 further out. */
	public float baseHeight(int quartX, int quartZ) {
		return baseHeightFor(this.roughness(quartX, quartZ));
	}

	/** V33a's {@code f4}: no variation at the centre, climbing without limit with distance. */
	public float heightVariation(int quartX, int quartZ) {
		return heightVariationFor(this.roughness(quartX, quartZ));
	}

	public static float baseHeightFor(double roughness) {
		return Math.max(BASE_HEIGHT_FLOOR, BASE_HEIGHT_MAX - (float)roughness * BASE_HEIGHT_MAX);
	}

	public static float heightVariationFor(double roughness) {
		return 0.5F * (float)roughness;
	}

	/**
	 * V33a getDistanceToNearestStructureChunkCoords. The monument is measured unconditionally and
	 * every structure competes with it, all in chunk coordinates.
	 *
	 * <p>Upstream reads each structure's {@code getCentralLocation()}, which before its generator has
	 * run is the placement itself — so an unported puzzle generator gives the same answer here.
	 */
	public double distanceToNearestStructureInChunks(int chunkX, int chunkZ) {
		double dx = (structures.getMonumentPosition().getX() >> 4) - chunkX;
		double dz = (structures.getMonumentPosition().getZ() >> 4) - chunkZ;
		double d = Math.sqrt(dx * dx + dz * dz);
		for (StructurePlacement s : structures.getPlacements()) {
			dx = chunkX - (s.placement().getX() >> 4);
			dz = chunkZ - (s.placement().getZ() >> 4);
			d = Math.min(d, Math.sqrt(dx * dx + dz * dz));
		}
		return d;
	}
}
