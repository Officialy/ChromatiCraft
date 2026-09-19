package reika.chromaticraft;

import java.io.File;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.magic.potions.PotionBetterSaturation;
import reika.chromaticraft.magic.potions.PotionCustomRegen;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaFeatures;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaPlacementModifiers;
import reika.chromaticraft.registry.ChromaTabs;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.dragonapi.base.DragonAPIMod;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * ChromatiCraft main mod class. Port-in-progress: this is the minimal 26.2 @Mod entry point that
 * wires the DeferredRegister families onto the mod event bus, growing as content is ported (mirrors
 * ReactorCraft.java). The 1.7.10 original (config, packets, proxies, tab objects, fluid, etc.) is
 * preserved in origin/master and re-expressed subsystem-by-subsystem as those areas port.
 */
@Mod(ChromatiCraft.MODID)
public class ChromatiCraft extends DragonAPIMod {

	public static final String MODID = "chromaticraft";
	public static final String packetChannel = "ChromaData";
	public static final Logger LOGGER = LogManager.getLogger("ChromatiCraft");

	public static ChromatiCraft instance;

	/** DragonAPI ControlledConfig; read by {@link ChromaOptions}. ID registry is null (see ChromaConfig). */
	public static ChromaConfig config;

	public static final DeferredRegister<MobEffect> MOB_EFFECTS =
			DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MODID);

	public static final DeferredHolder<MobEffect, MobEffect> betterRegen = MOB_EFFECTS.register("regeneration",
			() -> new PotionCustomRegen(MobEffectCategory.BENEFICIAL, 0xCD5CAB));
	public static final DeferredHolder<MobEffect, MobEffect> betterSat = MOB_EFFECTS.register("saturation",
			() -> new PotionBetterSaturation(MobEffectCategory.BENEFICIAL, 0xA55926));

	public ChromatiCraft(IEventBus modEventBus, ModContainer modContainer) {
		instance = this;

		config = new ChromaConfig(instance, ChromaOptions.optionList, null);
		config.loadSubfolderedConfigFile();
		config.initProps();
		// A Proxima puzzle enters the assignment pool only after its complete planner is present.
		reika.chromaticraft.world.dimension.DimensionStructureType.LIGHTPANEL.registerGenerator(
				reika.chromaticraft.world.dimension.structure.LightPanelStructureGenerator::new);
		reika.chromaticraft.world.dimension.DimensionStructureType.TDMAZE.registerGenerator(
				reika.chromaticraft.world.dimension.structure.ThreeDMazeStructureGenerator::new);
		reika.chromaticraft.world.dimension.DimensionStructureType.MUSIC.registerGenerator(
				reika.chromaticraft.world.dimension.structure.MusicStructureGenerator::new);
		reika.chromaticraft.world.dimension.DimensionStructureType.GOL.registerGenerator(
				reika.chromaticraft.world.dimension.structure.GOLStructureGenerator::new);

		ChromaBlocks.BLOCKS.register(modEventBus);
		ChromaBlocks.ITEMS.register(modEventBus);
		ChromaItems.ITEMS.register(modEventBus);
		ChromaBlockEntities.BLOCK_ENTITIES.register(modEventBus);
		modEventBus.addListener(ChromaBlockEntities::registerCapabilities);
		ChromaMenus.REGISTRY.register(modEventBus);
		ChromaEntityTypes.ENTITY_TYPES.register(modEventBus);
		modEventBus.addListener(EntityGlowCloud::registerAttributes);
		modEventBus.addListener(EntityGlowCloud::registerSpawnPlacements);
		modEventBus.addListener(reika.chromaticraft.entity.EntityTunnelNuker::registerSpawnPlacements);
		modEventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
				event.put(ChromaEntityTypes.TUNNEL_NUKER.get(), EntityTunnelNuker.createAttributes().build()));
		ChromaTabs.CREATIVE_MODE_TABS.register(modEventBus);
		ChromaFeatures.FEATURES.register(modEventBus);
		reika.chromaticraft.world.dimension.SkyRiverManager.TICKET_TYPES.register(modEventBus);
		reika.chromaticraft.world.dimension.structure.ProximaStructures.TYPES.register(modEventBus);
		reika.chromaticraft.world.dimension.structure.ProximaStructures.PLACEMENTS.register(modEventBus);
		reika.chromaticraft.world.dimension.structure.ProximaStructurePieces.PIECES.register(modEventBus);
		ChromaFeatures.BIOME_SOURCES.register(modEventBus);
		ChromaFeatures.DENSITY_FUNCTION_TYPES.register(modEventBus);
		ChromaPlacementModifiers.TYPES.register(modEventBus);
		MOB_EFFECTS.register(modEventBus);
		reika.chromaticraft.registry.ChromaRecipeTypes.RECIPE_TYPES.register(modEventBus);
		reika.chromaticraft.registry.ChromaRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
		reika.chromaticraft.registry.ChromaSounds.SOUND_EVENTS.register(modEventBus);
		reika.chromaticraft.registry.ChromaFluids.FLUID_TYPES.register(modEventBus);
		reika.chromaticraft.registry.ChromaFluids.FLUIDS.register(modEventBus);

		// In-world game tests (progression core). Runnable via `gradlew :ChromatiCraft:runGameTest`.
		modEventBus.addListener(ChromaGameTests::onRegisterGameTests);
		modEventBus.addListener(ChromaNetwork::register);
		ChromaGameTests.TEST_INSTANCE_TYPES.register(modEventBus);
		reika.chromaticraft.registry.ChromaLootProviders.NUMBER_PROVIDERS.register(modEventBus);
		reika.chromaticraft.registry.ChromaLootProviders.CONDITIONS.register(modEventBus);
		modEventBus.addListener(this::commonSetup);

		// Force-load the progression singleton so it wires ProgressionAPI.instance.progressManager
		// (consumed by CrystalElement.playerHas and others) before any gameplay query.
		reika.chromaticraft.magic.progression.ProgressionManager.init();
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			reika.chromaticraft.client.ChromaClientRenderers.init(modEventBus);
			modEventBus.addListener(ChromatiCraft::registerScreens);
		}

		NeoForge.EVENT_BUS.addListener(ChromatiCraft::registerCommands);
		NeoForge.EVENT_BUS.addListener(ChromatiCraft::playerLoggedIn);
		NeoForge.EVENT_BUS.addListener(reika.chromaticraft.magic.TunnelNukerSpawner::tick);
		// The discovery scan that grants CRYSTALS (and BEDROCK/DEEPCAVE/biome stages) on sight.
		reika.chromaticraft.auxiliary.ExplorationMonitor.register();
		// V33a's mining, dimension-entry, potion, death and boss-kill progression hooks.
		reika.chromaticraft.auxiliary.ProgressionEventBridge.register();
		// Proxima arrivals fall from Y=1024, and death there returns the player home with a buffer cost.
		reika.chromaticraft.world.dimension.ProximaPlayerSafety.register();
		reika.chromaticraft.world.dimension.ProximaStructureSessions.register();
		reika.chromaticraft.auxiliary.FocusCrystalTradeHandler.register();
		reika.chromaticraft.auxiliary.PoolAlloyingHandler.register();
		reika.chromaticraft.auxiliary.CobbleGeneratorItemExpiry.register();
		reika.chromaticraft.auxiliary.ChromaFreezeHandler.register();
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			PylonFinder.registerTransparentBlock(RotaryBlocks.BLASTGLASS.get());
			PylonFinder.registerTransparentBlock(RotaryBlocks.BLASTPANE.get());
			terrablender.api.Regions.register(new reika.chromaticraft.world.biome.ChromaRegion());
			terrablender.api.Regions.register(new reika.chromaticraft.world.biome.LuminousCliffsRegion());
		});
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		reika.chromaticraft.auxiliary.CrystalNetworkLogger.registerCommand(event.getDispatcher());
		reika.chromaticraft.command.DataTowerLocateCommand.register(event.getDispatcher());
		reika.chromaticraft.command.ProgressModifyCommand.register(event.getDispatcher());
	}

	private static void playerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
			ChromaNetwork.sendTowerLocations(player);
	}

	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ChromaMenus.RITUAL_TABLE.get(),
				reika.chromaticraft.client.gui.ScreenRitualTable::new);
		event.register(ChromaMenus.CASTING_TABLE.get(),
				reika.chromaticraft.client.gui.ScreenCastingTable::new);
		event.register(ChromaMenus.HEAT_LAMP.get(),
				reika.chromaticraft.client.gui.ScreenHeatLamp::new);
		event.register(ChromaMenus.CRYSTAL_CHARGER.get(),
				reika.chromaticraft.client.gui.ScreenCrystalCharger::new);
		event.register(ChromaMenus.COLLECTOR.get(),
				reika.chromaticraft.client.gui.ScreenCollector::new);
		event.register(ChromaMenus.LEXICON_PAGES.get(),
				reika.chromaticraft.client.gui.ScreenLexiconPages::new);
		event.register(ChromaMenus.FRAGMENT_SELECTION.get(),
				reika.chromaticraft.client.gui.ScreenFragmentSelectionMenu::new);
		event.register(ChromaMenus.STRUCTURE_PASSWORD.get(),
				reika.chromaticraft.client.gui.ScreenStructurePassword::new);
	}

	@Override
	public URL getDocumentationSite() {
		return null;
	}

	@Override
	public URL getBugSite() {
		return null;
	}

	@Override
	public File getConfigFolder() {
		return config.getConfigFolder();
	}

	@Override
	public String getUpdateCheckURL() {
		return null;
	}

	@Override
	public String getModId() {
		return MODID;
	}

	@Override
	public String getDisplayName() {
		return "ChromatiCraft";
	}

	@Override
	public String getModAuthorName() {
		return "Reika";
	}
}
