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

import java.util.Random;

import reika.dragonapi.instantiable.math.LobulatedCurve;

/**
 * V33a {@code RegionMapper}: the irregular boundary between Proxima's central region and its outer
 * regions.
 *
 * <p>It is a single twenty-lobe closed curve around the world origin, sized so it always encloses
 * every puzzle structure with room to spare — inner radius {@code maxStructureDistance + 200}, outer
 * {@code maxStructureDistance + 1500}. Everything inside is the dimension proper; stepping outside is
 * what {@code OuterRegionsEvents} reacts to, and {@code BiomeDistributor} paints the whole interior as
 * the Luminescent Sanctuary biome unless a structure blob claims it first.
 *
 * <p>This is why it had to wait for {@link StructureCalculator}: it cannot know its own size until the
 * structure ring's centre has been chosen, and it reads that through
 * {@link StructureCalculator#getMaximumDistanceFromOrigin()}.
 *
 * <h2>The wait loop is gone on purpose</h2>
 *
 * V33a runs its generators on separate threads, so this class polls
 * {@code calc.arePositionsDetermined()} in a {@code Thread.sleep(50)} loop until the structure
 * calculator releases it. The port runs them in order instead — {@link ProximaGenerators.Generator}
 * declares the dependency and {@link StructureCalculator} clears its bit the moment positions are
 * published — so the loop would be dead code. Handing an unfinished calculator to
 * {@link #generate} is an ordering mistake and fails loudly rather than sleeping forever or, worse,
 * sizing the region off an origin of zero.
 */
public final class RegionMapper {

	/** V33a: the closest the boundary may come to the outermost possible structure. */
	public static final double MIN_BUFFER = 200;
	/** V33a: and the farthest. Also the reach {@code SkyRiverGenerator} measures its rivers from. */
	public static final double MAX_BUFFER = 1500;

	/** V33a: twenty lobes, which is what makes the boundary read as organic rather than circular. */
	private static final int LOBES = 20;

	/**
	 * V33a keeps the curve in a static field because Proxima is a single level and every consumer —
	 * the biome distributor, the dimension ticker, the outer-region events — asks statically. The
	 * curve itself lives on the instance so it stays testable; this only tracks which instance the
	 * static accessors answer for.
	 */
	private static volatile RegionMapper active;

	private final LobulatedCurve region;

	private RegionMapper(LobulatedCurve region) {
		this.region = region;
	}

	/**
	 * V33a run(): size the boundary from the structure ring, then generate its lobes from the
	 * dimension seed.
	 *
	 * @param structures a {@link StructureCalculator} that has already placed its structures
	 * @param seed       the persistent dimension seed, so the boundary is reproducible
	 */
	public static RegionMapper generate(StructureCalculator structures, long seed) {
		if (!structures.arePositionsDetermined())
			throw new IllegalStateException("The Proxima central region cannot be sized before the "
					+ "structure ring is placed; run ProximaGenerators.Generator.STRUCTURE first");
		double maximumStructureDistance = structures.getMaximumDistanceFromOrigin();
		LobulatedCurve curve = LobulatedCurve.fromMinMaxRadii(
				maximumStructureDistance + MIN_BUFFER, maximumStructureDistance + MAX_BUFFER, LOBES)
				.generate(new Random(seed));
		RegionMapper mapper = new RegionMapper(curve);
		active = mapper;
		ProximaGenerators.finish(ProximaGenerators.Generator.REGION);
		return mapper;
	}

	/** V33a isPointInCentralRegion, as asked by the biome distributor and the dimension ticker. */
	public static boolean isPointInCentralRegion(double x, double z) {
		RegionMapper mapper = active;
		// Before the region is generated nothing can be inside it. V33a would have thrown here; the
		// port answers false so a query racing generation cannot crash a chunk build.
		return mapper != null && mapper.contains(x, z);
	}

	public boolean contains(double x, double z) {
		return region.isPointInsideCurve(x, z);
	}

	public LobulatedCurve curve() {
		return region;
	}

	/** The radius of the boundary along a given bearing, in degrees. */
	public double getRadius(double angle) {
		return region.getRadius(angle);
	}

	public static RegionMapper getActive() {
		return active;
	}

	/** Test/reset seam for a dimension whose layout is being recomputed. */
	public static void clear() {
		active = null;
	}

	/** V33a getStateMessage. */
	public String getStateMessage() {
		return "Central region created; " + region.minRadius + " +/- " + (region.amplitudeVariation * LOBES);
	}
}
