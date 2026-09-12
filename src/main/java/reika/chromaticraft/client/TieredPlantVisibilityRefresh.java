package reika.chromaticraft.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaTieredPlants;

/** Keeps viewer-gated tiered-plant chunk meshes synchronized with the local player's access. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class TieredPlantVisibilityRefresh {

	private static UUID lastPlayer;
	private static int lastVisibility = -1;

	private TieredPlantVisibilityRefresh() {}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			reset();
			return;
		}

		UUID player = minecraft.player.getUUID();
		int visibility = minecraft.player.isCreative() ? 1 : 0;
		for (int i = 0; i < ChromaTieredPlants.list.length; i++) {
			if (ChromaTieredPlants.list[i].stage().isPlayerAtStage(minecraft.player))
				visibility |= 1 << (i + 1);
		}

		if (!player.equals(lastPlayer) || visibility != lastVisibility) {
			lastPlayer = player;
			lastVisibility = visibility;
			// TieredPlantModel is baked into section meshes. Progression and game-mode changes do
			// not produce a block update, so invalidate compiled geometry exactly when the viewer's
			// visibility set changes instead of rebuilding sections continuously.
			minecraft.levelExtractor.allChanged();
		}
	}

	@SubscribeEvent
	public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		reset();
	}

	private static void reset() {
		lastPlayer = null;
		lastVisibility = -1;
	}
}
