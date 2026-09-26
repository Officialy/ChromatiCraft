/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.world.iwg.PylonGenerator;

/**
 * V33a {@code PylonCacheLoader} (a DragonAPI {@code PlayerTracker}): a player logging in or changing dimension is sent
 * the pylon cache for the dimension they are now in. The pylon cache's own lifecycle rides along: each server level
 * loads its saved pylon locations as it loads, entries are checked as their chunks load, and the server's cache is
 * dropped when the server stops (the client's goes on logout, in {@code PylonCacheClientEvents}).
 *
 * <p>CHROMA-PORT: V33a's tracker also synced the player's pylon link network ({@code PylonLinkNetwork.sync}) and the
 * lore towers ({@code LoreManager.sendTowersToClient}); neither has a client side in the port yet.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class PylonCacheLoader {

	private PylonCacheLoader() {

	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent evt) {
		if (evt.getEntity() instanceof ServerPlayer ep) {
			PylonGenerator.instance.sendDimensionCacheToPlayer(ep, ep.level().dimension());
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent evt) {
		if (evt.getEntity() instanceof ServerPlayer ep) {
			PylonGenerator.instance.sendDimensionCacheToPlayer(ep, evt.getTo());
		}
	}

	@SubscribeEvent
	public static void loadLevelCache(LevelEvent.Load evt) {
		if (evt.getLevel() instanceof ServerLevel level)
			CrystalNetworker.loadPylonLocationData(level);
	}

	@SubscribeEvent
	public static void validateChunk(ChunkEvent.Load evt) {
		PylonGenerator.instance.validateLoadedChunk(evt);
	}

	@SubscribeEvent
	public static void clearOnStop(ServerStoppedEvent evt) {
		PylonGenerator.instance.clearServerCache();
	}

}
