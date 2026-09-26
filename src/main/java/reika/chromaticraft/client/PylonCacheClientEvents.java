package reika.chromaticraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.iwg.PylonGenerator;

/** V33a {@code PylonGenerator.clearOnLogout}: the client forgets every pylon it was told of when it leaves a server. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class PylonCacheClientEvents {

	private PylonCacheClientEvents() {

	}

	@SubscribeEvent
	public static void clearOnLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
		PylonGenerator.instance.clearClientCache();
	}

}
