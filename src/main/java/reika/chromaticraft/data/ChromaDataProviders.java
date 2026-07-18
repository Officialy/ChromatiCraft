package reika.chromaticraft.data;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import reika.chromaticraft.ChromatiCraft;

/**
 * ChromatiCraft datagen entry point (port-in-progress). NeoForge 26.x split GatherDataEvent into
 * Client (models/lang) and Server (loot/tags/recipes); providers are added as content ports.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaDataProviders {

	private ChromaDataProviders() {}

	@SubscribeEvent
	public static void onGatherClient(GatherDataEvent.Client event) {
		event.createProvider(output -> new ChromaLang(output, "en_us"));
		event.createProvider(ChromaModelProvider::new);
	}

	@SubscribeEvent
	public static void onGatherServer(GatherDataEvent.Server event) {
		event.createProvider(ChromaLootProvider::new);
	}
}
