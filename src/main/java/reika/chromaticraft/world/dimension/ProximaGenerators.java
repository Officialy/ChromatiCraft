package reika.chromaticraft.world.dimension;

import java.util.EnumSet;
import java.util.Set;

/**
 * V33a's {@code ChunkProviderChroma.areGeneratorsReady()} gate, rebuilt as an explicit registry.
 *
 * <p>Upstream held a bitflag of five background generators — {@code STRUCTURE}, {@code BIOME},
 * {@code REGION}, {@code SKYRIVER} and {@code FISSUREPATTERNS} — set when the dimension's seed was
 * (re)computed and cleared one bit at a time as each finished. The Portal Rift refuses to carry
 * anyone while any bit is set, and keeps charging past its cap for as long as that is true, so that
 * a player cannot arrive in a Proxima whose global layout has not been decided yet.
 *
 * <p>The 26.2 equivalent keeps that contract literally: a generator declares itself pending when the
 * dimension seed is established and clears itself when its own computation lands. Only generators
 * that actually exist in the port are members — a permanently pending entry would be indistinguishable
 * from a broken gate — and each newly ported generator joins by adding its constant here.
 *
 * <p>Deferred: {@code STRUCTURE} (the puzzle-structure position calculator), {@code SKYRIVER} and
 * {@code FISSUREPATTERNS}. All three belong in this gate and must be added to {@link Generator} when
 * their generators land, so that the portal automatically starts waiting on them again rather than
 * needing the gate rewritten.
 *
 * <p><b>Dependency order matters here and is not obvious.</b> V33a's
 * {@code ThreadedGenerators.isDependentOn} makes both {@code BIOME} and {@code REGION} depend on
 * {@code STRUCTURE}, and the dependency is real rather than nominal: {@link RegionMapper} blocks on
 * {@code StructureCalculator.arePositionsDetermined()} and sizes the central region from
 * {@code getMaximumDistanceFromOrigin()}, while {@code BiomeDistributor} paints its Structure Field
 * and Monument Field biomes around those same positions. Proxima's biome layout therefore cannot be
 * ported before the puzzle-structure <em>position</em> calculator, even though the puzzle mechanics
 * themselves are deliberately deferred. Only the structure identities, sizes and placement rules are
 * needed for this — not their contents.
 */
public final class ProximaGenerators {

	public enum Generator {
		/** {@link BiomeDistributor}: which biome each Proxima region resolves to. */
		BIOME,
		/** {@link RegionMapper}: the concentric region layout the biome distributor reads. */
		REGION,
	}

	private static final Set<Generator> pending = EnumSet.noneOf(Generator.class);

	private ProximaGenerators() {}

	/** Called when the dimension seed is established or reset: every member becomes pending again. */
	public static synchronized void markAllPending() {
		pending.addAll(EnumSet.allOf(Generator.class));
	}

	public static synchronized void finish(Generator generator) {
		pending.remove(generator);
	}

	public static synchronized boolean isReady(Generator generator) {
		return !pending.contains(generator);
	}

	/** V33a areGeneratorsReady(): true only once every registered generator has finished. */
	public static synchronized boolean areGeneratorsReady() {
		return pending.isEmpty();
	}
}
