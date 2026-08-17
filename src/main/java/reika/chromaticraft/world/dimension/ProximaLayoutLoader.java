package reika.chromaticraft.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaDimensions;

/**
 * Runs Proxima's layout generators for the loaded world.
 *
 * <p>V33a builds this layout the first time its chunk provider is constructed. Nothing did so here, and
 * the consequence was quiet and total: {@code BiomeDistributor.getBiome} returns null with no layout,
 * {@link reika.chromaticraft.world.dimension.biome.ProximaBiomeSource} falls back to the Central biome
 * for that, and so every chunk of Proxima generated — and <em>saved</em> — as Luminescent Sanctuary. The
 * dimension worked; it was simply one biome everywhere.
 *
 * <p>Two hooks, for two different jobs:
 *
 * <ul>
 * <li>On server start the chain is kicked off-thread. The biome paint alone is a 4096x4096 job measured
 *     at roughly nineteen seconds, so it wants to run while the player is still in the Overworld
 *     nowhere near a rift.</li>
 * <li>When the Proxima level loads, the layout is waited for. Level load happens on the server thread
 *     before any chunk of that level is generated, so this is the last point at which a wait costs
 *     nothing but the first at which a missing layout would start being written into saved chunks.</li>
 * </ul>
 *
 * <p>The seed is the world's, taken from the overworld, which is what upstream keys its generators on.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ProximaLayoutLoader {

	private ProximaLayoutLoader() {}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		ProximaGenerators.regenerate(event.getServer().overworld().getSeed());
	}

	/** Proxima's per-tick work: for now, carrying anyone who has drifted into a sky river. */
	@SubscribeEvent
	public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level
				&& level.dimension() == ChromaDimensions.PROXIMA)
			SkyRiverManager.tick(level);
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		if (level.dimension() != ChromaDimensions.PROXIMA)
			return;
		// The overworld's seed, not this level's: they are the same value, but the overworld is what
		// the server-start kick used and the two must agree or the layout is rebuilt for nothing.
		ProximaGenerators.awaitLayout(level.getServer().overworld().getSeed());
	}
}
