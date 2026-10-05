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
		event.createProvider(ChromaFontProvider::new);
		event.createProvider(output -> new ChromaLang(output, "en_us"));
		event.createProvider(ChromaModelProvider::new);
		event.createProvider(ChromaSoundProvider::new);
        event.createProvider(ChromaVoidVisualData::new);
		event.createProvider(ChromaSpriteSourceProvider::new);
	}

	@SubscribeEvent
	public static void onGatherServer(GatherDataEvent.Server event) {
		event.createProvider(ChromaLootModifierProvider::new);
		event.createProvider(ChromaTestStructureProvider::new);
		event.createProvider(ChromaStructureTemplateProvider::new);
		event.createProvider(ChromaVillagePoolProvider::new);
		event.createWorldRegistryObjects(ChromaWorldGenProvider.buildRegistrySet());
		event.createReloadableRegistryObjects(ChromaWorldGenProvider.buildReloadableRegistrySet());
		event.createProvider(ChromaBiomeModifierProvider::new);
		event.createProvider((output, lookup) -> new ChromaBiomeTagProvider(output, lookup));
		event.createProvider((output, lookup) -> new ChromaConfiguredFeatureTagProvider(output, lookup));
		event.createProvider((output, lookup) -> new ChromaBlockTagsProvider(output, lookup));
		event.createProvider((output, lookup) -> new ChromaFluidTagsProvider(output, lookup));
		event.createProvider((output, lookup) -> new ChromaItemTagsProvider(output, lookup));
		event.createProvider((output, lookup) -> new ChromaDamageTypeTagsProvider(output, lookup));
	}
}
