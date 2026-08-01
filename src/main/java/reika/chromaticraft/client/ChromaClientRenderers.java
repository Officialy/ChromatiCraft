package reika.chromaticraft.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.entity.RenderGlowCloud;
import reika.chromaticraft.render.entity.RenderLumaBurst;
import reika.chromaticraft.render.entity.RenderPylonOverloadShock;
import reika.chromaticraft.render.item.ItemStandItemRenderer;
import reika.chromaticraft.render.tesr.RenderCrystalPylon;
import reika.chromaticraft.render.tesr.RenderCastingTable;
import reika.chromaticraft.render.tesr.RenderCrystalRepeater;
import reika.chromaticraft.render.tesr.RenderItemStand;

public final class ChromaClientRenderers {

	private ChromaClientRenderers() {}

	public static void init(IEventBus bus) {
		ChromaRenderPipelines.register(bus);
		NeoForge.EVENT_BUS.addListener(ChromaModelOutlineRenderer::extract);
		bus.addListener(ChromaClientRenderers::registerRenderers);
		bus.addListener(ChromaClientRenderers::registerLayers);
		bus.addListener(ChromaClientRenderers::registerSpecialModelRenderers);
		bus.addListener(PylonAttackOverlay::register);
	}

	private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
		event.register(ItemStandItemRenderer.ID, ItemStandItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.CaveCrystalItemRenderer.ID,
				reika.chromaticraft.render.item.CaveCrystalItemRenderer.Unbaked.MAP_CODEC);
	}

	private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(RenderItemStand.MODEL_LAYER, RenderItemStand::createStandLayer);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ChromaBlockEntities.ITEM_STAND.get(), RenderItemStand::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.CASTING_TABLE.get(), RenderCastingTable::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.PYLON.get(), RenderCrystalPylon::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.REPEATER.get(), RenderCrystalRepeater::new);
		event.registerEntityRenderer(ChromaEntityTypes.PYLON_OVERLOAD.get(), RenderPylonOverloadShock::new);
		event.registerEntityRenderer(ChromaEntityTypes.GLOW_CLOUD.get(), RenderGlowCloud::new);
		event.registerEntityRenderer(ChromaEntityTypes.LUMA_BURST.get(), RenderLumaBurst::new);
	}
}
