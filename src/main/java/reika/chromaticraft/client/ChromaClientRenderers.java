package reika.chromaticraft.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.entity.RenderGlowCloud;
import reika.chromaticraft.render.entity.RenderLumaBurst;
import reika.chromaticraft.render.entity.RenderLaserPulse;
import reika.chromaticraft.render.entity.RenderPylonOverloadShock;
import reika.chromaticraft.render.entity.RenderTunnelNuker;
import reika.chromaticraft.render.entity.RenderDataCrystal;
import reika.chromaticraft.render.item.ItemStandItemRenderer;
import reika.chromaticraft.render.item.CrystalChargerItemRenderer;
import reika.chromaticraft.render.item.ItemAuraInfuserRenderer;
import reika.chromaticraft.render.tesr.RenderCrystalPylon;
import reika.chromaticraft.render.tesr.RenderCastingTable;
import reika.chromaticraft.render.tesr.RenderCrystalRepeater;
import reika.chromaticraft.render.tesr.RenderDataNode;
import reika.chromaticraft.render.tesr.RenderItemStand;
import reika.chromaticraft.render.tesr.RenderLootChest;
import reika.chromaticraft.render.tesr.RenderCrystalCharger;
import reika.chromaticraft.render.tesr.RenderInfuser3;
import reika.chromaticraft.render.tesr.RenderCrystalPortal;
import reika.chromaticraft.render.tesr.RenderRelaySource;
import reika.chromaticraft.render.tesr.RenderLumenRelay;

public final class ChromaClientRenderers {

	private ChromaClientRenderers() {}

	public static void init(IEventBus bus) {
		ChromaRenderPipelines.register(bus);
		NeoForge.EVENT_BUS.addListener(ChromaModelOutlineRenderer::extract);
		bus.addListener(ChromaClientRenderers::registerRenderers);
		bus.addListener(ChromaClientRenderers::registerLayers);
		bus.addListener(ChromaClientRenderers::registerSpecialModelRenderers);
		bus.addListener(ChromaClientRenderers::registerItemModels);
		bus.addListener(ChromaClientRenderers::registerClientExtensions);
		bus.addListener(PylonAttackOverlay::register);
		bus.addListener(LoreDiscoveryOverlay::register);
		bus.addListener(reika.chromaticraft.client.gui.ProgressionOverlay::register);
		bus.addListener(reika.chromaticraft.client.gui.StructureNotificationOverlay::register);
		bus.addListener(MouseoverStatusOverlay::register);
		bus.addListener(ChromaClientRenderers::registerEnvironmentEffects);
	}

	/**
	 * Proxima's sky. It is reached by name from the dimension type's {@code neoforge:custom_skybox}
	 * attribute rather than by a level-render event, because the event that looks like the right hook
	 * only fires for dimensions whose skybox is not NONE.
	 */
	private static void registerEnvironmentEffects(
			net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent event) {
		event.registerSkyboxRenderer(
				reika.chromaticraft.world.dimension.ProximaSkyboxId.ID,
				reika.chromaticraft.client.render.ProximaSkyRenderer.INSTANCE);
	}

	private static void registerItemModels(RegisterItemModelsEvent event) {
		event.register(reika.chromaticraft.render.item.InfoFragmentItemModel.ID,
				reika.chromaticraft.render.item.InfoFragmentItemModel.Unbaked.MAP_CODEC);
	}

