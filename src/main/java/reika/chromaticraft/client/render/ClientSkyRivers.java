package reika.chromaticraft.client.render;

import reika.chromaticraft.world.dimension.SkyRiverGenerator;

/**
 * This client's own copy of Proxima's sky rivers, built from the seed the server sends.
 *
 * <p>Kept separate from {@link SkyRiverGenerator}'s server-side {@code active} instance so the two
 * cannot be confused: on an integrated server both exist in one process, and drawing from the server's
 * copy would work in single-player and silently draw nothing in multiplayer.
 */
public final class ClientSkyRivers {

	private static volatile SkyRiverGenerator rivers;

	private ClientSkyRivers() {}

	public static SkyRiverGenerator get() {
		return rivers;
	}

	public static void clear() {
		rivers = null;
	}

	/**
	 * Runs the river generator for this seed. It walks every ray and indexes tens of thousands of
	 * points, so it is done once per seed rather than per frame, off the render thread's critical path
	 * by virtue of running on the network handler's work queue.
	 */
	public static void rebuild(long seed) {
		rivers = SkyRiverGenerator.generateForClient(seed);
	}
}
