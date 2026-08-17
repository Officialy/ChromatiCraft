package reika.chromaticraft.world.dimension;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
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
 * <p>Deferred: {@code SKYRIVER} and {@code FISSUREPATTERNS}. Both belong in this gate and must be
 * added to {@link Generator} when their generators land, so that the portal automatically starts
 * waiting on them again rather than needing the gate rewritten.
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
		/**
		 * {@link StructureCalculator}: which puzzle structure each element gets and where it sits.
		 * Everything else in this gate depends on it, so it must finish first.
		 */
		STRUCTURE,
		/** {@code BiomeDistributor}: which biome each Proxima region resolves to. */
		BIOME,
		/** {@code RegionMapper}: the concentric region layout the biome distributor reads. */
		REGION,
	}

	/**
	 * Starts full, not empty. V33a sets every bit the moment the dimension seed is established, and
	 * before that point nothing has run — so "nothing pending" would mean "ready" and would let the
	 * Portal Rift carry a player into a Proxima whose layout had not been decided. The gate can only
	 * open by generators actually reporting in.
	 */
	private static final Set<Generator> pending = EnumSet.allOf(Generator.class);

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

	/**
	 * The finished layout, published as one object so a consumer either sees a complete Proxima or
	 * none of it. Null until {@link #regenerate} completes.
	 */
	public record Layout(long seed, StructureCalculator structures, RegionMapper region,
			BiomeDistributor biomes) {}

	private static volatile Layout layout;
	private static volatile CompletableFuture<Layout> running;

	public static Layout getLayout() {
		return layout;
	}

	/**
	 * V33a {@code ChunkProviderChroma.regenerateGenerators}: sets every bit pending, then runs the
	 * generators off the server thread in dependency order.
	 *
	 * <p>Running it off-thread is not an optimisation, it is the reason this gate exists. The biome
	 * map alone is a 4096x4096 paint that measures at roughly <b>19 seconds</b> on the machine this was
	 * ported on; doing that synchronously would stall the server outright. V33a runs all five of its
	 * generators on threads for exactly this reason, and the Portal Rift's "generators ready" check —
	 * which keeps a rift charging and refusing travel while any bit is set — is precisely the mechanism
	 * that makes the wait invisible to a player.
	 *
	 * <p>The chain is strictly ordered because the dependencies are real: the region is sized from the
	 * structure ring and the biome map paints its Structure and Monument Fields around the same
	 * placements. Upstream expresses that with a sleep-poll inside each generator; expressing it as
	 * call order is the same guarantee without the risk of a worldgen thread sleeping on a chunk build.
	 *
	 * @return a future that completes with the finished layout; already-running work is shared rather
	 *         than duplicated
	 */
	public static synchronized CompletableFuture<Layout> regenerate(long seed) {
		if (running != null && !running.isDone())
			return running;
		markAllPending();
		layout = null;
		running = CompletableFuture.supplyAsync(() -> generateNow(seed));
		return running;
	}

	/**
	 * Blocks until a layout for this seed exists, and is what every Proxima chunk depends on.
	 *
	 * <p>The biome source has to answer during chunk generation, and if it answers before the layout
	 * exists it falls back to the Central biome — which is then <em>baked into the saved chunk</em>. A
	 * fallback that persists is worse than a wait, so the wait is taken once, when the level loads,
	 * before any chunk of it is built.
	 *
	 * <p>In practice it does not wait: {@link #regenerate} is kicked when the server starts, so the paint
	 * is normally finished long before anyone reaches a rift. This is the guarantee, not the mechanism.
	 *
	 * <p>The running future is captured under the lock and joined <em>outside</em> it. Joining while
	 * holding it would deadlock: the worker calls {@link #finish} as each generator completes, and that
	 * is synchronized on this same class.
	 */
	public static Layout awaitLayout(long seed) {
		Layout current = layout;
		if (current != null && current.seed() == seed)
			return current;
		CompletableFuture<Layout> inFlight;
		synchronized (ProximaGenerators.class) {
			inFlight = running != null && !running.isDone() ? running : null;
		}
		if (inFlight != null) {
			Layout finished = inFlight.join();
			if (finished.seed() == seed)
				return finished;
		}
		// Nothing running, or what was running was for a different world: do it here and now.
		markAllPending();
		return generateNow(seed);
	}

	/**
	 * The same chain on the calling thread, for datagen, tests and any caller that genuinely needs the
	 * layout before it can continue. Ordinary gameplay should use {@link #regenerate}.
	 */
	public static Layout generateNow(long seed) {
		StructureCalculator structures = new StructureCalculator(seed);
		structures.generate();
		RegionMapper region = RegionMapper.generate(structures, seed);
		BiomeDistributor biomes = new BiomeDistributor(seed).generate(structures);
		Layout result = new Layout(seed, structures, region, biomes);
		layout = result;
		return result;
	}
}