	private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
		event.register(ItemStandItemRenderer.ID, ItemStandItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.CaveCrystalItemRenderer.ID,
				reika.chromaticraft.render.item.CaveCrystalItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.PowerCrystalItemRenderer.ID,
				reika.chromaticraft.render.item.PowerCrystalItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.DimensionCoreItemRenderer.ID,
				reika.chromaticraft.render.item.DimensionCoreItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.PortalItemRenderer.ID,
				reika.chromaticraft.render.item.PortalItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.EncrustedCrystalItemRenderer.ID,
				reika.chromaticraft.render.item.EncrustedCrystalItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.TieredOreItemRenderer.ID,
				reika.chromaticraft.render.item.TieredOreItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.LootChestItemRenderer.ID,
				reika.chromaticraft.render.item.LootChestItemRenderer.Unbaked.MAP_CODEC);
		event.register(CrystalChargerItemRenderer.ID, CrystalChargerItemRenderer.Unbaked.MAP_CODEC);
		event.register(ItemAuraInfuserRenderer.ID, ItemAuraInfuserRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.PersonalChargerItemRenderer.ID,
				reika.chromaticraft.render.item.PersonalChargerItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.EnderCrystalItemRenderer.ID,
				reika.chromaticraft.render.item.EnderCrystalItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.RelaySourceItemRenderer.ID,
				reika.chromaticraft.render.item.RelaySourceItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.FunctionRelayItemRenderer.ID,
				reika.chromaticraft.render.item.FunctionRelayItemRenderer.Unbaked.MAP_CODEC);
		event.register(reika.chromaticraft.render.item.FarmerItemRenderer.ID,
				reika.chromaticraft.render.item.FarmerItemRenderer.Unbaked.MAP_CODEC);
	}

	private static void registerClientExtensions(
			net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
		// These two effects deliberately reuse vanilla semantics and V33a icons. Registering the
		// vanilla atlas sprites avoids manufacturing duplicate PNGs under ChromatiCraft's namespace.
		event.registerMobEffect(vanillaEffectIcon("regeneration"),
				reika.chromaticraft.ChromatiCraft.betterRegen.get());
		event.registerMobEffect(vanillaEffectIcon("saturation"),
				reika.chromaticraft.ChromatiCraft.betterSat.get());

		// V33a Luma used a water-material camera overlay. The custom 26.2 FluidType had neither an
		// overlay nor fog, exposing unloaded/occluded terrain while the camera was submerged.
		event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
			private static final net.minecraft.resources.Identifier OVERLAY =
					net.minecraft.resources.Identifier.fromNamespaceAndPath(
						reika.chromaticraft.ChromatiCraft.MODID,
						"textures/block/fluid/aether/aether_full.png");

			@Override
			public net.minecraft.resources.Identifier getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {
				return OVERLAY;
			}

			@Override
			public void modifyFogColor(net.minecraft.client.Camera camera, float partialTick,
					net.minecraft.client.multiplayer.ClientLevel level, int renderDistance,
					float darkenWorldAmount, org.joml.Vector4f color) {
				color.set(0.035F, 0.055F, 0.11F, 1F);
			}

			@Override
			public void modifyFogRender(net.minecraft.client.Camera camera,
					net.minecraft.client.renderer.fog.environment.FogEnvironment environment,
					float renderDistance, float partialTick,
					net.minecraft.client.renderer.fog.FogData fog) {
				fog.environmentalStart = -4F;
				fog.environmentalEnd = 18F;
			}
		}, reika.chromaticraft.registry.ChromaFluids.LUMA_TYPE.get());
	}

	private static net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions vanillaEffectIcon(
			String name) {
		net.minecraft.resources.Identifier sprite = net.minecraft.resources.Identifier.withDefaultNamespace(
				"mob_effect/" + name);
		return new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
			@Override
			public boolean extractInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance,
					net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
					net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y,
					int width, int height, int color) {
				graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
						sprite, x, y, width, height, color);
				return true;
			}

			@Override
			public boolean extractHudIcon(net.minecraft.world.effect.MobEffectInstance instance,
					net.minecraft.client.gui.Hud hud,
					net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y,
					int width, int height, int color) {
				graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
						sprite, x, y, width, height, color);
				return true;
			}
		};
	}

	private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		 event.registerLayerDefinition(RenderItemStand.MODEL_LAYER, RenderItemStand::createStandLayer);
		event.registerLayerDefinition(RenderTunnelNuker.MODEL_LAYER, reika.chromaticraft.models.ModelTunnelNuker::createBodyLayer);
		event.registerLayerDefinition(RenderLootChest.MODEL_LAYER, reika.chromaticraft.models.ModelLootChest::createBodyLayer);
		event.registerLayerDefinition(RenderCrystalCharger.MODEL_LAYER,
				reika.chromaticraft.models.ModelCrystalCharger::createBodyLayer);
		event.registerLayerDefinition(RenderInfuser3.MODEL_LAYER, reika.chromaticraft.models.ModelInfuser2::createBodyLayer);
		event.registerLayerDefinition(RenderRelaySource.MODEL_LAYER,
				reika.chromaticraft.models.ModelRelaySource::createBodyLayer);
		event.registerLayerDefinition(reika.chromaticraft.render.tesr.RenderFarmer.MODEL_LAYER,
				reika.chromaticraft.models.ModelFarmer::createBodyLayer);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ChromaBlockEntities.ITEM_STAND.get(), RenderItemStand::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.GLOWING_CRACKS.get(),
				reika.chromaticraft.render.tesr.dimension.RenderGlowingCracks::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.CASTING_TABLE.get(), RenderCastingTable::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.PYLON.get(), RenderCrystalPylon::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.REPEATER.get(), RenderCrystalRepeater::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.WEAK_REPEATER.get(), RenderCrystalRepeater::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.RELAY_SOURCE.get(), RenderRelaySource::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.FUNCTION_RELAY.get(),
				reika.chromaticraft.render.tesr.RenderFunctionRelay::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.LUMEN_RELAY.get(), RenderLumenRelay::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.DATA_NODE.get(), RenderDataNode::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.STRUCTURE_CONTROLLER.get(),
				reika.chromaticraft.render.tesr.RenderStructureController::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.LOOT_CHEST.get(), RenderLootChest::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.CRYSTAL_CHARGER.get(), RenderCrystalCharger::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.ITEM_INFUSER.get(), RenderInfuser3::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.PLAYER_INFUSER.get(), RenderInfuser3::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.PERSONAL_CHARGER.get(),
				reika.chromaticraft.render.tesr.RenderPersonalCharger::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.PORTAL.get(), RenderCrystalPortal::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.VOID_RIFT.get(),
				reika.chromaticraft.render.tesr.dimension.RenderVoidRift::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.DIMENSION_CORE.get(),
				reika.chromaticraft.render.tesr.RenderDimensionCore::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.AURA_POINT.get(),
				reika.chromaticraft.render.tesr.RenderAuraPoint::new);
		event.registerBlockEntityRenderer(ChromaBlockEntities.FARMER.get(),
				reika.chromaticraft.render.tesr.RenderFarmer::new);
		event.registerEntityRenderer(ChromaEntityTypes.PYLON_OVERLOAD.get(), RenderPylonOverloadShock::new);
		event.registerEntityRenderer(ChromaEntityTypes.GLOW_CLOUD.get(), RenderGlowCloud::new);
		event.registerEntityRenderer(ChromaEntityTypes.LUMA_BURST.get(), RenderLumaBurst::new);
		event.registerEntityRenderer(ChromaEntityTypes.LASER_PULSE.get(), RenderLaserPulse::new);
		event.registerEntityRenderer(ChromaEntityTypes.TUNNEL_NUKER.get(), RenderTunnelNuker::new);
		event.registerEntityRenderer(ChromaEntityTypes.DATA_CRYSTAL.get(), RenderDataCrystal::new);
		event.registerEntityRenderer(ChromaEntityTypes.AURORA.get(),
				reika.chromaticraft.render.entity.RenderAurora::new);
	}
}
