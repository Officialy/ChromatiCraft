package reika.chromaticraft.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

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
	 * <li>Immediately before levels are loaded the chain is kicked off-thread. The biome paint alone is
	 *     a 4096x4096 job measured at roughly nineteen seconds, so it must already be running when the
	 *     Proxima level's load event supplies the correctness barrier below.</li>
 * <li>When the Proxima level loads, the layout is waited for. Level load happens on the server thread
 *     before any chunk of that level is generated, so this is the last point at which a wait costs
 *     nothing but the first at which a missing layout would start being written into saved chunks.</li>
 * </ul>
 *
	 * <p>The seed is the world's world-generation seed, which is what upstream keys its generators on.
	 * It is read from {@code MinecraftServer.getWorldGenSettings()} because this hook intentionally runs
	 * before the overworld {@code ServerLevel} exists.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ProximaLayoutLoader {

	private ProximaLayoutLoader() {}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event) {
		ProximaGenerators.regenerate(event.getServer().getWorldGenSettings().options().seed());
	}

	/**
	 * A player entering Proxima is told the layout seed so their client can build its own copy of the
	 * sky rivers and draw them. Sent on join and on every dimension change rather than only the first,
	 * because a client that logged in elsewhere has never been told.
	 */
	@SubscribeEvent
	public static void onPlayerChangedDimension(
			net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
		sendSeedIfProxima(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerJoin(
			net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
		sendSeedIfProxima(event.getEntity());
	}

	private static void sendSeedIfProxima(net.minecraft.world.entity.player.Player player) {
		if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))
			return;
		if (serverPlayer.level().dimension() != ChromaDimensions.PROXIMA)
			return;
		reika.chromaticraft.network.ChromaNetwork.sendProximaLayoutSeed(serverPlayer,
				serverPlayer.level().getServer().overworld().getSeed());
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
		// Use the same seed source as the pre-level-load kick. Reading it from the settings also keeps
		// this valid if vanilla ever changes when the overworld level is inserted into the level map.
		ProximaGenerators.awaitLayout(level.getServer().getWorldGenSettings().options().seed());
	}
}
