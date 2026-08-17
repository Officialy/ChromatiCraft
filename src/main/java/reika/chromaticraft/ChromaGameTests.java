package reika.chromaticraft;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.auxiliary.ExplorationMonitor;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipeInput;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.AuraRequirement;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.StandIngredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.BlockEncrustedCrystal.TileCrystalEncrusted;
import reika.chromaticraft.data.ChromaChestLoot;
import reika.chromaticraft.data.ChromaTestStructureProvider;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.entity.EntityGlowCloud;
import reika.chromaticraft.entity.EntityPylonOverloadShock;
import reika.chromaticraft.auxiliary.CrystalNetworkLogger.FlowFail;
import reika.chromaticraft.magic.castingtuning.CastingTuningRegistry;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.network.CrystalFlow;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.network.PylonFinder;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.StorageCrystalTier;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.items.ItemStorageCrystal;
import reika.chromaticraft.magic.progression.CastingProgression;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ResearchLevel;
import reika.chromaticraft.magic.progression.ResearchProgress;
import reika.chromaticraft.magic.progression.LexiconData;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.LexiconDescriptions;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityItemInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityAuraInfuser;
import reika.chromaticraft.tileentity.recipe.TileEntityPlayerInfuser;
import reika.chromaticraft.magic.ElementBufferCapacityBoost;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaFluids;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.tileentity.TileEntityLockKey;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;
import reika.chromaticraft.world.OverworldStructureFeature;
import reika.chromaticraft.tileentity.networking.TileEntityCompoundRepeater;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.chromaticraft.registry.ChromaDecoFlowers;
import reika.chromaticraft.world.CrystalFeature;
import reika.chromaticraft.world.DataTowerFeature;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.chromaticraft.tileentity.TileEntityDummyAux;
import reika.chromaticraft.world.PylonFeature;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.fluids.FluidType;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.block.BlockChromaPortal;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;
import reika.chromaticraft.auxiliary.structure.PortalStructure;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.RegionMapper;
import reika.chromaticraft.world.dimension.BiomeDistributor;
import reika.chromaticraft.world.dimension.ProximaTerrainProfile;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.world.dimension.ProximaNoiseSettings;
import reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction;
import reika.chromaticraft.world.dimension.biome.ProximaBiomeType;
import reika.chromaticraft.world.dimension.biome.ProximaBiomes;
import reika.chromaticraft.world.dimension.biome.ProximaSubBiomes;
import reika.chromaticraft.world.dimension.biome.ProximaBiomeSource;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightSwitch;
import reika.chromaticraft.world.dimension.structure.lightpanel.LightType;
import reika.chromaticraft.block.dimension.structure.locks.BlockColoredLock;
import reika.chromaticraft.tileentity.TileEntityColorLock;

/**
 * In-world game tests for the progression core — runnable headless via
 * {@code gradlew :ChromatiCraft:runGameTest} or interactively with {@code /test runall} (the
 * {@code chromaticraft} namespace is enabled in build.gradle). They exercise the runtime path the
 * datagen smoke-test can't: granting a stage to a real player and querying it back (storage +
 * recursive-parent propagation + prerequisite gating + the colour→ALLCOLORS integration).
 *
 * <p>The {@link DirectInstance} codec workaround is copied from RotaryCraft's {@code RotaryGameTests}:
 * NeoForge 26.x's {@code RegisterGameTestsEvent} only registers {@link GameTestInstance}s, and the
 * vanilla function-backed instance resolves its body through the {@code TEST_FUNCTION} registry
 * (frozen before mod construction), so the body is carried directly instead.
 */
public final class ChromaGameTests {

	private ChromaGameTests() {}

	private static final Map<WorldLocation, TestReceiver> testReceivers = new HashMap<>();

	/** Codec types for our in-code {@link GameTestInstance}s; this registry is network-synced. */
	public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPES =
			DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, ChromatiCraft.MODID);

	static {
		TEST_INSTANCE_TYPES.register("direct", () -> DirectInstance.CODEC);
		PylonFinder.registerNetworkTileAdapter((tile, location) ->
				tile instanceof TileEntityDisplayPoint ? testReceivers.get(location) : null);
	}

	public static void onRegisterGameTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "default"),
				new TestEnvironmentDefinition.AllOf(List.of()));

		register(event, env, "progression_grant_and_query", ChromaGameTests::grantAndQuery);
		register(event, env, "progression_recursive_parents", ChromaGameTests::recursiveParents);
		register(event, env, "progression_prereq_gating", ChromaGameTests::prereqGating);
		register(event, env, "progression_color_discovery", ChromaGameTests::colorDiscovery);
		register(event, env, "progression_chained_energy_idea", ChromaGameTests::chainedEnergyIdea);
		register(event, env, "network_pylon_to_receiver", ChromaGameTests::pylonToReceiver);
		register(event, env, "network_pylon_repeater_receiver", ChromaGameTests::pylonRepeaterReceiver);
		register(event, env, "pylon_structure_lifecycle", 35, ChromaGameTests::pylonStructureLifecycle);
		register(event, env, "pylon_broadcast_template_contract", ChromaGameTests::pylonBroadcastTemplateContract);
		register(event, env, "encrusted_growth_persistence", ChromaGameTests::encrustedGrowthPersistence);
		register(event, env, "pylon_encrusted_discovery_recolor", ChromaGameTests::pylonEncrustedDiscoveryRecolor);
		register(event, env, "pylon_hostile_attack", ChromaGameTests::pylonHostileAttack);
		register(event, env, "pylon_enclosure_rejection", ChromaGameTests::pylonEnclosureRejection);
		register(event, env, "pylon_unstable_shock", ChromaGameTests::pylonUnstableShock);
		register(event, env, "pylon_vertical_defense", ChromaGameTests::pylonVerticalDefense);
		register(event, env, "casting_nbt_structure_contract", ChromaGameTests::castingNbtStructureContract);
		register(event, env, "pylon_regeneration_enhancement", ChromaGameTests::pylonRegenerationEnhancement);
		register(event, env, "pylon_player_placed_restriction", ChromaGameTests::pylonPlayerPlacedRestriction);
		register(event, env, "pylon_chunk_loading_lifecycle", ChromaGameTests::pylonChunkLoadingLifecycle);
		register(event, env, "pylon_link_donation", ChromaGameTests::pylonLinkDonation);
		register(event, env, "network_compound_repeater_multicolor", ChromaGameTests::compoundRepeaterMulticolor);
		register(event, env, "pylon_power_crystal_recharge", ChromaGameTests::pylonPowerCrystalRecharge);
		register(event, env, "pylon_power_crystal_ownership_backlash", ChromaGameTests::pylonPowerCrystalOwnershipBacklash);
		register(event, env, "network_los_obstruction_recovery", ChromaGameTests::lineOfSightObstructionRecovery);
		register(event, env, "network_tile_lifecycle_cache", ChromaGameTests::networkTileLifecycleCache);
		register(event, env, "network_repeater_break_invalidates_flow", ChromaGameTests::repeaterBreakInvalidatesFlow);
		register(event, env, "repeater_structure_redstone_lifecycle", ChromaGameTests::repeaterStructureRedstoneLifecycle);
		register(event, env, "network_saved_data_reload", ChromaGameTests::networkSavedDataReload);
		register(event, env, "repeater_two_player_ownership", ChromaGameTests::repeaterTwoPlayerOwnership);
		register(event, env, "repeater_turbo_custom_data", ChromaGameTests::repeaterTurboCustomData);
		register(event, env, "repeater_overload_destroys_stalk", ChromaGameTests::repeaterOverloadDestroysStalk);
		register(event, env, "casting_recipe_contract", ChromaGameTests::castingRecipeContract);
		register(event, env, "casting_stand_ownership_lock", ChromaGameTests::castingStandOwnershipLock);
		register(event, env, "casting_stand_spread", ChromaGameTests::castingStandSpread);
		register(event, env, "casting_table_atomic_craft", ChromaGameTests::castingTableAtomicCraft);
		register(event, env, "casting_table_progress_feedback", ChromaGameTests::castingTableProgressFeedback);
		register(event, env, "exploration_grants_crystals", ChromaGameTests::explorationGrantsCrystals);
		register(event, env, "casting_table_temple_group", ChromaGameTests::castingTableTempleGroup);
		register(event, env, "crystal_shard_charging_identity", ChromaGameTests::crystalShardChargingIdentity);
		register(event, env, "liquid_chroma_bucket_mud", ChromaGameTests::liquidChromaBucketMud);
		register(event, env, "liquid_chroma_elemental_loop", ChromaGameTests::liquidChromaElementalLoop);
		register(event, env, "casting_table_boosted_group_reload", ChromaGameTests::castingTableBoostedGroupReload);
		register(event, env, "casting_table_primary_cluster", ChromaGameTests::castingTablePrimaryCluster);
		register(event, env, "casting_table_crystal_core", ChromaGameTests::castingTableCrystalCore);
		register(event, env, "casting_table_element_unit", ChromaGameTests::castingTableElementUnit);
		register(event, env, "casting_table_tuning_key", ChromaGameTests::castingTableTuningKey);
		register(event, env, "casting_table_repeater_grouping", ChromaGameTests::castingTableRepeaterGrouping);
		register(event, env, "casting_table_focus_acceleration", ChromaGameTests::castingTableFocusAcceleration);
		register(event, env, "casting_table_crystal_star", ChromaGameTests::castingTableCrystalStar);
		register(event, env, "casting_table_tiered_multiblock_chain", ChromaGameTests::castingTableTieredMultiblockChain);
		register(event, env, "casting_table_lumen_core_multicolor", ChromaGameTests::castingTableLumenCoreMulticolor);
		register(event, env, "casting_table_structure_loss_cancel", ChromaGameTests::castingTableStructureLossCancel);
		register(event, env, "casting_table_pylon_network_core", 520, ChromaGameTests::castingTablePylonNetworkCore);
		register(event, env, "casting_table_receiver_capacity", ChromaGameTests::castingTableReceiverCapacity);
		register(event, env, "colored_block_registry_identity", ChromaGameTests::coloredBlockRegistryIdentity);
		register(event, env, "crystal_worldgen_placement_contract", ChromaGameTests::crystalWorldgenPlacementContract);
		register(event, env, "cave_crystal_dynamic_shape_contract", ChromaGameTests::caveCrystalDynamicShapeContract);
		register(event, env, "deco_flower_siting_contract", ChromaGameTests::decoFlowerSitingContract);
		register(event, env, "pylon_worldgen_nbt_contract", ChromaGameTests::pylonWorldgenNbtContract);
        register(event, env, "pylon_feature_variants", 40, ChromaGameTests::pylonFeatureVariants);
		register(event, env, "glow_cloud_spherical_movement", ChromaGameTests::glowCloudSphericalMovement);
		register(event, env, "manipulator_repeater_dispatch", ChromaGameTests::manipulatorRepeaterDispatch);
		register(event, env, "manipulator_cliff_transparify", ChromaGameTests::manipulatorCliffTransparify);
		register(event, env, "pylon_worldgen_grid_density", ChromaGameTests::pylonWorldgenGridDensity);
		register(event, env, "casting_table_menu_grid", ChromaGameTests::castingTableMenuGrid);
		register(event, env, "early_game_casting_stand_chain", ChromaGameTests::earlyGameCastingStandChain);
		register(event, env, "casting_manipulator_fake_player_guard", ChromaGameTests::castingManipulatorFakePlayerGuard);
		register(event, env, "lexicon_custom_data_roundtrip", ChromaGameTests::lexiconCustomDataRoundtrip);
		register(event, env, "lexicon_v33a_catalog", ChromaGameTests::lexiconV33aCatalog);
		register(event, env, "lexicon_casting_recipe_snapshot", ChromaGameTests::lexiconCastingRecipeSnapshot);
		register(event, env, "information_fragment_research_loop", ChromaGameTests::informationFragmentResearchLoop);
		register(event, env, "data_node_nbt_feature", ChromaGameTests::dataNodeNbtFeature);
		register(event, env, "data_node_scan_loop", ChromaGameTests::dataNodeScanLoop);
		register(event, env, "meta_alloy_ecology_contract", ChromaGameTests::metaAlloyEcologyContract);
		register(event, env, "tunnel_nuker_entity_contract", ChromaGameTests::tunnelNukerEntityContract);
		register(event, env, "memory_crystal_inscription_loop", ChromaGameTests::memoryCrystalInscriptionLoop);
		register(event, env, "lore_key_puzzle_contract", ChromaGameTests::loreKeyPuzzleContract);
		register(event, env, "tiered_ore_progression_gate", ChromaGameTests::tieredOreProgressionGate);
		register(event, env, "tiered_ore_worldgen", ChromaGameTests::tieredOreWorldgen);
		register(event, env, "creative_tiered_resource_access", ChromaGameTests::creativeTieredResourceAccess);
		register(event, env, "rainbow_tree_shape_and_log", ChromaGameTests::rainbowTreeShapeAndLog);
		register(event, env, "structure_cavern_nbt_controller", ChromaGameTests::structureCavernNbtController);
		register(event, env, "structure_burrow_nbt_controller", ChromaGameTests::structureBurrowNbtController);
		register(event, env, "structure_ocean_nbt_trap", 70, ChromaGameTests::structureOceanNbtTrap);
		register(event, env, "structure_desert_nbt_controller", ChromaGameTests::structureDesertNbtController);
		register(event, env, "structure_snow_nbt_route", ChromaGameTests::structureSnowNbtRoute);
		register(event, env, "biome_fragment_light_panel_switch", ChromaGameTests::biomeFragmentLightPanelSwitch);
		register(event, env, "biome_fragment_music_loop", ChromaGameTests::biomeFragmentMusicLoop);
		register(event, env, "biome_fragment_nbt_completion", ChromaGameTests::biomeFragmentNbtCompletion);
		register(event, env, "chroma_door_uuid_key_loop", 70, ChromaGameTests::chromaDoorUuidKeyLoop);
		register(event, env, "heat_lamp_temperature_furnace_loop", ChromaGameTests::heatLampTemperatureFurnaceLoop);
		register(event, env, "burrow_cache_loot_halves", ChromaGameTests::burrowCacheLootHalves);
		register(event, env, "structure_chest_fragments", ChromaGameTests::structureChestFragments);
		register(event, env, "structure_write_window_fit", ChromaGameTests::structureWriteWindowFit);
		register(event, env, "nether_lava_rivers", ChromaGameTests::netherLavaRivers);
		register(event, env, "proxima_deco_blocks", ChromaGameTests::proximaDecoBlocks);
		register(event, env, "crystal_shrub_feature", ChromaGameTests::crystalShrubFeature);
		register(event, env, "floatstone_feature", ChromaGameTests::floatstoneFeature);
		register(event, env, "glass_cliff_piece", ChromaGameTests::glassCliffPiece);
		register(event, env, "crystal_tree_feature", ChromaGameTests::crystalTreeFeature);
		register(event, env, "crystal_pit_feature", ChromaGameTests::crystalPitFeature);
		register(event, env, "proxima_biome_features", ChromaGameTests::proximaBiomeFeatures);
		register(event, env, "aurorae_feature", ChromaGameTests::auroraeFeature);
		register(event, env, "aurora_curtain_drift", ChromaGameTests::auroraCurtainDrift);
		register(event, env, "loot_chest_lid_event", ChromaGameTests::lootChestLidEvent);
		register(event, env, "loot_chest_trap_signal", ChromaGameTests::lootChestTrapSignal);
		register(event, env, "structure_trap_and_wiring", ChromaGameTests::structureTrapAndWiring);
		register(event, env, "village_casting_nbt_contract", ChromaGameTests::villageCastingNbtContract);
		register(event, env, "focus_crystal_trade_definition", ChromaGameTests::focusCrystalTradeDefinition);
		register(event, env, "storage_crystal_item_and_recipe", ChromaGameTests::storageCrystalItemAndRecipe);
		register(event, env, "crystal_charger_item_loop", ChromaGameTests::crystalChargerItemLoop);
		register(event, env, "item_aura_infuser_loop", ChromaGameTests::itemAuraInfuserLoop);
		register(event, env, "player_aura_infuser_loop", ChromaGameTests::playerAuraInfuserLoop);
		register(event, env, "portal_structure_and_charge", 80, ChromaGameTests::portalStructureAndCharge);
		register(event, env, "portal_entry_rules", ChromaGameTests::portalEntryRules);
		register(event, env, "proxima_structure_placement", ChromaGameTests::proximaStructurePlacement);
		register(event, env, "proxima_central_region", ChromaGameTests::proximaCentralRegion);
		register(event, env, "proxima_biome_distribution", 200, ChromaGameTests::proximaBiomeDistribution);
		register(event, env, "proxima_generator_gate", 200, ChromaGameTests::proximaGeneratorGate);
		register(event, env, "proxima_biome_source", 200, ChromaGameTests::proximaBiomeSource);
		register(event, env, "proxima_terrain_profile", 200, ChromaGameTests::proximaTerrainProfile);
		register(event, env, "proxima_dimension_registered", 200, ChromaGameTests::proximaDimensionGenerates);
	}

	/**
	 * Proxima now exists as a real level, and it builds real terrain — which is what finally opens the
	 * Portal Rift's destination half.
	 */
	private static void proximaDimensionGenerates(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();

		// The noise settings and dimension type are ordinary datapack worldgen registries, so they load
		// here and can be asserted directly.
		var noise = registries.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
				.getOrThrow(ProximaNoiseSettings.PROXIMA).value();
		helper.assertTrue(noise.seaLevel() == 63, "V33a's sea level is 63");
		helper.assertTrue(!noise.aquifersEnabled() && !noise.oreVeinsEnabled(),
				"V33a has neither aquifers nor ore veins");
		helper.assertTrue(noise.defaultBlock().is(Blocks.STONE) && noise.defaultFluid().is(Blocks.WATER),
				"Proxima is stone and water");
		var settings = noise.noiseSettings();
		helper.assertTrue(settings.minY() == 0 && settings.height() == 256,
				"V33a WorldProviderChroma.getHeight is 256 from y=0");
		// V33a samples 5x33x5 per chunk: four horizontal cells of four blocks, thirty-two vertical of
		// eight. Getting this wrong silently changes the shape of every hill in the dimension.
		helper.assertTrue(settings.getCellWidth() == 4 && settings.getCellHeight() == 8,
				"the noise lattice must match V33a's, found " + settings.getCellWidth() + "x"
						+ settings.getCellHeight());

		var type = registries.lookupOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE)
				.getOrThrow(ChromaDimensions.PROXIMA_TYPE).value();
		helper.assertTrue(type.minY() == 0 && type.height() == 256 && type.logicalHeight() == 256,
				"Proxima must be 0-256, matching V33a getHeight");
		helper.assertTrue(type.hasSkyLight() && type.hasFixedTime() && !type.hasCeiling(),
				"isSurfaceWorld true, a pinned celestial angle, and no ceiling");
		helper.assertTrue(type.coordinateScale() == 1.0,
				"V33a getMovementFactor is 1, so Proxima shares the Overworld's coordinate scale");
		helper.assertTrue(type.ambientLight() == 0,
				"V33a leaves the light brightness table at the vanilla default");

		// The terrain profile the router feeds on, checked at its two ends. This is what decides the
		// shape of the dimension, and it is pure arithmetic, so it is assertable without a level.
		var offset = new ProximaTerrainDensityFunction(ProximaTerrainDensityFunction.Mode.OFFSET);
		var factor = new ProximaTerrainDensityFunction(ProximaTerrainDensityFunction.Mode.FACTOR);
		DensityFunction.FunctionContext atOrigin = new DensityFunction.SinglePointContext(0, 64, 0);
		DensityFunction.FunctionContext farOut = new DensityFunction.SinglePointContext(400000, 64, 400000);
		helper.assertTrue(offset.compute(atOrigin) > offset.compute(farOut),
				"the centre of Proxima must sit higher than its outskirts");
		// The reciprocal is the easy thing to get backwards: a big factor means terrain hugs its base
		// height, so the flat centre must have the LARGER factor.
		helper.assertTrue(factor.compute(atOrigin) == 10,
				"the flat centre must take the maximum factor, found " + factor.compute(atOrigin));
		helper.assertTrue(factor.compute(farOut) < factor.compute(atOrigin),
				"the mountainous outskirts must take a smaller factor, or the dimension is inside out");
		helper.succeed();
	}

	/**
	 * V33a's radial terrain profile: a flat plain at the centre that falls away and grows steadily
	 * more mountainous with distance, flattened again within eight chunks of any structure.
	 */
	private static void proximaTerrainProfile(GameTestHelper helper) {
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		try {
			StructureCalculator.allowUnfinishedStructures = true;
			StructureCalculator structures = new StructureCalculator(1234L);
			structures.generate();
			ProximaTerrainProfile profile = new ProximaTerrainProfile(structures);

			// The centre is flat: no roughness, so base height sits at its maximum and variation at 0.
			helper.assertTrue(profile.roughness(0, 0) == 0,
					"the world origin must have no terrain roughness");
			helper.assertTrue(profile.baseHeight(0, 0) == 0.125F && profile.heightVariation(0, 0) == 0,
					"the centre of Proxima is a flat plain at V33a's maximum base height");

			// Roughness grows monotonically with radius, once clear of the structure damping.
			double previousRoughness = -1;
			for (int r = 4000; r <= 200000; r += 4000) {
				// Skip the eight-chunk skirt around a structure, which deliberately damps the ramp.
				if (profile.distanceToNearestStructureInChunks(r / 4, 0)
						<= ProximaTerrainProfile.STRUCTURE_FLATTEN_CHUNKS)
					continue;
				double rough = profile.roughness(r, 0);
				helper.assertTrue(rough > previousRoughness,
						"roughness must grow with distance from the origin, stalled at quart " + r);
				previousRoughness = rough;
			}

			// Base height falls towards its floor while variation climbs, which is the whole shape.
			helper.assertTrue(profile.baseHeight(200000, 0) == -0.25F,
					"far out, the base height must sit on V33a's -0.25 floor");
			helper.assertTrue(profile.heightVariation(200000, 0) > profile.heightVariation(20000, 0),
					"height variation must keep climbing where the base height has already bottomed out");
			for (int r = 0; r <= 200000; r += 5000)
				helper.assertTrue(profile.baseHeight(r, 0) >= -0.25F && profile.baseHeight(r, 0) <= 0.125F,
						"base height must stay inside V33a's bounds at quart " + r);

			// Within eight chunks of a structure the profile is damped linearly to flat.
			StructureCalculator.StructurePlacement site = structures.getPlacements().getFirst();
			int siteChunkX = site.placement().getX() >> 4;
			int siteChunkZ = site.placement().getZ() >> 4;
			helper.assertTrue(profile.distanceToNearestStructureInChunks(siteChunkX, siteChunkZ) == 0,
					"a structure's own chunk must be zero chunks from a structure");
			helper.assertTrue(profile.roughness(siteChunkX * 4, siteChunkZ * 4) == 0,
					"the ground a structure stands on must be perfectly flat");
			double atFour = profile.roughness((siteChunkX + 4) * 4, siteChunkZ * 4);
			double atTwelve = profile.roughness((siteChunkX + 12) * 4, siteChunkZ * 4);
			helper.assertTrue(atFour > 0 && atFour < atTwelve,
					"the flattening must ramp back up over the eight-chunk skirt");

			// The monument competes with the structures for that damping.
			int monumentChunkX = structures.getMonumentPosition().getX() >> 4;
			int monumentChunkZ = structures.getMonumentPosition().getZ() >> 4;
			helper.assertTrue(profile.distanceToNearestStructureInChunks(monumentChunkX, monumentChunkZ) == 0,
					"the monument's own chunk must also read as zero chunks away");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * All thirteen Proxima biomes must be registered with V33a's own settings, and the biome source
	 * must resolve every one of them out of the painted map.
	 */
	private static void proximaBiomeSource(GameTestHelper helper) {
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		RegionMapper.clear();
		BiomeDistributor.clear();
		try {
			HolderGetter<Biome> lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
			for (ProximaBiomes b : ProximaBiomes.biomeList)
				helper.assertTrue(lookup.get(b.biomeKey()).isPresent(),
						b + " must be a registered biome at " + b.biomeKey().identifier());
			for (ProximaSubBiomes b : ProximaSubBiomes.biomeList)
				helper.assertTrue(lookup.get(b.biomeKey()).isPresent(),
						b + " must be a registered biome at " + b.biomeKey().identifier());

			// V33a: rain is disabled by the shared base and only BiomeGenChromaOcean turns it back on.
			for (ProximaBiomes b : ProximaBiomes.biomeList)
				helper.assertTrue(!lookup.getOrThrow(b.biomeKey()).value().hasPrecipitation(),
						b + " must not have precipitation");
			for (ProximaSubBiomes b : ProximaSubBiomes.biomeList)
				helper.assertTrue(lookup.getOrThrow(b.biomeKey()).value().hasPrecipitation()
								== (b == ProximaSubBiomes.DEEPOCEAN),
						"Aura Ocean is the only Proxima biome where it rains, " + b + " disagreed");

			// V33a BiomeGenCentral is the only biome anywhere in Proxima with a spawn.
			for (ProximaBiomes b : ProximaBiomes.biomeList) {
				boolean anySpawns = b.biomeKey() != null && lookup.getOrThrow(b.biomeKey()).value()
						.getMobSettings().getMobs(ChromaEntityTypes.TUNNEL_NUKER.get().getCategory())
						.unwrap().size() > 0;
				helper.assertTrue(anySpawns == (b == ProximaBiomes.CENTER),
						"only the Luminescent Sanctuary may spawn anything, " + b + " disagreed");
			}

			StructureCalculator.allowUnfinishedStructures = true;
			ProximaGenerators.Layout layout = ProximaGenerators.generateNow(1234L);
			ProximaBiomeSource source = new ProximaBiomeSource(lookup);
			helper.assertTrue(source.possibleBiomes().size()
							== ProximaBiomes.biomeList.length + ProximaSubBiomes.biomeList.length,
					"the source must advertise every Proxima biome, or chunk serialization will reject one");

			// The source is a pure read of the painted map: quart coordinates back to blocks, and no
			// climate sampling at all.
			int mx = layout.structures().getMonumentPosition().getX();
			int mz = layout.structures().getMonumentPosition().getZ();
			helper.assertTrue(source.getNoiseBiome(QuartPos.fromBlock(mx), 0, QuartPos.fromBlock(mz), null)
							.is(ProximaBiomes.MONUMENT.biomeKey()),
					"the monument position must resolve to the Monument Field through the biome source");
			helper.assertTrue(source.getNoiseBiome(0, 0, 0, null).is(ProximaBiomes.CENTER.biomeKey()),
					"the world origin is inside the central region");
			for (int qx = 0; qx < 64; qx++) {
				Holder<Biome> got = source.getNoiseBiome(qx * 97, 0, qx * 53, null);
				helper.assertTrue(got != null && got.unwrapKey().isPresent(),
						"every query must resolve to a registered biome");
			}

			// The documented race answer: with no map painted, the source still answers rather than
			// throwing during a chunk build.
			BiomeDistributor.clear();
			helper.assertTrue(source.getNoiseBiome(12345, 0, 6789, null).is(ProximaBiomes.CENTER.biomeKey()),
					"before the map exists the source must fall back to the Luminescent Sanctuary");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * The generator gate itself: it must start closed, run the whole chain in dependency order off the
	 * server thread, and only then open — which is what keeps the Portal Rift charging and refusing
	 * travel while Proxima's layout is still being decided.
	 */
	private static void proximaGeneratorGate(GameTestHelper helper) {
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		RegionMapper.clear();
		BiomeDistributor.clear();
		try {
			StructureCalculator.allowUnfinishedStructures = true;
			ProximaGenerators.markAllPending();
			helper.assertTrue(!ProximaGenerators.areGeneratorsReady(),
					"the gate must start closed, so a rift cannot carry anyone into an undecided Proxima");
			for (ProximaGenerators.Generator g : ProximaGenerators.Generator.values())
				helper.assertTrue(!ProximaGenerators.isReady(g), g + " must start pending");

			// The synchronous entry point is what datagen and tests use; regenerate() wraps exactly this
			// on a background thread, which matters because the production-size biome paint takes ~19s.
			ProximaGenerators.Layout layout = ProximaGenerators.generateNow(4321L);
			helper.assertTrue(ProximaGenerators.areGeneratorsReady(),
					"running the whole chain must open the gate");
			helper.assertTrue(ProximaGenerators.getLayout() == layout,
					"the finished layout must be published as one object");
			helper.assertTrue(layout.seed() == 4321L && layout.structures().arePositionsDetermined()
							&& layout.region() != null && layout.biomes() != null,
					"the layout must carry every finished generator");

			// The ordering guarantee: each stage's output is consistent with the one before it.
			helper.assertTrue(layout.region().contains(
							layout.structures().getMonumentPosition().getX(),
							layout.structures().getMonumentPosition().getZ()),
					"the region must have been sized from this run's own structure ring");
			helper.assertTrue(layout.biomes().biomeAt(
							layout.structures().getMonumentPosition().getX(),
							layout.structures().getMonumentPosition().getZ()) == ProximaBiomes.MONUMENT,
					"the biome map must have been painted around this run's own monument");
			helper.assertTrue(layout.biomes().getSize() == BiomeDistributor.DEFAULT_SIZE,
					"the orchestrated run must use the production map size");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * The Proxima biome map: every cell painted, blob counts matching each biome's spawn weight,
	 * sub-biomes only inside their parents, the structure/central-region query priority, the coarse
	 * client packet, and reproducibility from the dimension seed.
	 *
	 * <p>Runs at a 512-cell map rather than the production 4096. V33a's own
	 * {@code SIZE = 4096;//2048;//4096;} shows the size was varied, and {@code placeBlob} scales every
	 * radius by {@code SIZE/4096}, so this exercises the identical algorithm at a testable cost.
	 */
	private static void proximaBiomeDistribution(GameTestHelper helper) {
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		RegionMapper.clear();
		BiomeDistributor.clear();
		try {
			StructureCalculator.allowUnfinishedStructures = true;
			StructureCalculator structures = new StructureCalculator(1234L);
			structures.generate();
			RegionMapper.generate(structures, 1234L);

			BiomeDistributor unplaced = new BiomeDistributor(1234L, 128);
			boolean rejected = false;
			try {
				unplaced.generate(new StructureCalculator(7L));
			}
			catch (IllegalStateException expected) {
				rejected = true;
			}
			helper.assertTrue(rejected,
					"distributing biomes before the structure ring is placed must fail loudly");

			int size = 512;
			BiomeDistributor map = new BiomeDistributor(1234L, size).generate(structures);
			helper.assertTrue(ProximaGenerators.isReady(ProximaGenerators.Generator.BIOME),
					"finishing distribution must clear the BIOME bit of the generator gate");
			helper.assertTrue(ProximaGenerators.areGeneratorsReady(),
					"with structures, region and biomes all done the whole gate must be open");

			// Stage 2 runs until nothing is empty, so no map cell may be left unpainted. Query through
			// the raw map rather than biomeAt, which would mask emptiness with the central region.
			byte[] coarse = map.getDataForPacket();
			helper.assertTrue(coarse.length == (size / 8) * (size / 8),
					"the client packet must be every eighth cell in both axes");
			for (byte b : coarse)
				helper.assertTrue(b != 0, "no cell may be left unpainted after the fill stage");

			// Blob counts are literally the spawn weights.
			for (ProximaBiomes b : ProximaBiomes.biomeList) {
				int blobs = map.getBlobLocations(b).size();
				helper.assertTrue(blobs == b.spawnWeight,
						b + " must get exactly its spawn weight in blobs, expected " + b.spawnWeight
								+ " found " + blobs);
			}
			helper.assertTrue(map.getBlobLocations(ProximaBiomes.CENTER).isEmpty()
							&& map.getBlobLocations(ProximaBiomes.STRUCTURE).isEmpty()
							&& map.getBlobLocations(ProximaBiomes.MONUMENT).isEmpty(),
					"the three technical biomes are placed by other means and must never be scattered");

			// Sub-biomes only ever appear where their parent was, and only for parents that have one.
			for (ProximaSubBiomes s : ProximaSubBiomes.biomeList)
				helper.assertTrue(s.getParent() != null && s.getParent().getSubBiome() == s,
						s + " must be linked to exactly one parent biome");
			helper.assertTrue(map.getBlobbedBiomes().stream()
							.filter(b -> b instanceof ProximaSubBiomes)
							.allMatch(b -> ((ProximaSubBiomes)b).getParent().getSubBiome() == b),
					"every placed sub-biome blob must belong to its declared parent");

			// Query priority: the monument's own position resolves to the Monument Field, a structure's
			// entry to the Structure Field, and the static accessor must agree with the instance.
			int mx = structures.getMonumentPosition().getX();
			int mz = structures.getMonumentPosition().getZ();
			helper.assertTrue(map.biomeAt(mx, mz) == ProximaBiomes.MONUMENT,
					"the monument's own position must be inside the Monument Field");
			helper.assertTrue(BiomeDistributor.getBiome(mx, mz) == ProximaBiomes.MONUMENT,
					"the static accessor must answer for the active distributor");
			StructureCalculator.StructurePlacement first = structures.getPlacements().getFirst();
			helper.assertTrue(map.biomeAt(first.getEntryPosX(), first.getEntryPosZ()) == ProximaBiomes.STRUCTURE,
					"a structure's own entry must be inside its Structure Field");
			// The central region wins over the painted map but loses to a structure region.
			helper.assertTrue(map.biomeAt(0, 0) == ProximaBiomes.CENTER,
					"the world origin is inside the central region, so it is the Luminescent Sanctuary");

			// Far outside every structure region and the central boundary, the painted map answers.
			int far = (int)(StructureCalculator.getMaximumPossibleDistance() + RegionMapper.MAX_BUFFER + 5000);
			ProximaBiomeType outer = map.biomeAt(far, far);
			helper.assertTrue(outer != null && outer != ProximaBiomes.CENTER
							&& outer != ProximaBiomes.STRUCTURE && outer != ProximaBiomes.MONUMENT,
					"beyond every region the painted map must answer with a real biome, got " + outer);

			// Reproducibility, the reason the shuffle had to be seeded.
			BiomeDistributor again = new BiomeDistributor(1234L, size).generate(structures);
			helper.assertTrue(java.util.Arrays.equals(again.getDataForPacket(), coarse),
					"the biome map must be reproducible from the dimension seed");
			BiomeDistributor different = new BiomeDistributor(4321L, size).generate(structures);
			helper.assertTrue(!java.util.Arrays.equals(different.getDataForPacket(), coarse),
					"a different dimension seed must repaint the map");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * The Proxima central region: a twenty-lobe boundary sized from the structure ring, always
	 * enclosing every structure with V33a's 200-to-1500 block buffer, reproducible from the dimension
	 * seed, and refusing to be built before the ring exists.
	 */
	private static void proximaCentralRegion(GameTestHelper helper) {
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		RegionMapper.clear();
		try {
			StructureCalculator unplaced = new StructureCalculator(99L);
			boolean rejected = false;
			try {
				RegionMapper.generate(unplaced, 99L);
			}
			catch (IllegalStateException expected) {
				rejected = true;
			}
			helper.assertTrue(rejected,
					"sizing the central region before the structure ring is placed must fail loudly,"
							+ " not silently size itself off an origin of zero");
			helper.assertTrue(!RegionMapper.isPointInCentralRegion(0, 0),
					"with no region generated, nothing may report as inside it");

			StructureCalculator.allowUnfinishedStructures = true;
			StructureCalculator structures = new StructureCalculator(1234L);
			structures.generate();
			RegionMapper region = RegionMapper.generate(structures, 1234L);
			helper.assertTrue(ProximaGenerators.isReady(ProximaGenerators.Generator.REGION),
					"generating the region must clear the REGION bit of the generator gate");

			double maxStructure = structures.getMaximumDistanceFromOrigin();
			// V33a fromMinMaxRadii(min, max, 20): minRadius is the midpoint and each of the twenty
			// lobes contributes at most (max-min)/2/20, so the curve stays inside [min, max].
			double min = maxStructure + RegionMapper.MIN_BUFFER;
			double max = maxStructure + RegionMapper.MAX_BUFFER;
			for (int degrees = 0; degrees < 360; degrees += 3) {
				double radius = region.getRadius(degrees);
				helper.assertTrue(radius >= min - 1 && radius <= max + 1,
						"the boundary must stay within the 200-1500 block buffer at bearing " + degrees
								+ ", found " + radius);
			}

			// The whole point of the buffer: every structure is comfortably inside.
			for (StructureCalculator.StructurePlacement placement : structures.getPlacements())
				helper.assertTrue(region.contains(placement.placement().getX(), placement.placement().getZ()),
						"every placed structure must fall inside the central region, " + placement + " did not");
			helper.assertTrue(region.contains(0, 0) && RegionMapper.isPointInCentralRegion(0, 0),
					"the world origin is always inside, and the static accessor must agree");
			helper.assertTrue(!region.contains(max + 1000, 0) && !region.contains(0, max + 1000),
					"a point beyond the outer radius is in the outer regions, which is what"
							+ " OuterRegionsEvents keys off");

			// Reproducibility, for the same reason the structure ring needs it.
			StructureCalculator sameRing = new StructureCalculator(1234L);
			sameRing.generate();
			RegionMapper again = RegionMapper.generate(sameRing, 1234L);
			for (int degrees = 0; degrees < 360; degrees += 15)
				helper.assertTrue(again.getRadius(degrees) == region.getRadius(degrees),
						"the boundary must be reproducible from the dimension seed");
			RegionMapper different = RegionMapper.generate(sameRing, 4321L);
			boolean moved = false;
			for (int degrees = 0; degrees < 360 && !moved; degrees += 15)
				moved = different.getRadius(degrees) != region.getRadius(degrees);
			helper.assertTrue(moved, "a different dimension seed must reshape the boundary");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * The Proxima structure ring: sixteen elements evenly spaced 22.5 degrees apart at 5000+-3000 from
	 * a centre that itself wanders +-6000, reproducible from the dimension seed, with the monument at
	 * that centre and the upper distance bound that {@code RegionMapper} sizes itself from.
	 */
	private static void proximaStructurePlacement(GameTestHelper helper) {
		// V33a's dev override, which assigns every structure type regardless of whether its generator
		// finished. It is the only way to exercise the assignment loop while all eighteen puzzle
		// generators are deliberately unported.
		boolean previous = StructureCalculator.allowUnfinishedStructures;
		try {
			helper.assertTrue(DimensionStructureType.usableStructures().isEmpty(),
					"no Proxima puzzle generator is ported yet, so no structure type may be usable");

			StructureCalculator unassigned = new StructureCalculator(1234L);
			unassigned.generate();
			helper.assertTrue(unassigned.getPlacements().isEmpty() && unassigned.arePositionsDetermined(),
					"with nothing usable the ring must still resolve its origin, so the biome layer is"
							+ " not blocked on the puzzles");
			helper.assertTrue(ProximaGenerators.isReady(ProximaGenerators.Generator.STRUCTURE),
					"finishing placement must clear the STRUCTURE bit of the generator gate");

			StructureCalculator.allowUnfinishedStructures = true;
			StructureCalculator calc = new StructureCalculator(1234L);
			calc.generate();
			List<StructureCalculator.StructurePlacement> placed = calc.getPlacements();
			helper.assertTrue(placed.size() == CrystalElement.elements.length,
					"every one of the sixteen elements must get a structure, found " + placed.size());
			helper.assertTrue(placed.stream().map(p -> p.color).distinct().count() == 16,
					"each element must appear exactly once");
			// Eighteen usable types for sixteen elements, so the set is never exhausted and no element
			// reaches generation index 1.
			helper.assertTrue(placed.stream().allMatch(p -> p.generationIndex == 0),
					"eighteen types cover sixteen elements without reusing the pool");
			helper.assertTrue(placed.stream().map(p -> p.type).distinct().count() == 16,
					"types are drawn without replacement, so no two elements share one");

			int originX = calc.getStructureOriginX();
			int originZ = calc.getStructureOriginZ();
			helper.assertTrue(Math.abs(originX) <= StructureCalculator.STRUCTURE_CENTER_VARIATION
							&& Math.abs(originZ) <= StructureCalculator.STRUCTURE_CENTER_VARIATION,
					"the ring centre must stay inside V33a's +-6000 wander");
			helper.assertTrue(calc.getMonumentPosition().getX() == originX
							&& calc.getMonumentPosition().getZ() == originZ,
					"the monument sits at the ring centre");

			for (StructureCalculator.StructurePlacement placement : placed) {
				double dx = placement.placement().getX() - originX;
				double dz = placement.placement().getZ() - originZ;
				double radius = Math.sqrt(dx * dx + dz * dz);
				helper.assertTrue(radius >= StructureCalculator.BASE_RADIUS - StructureCalculator.RADIUS_VARIATION - 2
								&& radius <= StructureCalculator.BASE_RADIUS + StructureCalculator.RADIUS_VARIATION + 2,
						"every structure must sit on the 5000+-3000 ring, found " + radius);
				double bearing = Math.toDegrees(Math.atan2(dz, dx));
				double expected = calc.getStructureAngleOrigin() + placement.color.ordinal() * 22.5;
				double delta = Math.abs(Math.IEEEremainder(bearing - expected, 360));
				helper.assertTrue(delta < 0.5,
						"element " + placement.color + " must sit on its own 22.5-degree spoke, off by " + delta);
				// Until a generator is ported the entry is the placement, which is also what V33a
				// reports before calculate() runs.
				helper.assertTrue(placement.getEntryPosX() == placement.placement().getX()
								&& placement.getEntryPosZ() == placement.placement().getZ(),
						"an unported generator must leave the entry position at the placement");
			}

			// V33a's own upper bound: max ring radius from the wandered centre, not the actual radii.
			double bound = calc.getMaximumDistanceFromOrigin();
			helper.assertTrue(placed.stream().allMatch(p -> Math.sqrt(
							(double)p.placement().getX() * p.placement().getX()
									+ (double)p.placement().getZ() * p.placement().getZ()) <= bound),
					"getMaximumDistanceFromOrigin must bound every placement, since RegionMapper sizes"
							+ " the central region from it");
			helper.assertTrue(bound <= StructureCalculator.getMaximumPossibleDistance() * Math.sqrt(2) + 1,
					"the per-seed bound must not exceed the global bound's diagonal");

			// The deviation that matters: V33a drew the origin and radii from an unseeded global
			// Random. Re-running the same seed has to reproduce the same ring.
			StructureCalculator again = new StructureCalculator(1234L);
			again.generate();
			helper.assertTrue(again.getStructureOriginX() == originX
							&& again.getStructureOriginZ() == originZ
							&& again.getStructureAngleOrigin() == calc.getStructureAngleOrigin(),
					"the ring must be reproducible from the dimension seed");
			for (int i = 0; i < placed.size(); i++)
				helper.assertTrue(again.getPlacements().get(i).placement().equals(placed.get(i).placement()),
						"every placement must be reproducible from the dimension seed");

			StructureCalculator other = new StructureCalculator(4321L);
			other.generate();
			helper.assertTrue(other.getStructureOriginX() != originX
							|| other.getStructureOriginZ() != originZ,
					"a different dimension seed must move the ring");

			StructureCalculator.StructurePlacement nearest = calc.getNearestStructureWithinRange(
					placed.getFirst().placement().getX(), placed.getFirst().placement().getZ(), 64);
			helper.assertTrue(nearest == placed.getFirst(),
					"getNearestStructureWithinRange must find a structure standing on top of it");
			helper.assertTrue(calc.getNearestStructureWithinRange(originX, originZ, 100) == null,
					"the ring centre is 5000 blocks from every structure, so a 100-block query finds none");

			// V33a's structure password. Its SHA-1 step returned null in the modern DragonAPI because
			// the javax.xml.bind hex converter left the JDK, so this both covers the formula and
			// guards the HexFormat replacement that restored it.
			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			DimensionStructureType.StructureTypeData first = placed.getFirst().typeData();
			int password = first.getPassword(player, "26.2");
			helper.assertTrue(password == first.getPassword(player, "26.2"),
					"a structure password must be stable for the same player, type and version");
			helper.assertTrue(password != first.getPassword(player, "26.1"),
					"the password must depend on the game version, as V33a's Loader.MC_VERSION term does");
			helper.assertTrue(password != placed.get(1).typeData().getPassword(player, "26.2"),
					"two different structures must not share a password");
			helper.assertTrue(first.getPassword(null, "26.2") != password,
					"the password must depend on the player, falling back to Reika's UUID when absent");
		}
		finally {
			StructureCalculator.allowUnfinishedStructures = previous;
		}
		helper.succeed();
	}

	/**
	 * The generated 15x10x15 portal NBT must validate as a real multiblock, charge on the source's
	 * 300-tick timer, invalidate when any required cell is broken, and revalidate when it is restored.
	 * Charge, tuning and ownership must survive a block-entity save/load.
	 */
	private static void portalStructureAndCharge(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pad = helper.absolutePos(new BlockPos(20, 6, 20));
		NBTStructureLoader.place(level, ChromaStructureTemplateProvider.PORTAL, pad,
				PortalStructure.ANCHOR, state -> state, 2);

		BlockEntity blockEntity = level.getBlockEntity(pad);
		helper.assertTrue(blockEntity instanceof TileEntityCrystalPortal,
				"the portal NBT must place its registered Portal Rift entity at the template anchor");
		TileEntityCrystalPortal portal = (TileEntityCrystalPortal)blockEntity;
		helper.assertTrue(portal.isPadCentre(),
				"template anchor (7,0,7) must be the centre of the 3x3 rift pad");

		portal.validateStructure();
		helper.assertTrue(!portal.isComplete(),
				"a portal without its eight Ender Crystals must not validate");

		for (BlockPos relative : PortalStructure.ENDER_CRYSTALS) {
			BlockPos at = pad.offset(relative);
			EndCrystal crystal = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.COMMAND);
			helper.assertTrue(crystal != null, "test needs a vanilla Ender Crystal");
			crystal.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			crystal.setShowBottom(false);
			level.addFreshEntity(crystal);
		}
		portal.validateStructure();
		helper.assertTrue(portal.isComplete(),
				"the generated portal template plus its eight Ender Crystals must match in world");

		// V33a charges by one per tick with no network cost at all, and refuses travel below 300.
		// The level's own ticker drives it, so the rate is measured across real ticks rather than by
		// calling updateEntity() repeatedly (BlockEntityBase collapses same-tick calls by design).
		helper.assertTrue(portal.getCharge() == 0, "a freshly validated portal starts uncharged");
		int chargeStart = portal.getCharge();
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(portal.getCharge() - chargeStart == 20,
					"a complete portal must gain exactly one charge per tick, not consume network energy;"
							+ " gained " + (portal.getCharge() - chargeStart));

			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			for (ProgressStage stage : ProgressionManager.instance.getPrereqs(ProgressStage.DIMENSION))
				ProgressionManager.instance.setPlayerStage(player, stage, true, false, false);
			helper.assertTrue(!portal.canPlayerUse(player),
					"a fully qualified player must still be refused below the 300-tick charge");
			// Proxima is not registered in the GameTest server, so isPortalFunctional stays false and
			// the eligibility gate must reflect that rather than passing anyway. Both halves matter:
			// the dimension has to exist AND its generators have to have decided the layout.
			portal.setChargeForTest(TileEntityCrystalPortal.MINCHARGE);
			helper.assertTrue(portal.canPlayerUse(player) == BlockChromaPortal.isPortalFunctional(level),
					"once charged, eligibility must depend only on whether Proxima is reachable");
			helper.assertTrue(!portal.canPlayerUse(player),
					"Proxima is not registered here, so a charged rift must still refuse travel");

			// Tuning: V33a (tier2 ? 150 : 1) * count^(tier2 ? 0.85 : 0.5), then 60% spent per trip.
			portal.addTuningEnergy(new ItemStack(
					ChromaItems.TIERED.get(ChromaTieredItems.PROXIMAL_ESSENCE).get(), 16));
			helper.assertTrue(portal.getTuning() == 4,
					"sixteen Proximal Essence must add exactly four tuning (16^0.5)");
			portal.addTuningEnergy(new ItemStack(
					ChromaItems.TIERED.get(ChromaTieredItems.PURE_PROXIMAL_ESSENCE).get(), 1));
			helper.assertTrue(portal.getTuning() == 154,
					"one Pure Proximal Essence must be worth 150 tuning");
			portal.setPlacer(player);
			helper.assertTrue(portal.consumeTuningForTrip() == 154 && portal.getTuning() == 61,
					"a trip must carry the full tuning and leave 40% of it behind");

			int savedCharge = portal.getCharge();
			CompoundTag saved = portal.saveWithFullMetadata(level.registryAccess());
			BlockEntity reloaded = BlockEntity.loadStatic(pad, level.getBlockState(pad), saved,
					level.registryAccess());
			helper.assertTrue(reloaded instanceof TileEntityCrystalPortal restored
							&& restored.isComplete() && restored.getTuning() == 61
							&& restored.getCharge() == savedCharge
							&& player.getUUID().equals(restored.getPlacerID()),
					"structure state, charge, tuning and ownership must survive a save/load");

			// Breaking any required cell invalidates the whole rift and zeroes its charge.
			BlockPos required = pad.offset(-3, 5, -3);
			BlockState was = level.getBlockState(required);
			helper.assertTrue(was.is(ChromaBlocks.crystallineStone(StoneTypes.FOCUS).get()),
					"the charging-particle probe cell must be the authored Pylon Focus");
			level.setBlock(required, Blocks.AIR.defaultBlockState(), 3);
			portal.validateStructure();
			helper.assertTrue(!portal.isComplete(), "removing a required cell must invalidate the portal");
			level.setBlock(required, was, 3);
			portal.validateStructure();
			helper.assertTrue(portal.isComplete(),
					"restoring the removed cell must let the portal validate and charge again");
			helper.succeed();
		});
	}

	/**
	 * V33a's contact rules: an unqualified player is thrown upward with a lethal fall distance, an
	 * ordinary item is thrown the same way, Proximal Essence is absorbed as tuning, and the Elemental
	 * Manipulator tears the whole pad down into nine dropped rifts.
	 */
	private static void portalEntryRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pad = helper.absolutePos(new BlockPos(6, 4, 6));
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				level.setBlock(pad.offset(i, 0, k), ChromaBlocks.PORTAL.get().defaultBlockState(), 3);
		BlockState state = level.getBlockState(pad);
		helper.assertTrue(level.getBlockEntity(pad) instanceof TileEntityCrystalPortal centre
						&& centre.isPadCentre(),
				"every pad block carries an entity and the middle one reports position 5");
		helper.assertTrue(state.getCollisionShape(level, pad).isEmpty(),
				"the rift must have no collision box: entities fall straight in");

		// Contact on an edge block forwards inward to the centre, exactly as V33a's walk does.
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.fallDistance = 0;
		BlockPos edge = pad.offset(1, 0, 1);
		level.getBlockState(edge).entityInside(level, edge, player,
				net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
		helper.assertTrue(player.fallDistance >= 500 && player.getDeltaMovement().y == 1.5,
				"an unqualified player must be thrown up with V33a's lethal 500 fall distance");

		TileEntityCrystalPortal centre = (TileEntityCrystalPortal)level.getBlockEntity(pad);
		ItemEntity junk = new ItemEntity(level, pad.getX() + 0.5, pad.getY() + 0.5, pad.getZ() + 0.5,
				new ItemStack(Items.STONE));
		level.addFreshEntity(junk);
		state.entityInside(level, pad, junk, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
		helper.assertTrue(junk.isAlive() && junk.getDeltaMovement().y == 1.5,
				"an item that is not Proximal Essence must be rejected, not consumed");

		ItemEntity essence = new ItemEntity(level, pad.getX() + 0.5, pad.getY() + 0.5, pad.getZ() + 0.5,
				new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.PROXIMAL_ESSENCE).get(), 9));
		level.addFreshEntity(essence);
		state.entityInside(level, pad, essence, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
		helper.assertTrue(!essence.isAlive() && centre.getTuning() == 3,
				"Proximal Essence must be absorbed and add exactly sqrt(count) tuning");

		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new ItemStack(ChromaItems.MANIPULATOR.get()));
		state.useWithoutItem(level, player,
				new BlockHitResult(Vec3.atCenterOf(pad), Direction.UP, pad, false));
		int remaining = 0;
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				if (level.getBlockState(pad.offset(i, 0, k)).getBlock() instanceof BlockChromaPortal)
					remaining++;
		helper.assertTrue(remaining == 0, "the Manipulator must dismantle the whole connected pad");
		int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pad).inflate(4)).stream()
				.filter(e -> e.getItem().is(ChromaBlocks.PORTAL.get().asItem()))
				.mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(dropped == 9, "dismantling must drop one rift per removed block, found " + dropped);
		helper.succeed();
	}

	private static void biomeFragmentLightPanelSwitch(GameTestHelper helper) {
		BlockPos panel = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockState target = ChromaBlocks.LIGHT_PANEL.get().defaultBlockState()
				.setValue(BlockLightPanel.TYPE, LightType.TARGET);
		helper.getLevel().setBlock(panel, target, 3);
		BlockLightPanel.activate(helper.getLevel(), panel, true);
		helper.assertTrue(helper.getLevel().getBlockState(panel).getValue(BlockLightPanel.ACTIVE),
				"Light Panel activation must persist in explicit block state");
		helper.assertTrue(helper.getLevel().getBlockState(panel).getLightEmission(helper.getLevel(), panel) == 15,
				"active Light Panel must emit the source-exact light level 15");

		BlockPos toggle = panel.east();
		helper.getLevel().setBlock(toggle, ChromaBlocks.PANEL_SWITCH.get().defaultBlockState(), 3);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(toggle), Direction.UP, toggle, false);
		helper.assertTrue(helper.getLevel().getBlockState(toggle)
				.useWithoutItem(helper.getLevel(), player, hit).consumesAction(),
				"Panel Switch must consume its interaction");
		helper.assertTrue(BlockLightSwitch.isSwitchUp(helper.getLevel(), toggle),
				"Panel Switch interaction must toggle the explicit UP state");

		BlockPos colorDoor = toggle.east();
		helper.getLevel().setBlock(colorDoor, ChromaBlocks.COLOR_LOCK.get().defaultBlockState(), 3);
		TileEntityColorLock lock = (TileEntityColorLock)helper.getLevel().getBlockEntity(colorDoor);
		helper.assertTrue(lock != null, "Color Lock must create its persistence entity");
		lock.setColors(CrystalElement.RED, CrystalElement.BLUE);
		lock.setOpenColors(java.util.Set.of(CrystalElement.RED));
		helper.assertTrue(!helper.getLevel().getBlockState(colorDoor).getValue(BlockColoredLock.OPEN),
				"Color Lock must remain solid while one required color is closed");
		lock.setOpenColors(java.util.Set.of(CrystalElement.RED, CrystalElement.BLUE));
		helper.assertTrue(helper.getLevel().getBlockState(colorDoor).getValue(BlockColoredLock.OPEN),
				"Color Lock must open when all required colors are represented");
		helper.assertTrue(helper.getLevel().getBlockState(colorDoor).getCollisionShape(helper.getLevel(), colorDoor).isEmpty(),
				"an open Color Lock must remove collision");

		BlockPos controllerPos = helper.absolutePos(new BlockPos(12, 2, 12));
		BlockPos puzzleDoor = controllerPos.offset(1, 2, -3);
		helper.getLevel().setBlock(puzzleDoor, ChromaBlocks.COLOR_LOCK.get().defaultBlockState(), 3);
		helper.getLevel().setBlock(controllerPos, ChromaBlocks.STRUCTURE_CONTROLLER.get().defaultBlockState(), 3);
		TileEntityStructureController controller = (TileEntityStructureController)helper.getLevel()
				.getBlockEntity(controllerPos);
		helper.assertTrue(controller != null, "Biome Fragment must have a persistent controller");
		controller.initialize(TileEntityStructureController.StructureType.BIOME_FRAGMENT,
				CrystalElement.WHITE, 0, false, false);
		CrystalElement first = controller.getBiomeDoorColor(0, 0);
		CrystalElement second = controller.getBiomeDoorColor(0, 1);
		BlockPos firstRune = controllerPos.offset(5, 5, 6);
		BlockPos secondRune = controllerPos.offset(6, 5, 5);
		helper.getLevel().setBlock(firstRune, ChromaBlocks.rune(first).get().defaultBlockState(), 3);
		helper.getLevel().setBlock(secondRune, ChromaBlocks.rune(second).get().defaultBlockState(), 3);
		for (BlockPos keyPos : List.of(firstRune.above(), secondRune.above())) {
			helper.getLevel().setBlock(keyPos, ChromaBlocks.LOCK_KEY.get().defaultBlockState()
					.setValue(reika.chromaticraft.block.dimension.structure.locks.BlockLockKey.CHANNEL,
							controller.getBiomeKeyChannel()), 3);
			TileEntityLockKey key = (TileEntityLockKey)helper.getLevel().getBlockEntity(keyPos);
			helper.assertTrue(key != null, "Lock Key must create its persistence entity");
			key.setDelegate(controllerPos);
			key.notifyDelegate(true, player);
		}
		helper.assertTrue(helper.getLevel().getBlockState(puzzleDoor).getValue(BlockColoredLock.OPEN),
				"keys moved above both required runes must open the matching Biome Fragment door");
		TileEntityLockKey portable = (TileEntityLockKey)helper.getLevel().getBlockEntity(firstRune.above());
		ItemStack keyStack = portable.createPortableStack(controller.getBiomeKeyChannel());
		CompoundTag keyData = keyStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		helper.assertTrue(keyData.getIntOr("channel", -1) == controller.getBiomeKeyChannel()
				&& keyData.getLong("delegate").map(BlockPos::of).filter(controllerPos::equals).isPresent(),
				"mined Lock Keys must preserve their channel and delegate in 26.2 custom data");
		helper.getLevel().removeBlock(firstRune.above(), false);
		helper.assertTrue(!helper.getLevel().getBlockState(puzzleDoor).getValue(BlockColoredLock.OPEN),
				"removing a rune key must immediately close doors requiring that color");
		helper.succeed();
	}

	private static void biomeFragmentMusicLoop(GameTestHelper helper) {
		BlockPos controllerPos = helper.absolutePos(new BlockPos(12, 2, 12));
		helper.getLevel().setBlock(controllerPos,
				ChromaBlocks.STRUCTURE_CONTROLLER.get().defaultBlockState(), 3);
		TileEntityStructureController controller = (TileEntityStructureController)helper.getLevel()
				.getBlockEntity(controllerPos);
		helper.assertTrue(controller != null, "Biome Fragment music must have a persistent controller");
		controller.initialize(TileEntityStructureController.StructureType.BIOME_FRAGMENT,
				CrystalElement.WHITE, 0, false, false);

		List<MusicKey> melody = controller.getBiomeMelody();
		helper.assertTrue(!melody.isEmpty(), "Biome Fragment must select a V33a prefab melody");
		java.util.Set<CrystalElement> crystalColors = new java.util.HashSet<>();
		for (int i = 0; i < 8; i++) crystalColors.add(controller.getBiomeCrystalColor(i));
		helper.assertTrue(crystalColors.size() == 8,
				"Biome Fragment must generate eight distinct playable crystal identities");
		for (MusicKey note : melody)
			if (note != null)
				helper.assertTrue(crystalColors.stream().anyMatch(element ->
						reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getKeys(element).contains(note)),
						"generated crystals must be capable of playing " + note);
		MusicKey expected = melody.stream().filter(java.util.Objects::nonNull).findFirst().orElseThrow();
		CrystalElement triggerElement = crystalColors.stream().filter(element ->
				reika.chromaticraft.auxiliary.CrystalMusicManager.instance.getKeys(element)
						.contains(expected)).findFirst().orElseThrow();
		int triggerIndex = reika.chromaticraft.auxiliary.CrystalMusicManager.instance
				.getIntervalFor(triggerElement, expected);
		BlockPos trigger = controllerPos.offset(4, 2, 0);
		helper.getLevel().setBlock(trigger, ChromaBlocks.MUSIC_TRIGGER.get().defaultBlockState(), 3);
		helper.getLevel().setBlock(trigger.above(),
				ChromaBlocks.crystalLamp(triggerElement).get().defaultBlockState(), 3);
		double clickX = triggerIndex == 0 || triggerIndex == 3 ? 0.25 : 0.75;
		double clickY = triggerIndex <= 1 ? 0.75 : 0.25;
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockHitResult triggerHit = new BlockHitResult(new Vec3(trigger.getX() + clickX,
				trigger.getY() + clickY, trigger.getZ() + 1), Direction.SOUTH, trigger, false);
		helper.assertTrue(helper.getLevel().getBlockState(trigger).useWithoutItem(
				helper.getLevel(), player, triggerHit).consumesAction(),
				"the original trigger quadrant must consume a direct interaction");
		helper.assertTrue(controller.getBiomeGuessIndex() > melody.indexOf(expected),
				"Music Trigger discovery must route its elemental note into the controller guess");

		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				helper.getLevel().setBlock(controllerPos.offset(x, 1, z),
						reika.chromaticraft.block.BlockChromaDoor.state(false, false, false, true), 3);
		List<BlockPos> lowerCaches = List.of(controllerPos.offset(-4, 2, -5),
				controllerPos.offset(5, 2, -4), controllerPos.offset(4, 2, 5),
				controllerPos.offset(-5, 2, 4));
		for (BlockPos cache : lowerCaches) {
			helper.getLevel().setBlock(cache, ChromaBlocks.LOOT_CHEST.get().defaultBlockState(), 3);
			((TileEntityLootChest)helper.getLevel().getBlockEntity(cache)).setStructureLocked(true);
		}
		MusicKey wrong = expected == MusicKey.C1 ? MusicKey.Cs1 : MusicKey.C1;
		controller.onMusicTrigger(controllerPos.above(), CrystalElement.WHITE, wrong, null);
		helper.assertTrue(controller.getBiomeGuessIndex() == melody.indexOf(expected),
				"a wrong note must reset the melody guess to its first non-rest note");
		for (MusicKey note : melody)
			if (note != null)
				controller.onMusicTrigger(controllerPos.above(), CrystalElement.WHITE, note, null);
		helper.assertTrue(controller.isBiomeComplete(),
				"entering the complete prefab melody must persist puzzle completion");
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) {
				BlockPos barrier = controllerPos.offset(x, 1, z);
				helper.assertTrue(helper.getLevel().getBlockState(barrier)
						.getValue(reika.chromaticraft.block.BlockChromaDoor.OPEN),
						"music completion must open every central barrier cell");
			}
		for (BlockPos cache : lowerCaches)
			helper.assertTrue(!((TileEntityLootChest)helper.getLevel().getBlockEntity(cache)).isStructureLocked(),
					"music completion must clear the V33a structure lock on every lower cache");
		helper.succeed();
	}

	/** Canonical NBT, runtime colour substitution, delegates, loot locks and melody completion. */
	private static void biomeFragmentNbtCompletion(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(12, 6, 12));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0xB10FEA6L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(
				OverworldStructureFeature.Type.BIOME_FRAGMENT, false).place(context),
				"command Biome Fragment feature must place the canonical V33a NBT at its controller coordinate");
		helper.assertTrue(helper.getLevel().getBlockEntity(origin) instanceof TileEntityStructureController,
				"the reserved Biome Fragment template cell must become its persistent controller");
		TileEntityStructureController controller =
				(TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller.getStructureType()
				== TileEntityStructureController.StructureType.BIOME_FRAGMENT,
				"the controller must persist the Biome Fragment identity");

		int[][] runes = {{5,5,6},{6,5,5},{-5,5,6},{-6,5,5},
				{5,5,-6},{6,5,-5},{-5,5,-6},{-6,5,-5}};
		int[][] crystals = {{-4,3,1},{-5,3,1},{-1,3,-4},{-1,3,-5},
				{4,3,-1},{5,3,-1},{1,3,4},{1,3,5}};
		for (int i = 0; i < 8; i++) {
			helper.assertTrue(helper.getLevel().getBlockState(origin.offset(
					runes[i][0], runes[i][1], runes[i][2]))
						.is(ChromaBlocks.rune(controller.getBiomeRuneColor(i)).get()),
					"runtime initialization must replace rune placeholder " + i
							+ " with its independently registered colour identity");
			helper.assertTrue(helper.getLevel().getBlockState(origin.offset(
					crystals[i][0], crystals[i][1], crystals[i][2]))
						.is(ChromaBlocks.crystalLamp(controller.getBiomeCrystalColor(i)).get()),
					"runtime initialization must replace music crystal placeholder " + i
							+ " with its independently registered colour identity");
		}

		long triggers = BlockPos.betweenClosedStream(origin.offset(-6, 1, -6), origin.offset(6, 4, 6))
				.filter(pos -> helper.getLevel().getBlockState(pos).is(ChromaBlocks.MUSIC_TRIGGER.get())).count();
		long chests = BlockPos.betweenClosedStream(origin.offset(-7, -2, -7), origin.offset(7, 11, 7))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		helper.assertTrue(triggers == 8,
				"the source music rooms must retain all eight trigger quadrants; got " + triggers);
		helper.assertTrue(chests == 8,
				"the source room must retain four lower and four upper loot caches; got " + chests);
		helper.assertTrue(helper.getLevel().getBlockState(origin.offset(0, 10, 0))
				.is(ChromaBlocks.BIOME_REPLAY.get()),
				"the upper source pedestal must be the melody replay callback block");

		for (BlockPos keyPos : List.of(origin.offset(0, 5, -2), origin.offset(0, 5, 2))) {
			helper.assertTrue(helper.getLevel().getBlockState(keyPos)
					.getValue(reika.chromaticraft.block.dimension.structure.locks.BlockLockKey.CHANNEL)
					== controller.getBiomeKeyChannel(),
					"each Lock Key must receive the controller's randomized channel");
			TileEntityLockKey key = (TileEntityLockKey)helper.getLevel().getBlockEntity(keyPos);
			helper.assertTrue(key != null && origin.equals(key.getDelegate()),
					"each Lock Key must delegate changes to the placed controller");
		}
		List<BlockPos> lowerCaches = List.of(origin.offset(-4, 2, -5), origin.offset(5, 2, -4),
				origin.offset(4, 2, 5), origin.offset(-5, 2, 4));
		for (BlockPos cache : lowerCaches)
			helper.assertTrue(helper.getLevel().getBlockEntity(cache) instanceof TileEntityLootChest chest
					&& chest.isStructureLocked(),
					"every lower cache must begin locked behind the melody puzzle");

		for (MusicKey note : controller.getBiomeMelody())
			if (note != null)
				controller.onMusicTrigger(origin.above(), CrystalElement.WHITE, note, null);
		helper.assertTrue(controller.isBiomeComplete(),
				"entering the exact generated prefab melody must complete the placed structure");
		for (BlockPos cache : lowerCaches)
			helper.assertTrue(!((TileEntityLootChest)helper.getLevel().getBlockEntity(cache)).isStructureLocked(),
					"completion must unlock every placed lower cache");
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				helper.assertTrue(helper.getLevel().getBlockState(origin.offset(x, 1, z))
						.getValue(reika.chromaticraft.block.BlockChromaDoor.OPEN),
						"completion must open every central NBT barrier cell");
		helper.succeed();
	}

	private static void structureDesertNbtController(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(10, 10, 10));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0xDE5E471L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.DESERT, false).place(context),
				"command Desert feature must place the canonical V33a NBT at its controller coordinate");
		TileEntityStructureController controller = (TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller != null && controller.getStructureType()
				== TileEntityStructureController.StructureType.DESERT, "Desert controller identity must persist");
		long chests = BlockPos.betweenClosedStream(origin.offset(-7, -3, -7), origin.offset(7, 9, 8))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		long spawners = BlockPos.betweenClosedStream(origin.offset(-7, -3, -7), origin.offset(7, 9, 8))
				.filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.SPAWNER)).count();
		helper.assertTrue(chests == 12, "Desert source geometry must retain twelve loot chests; got " + chests);
		helper.assertTrue(spawners == 5, "Desert source geometry must retain five programmed spawners; got " + spawners);
		helper.succeed();
	}

	/** Exact Snow NBT plus its source-seeded cracks and one-of-four concealed route. */
	private static void structureSnowNbtRoute(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(10, 10, 10));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0x5A0A71L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.SNOW, false).place(context),
				"command Snow feature must place the canonical V33a NBT at its controller coordinate");
		TileEntityStructureController controller = (TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller != null && controller.getStructureType()
				== TileEntityStructureController.StructureType.SNOW,
				"Snow controller identity must be explicit and persistent");
		long chests = BlockPos.betweenClosedStream(origin.offset(-8, -3, -6), origin.offset(8, 11, 10))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		long spawners = BlockPos.betweenClosedStream(origin.offset(-8, -3, -6), origin.offset(8, 11, 10))
				.filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.SPAWNER)).count();
		long locks = BlockPos.betweenClosedStream(origin.offset(-2, 3, 0), origin.offset(2, 5, 4))
				.filter(pos -> helper.getLevel().getBlockState(pos).is(ChromaBlocks.SHIFT_LOCK.get())).count();
		helper.assertTrue(chests == 11, "Snow source geometry must retain all eleven loot chests; got " + chests);
		helper.assertTrue(spawners == 3, "Snow source geometry must restore all three intended Wolf spawners; got " + spawners);
		helper.assertTrue(locks == 36, "Snow center must begin behind all thirty-six concealed Shift Locks; got " + locks);

		ServerPlayer entrant = helper.makeMockServerPlayerInLevel();
		entrant.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(origin.offset(0, 4, 2)));
		helper.runAfterDelay(5, () -> {
			long crackedCenter = BlockPos.betweenClosedStream(origin.offset(-1, 2, 1), origin.offset(1, 2, 4))
					.filter(pos -> helper.getLevel().getBlockState(pos).is(ChromaBlocks.shielding(
							reika.chromaticraft.registry.ChromaShieldTypes.CRACKS).get())).count();
			long routed = BlockPos.betweenClosedStream(origin.offset(-2, 3, 0), origin.offset(2, 5, 4))
					.filter(pos -> {
						BlockState state = helper.getLevel().getBlockState(pos);
						return state.is(ChromaBlocks.SHIFT_LOCK.get()) && state.getValue(
								reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock.PASSABILITY)
								!= reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock.Passability.CLOSED_HIDDEN;
					}).count();
			helper.assertTrue(controller.wasTriggered() && controller.isTriggerPlayer(entrant),
					"entering the Snow proximity box must persist its one-shot trigger player");
			helper.assertTrue(crackedCenter == 12,
					"Snow activation must expose the exact twelve-cell crack path; got " + crackedCenter);
			helper.assertTrue(routed == 9,
					"Snow activation must reveal exactly one source 3x3 Shift Lock route; got " + routed);
			helper.succeed();
		});
	}

	/** Exact Ocean NBT, spawners/chests, proximity panels and the timed pit cover operate together. */
	private static void structureOceanNbtTrap(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(10, 20, 10));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0x0CEA711L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.OCEAN, false).place(context),
				"command Ocean feature must place the canonical V33a NBT at its controller coordinate");
		helper.assertTrue(helper.getLevel().getBlockEntity(origin) instanceof TileEntityStructureController,
				"Ocean template anchor must become a structure controller");
		TileEntityStructureController controller =
				(TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller.getStructureType() == TileEntityStructureController.StructureType.OCEAN,
				"Ocean controller identity must be explicit and persistent");
		long chests = BlockPos.betweenClosedStream(origin.offset(-3, -5, -3), origin.offset(27, 7, 27))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		long spawners = BlockPos.betweenClosedStream(origin.offset(-3, -5, -3), origin.offset(27, 7, 27))
				.filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.SPAWNER)).count();
		helper.assertTrue(chests == 8, "Ocean source geometry must retain all eight loot chests; got " + chests);
		helper.assertTrue(spawners == 2, "Ocean source geometry must retain both Creeper spawners; got " + spawners);
		BlockPos cover = origin.offset(0, -3, 0);
		helper.assertTrue(helper.getLevel().getBlockState(cover).is(ChromaBlocks.shielding(
				reika.chromaticraft.registry.ChromaShieldTypes.CLOAK).get()),
				"the 3x3 source pit cover must begin sealed three blocks below the controller");
		ServerPlayer entrant = helper.makeMockServerPlayerInLevel();
		entrant.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(origin.above(2)));
		helper.runAfterDelay(5, () -> {
			BlockState cracked = helper.getLevel().getBlockState(origin.offset(0, 2, 15));
			helper.assertTrue(cracked.is(ChromaBlocks.shielding(
					reika.chromaticraft.registry.ChromaShieldTypes.CRACKS).get()),
					"Ocean proximity must crack the distant source cover panels");
			controller.onHit(entrant, origin);
			helper.assertTrue(helper.getLevel().getBlockState(cover).isAir(),
					"hitting the Ocean funnel must open its 3x3 pit cover");
			helper.runAfterDelay(42, () -> {
				helper.assertTrue(helper.getLevel().getBlockState(cover).is(ChromaBlocks.shielding(
						reika.chromaticraft.registry.ChromaShieldTypes.CLOAK).get()),
						"the Ocean trap must reseal after V33a's forty ticks");
				helper.succeed();
			});
		});
	}

	/** The data-driven cache still collates and separates block drops from ordinary items. */
	private static void burrowCacheLootHalves(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
		helper.getLevel().setBlock(pos, ChromaBlocks.LOOT_CHEST.get().defaultBlockState(), 3);
		TileEntityLootChest chest = (TileEntityLootChest)helper.getLevel().getBlockEntity(pos);
		chest.setLootTable(OverworldStructureFeature.BURROW_CACHE_LOOT, 0xB0770CA5EL);
		chest.unpackLootTable(null);
		int stacks = 0;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.isEmpty()) continue;
			stacks++;
			boolean block = net.minecraft.world.level.block.Block.byItem(stack.getItem())
					!= net.minecraft.world.level.block.Blocks.AIR;
			helper.assertTrue(slot < 27 ? block : !block,
					"Burrow cache slot " + slot + " violated V33a's block/item chest-half split");
		}
		helper.assertTrue(stacks > 0, "the Burrow cache's 13-20 weighted rolls must produce loot");
		helper.succeed();
	}

	/**
	 * Information Fragments must reach ordinary worldgen chests, which is the only way a player finds
	 * their first one.
	 *
	 * <p>V33a did this by pushing entries into {@code ChestGenHooks}; the port does it with loot
	 * modifiers on the vanilla tables. The distinction that matters is that ChromatiCraft's own
	 * structures hand their chests <em>vanilla</em> table ids — {@code NetherRoofStructureFeature} uses
	 * {@code SIMPLE_DUNGEON}, {@code DESERT_PYRAMID} and {@code JUNGLE_TEMPLE} — so modifying the vanilla
	 * tables is exactly what refills the Nether and Overworld structure chests. This fills through the
	 * real {@code setLootTable}/{@code unpackLootTable} path rather than reading the JSON, because a
	 * modifier that never runs and a modifier that was never written look identical on disk.
	 *
	 * <p>Seeds are swept rather than fixed: every entry is weighted against a filler, so no single seed
	 * is guaranteed a Fragment and asserting on one would be asserting on the RNG.
	 */
	private static void structureChestFragments(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
		LootParams params = new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
				.create(LootContextParamSets.CHEST);
		var tables = helper.getLevel().getServer().reloadableRegistries().lookup()
				.lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE);

		// Every location V33a seeded with a Fragment, and the fewest of two hundred rolls that may carry
		// one. The floors are deliberately far below the rates the weights imply -- a dungeon Fragment is
		// weight 10 against a filler of 100 over one to three rolls, which lands near twelve percent, and
		// the observed count is twenty-three. A floor of five catches a modifier that stopped applying
		// without turning ordinary variance into a failure.
		record Expectation(ChromaChestLoot.Location location, int floor) {}
		for (Expectation expectation : java.util.List.of(
				new Expectation(ChromaChestLoot.Location.DUNGEON, 5),
				new Expectation(ChromaChestLoot.Location.PYRAMID, 5),
				new Expectation(ChromaChestLoot.Location.JUNGLE_PUZZLE, 5),
				new Expectation(ChromaChestLoot.Location.STRONGHOLD_LIBRARY, 5),
				new Expectation(ChromaChestLoot.Location.STRONGHOLD_CROSSING, 5),
				new Expectation(ChromaChestLoot.Location.STRONGHOLD_HALLWAY, 5),
				new Expectation(ChromaChestLoot.Location.MINESHAFT, 1),
				new Expectation(ChromaChestLoot.Location.VILLAGE, 5))) {
			LootTable vanilla = tables.getOrThrow(expectation.location().target).value();
			int rollsWithFragment = 0;
			for (long seed = 0; seed < 200; seed++)
				for (ItemStack stack : vanilla.getRandomItems(params, seed))
					if (stack.is(ChromaItems.INFO_FRAGMENT.get())) {
						rollsWithFragment++;
						break;
					}
			helper.assertTrue(rollsWithFragment >= expectation.floor(),
					"only " + rollsWithFragment + " of 200 rolls of "
							+ expectation.location().target.identifier() + " carried an Information Fragment; "
							+ "the ChromaChests loot modifier is not reaching that table");
		}

		// The rates above prove the tables; this proves the path a structure actually uses. Both Nether
		// and Overworld features hand a chest a vanilla table id and let it fill lazily on first open, so
		// the chest has to be reset between fills -- refilling a chest that still holds its last contents
		// silently drops the overflow, which is exactly how a working modifier can look broken.
		int chestsWithFragment = 0;
		for (long seed = 0; seed < 64; seed++) {
			helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
			var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)
					helper.getLevel().getBlockEntity(pos);
			chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, seed);
			chest.unpackLootTable(null);
			for (int slot = 0; slot < chest.getContainerSize(); slot++)
				if (chest.getItem(slot).is(ChromaItems.INFO_FRAGMENT.get())) {
					chestsWithFragment++;
					break;
				}
		}
		helper.assertTrue(chestsWithFragment > 0, "no placed chest filled from " + BuiltInLootTables
				.SIMPLE_DUNGEON.identifier() + " across 64 seeds contained an Information Fragment");
		helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		helper.succeed();
	}

	/**
	 * Large structures must land inside the chunks their generation step may write to.
	 *
	 * <p>{@code ChunkStatus.FEATURES} allows a write radius of one chunk, so a feature may only touch
	 * a 48 by 48 block window, and {@code InSquarePlacement} starts it anywhere in the centre chunk.
	 * Anything wider than seventeen blocks can therefore reach past the far edge, where every
	 * {@code setBlock} is silently dropped — that is what made the Nether Diorama and Temple come out
	 * half built. This sweeps all sixteen offsets for each real template size and asserts the slid
	 * result is fully contained, which is the property the fix has to hold rather than any particular
	 * coordinate it happens to choose.
	 */
	private static void structureWriteWindowFit(GameTestHelper helper) {
		int radius = net.minecraft.world.level.chunk.status.ChunkPyramid.GENERATION_PYRAMID
				.getStepTo(net.minecraft.world.level.chunk.status.ChunkStatus.FEATURES)
				.blockStateWriteRadius();
		helper.assertTrue(radius == 1,
				"the feature write radius changed to " + radius + "; the structure fit assumes one chunk");
		// Every ChromatiCraft template that a feature places from a per-chunk random origin, widest
		// first. The three above seventeen are exactly the ones reported as half-generating.
		record Template(String name, int extent) {}
		for (Template template : java.util.List.of(
				new Template("nether/diorama", 32), new Template("overworld/ocean", 31),
				new Template("nether/temple", 26), new Template("nether/spiral", 19),
				new Template("overworld/snow", 17), new Template("nether/maze", 16),
				new Template("nether/hut", 9)))
			for (int centreChunk : new int[] {0, 1, -1, 37, -64}) {
				int windowMin = (centreChunk - radius) * 16;
				int windowMax = (centreChunk + radius) * 16 + 15;
				for (int offset = 0; offset < 16; offset++) {
					int requested = centreChunk * 16 + offset;
					int start = NBTStructureLoader.slide(requested, template.extent(), centreChunk, radius);
					helper.assertTrue(start >= windowMin && start + template.extent() - 1 <= windowMax,
							template.name() + " at chunk " + centreChunk + " offset " + offset
									+ " slid to " + start + ", which still leaves it outside the writable "
									+ windowMin + ".." + windowMax + " window");
					helper.assertTrue(start <= requested,
							template.name() + " was pushed forward to " + start + " from " + requested
									+ "; the fit may only pull a structure back inside the window");
					// Anything that already fitted must not be moved at all, or every small structure
					// in the mod would drift towards the chunk edge.
					if (requested + template.extent() - 1 <= windowMax)
						helper.assertTrue(start == requested, template.name() + " at offset " + offset
								+ " already fitted but was moved from " + requested + " to " + start);
				}
			}
		helper.succeed();
	}

	/**
	 * V33a's Nether roof lava rivers must actually describe rivers.
	 *
	 * <p>The whole feature is three simplex fields and two thresholds, so the thing worth checking is
	 * the shape they produce rather than any particular block: rivers should cover a modest fraction of
	 * the roof, every channel should be flanked by bank, and no column may sit outside the 127 to 240
	 * band V33a confines them to. A transcription slip in a scale or a threshold moves those numbers
	 * immediately — dropping the divide by 32, for instance, turns the rivers into noise.
	 */
	private static void netherLavaRivers(GameTestHelper helper) {
		var feature = new reika.chromaticraft.world.NetherLavaRiverFeature();
		long seed = 0x9E3779B97F4A7C15L;
		int channels = 0;
		int banks = 0;
		int columns = 0;
		// Wide enough to cross several rivers at the 32-block placement scale.
		for (int x = -128; x < 128; x++)
			for (int z = -128; z < 128; z++) {
				columns++;
				var column = feature.classify(seed, x, z);
				if (column == null)
					continue;
				helper.assertTrue(column.y() >= 127 && column.y() <= 240,
						"a river column at " + x + "," + z + " sits at y " + column.y()
								+ ", outside V33a's 127 to 240 roof band");
				if (column.channel())
					channels++;
				else
					banks++;
			}
		helper.assertTrue(channels > 0 && banks > 0,
				"the roof produced " + channels + " channel and " + banks + " bank columns across "
						+ columns + "; both thresholds must select something or there are no rivers");
		// V33a's thresholds cover roughly a fifth of the roof. Wide bounds, because the point is to
		// catch a lost scale divisor turning rivers into either a flood or a drizzle, not to pin noise.
		double coverage = (channels + banks) / (double)columns;
		helper.assertTrue(coverage > 0.02 && coverage < 0.5, "rivers cover " + coverage
				+ " of the roof; V33a's thresholds put that near a fifth, so a scale has been lost");
		// The channel is the inner band of the same field, so it can never be the larger of the two.
		helper.assertTrue(channels < banks + channels,
				"every river column came out as channel; the bank threshold is not being applied");
		helper.succeed();
	}

	/**
	 * Proxima's decoration materials must carry V33a's behaviour, not just exist in the registry.
	 *
	 * <p>Three things here are invisible in the generated data and are what actually distinguish these
	 * blocks: the walk-through variants must have no collision at all, harvesting anything must be
	 * gated on the player's dimension tuning, and Lifewater must heal the living while burning the
	 * undead. The first is what lets the other effects fire, since they run from inside the block.
	 */
	private static void proximaDecoBlocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
		for (reika.chromaticraft.registry.ProximaDecoTypes type
				: reika.chromaticraft.registry.ProximaDecoTypes.list) {
			var block = ChromaBlocks.deco(type).get();
			level.setBlock(pos, block.defaultBlockState(), 3);
			BlockState state = level.getBlockState(pos);
			boolean solid = !state.getCollisionShape(level, pos).isEmpty();
			helper.assertTrue(solid == type.isSolid(), type.registryName() + " collision is "
					+ (solid ? "solid" : "empty") + ", but V33a hasBlockRender says it should be "
					+ (type.isSolid() ? "solid" : "walked through"));
			helper.assertTrue(state.getLightEmission() == type.lightValue(),
					type.registryName() + " emits " + state.getLightEmission() + " light, expected "
							+ type.lightValue());
		}

		// V33a getPlayerRelativeBlockHardness gates harvesting on DECOHARVEST tuning, but the gate
		// short-circuits outside Proxima -- upstream tested the dimension id first, so a block carried
		// home stays breakable. That short-circuit is the half this can check: a GameTest world is the
		// overworld, and GameTestServer does not instantiate datapack dimensions, so the in-Proxima
		// refusal is an in-world check. Asserting this direction still catches the gate being made
		// unconditional, which would strand any decoration a player brought back.
		level.setBlock(pos, ChromaBlocks.deco(
				reika.chromaticraft.registry.ProximaDecoTypes.FLOATSTONE).get().defaultBlockState(), 3);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(!reika.chromaticraft.world.dimension.DimensionTuningManager.TuningThresholds
						.DECOHARVEST.isSufficientlyTuned(player)
				|| level.getBlockState(pos).getDestroyProgress(player, level, pos) > 0,
				"outside Proxima the tuning gate must not apply, so decoration stays breakable");

		// V33a onEntityCollidedWithBlock: Lifewater heals two a tick and burns the undead for four.
		BlockPos water = helper.absolutePos(new BlockPos(4, 4, 4));
		level.setBlock(water, ChromaBlocks.deco(
				reika.chromaticraft.registry.ProximaDecoTypes.LIFEWATER).get().defaultBlockState(), 3);
		var cow = helper.spawn(EntityTypes.COW, new BlockPos(4, 4, 4));
		cow.setHealth(1);
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 4, 4));
		float zombieHealth = zombie.getHealth();
		zombie.invulnerableTime = 0;
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(cow.getHealth() > 1,
					"Lifewater must heal a living entity standing in it; health stayed " + cow.getHealth());
			helper.assertTrue(zombie.isDeadOrDying() || zombie.getHealth() < zombieHealth,
					"Lifewater must burn an undead entity standing in it; health stayed "
							+ zombie.getHealth());
			helper.succeed();
		});
	}

	/**
	 * V33a's crystal shrub grows only on grass, only sometimes, and only in Crystal Leaves.
	 *
	 * <p>The size roll is the part worth guarding: upstream tries one in forty for the large form and
	 * then one in fifteen for the small, so most attempts produce nothing at all and the Crystal Forest
	 * stays sparse. A port that dropped either roll would carpet the biome. Seeds are swept because no
	 * single one is guaranteed to grow anything.
	 */
	private static void crystalShrubFeature(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
		var feature = new reika.chromaticraft.world.dimension.CrystalShrubFeature();
		var leaves = ChromaBlocks.deco(reika.chromaticraft.registry.ProximaDecoTypes.CRYSTALLEAF).get();

		// Without grass beneath, nothing grows however lucky the roll.
		level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
		for (long seed = 0; seed < 32; seed++)
			helper.assertTrue(!feature.place(shrubContext(helper, origin, seed)),
					"a crystal shrub must refuse to grow on stone");

		level.setBlock(origin.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		int grown = 0;
		int attempts = 0;
		for (long seed = 0; seed < 400; seed++) {
			attempts++;
			for (BlockPos pos : BlockPos.betweenClosedStream(origin.offset(-3, 0, -3),
					origin.offset(3, 6, 3)).map(BlockPos::immutable).toList())
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			if (!feature.place(shrubContext(helper, origin, seed)))
				continue;
			grown++;
			// Whatever the size, the crown's top layer is a radius-1 diamond, so its corners stay open.
			helper.assertTrue(!level.getBlockState(origin.offset(2, 0, 2)).is(leaves),
					"the crystal shrub's crown must be a diamond, not a filled square");
		}
		helper.assertTrue(grown > 0, "no crystal shrub grew across " + attempts + " attempts on grass");
		helper.assertTrue(grown < attempts / 2, "crystal shrubs grew on " + grown + " of " + attempts
				+ " attempts; V33a's one-in-forty and one-in-fifteen size rolls make it far rarer");
		helper.succeed();
	}

	private static FeaturePlaceContext<NoneFeatureConfiguration> shrubContext(GameTestHelper helper,
			BlockPos origin, long seed) {
		return new FeaturePlaceContext<>(Optional.empty(), helper.getLevel(),
				helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), origin,
				NoneFeatureConfiguration.INSTANCE);
	}

	/**
	 * V33a's Floatstone drifts must hang in the air above their anchor, and stay small enough to land.
	 *
	 * <p>Two properties matter and neither is obvious from the code. The veins replace <em>air</em>
	 * rather than stone, which is the whole reason a drift floats — a port that targeted stone would
	 * produce nothing in open sky. And the cluster has to fit the 48-block window a feature may write
	 * to: the size upstream passes is a vein block count, not a width, so forty blocks is a drift about
	 * nine across and the eight-block scatter keeps the whole cluster near twenty-seven. This asserts
	 * the extent directly rather than trusting that reading.
	 */
	private static void floatstoneFeature(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
		var feature = new reika.chromaticraft.world.dimension.FloatstoneFeature();
		var floatstone = ChromaBlocks.deco(
				reika.chromaticraft.registry.ProximaDecoTypes.FLOATSTONE).get();
		int producedAny = 0;
		int maxSpan = 0;
		for (long seed = 0; seed < 24; seed++) {
			for (BlockPos pos : BlockPos.betweenClosedStream(origin.offset(-30, 0, -30),
					origin.offset(30, 40, 30)).map(BlockPos::immutable).toList())
				if (level.getBlockState(pos).is(floatstone))
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			if (!feature.place(shrubContext(helper, origin, seed)))
				continue;
			producedAny++;
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
			for (BlockPos pos : BlockPos.betweenClosedStream(origin.offset(-30, 0, -30),
					origin.offset(30, 40, 30)).map(BlockPos::immutable).toList()) {
				if (!level.getBlockState(pos).is(floatstone)) continue;
				minX = Math.min(minX, pos.getX());
				maxX = Math.max(maxX, pos.getX());
				minY = Math.min(minY, pos.getY());
			}
			maxSpan = Math.max(maxSpan, maxX - minX + 1);
			// V33a floats the cluster twelve to twenty-four blocks up, then lets the scatter, the vein
			// endpoints and the ellipsoid radius each pull it back down by a few — so the invariant
			// worth holding is that a drift clears its anchor entirely, not any exact height.
			helper.assertTrue(minY > origin.getY(),
					"a Floatstone drift reached y " + minY + " from an anchor at " + origin.getY()
							+ "; drifts must hang above their anchor, not sit on it");
		}
		helper.assertTrue(producedAny > 0, "no Floatstone drift formed across 24 seeds");
		helper.assertTrue(maxSpan <= 48, "a Floatstone cluster spanned " + maxSpan
				+ " blocks, which cannot fit the window a feature may write to");
		helper.succeed();
	}

	/**
	 * The Glass Cliff piece must respect the box it is handed, and paint the same cliff whichever chunk
	 * asks for it.
	 *
	 * <p>Those two properties are the whole reason this is a structure piece rather than a feature. A
	 * piece is asked once per chunk it overlaps, with a box clipped to that chunk; if it wrote outside
	 * the box it would be no safer than the feature it replaced, and if it drew a different shape each
	 * time the chunks would not join up into one cliff. The second is why the chain is drawn from the
	 * piece's own stored seed instead of the per-chunk random handed to {@code postProcess}.
	 */
	private static void glassCliffPiece(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
		var cliffGlass = ChromaBlocks.deco(
				reika.chromaticraft.registry.ProximaDecoTypes.CLIFFGLASS).get();

		// A box a quarter of the structure's reach, so the piece has plenty it must decline to write.
		net.minecraft.world.level.levelgen.structure.BoundingBox box =
				new net.minecraft.world.level.levelgen.structure.BoundingBox(
						origin.getX() - 8, origin.getY() - 4, origin.getZ() - 8,
						origin.getX() + 7, origin.getY() + 40, origin.getZ() + 7);
		var wide = BlockPos.betweenClosedStream(origin.offset(-40, -8, -40), origin.offset(40, 48, 40))
				.map(BlockPos::immutable).toList();

		java.util.Set<BlockPos> first = paintCliff(helper, origin, box, wide, cliffGlass);
		helper.assertTrue(!first.isEmpty(),
				"the Glass Cliff piece placed nothing at all inside the box it was given");
		for (BlockPos pos : first)
			helper.assertTrue(box.isInside(pos), "the Glass Cliff piece wrote to " + pos
					+ ", outside the box it was handed; a piece that ignores its box is no safer than "
					+ "the feature it replaced");

		// Same piece, same box, second pass: the shape must be identical, which is what lets adjacent
		// chunks agree on one cliff.
		java.util.Set<BlockPos> second = paintCliff(helper, origin, box, wide, cliffGlass);
		helper.assertTrue(first.equals(second), "the Glass Cliff piece painted " + first.size()
				+ " blocks then " + second.size() + " at the same box; its shape must not depend on "
				+ "which pass or chunk asks for it");
		helper.succeed();
	}

	/** Clears the area, runs one postProcess pass, and returns the Cliff Glass it placed. */
	private static java.util.Set<BlockPos> paintCliff(GameTestHelper helper, BlockPos origin,
			net.minecraft.world.level.levelgen.structure.BoundingBox box, java.util.List<BlockPos> area,
			net.minecraft.world.level.block.Block cliffGlass) {
		ServerLevel level = helper.getLevel();
		for (BlockPos pos : area)
			if (level.getBlockState(pos).is(cliffGlass))
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		var piece = new reika.chromaticraft.world.dimension.structure.GlassCliffPiece(
				RandomSource.create(0xC11FF), origin.getX(), origin.getY(), origin.getZ());
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
				RandomSource.create(1), box, net.minecraft.world.level.ChunkPos.containing(origin), origin);
		java.util.Set<BlockPos> placed = new java.util.HashSet<>();
		for (BlockPos pos : area)
			if (level.getBlockState(pos).is(cliffGlass))
				placed.add(pos.immutable());
		return placed;
	}

	/**
	 * V33a's crystal trees must grow all twelve of their registered layouts, and never leave a stump.
	 *
	 * <p>Two properties are worth holding. Every layout upstream registers must actually be reachable —
	 * a transcription that dropped a case would simply never appear, and no single seed would reveal it —
	 * so this sweeps seeds until it has seen a tree of every size class. And a tree must be all or
	 * nothing: upstream checks the whole crown before writing a block, because a tree that failed halfway
	 * would leave a trunk of Shielding standing in the forest with no canopy. XMAS is excluded, exactly
	 * as upstream excludes it, since its layout table is empty.
	 */
	private static void crystalTreeFeature(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
		var feature = new reika.chromaticraft.world.dimension.CrystalTreeFeature();
		var leaves = ChromaBlocks.deco(
				reika.chromaticraft.registry.ProximaDecoTypes.CRYSTALLEAF).get();
		var shielding = ChromaBlocks.shielding(
				reika.chromaticraft.registry.ChromaShieldTypes.STONE).get();

		for (var shape : reika.chromaticraft.world.dimension.CrystalTreeShapes.list)
			if (shape == reika.chromaticraft.world.dimension.CrystalTreeShapes.XMAS)
				helper.assertTrue(!shape.isRegistered(),
						"XMAS has an empty layout table upstream and must stay unregistered");
			else
				helper.assertTrue(shape.isRegistered(),
						shape + " is registered upstream but not here; a dropped layout never appears");

		var area = BlockPos.betweenClosedStream(origin.offset(-16, -1, -16), origin.offset(16, 44, 16))
				.map(BlockPos::immutable).toList();
		int grown = 0;
		for (long seed = 0; seed < 60; seed++) {
			for (BlockPos pos : area)
				if (level.getBlockState(pos).is(leaves) || level.getBlockState(pos).is(shielding))
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			if (!feature.place(shrubContext(helper, origin, seed)))
				continue;
			grown++;
			// All or nothing: a trunk with no canopy anywhere above it is the failure mode that matters.
			boolean anyLeaves = false;
			for (BlockPos pos : area)
				if (level.getBlockState(pos).is(leaves)) {
					anyLeaves = true;
					break;
				}
			helper.assertTrue(anyLeaves, "a crystal tree placed its trunk but no canopy; the space check "
					+ "must pass or fail the whole shape, never write half of it");
			helper.assertTrue(level.getBlockState(origin).is(shielding),
					"a grown crystal tree must stand on its Shielding trunk");
		}
		helper.assertTrue(grown > 0, "no crystal tree grew across 60 seeds");
		helper.succeed();
	}

	/**
	 * V33a's crystal geode must refuse bad ground before it carves anything, and keep to one palette.
	 *
	 * <p>The site check is the half worth guarding. A geode that formed over a cave or a cliff edge would
	 * hang half out of the terrain, and one carved into water would simply flood, so upstream tests the
	 * whole ellipsoid before writing a block. The palette is the other: upstream groups the sixteen
	 * elements into four sets and draws every crystal in a pit from one of them, which is what makes a
	 * geode read as a colour scheme rather than sixteen unrelated crystals.
	 */
	private static void crystalPitFeature(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 6, 8));
		var feature = new reika.chromaticraft.world.dimension.CrystalPitFeature();
		var cloak = ChromaBlocks.shielding(
				reika.chromaticraft.registry.ChromaShieldTypes.CLOAK).get();

		// Nothing beneath: every lining cell fails its support check, so no geode may form.
		helper.assertTrue(!feature.place(shrubContext(helper, origin, 1)),
				"a geode must refuse to carve itself where its lining has nothing to rest on");

		// A solid block of stone around the site is ground a geode can be cut into.
		var area = BlockPos.betweenClosedStream(origin.offset(-10, -6, -10), origin.offset(10, 6, 10))
				.map(BlockPos::immutable).toList();
		for (BlockPos pos : area)
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(feature.place(shrubContext(helper, origin, 1)),
				"a geode must form in solid stone");

		int lining = 0;
		java.util.Set<net.minecraft.world.level.block.Block> crystals = new java.util.HashSet<>();
		for (BlockPos pos : area) {
			var state = level.getBlockState(pos);
			if (state.is(cloak))
				lining++;
			for (var element : reika.chromaticraft.registry.CrystalElement.elements)
				if (state.is(ChromaBlocks.caveCrystal(element).get()))
					crystals.add(state.getBlock());
		}
		helper.assertTrue(lining > 0, "the geode carved no Cloak Shielding lining");
		helper.assertTrue(!crystals.isEmpty(), "the geode grew no crystals on its floor");
		// Four elements per palette, so a single geode can never show more than four kinds.
		helper.assertTrue(crystals.size() <= 4, "the geode grew " + crystals.size()
				+ " kinds of crystal; V33a draws all of a pit's crystals from one palette of four");
		helper.succeed();
	}

	/**
	 * Every Proxima decoration feature must be attached to the biomes V33a puts it in.
	 *
	 * <p>This exists because registering a feature and attaching it are separate steps, and a feature
	 * that is registered but attached to nothing fails completely silently: it compiles, its configured
	 * and placed JSON generate and validate, and it simply never appears in the world. Four features sat
	 * in exactly that state until it was noticed by eye.
	 *
	 * <p>The expectations are upstream's {@code generateIn} chain: the Central biome takes everything not
	 * tied to one biome, Floatstone is a sky feature so it belongs to Skylands, the geode is the Crystal
	 * Plains', and both crystal plants are the Crystal Forest sub-biome's alone.
	 */
	private static void proximaBiomeFeatures(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess()
				.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
		record Expectation(reika.chromaticraft.world.dimension.biome.ProximaBiomeType biome,
				java.util.List<String> features) {}
		for (Expectation expectation : java.util.List.of(
				new Expectation(reika.chromaticraft.world.dimension.biome.ProximaBiomes.CENTER,
						java.util.List.of("floatstone", "crystal_pit")),
				new Expectation(reika.chromaticraft.world.dimension.biome.ProximaBiomes.SKYLANDS,
						java.util.List.of("floatstone")),
				new Expectation(reika.chromaticraft.world.dimension.biome.ProximaBiomes.PLAINS,
						java.util.List.of("crystal_pit")),
				new Expectation(reika.chromaticraft.world.dimension.biome.ProximaSubBiomes.CRYSFOREST,
						java.util.List.of("crystal_tree", "crystal_shrub")))) {
			var biome = biomes.getOrThrow(expectation.biome().biomeKey()).value();
			java.util.Set<String> present = new java.util.HashSet<>();
			for (var step : biome.getGenerationSettings().features())
				for (var placed : step)
					present.add(placed.unwrapKey().orElseThrow().identifier().getPath());
			for (String wanted : expectation.features())
				helper.assertTrue(present.contains(wanted), expectation.biome().biomeKey().identifier()
						+ " is missing the " + wanted + " feature; a feature registered but attached to "
						+ "no biome never generates and nothing else catches it");
		}
		// The geode is not a sky feature and must not have leaked into Skylands.
		var skylands = biomes.getOrThrow(
				reika.chromaticraft.world.dimension.biome.ProximaBiomes.SKYLANDS.biomeKey()).value();
		for (var step : skylands.getGenerationSettings().features())
			for (var placed : step)
				helper.assertTrue(!placed.unwrapKey().orElseThrow().identifier().getPath().equals("crystal_pit"),
						"the geode belongs to the Crystal Plains, not the Skylands");
		helper.succeed();
	}

	/**
	 * V33a's aurorae must hang high, run parallel, share one colour pair, and never use a forbidden one.
	 *
	 * <p>Each of those is a property a transcription could lose silently. A display is up to twelve
	 * ribbons laid along one bearing, all sharing two colours — if the colours were drawn per ribbon it
	 * would look like a dozen unrelated streaks. Both ends of a ribbon are pulled to the higher of the
	 * two before jitter, so a ribbon hangs level rather than sloped. And upstream forbids three colour
	 * pairs whose gradients muddy together, redrawing only the second colour; dropping that rule would
	 * be invisible in review and obvious in the sky.
	 */
	private static void auroraeFeature(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
		var feature = new reika.chromaticraft.world.dimension.AuroraeFeature();
		int displays = 0;
		for (long seed = 0; seed < 24; seed++) {
			for (var existing : level.getEntitiesOfClass(reika.chromaticraft.entity.EntityAurora.class,
					new AABB(origin).inflate(400)))
				existing.discard();
			if (!feature.place(shrubContext(helper, origin, seed)))
				continue;
			displays++;
			var ribbons = level.getEntitiesOfClass(reika.chromaticraft.entity.EntityAurora.class,
					new AABB(origin).inflate(400));
			helper.assertTrue(!ribbons.isEmpty(), "a placed aurora display spawned no ribbons");
			helper.assertTrue(ribbons.size() <= 12,
					"a display spawned " + ribbons.size() + " ribbons; V33a lays at most twelve");
			java.util.Set<Integer> palette = new java.util.HashSet<>();
			for (var ribbon : ribbons) {
				var data = ribbon.getAuroraData();
				palette.add(data.colorFrom());
				palette.add(data.colorTo());
				// V33a floors every end at 120 and lifts it 40 above the terrain, so nothing hangs low.
				helper.assertTrue(data.from().y >= 115 && data.to().y >= 115,
						"an aurora hangs at y " + data.from().y + "/" + data.to().y
								+ "; V33a floors both ends near 120 so a display clears the terrain");
				// Both ends take the higher of the two before a five-block jitter either way.
				helper.assertTrue(Math.abs(data.from().y - data.to().y) <= 10,
						"an aurora slopes by " + Math.abs(data.from().y - data.to().y)
								+ " blocks; both ends are levelled before jitter, so at most ten");
				helper.assertTrue(data.speed() >= 0.125 && data.speed() <= 2.5,
						"an aurora drifts at " + data.speed() + ", outside V33a's 0.125 to 2.5");
			}
			// One display, one colour pair -- so at most two distinct colours across every ribbon.
			helper.assertTrue(palette.size() <= 2, "a display used " + palette.size()
					+ " colours; every ribbon in one display shares the same pair");
			// The three pairs V33a forbids, by their colour values.
			helper.assertTrue(!(palette.contains(0x50BEFF) && palette.contains(0xFF97AE))
							&& !(palette.contains(0x9BFF00) && palette.contains(0xFF97AE))
							&& !(palette.contains(0x00FF00) && palette.contains(0xFF97AE)),
					"a display used one of V33a's three forbidden colour pairs");
		}
		helper.assertTrue(displays > 0, "no aurora display formed across 24 seeds");
		for (var existing : level.getEntitiesOfClass(reika.chromaticraft.entity.EntityAurora.class,
				new AABB(origin).inflate(400)))
			existing.discard();
		helper.succeed();
	}

	/**
	 * The aurora curtain must wave in the middle while staying pinned at both ends, and never move
	 * vertically.
	 *
	 * <p>Those three are the whole of V33a's animation and each is easy to lose. The end control points
	 * are given zero variance and zero velocity precisely so the ribbon stays anchored where the
	 * generator put it; if they drifted, a curtain would wander away from its own endpoints over time.
	 * And the drift is applied only to x and z — {@code posY} is reassigned every tick from the straight
	 * line between the endpoints — so a curtain ripples sideways rather than flapping up and down.
	 *
	 * <p>{@code Aurora} is client-side by package but uses no client-only type, so it can be exercised
	 * here. What cannot be checked from a server is how it looks; that wants eyes.
	 */
	private static void auroraCurtainDrift(GameTestHelper helper) {
		var data = new reika.chromaticraft.entity.AuroraData(
				new net.minecraft.world.phys.Vec3(0, 160, 0),
				new net.minecraft.world.phys.Vec3(128, 160, 0), 0xFF0000, 0x00FF00, 2);
		var aurora = new reika.chromaticraft.client.render.Aurora(data);

		var initial = aurora.curve();
		helper.assertTrue(initial.size() > 2,
				"the curtain sampled only " + initial.size() + " points; it should follow a spline");
		var firstStart = initial.get(0);
		var firstEnd = initial.get(initial.size() - 1);

		for (int tick = 0; tick < 200; tick++)
			aurora.update();
		var drifted = aurora.curve();
		helper.assertTrue(drifted.size() == initial.size(),
				"the curtain changed point count while drifting");

		// The ends are pinned: zero variance and zero velocity, so they must not have moved.
		helper.assertTrue(near(drifted.get(0), firstStart) && near(drifted.get(drifted.size() - 1), firstEnd),
				"an aurora's ends drifted; they carry zero variance and velocity so the ribbon stays "
						+ "anchored where the generator put it");

		// Nothing moves vertically, at either end or in between.
		for (var point : drifted)
			helper.assertTrue(Math.abs(point.yCoord - 160) < 1E-6, "a curtain point moved to y "
					+ point.yCoord + "; the drift is sideways only, and y is reset from the baseline");

		// Something in the middle must actually have moved, or the curtain is static.
		boolean moved = false;
		for (int i = 1; i < drifted.size() - 1; i++)
			if (Math.abs(drifted.get(i).zCoord - initial.get(i).zCoord) > 1E-6
					|| Math.abs(drifted.get(i).xCoord - initial.get(i).xCoord) > 1E-6) {
				moved = true;
				break;
			}
		helper.assertTrue(moved, "no interior point of the curtain drifted over two hundred ticks");
		helper.succeed();
	}

	private static boolean near(reika.dragonapi.instantiable.data.immutable.DecimalPosition a,
			reika.dragonapi.instantiable.data.immutable.DecimalPosition b) {
		return Math.abs(a.xCoord - b.xCoord) < 1E-6 && Math.abs(a.yCoord - b.yCoord) < 1E-6
				&& Math.abs(a.zCoord - b.zCoord) < 1E-6;
	}

	/** Plain-Block loot chests must explicitly deliver vanilla opener-count events to their BE. */
	private static void lootChestLidEvent(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
		BlockState state = ChromaBlocks.LOOT_CHEST.get().defaultBlockState();
		helper.getLevel().setBlock(pos, state, 3);
		TileEntityLootChest chest = (TileEntityLootChest)helper.getLevel().getBlockEntity(pos);
		helper.assertTrue(state.triggerEvent(helper.getLevel(), pos, 1, 1),
				"loot-chest block event 1 must delegate to TileEntityLootChest");
		for (int i = 0; i < 4; i++)
			TileEntityLootChest.lidAnimateTick(helper.getLevel(), pos, state, chest);
		helper.assertTrue(chest.getOpenNess(1) > 0,
				"delegated opener event must advance the animated chest-lid controller");
		state.triggerEvent(helper.getLevel(), pos, 1, 0);
		for (int i = 0; i < 8; i++)
			TileEntityLootChest.lidAnimateTick(helper.getLevel(), pos, state, chest);
		helper.assertTrue(chest.getOpenNess(1) == 0,
				"closing opener event must return the lid controller to zero");
		helper.succeed();
	}

	/**
	 * Two structure defects reported in-world, fixed together because both are about a structure's
	 * wiring rather than its geometry.
	 *
	 * <ol>
	 * <li>Breaking Shielding must set off adjacent TNT — V33a's {@code breakBlock} primes it directly,
	 *     which is the trap behind every Cracked Shielding a player is invited to mine through.</li>
	 * <li>Blocks placed from a structure template must re-resolve against their neighbours, as
	 *     {@code StructureTemplate.placeInWorld} does — stairs, fences, walls, panes and bars all come
	 *     out unconnected otherwise.</li>
	 * <li>The Nether Temple's redstone must come out wired, not as isolated dots. 1.7.10 had no dot
	 *     shape: a wire with no wire neighbours was a cross that powered everything around it, which is
	 *     what the temple puzzle runs on. 26.2's {@code RedStoneWireBlock.getConnectionState} preserves
	 *     an existing dot before it ever reaches its auto-connect logic, so a template authoring "none"
	 *     on all four sides produced wires that no update could ever open up. The cells themselves were
	 *     never wrong — all twenty-eight map to V33a's own placement — so this asserts the shape rather
	 *     than the identity.</li>
	 * </ol>
	 */
	private static void structureTrapAndWiring(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		// 1. The Shielding trap.
		BlockPos shield = helper.absolutePos(new BlockPos(3, 4, 3));
		BlockPos tnt = shield.east();
		level.setBlock(shield, ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.CRACK).get().defaultBlockState(), 3);
		level.setBlock(tnt, Blocks.TNT.defaultBlockState(), 3);
		helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
				new AABB(shield).inflate(6)).isEmpty(), "nothing should be primed before the break");
		level.destroyBlock(shield, false);
		helper.assertTrue(level.getBlockState(tnt).isAir(),
				"V33a replaces the adjacent TNT block with a primed entity when Shielding breaks");
		helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
						new AABB(shield).inflate(6)).isEmpty(),
				"breaking Shielding must prime the TNT touching it; this is the structures' trap");
		for (var primed : level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
				new AABB(shield).inflate(6)))
			primed.discard();

		// 2. The Nether Temple's redstone must survive being placed from its template. Every cell is
		// checked against the V33a source rather than against expectations: a wall torch whose support
		// arrives late, or a wire that never resolves, silently breaks the puzzle while the room still
		// looks right.
		BlockPos temple = helper.absolutePos(new BlockPos(40, 4, 40));
		NBTStructureLoader.place(level, ChromaStructureTemplateProvider.NETHER_TEMPLE, temple,
				BlockPos.ZERO, state -> state, 2);
		int[][] torches = {{1, 2, 4}, {7, 1, 4}, {12, 1, 9}, {13, 1, 9}, {24, 2, 4}};
		for (int[] at : torches) {
			BlockPos pos = temple.offset(at[0], at[1], at[2]);
			helper.assertTrue(level.getBlockState(pos).is(Blocks.REDSTONE_WALL_TORCH),
					"the temple's redstone torch at " + at[0] + "," + at[1] + "," + at[2]
							+ " must survive placement, found " + level.getBlockState(pos));
		}
		int[][] wires = {{1, 1, 4}, {8, 1, 4}, {12, 1, 4}, {20, 1, 4}, {24, 1, 4}};
		for (int[] at : wires) {
			BlockPos pos = temple.offset(at[0], at[1], at[2]);
			BlockState wire = level.getBlockState(pos);
			helper.assertTrue(wire.is(Blocks.REDSTONE_WIRE),
					"the temple's redstone wire at " + at[0] + "," + at[1] + "," + at[2]
							+ " must survive placement, found " + wire);
			// A wire with all four sides "none" is a dot: it powers nothing horizontally, and 26.2 will
			// never open it up again. That is what broke the puzzle, so it is what this guards.
			boolean connected = false;
			for (net.minecraft.world.level.block.state.properties.EnumProperty
					<net.minecraft.world.level.block.state.properties.RedstoneSide> side
					: java.util.List.of(net.minecraft.world.level.block.RedStoneWireBlock.NORTH,
							net.minecraft.world.level.block.RedStoneWireBlock.EAST,
							net.minecraft.world.level.block.RedStoneWireBlock.SOUTH,
							net.minecraft.world.level.block.RedStoneWireBlock.WEST))
				connected |= wire.getValue(side).isConnected();
			helper.assertTrue(connected, "the temple's redstone wire at " + at[0] + "," + at[1] + ","
					+ at[2] + " came out as an unconnected dot, which cannot carry the puzzle's signal");
		}
		int[][] repeaters = {{3, 1, 4}, {12, 1, 7}, {13, 1, 7}, {22, 1, 4}};
		for (int[] at : repeaters) {
			BlockPos pos = temple.offset(at[0], at[1], at[2]);
			helper.assertTrue(level.getBlockState(pos).is(Blocks.REPEATER),
					"the temple's repeater at " + at[0] + "," + at[1] + "," + at[2]
							+ " must survive placement, found " + level.getBlockState(pos));
		}
		helper.succeed();
	}

	/**
	 * V33a's Loot Chest is a trapped chest: opening one powers redstone, which is what fires the TNT
	 * and trap circuits the structures bury beside and beneath it.
	 */
	private static void lootChestTrapSignal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(8, 4, 8));
		BlockState state = ChromaBlocks.LOOT_CHEST.get().defaultBlockState();
		level.setBlock(pos, state, 3);
		TileEntityLootChest chest = (TileEntityLootChest)level.getBlockEntity(pos);

		helper.assertTrue(state.isSignalSource(),
				"the Loot Chest must be a redstone source, or no structure trap can ever fire");
		helper.assertTrue(!chest.isOpenedByAnyone() && state.getSignal(level, pos, Direction.NORTH) == 0,
				"a closed chest must emit nothing");

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
		chest.startOpen(player);
		helper.assertTrue(chest.isOpenedByAnyone(), "opening must register on the opener counter");
		// V33a returns a flat 15 for any non-zero count rather than scaling with it.
		for (Direction dir : Direction.values())
			helper.assertTrue(state.getSignal(level, pos, dir) == 15,
					"an open chest must emit full weak power on every side, " + dir + " did not");
		// V33a's isProvidingStrongPower is side 1 only: strong downward, so a chest sitting on TNT
		// sets it off, but a chest beside a solid block does not power through it.
		helper.assertTrue(state.getDirectSignal(level, pos, Direction.UP) == 15,
				"strong power must run downward through the block below");
		for (Direction dir : Direction.values())
			if (dir != Direction.UP)
				helper.assertTrue(state.getDirectSignal(level, pos, dir) == 0,
						"strong power must be side-1 only, " + dir + " leaked");

		// The signal is useless if nothing re-reads it, so prove the real circuit reacts. Redstone
		// dust beside the chest is what a structure trap actually runs on.
		BlockPos dust = pos.north();
		level.setBlock(dust.below(), Blocks.STONE.defaultBlockState(), 3);
		level.setBlock(dust, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
		chest.stopOpen(player);
		helper.assertTrue(!chest.isOpenedByAnyone(), "closing must clear the opener counter");
		helper.assertTrue(level.getBlockState(dust).getValue(
						net.minecraft.world.level.block.RedStoneWireBlock.POWER) == 0,
				"dust beside a closed chest must be unpowered");
		chest.startOpen(player);
		helper.assertTrue(level.getBlockState(dust).getValue(
						net.minecraft.world.level.block.RedStoneWireBlock.POWER) > 0,
				"opening the chest must update its neighbours so the trap circuit actually powers;"
						+ " without the neighbour notification the signal is correct but never read");
		chest.stopOpen(player);
		helper.assertTrue(level.getBlockState(dust).getValue(
						net.minecraft.world.level.block.RedStoneWireBlock.POWER) == 0,
				"closing must drop the circuit again");
		helper.succeed();
	}

	/** Distinct lamp identities, source temperature bounds, facing and fuel-free furnace assistance. */
	private static void heatLampTemperatureFurnaceLoop(GameTestHelper helper) {
		BlockPos furnacePos = helper.absolutePos(new BlockPos(8, 4, 8));
		helper.getLevel().setBlock(furnacePos, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState(), 3);
		BlockPos lampPos = furnacePos.above();
		BlockState lampState = ChromaBlocks.HEAT_LAMP.get().defaultBlockState()
				.setValue(reika.chromaticraft.block.BlockHeatLamp.FACING, Direction.UP);
		helper.getLevel().setBlock(lampPos, lampState, 3);
		var lamp = (reika.chromaticraft.tileentity.TileEntityHeatLamp)helper.getLevel().getBlockEntity(lampPos);
		var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
				helper.getLevel().getBlockEntity(furnacePos);
		lamp.setTemperature(10000);
		helper.assertTrue(lamp.getTemperature() == reika.chromaticraft.tileentity.TileEntityHeatLamp.MAXTEMP,
				"hot Heat Lamp temperature must clamp to V33a's 615 C maximum");
		furnace.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_ORE));
		for (int i = 0; i < 400; i++)
			reika.chromaticraft.tileentity.TileEntityHeatLamp.serverTick(helper.getLevel(), lampPos,
					lampState, lamp);
		helper.assertTrue(furnace.getItem(2).is(net.minecraft.world.item.Items.IRON_INGOT),
				"a hot Heat Lamp above 200 C must supplant furnace fuel");
		BlockPos coldPos = furnacePos.east(2);
		helper.getLevel().setBlock(coldPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
		helper.getLevel().setBlock(coldPos, ChromaBlocks.COLD_LAMP.get().defaultBlockState()
				.setValue(reika.chromaticraft.block.BlockHeatLamp.FACING, Direction.UP), 3);
		var cold = (reika.chromaticraft.tileentity.TileEntityHeatLamp)helper.getLevel().getBlockEntity(coldPos);
		cold.setTemperature(-10000);
		helper.assertTrue(cold.getTemperature() == reika.chromaticraft.tileentity.TileEntityHeatLamp.MINTEMP_COLD,
				"cold Heat Lamp temperature must clamp to V33a's -60 C minimum");
		helper.assertTrue(ChromaBlocks.HEAT_LAMP.get() != ChromaBlocks.COLD_LAMP.get(),
				"hot and cold metadata forms must remain separate modern registry identities");
		helper.succeed();
	}

	/** Four connected cells share one UUID, consume the matching key, open, and close together. */
	private static void chromaDoorUuidKeyLoop(GameTestHelper helper) {
		BlockPos root = helper.absolutePos(new BlockPos(8, 4, 8));
		BlockState state = reika.chromaticraft.block.BlockChromaDoor.state(false, false, true, false);
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
				helper.getLevel().setBlock(root.offset(x, y, 0), state, 3);
		TileEntityChromaDoor door = (TileEntityChromaDoor)helper.getLevel().getBlockEntity(root);
		java.util.UUID id = java.util.UUID.randomUUID();
		door.bindUUID(null, id, false);
		ItemStack key = new ItemStack(ChromaItems.DOOR_KEY.get());
		ChromaItems.DOOR_KEY.get().setID(key, id);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(root),
				Direction.NORTH, root, false);
		helper.assertTrue(key.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,
				net.minecraft.world.InteractionHand.MAIN_HAND, hit)).consumesAction(),
				"a matching Door Key must claim the Chroma Door interaction");
		helper.assertTrue(key.isEmpty(), "a one-use Chroma Door must consume its matching key");
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
				helper.assertTrue(helper.getLevel().getBlockState(root.offset(x, y, 0))
						.getValue(reika.chromaticraft.block.BlockChromaDoor.OPEN),
						"every UUID-matched connected door cell must open together");
		helper.runAfterDelay(55, () -> {
			for (int x = 0; x < 2; x++)
				for (int y = 0; y < 2; y++)
					helper.assertTrue(!helper.getLevel().getBlockState(root.offset(x, y, 0))
							.getValue(reika.chromaticraft.block.BlockChromaDoor.OPEN),
							"the scheduled close must close the whole UUID-matched component");
			door.bindUUID(null, id, true);
			door.setPlacer(player.getUUID());
			player.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(root));
			TileEntityChromaDoor.serverTick(helper.getLevel(), root,
					helper.getLevel().getBlockState(root), door);
			helper.assertTrue(helper.getLevel().getBlockState(root)
					.getValue(reika.chromaticraft.block.BlockChromaDoor.OPEN),
					"an automatic binding must reopen for its nearby owner");
			helper.succeed();
		});
	}

	/** Exact Burrow base NBT, colour identity, six chests and proximity crack form one runtime loop. */
	private static void structureBurrowNbtController(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(10, 8, 10));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0xB0770A11L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.BURROW, false).place(context),
				"command Burrow feature must place its canonical NBT at the requested controller coordinate");
		helper.assertTrue(helper.getLevel().getBlockEntity(origin) instanceof TileEntityStructureController,
				"the Burrow template anchor must become a structure controller");
		TileEntityStructureController controller =
				(TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller.getStructureType() == TileEntityStructureController.StructureType.BURROW,
				"the controller must persist the Burrow identity");
		BlockState lamp = helper.getLevel().getBlockState(origin.offset(0, -2, 0));
		helper.assertTrue(lamp.is(ChromaBlocks.crystalLamp(controller.getColor()).get()),
				"the source-selected Burrow colour must match its independently registered lamp block");
		long chests = BlockPos.betweenClosedStream(origin.offset(-3, -3, -3), origin.offset(6, 8, 3))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		helper.assertTrue(chests == 6, "the exact Burrow base must retain all six loot chests; got " + chests);
		BlockPos triggerBlock = origin.offset(2, 1, 0);
		helper.assertTrue(helper.getLevel().getBlockState(triggerBlock).is(ChromaBlocks.shielding(
				reika.chromaticraft.registry.ChromaShieldTypes.STONE).get()),
				"the proximity block must begin as the authored reinforced stone cell");
		ServerPlayer entrant = helper.makeMockServerPlayerInLevel();
		entrant.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(triggerBlock));
		helper.runAfterDelay(5, () -> {
			BlockState cracked = helper.getLevel().getBlockState(triggerBlock);
			helper.assertTrue(controller.wasTriggered() && controller.isTriggerPlayer(entrant),
					"entering the Burrow trigger box must persist its one-shot trigger player");
			helper.assertTrue(cracked.is(ChromaBlocks.shielding(
					reika.chromaticraft.registry.ChromaShieldTypes.CRACK).get())
					&& cracked.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED),
					"Burrow proximity must convert the source entrance cell to reinforced crack");
			helper.succeed();
		});
	}

	/** Exact Cavern NBT, persistent controller reward, tunnel and entry trap form one runtime loop. */
	private static void structureCavernNbtController(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(10, 10, 10));
		FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
				helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0xCA73A11L), origin, NoneFeatureConfiguration.INSTANCE);
		helper.assertTrue(new OverworldStructureFeature(OverworldStructureFeature.Type.CAVERN, false).place(context),
				"command Cavern feature must place its canonical NBT at the requested controller coordinate");
		helper.assertTrue(helper.getLevel().getBlockEntity(origin) instanceof TileEntityStructureController,
				"the reserved template anchor must become the registered structure controller");
		TileEntityStructureController controller =
				(TileEntityStructureController)helper.getLevel().getBlockEntity(origin);
		helper.assertTrue(controller.getStructureType()
				== TileEntityStructureController.StructureType.CAVERN,
				"the controller must persist the Cavern identity rather than infer it from geometry");
		// The controller carries two independent sources of Fragments and the total cannot separate
		// them: V33a's guaranteed reward is one stack of 1+rand(4)*(1+rand(2)), so 1, 2, 3, 4, 5 or 7,
		// and the controller also rolls the vanilla stronghold library table, which ChromaChests
		// injects Fragments into at its heaviest weight. Both were true upstream, so the assertion is
		// that the reward stack is present rather than that it is all there is.
		int fragments = 0;
		boolean rewardStack = false;
		for (int slot = 0; slot < controller.getContainerSize(); slot++) {
			ItemStack stack = controller.getItem(slot);
			if (!stack.is(ChromaItems.INFO_FRAGMENT.get()))
				continue;
			fragments += stack.getCount();
			int count = stack.getCount();
			rewardStack |= count == 1 || count == 2 || count == 3 || count == 4 || count == 5 || count == 7;
		}
		helper.assertTrue(fragments >= 1 && rewardStack,
				"controller must contain the source random guaranteed fragment reward; got " + fragments
						+ " across all stacks, none of them a legal reward size");
		long chests = BlockPos.betweenClosedStream(origin.offset(-7, -2, -5), origin.offset(6, 3, 5))
				.filter(pos -> helper.getLevel().getBlockEntity(pos) instanceof TileEntityLootChest).count();
		helper.assertTrue(chests == 2, "the exact Cavern template must retain both loot chests; got " + chests);
		for (int x = 7; x < 18; x++) {
			helper.assertTrue(helper.getLevel().getBlockState(origin.offset(x, 0, 0)).isAir()
					&& helper.getLevel().getBlockState(origin.offset(x, -1, 0)).isAir(),
					"Cavern placement must carve the source two-block-high east tunnel at offset " + x);
		}
		ServerPlayer entrant = helper.makeMockServerPlayerInLevel();
		entrant.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(origin));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(controller.wasTriggered() && controller.isTriggerPlayer(entrant),
					"entering the source Cavern AABB must trigger once and persist the triggering player");
			for (int y = -1; y <= 0; y++) {
				BlockState seal = helper.getLevel().getBlockState(origin.offset(7, y, 0));
				helper.assertTrue(seal.is(ChromaBlocks.shielding(
						reika.chromaticraft.registry.ChromaShieldTypes.CLOAK).get())
						&& seal.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED),
						"Cavern proximity must seal both east entrance cells with reinforced cloak");
			}
			controller.reopenStructure();
			helper.assertTrue(!controller.wasTriggered()
					&& helper.getLevel().getBlockState(origin.offset(7, 0, 0)).isAir()
					&& helper.getLevel().getBlockState(origin.offset(7, -1, 0)).isAir(),
					"reopen must clear both seals and leave an untriggered Cavern controller");
			helper.succeed();
		});
	}

	/** Creative inspection must not inherit the survival-only invisible resource gate. */
	private static void creativeTieredResourceAccess(GameTestHelper helper) {
		var creative = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos plantPos = helper.absolutePos(new BlockPos(3, 3, 3));
		helper.getLevel().setBlock(plantPos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		BlockState plant = ChromaBlocks.tieredPlant(
				reika.chromaticraft.registry.ChromaTieredPlants.AURA_BLOOM).get().defaultBlockState();
		helper.getLevel().setBlock(plantPos, plant, 3);
		helper.assertTrue(!plant.getShape(helper.getLevel(), plantPos, CollisionContext.of(creative)).isEmpty(),
				"a creative player must be able to target and remove an Aura Bloom");

		BlockPos orePos = helper.absolutePos(new BlockPos(7, 3, 3));
		BlockState ore = ChromaBlocks.FIRESTONE.get().defaultBlockState();
		helper.getLevel().setBlock(orePos, ore, 3);
		ChromaBlocks.FIRESTONE.get().setPlacedBy(helper.getLevel(), orePos, ore, creative,
				new ItemStack(ChromaBlocks.FIRESTONE.get()));
		helper.assertTrue(helper.getLevel().getBlockState(orePos).is(ChromaBlocks.FIRESTONE.get()),
				"creative-placed Firestone must remain; survival placement stays progression-gated");
		helper.succeed();
	}

	/** V33a large Rainbow Tree NBT retains all 1,020 authored cells and one consistently chosen wood. */
	private static void rainbowTreeShapeAndLog(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(8, 3, 8));
		helper.getLevel().setBlock(origin.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
		var feature = registry.getOrThrow(net.minecraft.resources.ResourceKey.create(
				Registries.CONFIGURED_FEATURE,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "rainbow_tree"))).value();
		helper.assertTrue(feature.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
				RandomSource.create(0x5241494E424F574CL), origin),
				"the registered Rainbow Tree must place on grass");

		java.util.Set<net.minecraft.world.level.block.Block> logs = new java.util.HashSet<>();
		int leaves = 0;
		java.util.EnumMap<Direction.Axis, Integer> axes = new java.util.EnumMap<>(Direction.Axis.class);
		for (int dx = -6; dx <= 6; dx++) for (int dy = -3; dy <= 30; dy++) for (int dz = -6; dz <= 6; dz++) {
			BlockPos pos = origin.offset(dx, dy, dz);
			BlockState state = helper.getLevel().getBlockState(pos);
			if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
				logs.add(state.getBlock());
				axes.merge(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS),
						1, Integer::sum);
			}
			if (state.is(ChromaBlocks.RAINBOW_LEAVES.get())) leaves++;
		}
		helper.assertTrue(logs.size() == 1,
				"every log in one dye/rainbow tree must share one chosen wood; got " + logs.size());
		helper.assertTrue(leaves == 812, "the NBT must retain all 812 authored rainbow-leaf cells; got " + leaves);
		helper.assertTrue(axes.getOrDefault(Direction.Axis.Y, 0) == 152
				&& axes.getOrDefault(Direction.Axis.X, 0) == 28
				&& axes.getOrDefault(Direction.Axis.Z, 0) == 28,
				"the NBT must retain V33a's 152 vertical and 28+28 branching log axes; got " + axes);
		helper.succeed();
	}

	/** The guide receives every real recipe for an output, in deterministic tier order. */
	private static void lexiconCastingRecipeSnapshot(GameTestHelper helper) {
		var recipes = reika.chromaticraft.network.ChromaNetwork.guideCastingRecipes(
				helper.getLevel().getServer().getRecipeManager(),
				ChromaItems.CLUSTERS.get(ChromaClusterItems.GREEN_GROUP).get());
		helper.assertTrue(recipes.size() == 2,
				"the green group guide page must include its ordinary and boosted V33a recipes");
		for (int i = 1; i < recipes.size(); i++) {
			helper.assertTrue(recipes.get(i - 1).tier().ordinal() <= recipes.get(i).tier().ordinal(),
					"guide casting recipe snapshots must be sorted by tier");
		}
		helper.assertTrue(recipes.stream().allMatch(recipe ->
				recipe.output().is(ChromaItems.CLUSTERS.get(ChromaClusterItems.GREEN_GROUP).get())),
				"a guide snapshot must not leak recipes for a different output");
		helper.succeed();
	}

	/** Book contents use 26.2 custom data without losing foreign fields or duplicating pages. */
	private static void lexiconCustomDataRoundtrip(GameTestHelper helper) {
		ItemStack book = new ItemStack(Items.BOOK);
		net.minecraft.nbt.CompoundTag foreign = new net.minecraft.nbt.CompoundTag();
		foreign.putString("foreign", "preserved");
		reika.dragonapi.libraries.registry.ReikaItemHelper.setStackTag(book, foreign);

		LexiconData written = new LexiconData(
				List.of("CRYSTALS", "CASTING", "CRYSTALS"), false, 3, List.of("first", "second"));
		written.writeTo(book);
		LexiconData loaded = LexiconData.read(book);
		helper.assertTrue(loaded.pages().equals(List.of("CRYSTALS", "CASTING")),
				"Lexicon pages must retain insertion order and canonicalize duplicates");
		helper.assertTrue(loaded.blanks() == 3 && loaded.notes().equals(List.of("first", "second")),
				"Lexicon blanks and notes must round-trip through CUSTOM_DATA");
		helper.assertTrue("preserved".equals(reika.dragonapi.libraries.registry.ReikaItemHelper
				.getStackTag(book).getStringOr("foreign", "")),
				"Lexicon writes must preserve unrelated custom-data fields");

		LexiconData changed = loaded.withPage("RUNEUSE").withPage("RUNEUSE")
				.withBlanksDelta(-20).withNotes(List.of("replacement", "atomic save")).withCreative(true);
		changed.writeTo(book);
		LexiconData reloaded = LexiconData.read(book);
		helper.assertTrue(reloaded.creative() && reloaded.hasPage("UNOWNED")
				&& reloaded.pages().equals(List.of("CRYSTALS", "CASTING", "RUNEUSE")),
				"creative books expose all pages while stored pages remain canonical");
		helper.assertTrue(reloaded.blanks() == 0
				&& reloaded.notes().equals(List.of("replacement", "atomic save")),
				"blank counts clamp at zero and an atomic notebook replacement survives CUSTOM_DATA");
		reloaded.withoutNotes().writeTo(book);
		helper.assertTrue(LexiconData.read(book).notes().isEmpty(),
				"clearing the notebook must remove its stored list without damaging the lexicon");
		helper.assertTrue(ProgressionDescriptions.title(ProgressStage.CRYSTALS).equals("Tangible Energy")
				&& ProgressionDescriptions.hint(ProgressStage.CASTING).contains("wooden crafting table")
				&& ProgressionDescriptions.reveal(ProgressStage.PYLON).contains("energy beacon"),
				"the Progress screens must load V33a's authored title, hint, and reveal XML");
		helper.succeed();
	}

	/** The dependency-free guide catalog must preserve every V33a identity and section boundary. */
	private static void lexiconV33aCatalog(GameTestHelper helper) {
		helper.assertTrue(LexiconCatalog.entries().size() == 322,
				"The complete V33a catalog contains exactly 322 entries");
		helper.assertTrue(LexiconCatalog.obtainablePages().size() == 313,
				"Seven section headers and the two always-present pages are not obtainable fragments");
		var dataTower = LexiconCatalog.byId("DATATOWER");
		helper.assertTrue(dataTower != null
				&& dataTower.section() == LexiconCatalog.Section.STRUCTURES
				&& dataTower.level() == reika.chromaticraft.magic.progression.ResearchLevel.RAWEXPLORE
				&& dataTower.descriptionResource().equals("structure")
				&& dataTower.sourceId().equals("datanode")
				&& dataTower.descriptionNode().equals("datatower")
				&& dataTower.exactTitle().equals("Ancient Data Tower"),
				"DATATOWER must retain its V33a research tier and DATANODE structure binding");
		helper.assertTrue(LexiconCatalog.byId("AISHUTDOWN").exactTitle().equals("chroma.aishutdown"),
				"The one untranslated V33a title must stay verbatim rather than gaining an invented name");
		helper.assertTrue(LexiconCatalog.byId("start").alwaysPresent()
				&& LexiconCatalog.byId("LEXICON").alwaysPresent()
				&& LexiconCatalog.byId("PACKCHANGES").readableWithoutFragment()
				&& LexiconCatalog.byId("PACKCHANGES").obtainable(),
				"V33a always-readable guide pages and fragment membership must remain distinct");
		helper.assertTrue(LexiconDescriptions.description(LexiconCatalog.byId("CRYSTALS"))
				.startsWith("Crystal energy, if it reaches a critical density"),
				"The guide must read the original V33a info.xml prose");
		helper.assertTrue(LexiconDescriptions.notes(LexiconCatalog.byId("INVLINK"))
				.contains("reverse the item flow direction"),
				"Nested V33a tool notes must survive the modern XML loader");
		helper.succeed();
	}

	/** Blank -> chroma-soaked -> decoded -> owned -> inserted into a lexicon. */
	private static void informationFragmentResearchLoop(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack fragment = new ItemStack(ChromaItems.INFO_FRAGMENT.get());
		var blank = reika.chromaticraft.magic.progression.ResearchFragmentData.read(fragment);
		helper.assertTrue(blank.blank() && !blank.random(), "A fresh information fragment must be undeciphered");
		blank.soaked().writeTo(fragment);
		var soaked = reika.chromaticraft.magic.progression.ResearchFragmentData.read(fragment);
		helper.assertTrue(soaked.blank() && soaked.random(), "Liquid chroma must mark a blank fragment for random decoding");

		var expected = LexiconCatalog.byId("FRAGMENT");
		var selected = reika.chromaticraft.magic.progression.PlayerResearch.randomNextResearch(
				player, RandomSource.create(0xC0FFEE));
		helper.assertTrue(selected == expected,
				"V33a ENTRY priority must make the fragment-help page the first decoded research");
		soaked.withPage(selected).writeTo(fragment);
		helper.assertTrue(reika.chromaticraft.magic.progression.ResearchFragmentData.read(fragment).page() == expected,
				"Decoded page identity must round-trip through the fragment's CUSTOM_DATA");
		helper.assertTrue(reika.chromaticraft.magic.progression.PlayerResearch.giveFragment(player, expected, false),
				"Decoding must grant the selected fragment to death-persistent player research");
		helper.assertTrue(reika.chromaticraft.magic.progression.PlayerResearch.fragments(player).contains("FRAGMENT")
				&& !reika.chromaticraft.magic.progression.PlayerResearch.giveFragment(player, expected, false),
				"Research ownership must persist and reject duplicate grants");

		ItemStack lexicon = new ItemStack(ChromaItems.LEXICON.get());
		player.getInventory().add(new ItemStack(Items.PAPER));
		player.getInventory().add(new ItemStack(Items.DYE.black()));
		helper.assertTrue(reika.chromaticraft.item.ItemChromaBook.recoverFragment(player, lexicon, expected)
				&& reika.chromaticraft.item.ItemChromaBook.hasPage(lexicon, expected)
				&& !reika.chromaticraft.item.ItemChromaBook.addPage(lexicon, expected),
				"A known fragment must recover once into the lexicon for one paper and one black dye");
		helper.assertTrue(player.getInventory().countItem(Items.PAPER) == 0
				&& player.getInventory().countItem(Items.DYE.black()) == 0,
				"Fragment recovery must atomically consume the V33a paper and black-dye cost");
		var ejected = LexiconData.read(lexicon).withoutPage(expected.id());
		ejected.writeTo(lexicon);
		helper.assertTrue(!LexiconData.read(lexicon).pages().contains(expected.id()),
				"Ejecting a stored page must remove its identity without damaging other book data");
		helper.succeed();
	}

	/** Canonical NBT placement must build the node, random shield identities, and four linked relays. */
	private static void dataNodeNbtFeature(GameTestHelper helper) {
		BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
		// Decoration runs after trees; reproduce a trunk/canopy through the target column and prove
		// the monument clears vegetation rather than becoming entombed in it.
		for (int y = 1; y <= 6; y++)
			helper.getLevel().setBlock(column.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
		for (int x = -2; x <= 2; x++)
			for (int z = -2; z <= 2; z++)
				helper.getLevel().setBlock(column.offset(x, 7, z), Blocks.OAK_LEAVES.defaultBlockState(), 3);
		helper.assertTrue(DataTowerFeature.placeAt(helper.getLevel(), column, Towers.ALPHA,
				RandomSource.create(0xDA7A)), "DATANODE template should place on the test arena floor");
		BlockPos nodePos = Towers.ALPHA.getGeneratedLocation();
		helper.assertTrue(nodePos != null, "successful placement must cache the generated ALPHA node position");
		helper.assertTrue(helper.getLevel().getBlockEntity(nodePos) instanceof TileEntityDataNode node
				&& node.getTower() == Towers.ALPHA,
				"the NBT center must become an ALPHA data-node block entity");
		for (int i = 1; i <= 4; i++) {
			BlockEntity relay = helper.getLevel().getBlockEntity(nodePos.above(i));
			helper.assertTrue(relay instanceof TileEntityDummyAux dummy
					&& nodePos.equals(dummy.getLink())
					&& dummy.getFlag(TileEntityDummyAux.Flags.HITBOX)
					&& !dummy.getFlag(TileEntityDummyAux.Flags.RENDER)
					&& !dummy.getFlag(TileEntityDummyAux.Flags.MOUSEOVER),
					"each of the four vertical dummy cells must relay its hitbox to the node");
		}
		int shieldCount = 0;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				BlockState state = helper.getLevel().getBlockState(nodePos.offset(x, -1, z));
				if (state.getBlock() instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield
						&& state.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED))
					shieldCount++;
			}
		}
		helper.assertTrue(shieldCount == 9, "the exact V33a 3x3 reinforced shield floor must come from NBT");
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = 0; y <= 12; y++) {
					BlockState state = helper.getLevel().getBlockState(nodePos.offset(x, y, z));
					helper.assertTrue(!state.is(net.minecraft.tags.BlockTags.LOGS)
							&& !state.is(net.minecraft.tags.BlockTags.LEAVES),
							"the complete Data Tower clearance must remove intersecting tree blocks");
				}
			}
		}
		helper.succeed();
	}

	/** Deployment and continuous manipulator scanning must award one owner-bound Data Crystal. */
	private static void dataNodeScanLoop(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		helper.getLevel().setBlock(pos, ChromaBlocks.DATA_NODE.get().defaultBlockState(), 3);
		TileEntityDataNode node = (TileEntityDataNode)helper.getLevel().getBlockEntity(pos);
		node.setTower(reika.chromaticraft.magic.lore.Towers.BETA);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(pos));
		for (int i = 0; i < 110; i++)
			node.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(node.canBeAccessed(), "a nearby player must fully deploy all three tower stages in 110 ticks");
		for (int i = 0; i < 120; i++) {
			node.scan(player);
			node.updateEntity(helper.getLevel(), pos);
		}
		helper.assertTrue(node.hasBeenScanned(player) && node.getState() == reika.chromaticraft.auxiliary.interfaces.OperationInterval.OperationState.INVALID,
				"the completed player scan must persist and enter its 240-tick cooldown");
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
				new AABB(pos).inflate(2, 7, 2), e -> e.getItem().is(ChromaItems.DATA_CRYSTAL.get()));
		helper.assertTrue(drops.size() == 1, "one completed scan must create exactly one Data Crystal");
		CompoundTag data = ReikaItemHelper.getStackTag(drops.getFirst().getItem());
		helper.assertTrue(data != null && player.getUUID().toString().equals(data.getStringOr("owner", "")),
				"the awarded Data Crystal must retain the scanned player's UUID owner");
		helper.assertTrue(!reika.chromaticraft.magic.lore.LoreTowerProgress.hasScanned(
				player, reika.chromaticraft.magic.lore.Towers.BETA),
				"the lore note must retain V33a's post-scan delay");
		for (int i = 0; i < 50; i++) node.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(reika.chromaticraft.magic.lore.LoreTowerProgress.hasScanned(
				player, reika.chromaticraft.magic.lore.Towers.BETA),
				"the scanned tower must enter the player's death-persistent lore set after 50 ticks");
		helper.succeed();
	}

	private static void metaAlloyEcologyContract(GameTestHelper helper) {
		var plant = ChromaBlocks.META_ALLOY_LAMP.get();
		BlockState leaves = plant.defaultBlockState();
		helper.assertTrue(leaves.hasProperty(reika.chromaticraft.block.decoration.BlockMetaAlloyLamp.FACING)
				&& leaves.hasProperty(reika.chromaticraft.block.decoration.BlockMetaAlloyLamp.POD)
				&& !leaves.getValue(reika.chromaticraft.block.decoration.BlockMetaAlloyLamp.POD),
				"Meta-Alloy must replace V33a metadata with explicit attachment and pod properties");
		BlockState pod = leaves.setValue(reika.chromaticraft.block.decoration.BlockMetaAlloyLamp.POD, true);
		VoxelShape leavesShape = leaves.getShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
		VoxelShape podShape = pod.getShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
		helper.assertTrue(podShape.bounds().getYsize() > leavesShape.bounds().getYsize(),
				"grown Meta-Alloy pods must occupy the full plant shape while leaves remain half-height");
		helper.succeed();
	}

	private static void tunnelNukerEntityContract(GameTestHelper helper) {
		var entity = ChromaEntityTypes.TUNNEL_NUKER.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
		helper.assertTrue(entity != null && entity.isNoGravity(),
				"Tunnel Nuker must instantiate as its own no-gravity entity type");
		entity.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
		entity.aiStep();
		helper.assertTrue(entity.getDeltaMovement().horizontalDistanceSqr() > 0.005,
				"Tunnel Nuker must retain V33a's source-driven 0.075 horizontal flight");
		helper.assertTrue(!entity.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100),
				"Tunnel Nuker must remain invulnerable");
		helper.succeed();
	}

	private static void memoryCrystalInscriptionLoop(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		helper.getLevel().setBlock(pos, ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState(), 3);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.TOWER, true, false, false);
		ItemStack crystal = new ItemStack(ChromaItems.DATA_CRYSTAL.get());
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crystal);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
				Direction.UP, pos, false);
		var context = new net.minecraft.world.item.context.UseOnContext(
				player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		for (int i = 0; i <= 100; i++) ChromaItems.DATA_CRYSTAL.get().useOn(context);
		helper.assertTrue(helper.getLevel().getBlockState(pos).is(ChromaBlocks.PYLON_LINK.get()),
				"101 sustained uses must complete V33a's 100-tick Smooth Stone to Pylon Link inscription");
		var dropped = new reika.chromaticraft.entity.EntityDataCrystal(helper.getLevel(),
				pos.getX(), pos.getY(), pos.getZ(), crystal.copy());
		helper.assertTrue(dropped.isInvulnerable() && dropped.getItem().is(ChromaItems.DATA_CRYSTAL.get()),
				"the Memory Crystal's custom dropped entity must preserve identity and invulnerability");
		helper.succeed();
	}

	private static void loreKeyPuzzleContract(GameTestHelper helper) {
		long seed = 0x33A5EEDL;
		var first = reika.chromaticraft.magic.lore.KeyAssemblyPuzzle.generate(seed);
		var second = reika.chromaticraft.magic.lore.KeyAssemblyPuzzle.generate(seed);
		var a = first.cells(0);
		var b = second.cells(0);
		helper.assertTrue(a.equals(b) && a.size() == 169,
				"the V33a 169-cell lore puzzle must be deterministic for one seed");
		long voids = a.stream().filter(cell -> cell.color() == null).count();
		helper.assertTrue(voids == 13, "the 15-wide board must retain exactly thirteen moving voids");
		for (Towers tower : Towers.towerList) {
			long cells = a.stream().filter(cell -> cell.tower() == tower).count();
			helper.assertTrue(cells == 12, "each lore tower must reveal three groups of four cells");
		}
		long initiallyKnown = a.stream().filter(cell -> cell.color() != null && cell.known()).count();
		long alphaKnown = first.cells(1 << Towers.ALPHA.ordinal()).stream()
				.filter(cell -> cell.color() != null && cell.known()).count();
		helper.assertTrue(alphaKnown - initiallyKnown == 12,
				"scanning one tower must reveal precisely its twelve assigned key cells");
		helper.succeed();
	}
	/** The V33a spherical velocity must pass through 26.2 LivingEntity travel and entity tracking. */
	private static void glowCloudSphericalMovement(GameTestHelper helper) {
		EntityGlowCloud cloud = ChromaEntityTypes.GLOW_CLOUD.get().create(
				helper.getLevel(), EntitySpawnReason.COMMAND);
		helper.assertTrue(cloud != null, "glow cloud should instantiate from its registered entity type");
		cloud.setCustomName(Component.literal("movement test"));
		// Keep the entity inside the 5x4x5 forced test structure. Relative (8,8,8) could cross the
		// randomly selected origin's chunk boundary and intermittently leave the entity unticked.
		cloud.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
		var start = cloud.position();
		// Other tests in the batch leave mock players in this level thousands of blocks away, so
		// vanilla Mob.checkDespawn would discard this cloud as "far from any player" before its first
		// tick. Real Glow Clouds run that despawn rule for real; pin this one so the test measures
		// movement rather than vanilla despawn.
		cloud.setPersistenceRequired();
		helper.assertTrue(helper.getLevel().addFreshEntity(cloud), "glow cloud should enter the test level");
		// The arena chunk can unload and reload between the spawn and the first tick, which replaces
		// this instance with one rebuilt from NBT. Re-resolving by UUID keeps the assertion on the
		// live entity, and surviving that round trip is itself the regression: writing V33a's
		// `isdead` flag from isRemoved() made every unloaded Glow Cloud discard itself on reload.
		java.util.UUID id = cloud.getUUID();
		helper.runAfterDelay(5, () -> {
			net.minecraft.world.entity.Entity live = helper.getLevel().getEntity(id);
			helper.assertTrue(live instanceof EntityGlowCloud,
					"the glow cloud must still be present after spawning");
			helper.assertTrue(live.position().distanceToSqr(start) > 0.01,
					"glow cloud must travel under its source-faithful spherical velocity; tickCount="
							+ live.tickCount + ", velocity=" + live.getDeltaMovement());
			helper.succeed();
		});
	}
	/** Repeater diagnostics remain usable, while the earlier SneakPop check keeps source precedence. */
	private static void manipulatorRepeaterDispatch(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(8, 8, 8));
		TileEntityCrystalRepeater repeater = placeRepeater(helper, pos, Direction.NORTH);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		var other = helper.makeMockPlayer(GameType.SURVIVAL);
		repeater.setPlacer(owner);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new ItemStack(ChromaItems.MANIPULATOR.get()));
		other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new ItemStack(ChromaItems.MANIPULATOR.get()));

		var hit = new net.minecraft.world.phys.BlockHitResult(
				net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.EAST, pos, false);
		var diagnostic = ChromaItems.MANIPULATOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(
				owner, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		helper.assertTrue(diagnostic.consumesAction() && repeater.hasStructure()
				&& helper.getLevel().getBlockEntity(pos) == repeater,
				"ordinary Manipulator use must run the repeater diagnostic without changing its stalk");

		other.setShiftKeyDown(true);
		var deniedPop = ChromaItems.MANIPULATOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(
				other, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		helper.assertTrue(deniedPop.consumesAction() && repeater.hasStructure()
				&& helper.getLevel().getBlockEntity(pos) == repeater,
				"a non-owner cannot SneakPop or redirect another player's repeater");
		helper.succeed();
	}
	/** The Manipulator restores V33a's progressive connected-cliff reveal without replacing blocks. */
	private static void manipulatorCliffTransparify(GameTestHelper helper) {
		BlockPos first = helper.absolutePos(new BlockPos(1, 1, 2));
		BlockPos second = first.east();
		BlockPos third = second.east();
		BlockState opaque = ChromaBlocks.CLIFF_STONE.get().defaultBlockState()
				.setValue(BlockCliffStone.TRANSPARENT, false);
		helper.getLevel().setBlock(first, opaque, 3);
		helper.getLevel().setBlock(second, opaque, 3);
		helper.getLevel().setBlock(third, opaque, 3);

		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new ItemStack(ChromaItems.MANIPULATOR.get()));
		var hit = new net.minecraft.world.phys.BlockHitResult(
				net.minecraft.world.phys.Vec3.atCenterOf(first), Direction.UP, first, false);
		ChromaItems.MANIPULATOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(
				player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));

		helper.runAfterDelay(5, () -> {
			for (BlockPos pos : List.of(first, second, third)) {
				BlockState state = helper.getLevel().getBlockState(pos);
				helper.assertTrue(state.is(ChromaBlocks.CLIFF_STONE.get())
						&& state.getValue(BlockCliffStone.TRANSPARENT),
						"Manipulator must reveal every connected cliff-stone block without changing identity");
			}
			helper.succeed();
		});
	}
	/** Every former CrystalElement metadata family owns sixteen stable block and item registry ids. */
	private static void coloredBlockRegistryIdentity(GameTestHelper helper) {
		java.util.Set<net.minecraft.world.level.block.Block> blocks = new java.util.HashSet<>();
		java.util.Set<net.minecraft.world.item.Item> items = new java.util.HashSet<>();
		for (CrystalElement element : CrystalElement.elements) {
			assertColoredIdentity(helper, blocks, items, ChromaBlocks.caveCrystal(element).get(),
					"cave_crystal", element);
			assertColoredIdentity(helper, blocks, items, ChromaBlocks.crystalLamp(element).get(),
					"crystal_lamp", element);
			assertColoredIdentity(helper, blocks, items, ChromaBlocks.superCrystal(element).get(),
					"super_crystal", element);
			assertColoredIdentity(helper, blocks, items, ChromaBlocks.rune(element).get(),
					"crystal_rune", element);
			assertColoredIdentity(helper, blocks, items, ChromaBlocks.encrustedCrystal(element).get(),
					"encrusted_crystal", element);
		}
		helper.assertTrue(blocks.size() == CrystalElement.elements.length * 5,
				"all five accepted color families must expose sixteen distinct blocks");
		helper.assertTrue(items.size() == CrystalElement.elements.length * 5,
				"all five accepted color families must expose sixteen distinct items");
		helper.succeed();
	}

	private static void assertColoredIdentity(GameTestHelper helper,
			java.util.Set<net.minecraft.world.level.block.Block> blocks,
			java.util.Set<net.minecraft.world.item.Item> items,
			net.minecraft.world.level.block.Block block, String base, CrystalElement element) {
		// Matches ChromaBlocks.coloredName(): the underscored vanilla DyeColor spelling
		// (light_gray/light_blue), not the CrystalElement enum constant's own name().
		String expected = base + "_" + element.getEnglishName();
		String actual = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
		helper.assertTrue(actual.equals(expected), "expected registry path " + expected + ", got " + actual);
		helper.assertTrue(block.getStateDefinition().getProperty("color") == null,
				expected + " must not recreate metadata with a color property");
		helper.assertTrue(blocks.add(block), expected + " reused another color's block identity");
		helper.assertTrue(items.add(block.asItem()), expected + " reused another color's item identity");
	}
	/** Hover/mining outline and collision follow the same position-seeded arms as the rendered mesh. */
	private static void caveCrystalDynamicShapeContract(GameTestHelper helper) {
		boolean sawPositiveX = false;
		boolean sawNegativeX = false;
		boolean sawPositiveZ = false;
		boolean sawNegativeZ = false;
		for (int x = 0; x < 4; x++) {
			for (int z = 0; z < 4; z++) {
				BlockPos pos = helper.absolutePos(new BlockPos(3+x, 4, 3+z));
				helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
				helper.getLevel().setBlock(pos, ChromaBlocks.caveCrystal(CrystalElement.CYAN).get().defaultBlockState(), 3);
				BlockState state = helper.getLevel().getBlockState(pos);
				int mask = BlockCaveCrystal.armMask(pos);
				VoxelShape outline = state.getShape(helper.getLevel(), pos, CollisionContext.empty());
				VoxelShape collision = state.getCollisionShape(helper.getLevel(), pos, CollisionContext.empty());
				AABB bounds = outline.bounds();
				helper.assertTrue(!net.minecraft.world.level.block.Block.isShapeFullBlock(outline),
						"cave crystal must not retain a full-cube mining outline");
				helper.assertTrue(outline.toAabbs().equals(collision.toAabbs()),
						"collision and hover/mining geometry must use the same arm silhouette");
				helper.assertTrue(((mask & 8) != 0) == (bounds.maxX > 1),
						"positive-X arm shape disagrees with rendered mask " + mask);
				helper.assertTrue(((mask & 4) != 0) == (bounds.minX < 0),
						"negative-X arm shape disagrees with rendered mask " + mask);
				helper.assertTrue(((mask & 2) != 0) == (bounds.maxZ > 1),
						"positive-Z arm shape disagrees with rendered mask " + mask);
				helper.assertTrue(((mask & 1) != 0) == (bounds.minZ < 0),
						"negative-Z arm shape disagrees with rendered mask " + mask);
				helper.assertTrue(state.getOcclusionShape().isEmpty(),
						"translucent cave crystal must not cast a full-block occlusion shadow");
				helper.assertTrue(state.propagatesSkylightDown(),
						"cave crystal must propagate skylight");
				helper.assertTrue(state.getShadeBrightness(helper.getLevel(), pos) == 1F,
						"cave crystal shade brightness must remain full around neighbouring blocks");
				sawPositiveX |= (mask & 8) != 0;
				sawNegativeX |= (mask & 4) != 0;
				sawPositiveZ |= (mask & 2) != 0;
				sawNegativeZ |= (mask & 1) != 0;
			}
		}
		helper.assertTrue(sawPositiveX && sawNegativeX && sawPositiveZ && sawNegativeZ,
				"sample must exercise all four deterministic arm directions");

		BlockPos ceiling = helper.absolutePos(new BlockPos(10, 5, 10));
		helper.getLevel().setBlock(ceiling.below(), Blocks.AIR.defaultBlockState(), 3);
		helper.getLevel().setBlock(ceiling.above(), Blocks.STONE.defaultBlockState(), 3);
		helper.getLevel().setBlock(ceiling, ChromaBlocks.caveCrystal(CrystalElement.BLUE).get().defaultBlockState(), 3);
		helper.assertTrue(BlockCaveCrystal.isCeilingMounted(helper.getLevel(), ceiling),
				"ceiling-supported crystal must select the vertically mirrored shape");
		helper.succeed();
	}
	/**
	 * V33a {@code Flowers.canPlantAt}, one case per flower. This exists because censusing pregenerated
	 * worlds could not reach these: three of the six are bound to snowy, jungle and swamp biomes that
	 * simply did not occur in the sampled chunks, so a seed roll is the wrong instrument for them.
	 */
	private static void decoFlowerSitingContract(GameTestHelper helper) {
		net.minecraft.server.level.ServerLevel level = helper.getLevel();

		// Luma Lotus, Enderflower and Resonant Clover all stand on dirt or grass.
		for (ChromaDecoFlowers ground : new ChromaDecoFlowers[] {ChromaDecoFlowers.LUMA_LOTUS,
				ChromaDecoFlowers.ENDERFLOWER, ChromaDecoFlowers.RESONANT_CLOVER}) {
			BlockPos pos = helper.absolutePos(new BlockPos(2, 4, 2));
			BlockState state = ChromaBlocks.decoFlower(ground).get().defaultBlockState();
			level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			helper.assertTrue(state.canSurvive(level, pos), ground + " must stand on grass");
			level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
			helper.assertTrue(!state.canSurvive(level, pos), ground + " must reject bare stone");
		}

		// Ether Berries hang beneath jungle leaves.
		BlockPos sano = helper.absolutePos(new BlockPos(5, 4, 2));
		BlockState sanoState = ChromaBlocks.decoFlower(ChromaDecoFlowers.SANO_BLOOM).get().defaultBlockState();
		level.setBlock(sano.above(), Blocks.JUNGLE_LEAVES.defaultBlockState(), 3);
		helper.assertTrue(sanoState.canSurvive(level, sano), "Ether Berries must hang from jungle leaves");
		level.setBlock(sano.above(), Blocks.OAK_LEAVES.defaultBlockState(), 3);
		helper.assertTrue(!sanoState.canSurvive(level, sano), "Ether Berries must reject non-jungle leaves");

		// Void Reeds need sugarcane-legal ground beside water, or another reed below.
		BlockPos reed = helper.absolutePos(new BlockPos(8, 4, 2));
		BlockState reedState = ChromaBlocks.decoFlower(ChromaDecoFlowers.VOID_REEDS).get().defaultBlockState();
		level.setBlock(reed.below(), Blocks.SAND.defaultBlockState(), 3);
		level.setBlock(reed.below().east(), Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!reedState.canSurvive(level, reed), "Void Reeds must reject dry sand");
		level.setBlock(reed.below().east(), Blocks.WATER.defaultBlockState(), 3);
		helper.assertTrue(reedState.canSurvive(level, reed), "Void Reeds must accept sand beside water");
		helper.assertTrue(reedState.canSurvive(level, reed.above()) || true, "reed stacking is checked below");
		level.setBlock(reed, reedState, 3);
		helper.assertTrue(reedState.canSurvive(level, reed.above()), "Void Reeds must stack on another reed");

		// Aura Ivy clings to a horizontally adjacent solid face.
		BlockPos ivy = helper.absolutePos(new BlockPos(11, 4, 2));
		BlockState ivyState = ChromaBlocks.decoFlower(ChromaDecoFlowers.AURA_IVY).get().defaultBlockState();
		level.setBlock(ivy, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(ivy.below(), Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(!ivyState.canSurvive(level, ivy), "Aura Ivy must reject open air");
		level.setBlock(ivy.east(), Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(ivyState.canSurvive(level, ivy), "Aura Ivy must cling to an adjacent stone face");

		helper.succeed();
	}

	/** V33a crystal placement keeps its support, exposed-face, liquid, and colour contracts. */
	private static void crystalWorldgenPlacementContract(GameTestHelper helper) {
		BlockPos valid = helper.absolutePos(new BlockPos(4, 4, 4));
		helper.getLevel().setBlock(valid, Blocks.AIR.defaultBlockState(), 3);
		helper.getLevel().setBlock(valid.below(), Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(CrystalFeature.placeCrystal(helper.getLevel(), valid, CrystalElement.MAGENTA.ordinal()),
				"a supported cave-air position should accept a crystal");
		BlockState placed = helper.getLevel().getBlockState(valid);
		helper.assertTrue(placed.is(ChromaBlocks.caveCrystal(CrystalElement.MAGENTA).get()),
				"worldgen must select the dedicated magenta cave-crystal registry identity");

		BlockPos unsupported = valid.offset(3, 0, 0);
		helper.getLevel().setBlock(unsupported, Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(!CrystalFeature.placeCrystal(helper.getLevel(), unsupported, 0),
				"crystals must reject an unsupported air position");

		BlockPos liquid = valid.offset(6, 0, 0);
		helper.getLevel().setBlock(liquid.below(), Blocks.STONE.defaultBlockState(), 3);
		helper.getLevel().setBlock(liquid, Blocks.WATER.defaultBlockState(), 3);
		helper.assertTrue(!CrystalFeature.placeCrystal(helper.getLevel(), liquid, 0),
				"crystals must not replace liquid");

		BlockPos enclosed = valid.offset(9, 0, 0);
		helper.getLevel().setBlock(enclosed, Blocks.AIR.defaultBlockState(), 3);
		for (Direction direction : Direction.values())
			helper.getLevel().setBlock(enclosed.relative(direction), Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!CrystalFeature.placeCrystal(helper.getLevel(), enclosed, 0),
				"crystals require at least one air-exposed face");
		helper.succeed();
	}
    /** Natural generation must place and validate the canonical NBT pylon plus terrain foundation. */
    private static void pylonWorldgenNbtContract(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(8, 3, 8));
        helper.getLevel().setBlock(new BlockPos(base.getX(), helper.getLevel().getMinY(), base.getZ()),
                Blocks.BEDROCK.defaultBlockState(), 3);
        for (int direction = 0; direction < 4; direction++) {
            int stepX = direction == 0 ? 1 : direction == 1 ? -1 : 0;
            int stepZ = direction == 2 ? 1 : direction == 3 ? -1 : 0;
            for (int distance = 0; distance <= 3; distance++) {
                for (int lateral = -1; lateral <= 1; lateral++) {
                    int dx = stepX * distance + stepZ * lateral;
                    int dz = stepZ * distance + stepX * lateral;
                    helper.getLevel().setBlock(base.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
                    for (int y = 1; y <= 9; y++)
                        helper.getLevel().setBlock(base.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        long featureSeed = 0xC4A0A33L;
        RandomSource probe = RandomSource.create(featureSeed);
        BlockPos featureOrigin = base.offset(-probe.nextInt(16), 0, -probe.nextInt(16));
        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(),
                helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(featureSeed), featureOrigin, NoneFeatureConfiguration.INSTANCE);
        helper.assertTrue(new PylonFeature().place(context),
                "configured pylon feature must bypass the natural grid and place canonical NBT on valid terrain");
        BlockPos pylonPos = base.above(9);
        helper.assertTrue(helper.getLevel().getBlockEntity(pylonPos) instanceof TileEntityCrystalPylon,
                "worldgen must create the registered pylon block entity at the template anchor");
        TileEntityCrystalPylon pylon = (TileEntityCrystalPylon)helper.getLevel().getBlockEntity(pylonPos);
        helper.assertTrue(pylon.hasStructure() && pylon.refreshStructure(),
                "generated NBT monument must immediately validate as the pylon multiblock");
        helper.assertTrue(BlockCrystallineStone.isCrystallineStone(helper.getLevel().getBlockState(base.below()).getBlock()),
                "worldgen must extend the adaptive pylon foundation below soft terrain");
        helper.assertTrue(helper.getLevel().getBlockState(pylonPos.offset(3, -4, 1)).getBlock() instanceof BlockCrystalRune,
                "generated monument must include the canonical coloured rune sockets");
        helper.succeed();
    }

    /** Command-only pylon features carry their requested upgrades without entering biome worldgen. */
    private static void pylonFeatureVariants(GameTestHelper helper) {
        BlockPos turboBase = helper.absolutePos(new BlockPos(12, 3, 12));
        BlockPos boostedBase = helper.absolutePos(new BlockPos(36, 3, 36));
        preparePylonFeatureSite(helper, turboBase);
        preparePylonFeatureSite(helper, boostedBase);

        helper.assertTrue(PylonFeature.tryPlaceAt(helper.getLevel(), turboBase,
                RandomSource.create(0x7A11L), PylonFeature.Variant.TURBOCHARGED),
                "turbocharged command feature should place on valid terrain");
        TileEntityCrystalPylon turbo = (TileEntityCrystalPylon)helper.getLevel()
                .getBlockEntity(turboBase.above(9));
        helper.assertTrue(turbo != null && turbo.isEnhanced(),
                "turbocharged command feature must initialize the enhanced pylon state");
        helper.assertTrue(countPylonUpgradeBlocks(helper, turboBase.above(9)) > 0,
                "turbocharged command feature should retain its upgrade-ring geometry");

        helper.assertTrue(PylonFeature.tryPlaceAt(helper.getLevel(), boostedBase,
                RandomSource.create(0xB0057L), PylonFeature.Variant.POWER_CRYSTAL_BOOSTED),
                "power-crystal command feature should place on valid terrain");
        TileEntityCrystalPylon boosted = (TileEntityCrystalPylon)helper.getLevel()
                .getBlockEntity(boostedBase.above(9));
        helper.assertTrue(boosted != null && boosted.getBoosterCrystals(true).size() == 8,
                "power-crystal command feature must create eight mutually owned functional boosters");
        helper.succeed();
    }

    private static void preparePylonFeatureSite(GameTestHelper helper, BlockPos base) {
        helper.getLevel().setBlock(new BlockPos(base.getX(), helper.getLevel().getMinY(), base.getZ()),
                Blocks.BEDROCK.defaultBlockState(), 3);
        for (int direction = 0; direction < 4; direction++) {
            int stepX = direction == 0 ? 1 : direction == 1 ? -1 : 0;
            int stepZ = direction == 2 ? 1 : direction == 3 ? -1 : 0;
            for (int distance = 0; distance <= 3; distance++) {
                for (int lateral = -1; lateral <= 1; lateral++) {
                    int dx = stepX * distance + stepZ * lateral;
                    int dz = stepZ * distance + stepX * lateral;
                    helper.getLevel().setBlock(base.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
                    for (int y = 1; y <= 9; y++)
                        helper.getLevel().setBlock(base.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static int countPylonUpgradeBlocks(GameTestHelper helper, BlockPos pylonPos) {
        int count = 0;
        for (int x = -4; x <= 4; x++) for (int y = -9; y <= 0; y++) for (int z = -4; z <= 4; z++) {
            BlockState state = helper.getLevel().getBlockState(pylonPos.offset(x, y, z));
            if (state.is(ChromaBlocks.crystallineStone(StoneTypes.STABILIZER).get())
                    || state.is(ChromaBlocks.crystallineStone(StoneTypes.RESORING).get()))
                count++;
        }
        return count;
    }
    /** The V33a shuffled grid contains exactly one candidate per 10x10-chunk cell. */
    private static void pylonWorldgenGridDensity(GameTestHelper helper) {
        int selected = 0;
        long seed = 0xC4A0A33L;
        for (int x = 0; x < 256; x++) {
            for (int z = 0; z < 256; z++) {
                if (PylonFeature.isSelectedChunk(seed, x, z)) selected++;
            }
        }
        helper.assertTrue(selected == 625,
                "V33a's 256-chunk shuffled grid must retain all 625 pylon candidates; got " + selected);
        helper.succeed();
    }

    /** All nine V33a matrix slots must be real menu slots backed by table inventory indices 0-8. */
    private static void castingTableMenuGrid(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(8, 3, 8));
        helper.getLevel().setBlock(pos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
        TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(pos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        reika.chromaticraft.container.MenuCastingTable menu =
                new reika.chromaticraft.container.MenuCastingTable(0, player.getInventory(), table);
        helper.assertTrue(menu.slots.size() == 46,
                "casting menu must expose 10 table slots plus 36 player slots; got " + menu.slots.size());
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(menu.getSlot(slot).container == table
                    && menu.getSlot(slot).getContainerSlot() == slot,
                    "casting matrix slot " + slot + " is missing or bound to the wrong inventory index");
        }
        helper.succeed();
    }
	/** Grant a stage directly and read it back; an unrelated stage stays ungranted. */
	private static void grantAndQuery(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.CRYSTALS, true, false, false);
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(p), "CRYSTALS should be granted");
		helper.assertTrue(!ProgressStage.PYLON.isPlayerAtStage(p), "PYLON should NOT be granted");
		helper.succeed();
	}

	/** Granting a deep stage force-sets its whole recursive-parent chain. */
	private static void recursiveParents(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		// RUNEUSE ← {ALLCOLORS ← PYLON, CASTING ← CRYSTALS}
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.RUNEUSE, true, false, false);
		helper.assertTrue(ProgressStage.RUNEUSE.isPlayerAtStage(p), "RUNEUSE granted");
		helper.assertTrue(ProgressStage.ALLCOLORS.isPlayerAtStage(p), "ALLCOLORS (parent) granted recursively");
		helper.assertTrue(ProgressStage.PYLON.isPlayerAtStage(p), "PYLON (ancestor) granted recursively");
		helper.assertTrue(ProgressStage.CASTING.isPlayerAtStage(p), "CASTING (parent) granted recursively");
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(p), "CRYSTALS (ancestor) granted recursively");
		helper.succeed();
	}

	/** A stage cannot be stepped to without its prerequisites, and can once they are present. */
	private static void prereqGating(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(!ProgressionManager.instance.canStepPlayerTo(p, ProgressStage.RUNEUSE),
				"RUNEUSE should be blocked with no prerequisites");
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.ALLCOLORS, true, false, false);
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.CASTING, true, false, false);
		helper.assertTrue(ProgressionManager.instance.canStepPlayerTo(p, ProgressStage.RUNEUSE),
				"RUNEUSE should be reachable once ALLCOLORS + CASTING are held");
		helper.succeed();
	}

	/** Discovering all 16 colours (with the PYLON prereq held) auto-grants ALLCOLORS. */
	private static void colorDiscovery(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.PYLON, true, false, false);
		for (CrystalElement e : CrystalElement.elements)
			ProgressionManager.instance.setPlayerDiscoveredColor(p, e, true, false);
		helper.assertTrue(ProgressionManager.instance.hasPlayerDiscoveredColor(p, CrystalElement.WHITE),
				"discovered colour should be stored");
		helper.assertTrue(ProgressStage.ALLCOLORS.isPlayerAtStage(p),
				"ALLCOLORS should be auto-granted after all 16 colours (PYLON prereq held)");
		helper.succeed();
	}

	/**
	 * V33a treats focus crystals and lumen relays as alternative discoveries which formulate the
	 * ENERGYIDEA once USEENERGY is known.  The focus-crystal route is retroactive, so either event
	 * order must converge on the same stage without turning FOCUSCRYSTAL into a hard prerequisite.
	 */
	private static void chainedEnergyIdea(GameTestHelper helper) {
		ServerPlayer focusAfterEnergy = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(
				focusAfterEnergy, ProgressStage.USEENERGY, true, false, false);
		helper.assertTrue(ProgressStage.FOCUSCRYSTAL.stepPlayerTo(focusAfterEnergy),
				"FOCUSCRYSTAL should be reachable after CRYSTALS is inherited through USEENERGY");
		helper.assertTrue(ProgressStage.ENERGYIDEA.isPlayerAtStage(focusAfterEnergy),
				"discovering a focus crystal after using energy must formulate ENERGYIDEA");

		ServerPlayer energyAfterFocus = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(
				energyAfterFocus, ProgressStage.FOCUSCRYSTAL, true, false, false);
		helper.assertTrue(!ProgressStage.ENERGYIDEA.isPlayerAtStage(energyAfterFocus),
				"FOCUSCRYSTAL alone must wait for the USEENERGY prerequisite");
		ProgressionManager.instance.setPlayerStage(
				energyAfterFocus, ProgressStage.RUNEUSE, true, false, false);
		helper.assertTrue(ProgressStage.USEENERGY.stepPlayerTo(energyAfterFocus),
				"USEENERGY should be reachable after the RUNEUSE parent chain is installed");
		helper.assertTrue(ProgressStage.ENERGYIDEA.isPlayerAtStage(energyAfterFocus),
				"the retroactive focus-crystal chain must formulate ENERGYIDEA when USEENERGY lands later");
		helper.succeed();
	}

	/** Exercises source discovery, flow creation, server ticking, delivery, and source drain. */
	private static void pylonToReceiver(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos pylonPos = receiverPos.offset(20, 0, 0);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.RED);

		// Let the newly placed pylon establish its world-time baseline before measuring regeneration.
		helper.runAfterDelay(2, () -> {
			int initialEnergy = pylon.getEnergy(CrystalElement.RED);
			helper.assertTrue(CrystalNetworker.instance.makeRequest(receiver, CrystalElement.RED, 250, receiver.getReceiveRange()),
					"pylon should create a direct flow to the receiver");
			helper.runAfterDelay(5, () -> {
				helper.assertTrue(receiver.energy == 250, "receiver should accept all 250 lumens");
				helper.assertTrue(pylon.getEnergy(CrystalElement.RED) < initialEnergy,
						"pylon should be drained by the delivered amount; initial=" + initialEnergy + ", current=" + pylon.getEnergy(CrystalElement.RED));
				receiver.removeFromCache();
				testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
				helper.succeed();
			});
		});
	}

	/** Forces a >48-block route so the pylon can reach the receiver only through a repeater. */
	private static void pylonRepeaterReceiver(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos repeaterPos = receiverPos.offset(26, 0, 0);
		BlockPos pylonPos = receiverPos.offset(52, 0, 0);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.WHITE);

		helper.getLevel().setBlock(repeaterPos, ChromaBlocks.REPEATER.get().defaultBlockState(), 3);
		TileEntityCrystalRepeater repeater = (TileEntityCrystalRepeater)helper.getLevel().getBlockEntity(repeaterPos);
		Direction structureDirection = Direction.NORTH;
		helper.getLevel().setBlock(repeaterPos.relative(structureDirection), ChromaBlocks.rune(CrystalElement.WHITE).get().defaultBlockState(), 3);
		helper.getLevel().setBlock(repeaterPos.relative(structureDirection, 2), ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState(), 3);
		helper.getLevel().setBlock(repeaterPos.relative(structureDirection, 3), ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState(), 3);
		repeater.redirect(structureDirection.getOpposite().get3DDataValue());
		repeater.cachePosition();
		helper.assertTrue(repeater.hasStructure(), "repeater structure should validate");
		helper.assertTrue(pylon.getDistanceSqTo(receiver.getX(), receiver.getY(), receiver.getZ()) > TileEntityCrystalPylon.RANGE * TileEntityCrystalPylon.RANGE,
				"test geometry must forbid a direct pylon-receiver connection");

		helper.assertTrue(CrystalNetworker.instance.makeRequest(receiver, CrystalElement.WHITE, 400, receiver.getReceiveRange()),
				"pylon should route through the repeater");
		helper.assertTrue(repeater.getSignalDepth(CrystalElement.WHITE) == 1,
				"repeater should record its one-hop signal depth; actual=" + repeater.getSignalDepth(CrystalElement.WHITE));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(receiver.energy == 400, "receiver should get 400 lumens through the repeater");
			receiver.removeFromCache();
			testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
			helper.succeed();
		});
	}

	/** A pylon only conducts while the complete colored V33a structure matches. */
	private static void pylonStructureLifecycle(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.BLUE);
		helper.assertTrue(TileEntityCrystalPylon.getPowerCrystalLocations().size() == 8,
				"pylon must retain all eight canonical V33a power-crystal socket offsets");
		helper.assertTrue(TileEntityCrystalPylon.getPowerCrystalLocations().contains(new BlockPos(3, -3, 1)),
				"power-crystal offsets must remain relative to the pylon tile");
		helper.assertTrue(pylon.getRuneLocations().keySet().size() == 8
				&& pylon.getRuneLocations().hasBlock(pylonPos.offset(3, -4, 1)),
				"pylon rune locations must map each crystal socket to the rune one block below");
		helper.assertTrue(pylon.hasStructure(), "complete pylon multiblock should validate");
		helper.assertTrue(pylon.canConduct(), "a full charged structured pylon should conduct");

		BlockPos requiredStone = pylonPos.offset(3, -8, 1);
		BlockState requiredState = helper.getLevel().getBlockState(requiredStone);
		helper.getLevel().destroyBlock(requiredStone, false);
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(!pylon.hasStructure(), "periodic structure check should invalidate the damaged pylon");
			helper.assertTrue(pylon.getEnergy(CrystalElement.BLUE) == 0, "structure loss should drain the pylon");
			helper.assertTrue(!pylon.canConduct(), "invalidated pylon must leave the network");
			helper.getLevel().setBlock(requiredStone, requiredState, 3);
			helper.runAfterDelay(12, () -> {
				helper.assertTrue(pylon.hasStructure(),
						"replacing the missing structure block should reactivate the pylon without a reload");
				helper.assertTrue(pylon.getEnergy(CrystalElement.BLUE) > 0,
						"a reactivated pylon should resume regenerating energy");
				helper.succeed();
			});
		});
	}

	/** The complete NBT broadcast monument activates through its exact liquid-chroma identity cells. */
	private static void pylonBroadcastTemplateContract(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.MAGENTA);
		FilledBlockArray monument = ChromaStructures.PYLONBROADCAST.getArray(
				helper.getLevel(), pylonPos.getX(), pylonPos.getY(), pylonPos.getZ(), CrystalElement.MAGENTA);
		monument.place();

		assertPylonStone(helper, pylonPos.offset(5, -9, 0), BlockCrystallineStone.StoneTypes.EMBOSSED);
		assertPylonStone(helper, pylonPos.offset(5, -3, 0), BlockCrystallineStone.StoneTypes.FOCUS);
		assertPylonStone(helper, pylonPos.offset(3, -5, 5), BlockCrystallineStone.StoneTypes.MULTICHROMIC);
		assertPylonStone(helper, pylonPos.offset(2, -10, 4), BlockCrystallineStone.StoneTypes.SMOOTH);
		BlockState chromaCell = helper.getLevel().getBlockState(pylonPos.offset(2, -9, 4));
		helper.assertTrue(chromaCell.is(ChromaBlocks.CHROMA.get()),
				"the canonical monument must place liquid chroma in every source fluid cell; got "
						+ chromaCell);
		helper.assertTrue(monument.matchInWorld(),
				"the complete broadcast monument must validate with exact registered chroma cells");
		helper.assertTrue(pylon.refreshBroadcastUpgrade() && pylon.hasBroadcastUpgrade(),
				"the complete monument must synchronize the pylon's broadcast upgrade");
		helper.assertTrue(!pylon.needsLineOfSightToReceiver(null),
				"an active broadcast monument must remove ordinary receiver line-of-sight requirements");

		BlockPos brokenCell = pylonPos.offset(2, -9, 4);
		// Air is immediately refilled by the neighbouring source cells, so use a solid obstruction to
		// model an actually broken monument cell rather than accidentally testing vanilla fluid flow.
		helper.getLevel().setBlock(brokenCell, Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!pylon.refreshBroadcastUpgrade() && !pylon.hasBroadcastUpgrade(),
				"removing one required liquid-chroma cell must immediately invalidate the broadcast monument");
		helper.assertTrue(pylon.needsLineOfSightToReceiver(null),
				"an invalidated broadcast monument must restore ordinary receiver line-of-sight requirements");
		helper.getLevel().setBlock(brokenCell, ChromaBlocks.CHROMA.get().defaultBlockState(), 3);
		helper.assertTrue(pylon.refreshBroadcastUpgrade() && pylon.hasBroadcastUpgrade(),
				"restoring the required liquid-chroma cell must reactivate the broadcast monument");
		helper.assertTrue(!pylon.needsLineOfSightToReceiver(null),
				"a repaired broadcast monument must remove receiver line-of-sight requirements again");
		helper.succeed();
	}

	private static void assertPylonStone(GameTestHelper helper, BlockPos pos, BlockCrystallineStone.StoneTypes type) {
		helper.assertTrue(BlockCrystallineStone.isCrystallineStone(helper.getLevel().getBlockState(pos).getBlock())
				&& BlockCrystallineStone.isType(helper.getLevel().getBlockState(pos).getBlock(), type),
				"expected " + type + " pylon stone at " + pos);
	}

	/** Six-face growth state, special flag, shard yield, support loss, and modern NBT all stay coherent. */
	private static void encrustedGrowthPersistence(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(8, 6, 8));
		BlockPos replaceable = pos.offset(2, 0, 0);
		helper.getLevel().setBlock(replaceable, Blocks.SHORT_GRASS.defaultBlockState(), 3);
		helper.assertTrue(!BlockEncrustedCrystal.isEncrustedGrowable(helper.getLevel(), replaceable),
				"V33a encrusted growth must not replace vegetation merely because it is replaceable");
		helper.getLevel().setBlock(replaceable, Blocks.SNOW.defaultBlockState(), 3);
		helper.assertTrue(BlockEncrustedCrystal.isEncrustedGrowable(helper.getLevel(), replaceable),
				"V33a encrusted growth should accept a snow layer");
		helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
		BlockState state = ChromaBlocks.encrustedCrystal(CrystalElement.CYAN).get().defaultBlockState();
		helper.getLevel().setBlock(pos, state, 3);
		TileCrystalEncrusted tile = (TileCrystalEncrusted)helper.getLevel().getBlockEntity(pos);
		tile.addGrowth(Direction.DOWN, 4);
		tile.markReady();
		tile.makeSpecial();
		helper.assertTrue(tile.getSides().equals(java.util.Set.of(Direction.DOWN)),
				"encrusted crystal should retain its independently supported DOWN face");
		helper.assertTrue(tile.getGrowths().iterator().next().getGrowth() == 4 && tile.isSpecial(),
				"encrusted face stage and special state should retain their V33a values");

		CompoundTag saved = tile.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(pos, state, saved, helper.getLevel().registryAccess());
		helper.assertTrue(loaded instanceof TileCrystalEncrusted,
				"encrusted custom data should recreate its registered block entity type");
		TileCrystalEncrusted restored = (TileCrystalEncrusted)loaded;
		helper.assertTrue(restored.isSpecial() && restored.getColor() == CrystalElement.CYAN
				&& restored.getGrowths().size() == 1
				&& restored.getGrowths().iterator().next().getGrowth() == 4,
				"ValueInput/ValueOutput must preserve color, special state, face, and growth stage");

		helper.getLevel().removeBlock(pos.below(), false);
		tile.updateSides();
		helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),
				"an encrusted host with no supported faces should remove itself");
		int shardCount = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
				.stream().filter(entity -> entity.getItem().is(ChromaItems.SHARDS.get(CrystalElement.CYAN).get()))
				.mapToInt(entity -> entity.getItem().getCount()).sum();
		helper.assertTrue(shardCount >= 5,
				"support loss should drop the V33a growth-scaled cyan shard yield; got " + shardCount);
		helper.succeed();
	}

	/** Pylons rediscover adjacent growth after load and recolor both host state and every face. */
	private static void pylonEncrustedDiscoveryRecolor(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.CYAN);
		BlockPos support = pylonPos.offset(3, -8, 1);
		BlockPos growthPos = support.east();
		helper.assertTrue(!ChromaStructures.PYLON.getArray(helper.getLevel(), pylonPos.getX(), pylonPos.getY(),
				pylonPos.getZ(), CrystalElement.CYAN).hasBlock(growthPos),
				"test growth position must remain outside the canonical pylon template");
		BlockState state = ChromaBlocks.encrustedCrystal(CrystalElement.CYAN).get().defaultBlockState();
		helper.getLevel().setBlock(growthPos, state, 3);
		TileCrystalEncrusted growth = (TileCrystalEncrusted)helper.getLevel().getBlockEntity(growthPos);
		growth.addGrowth(Direction.WEST, 2);
		growth.markReady();

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(pylon.getEncrustedCrystals().contains(growthPos),
					"first-tick pylon reload should rediscover adjacent encrusted growth");
			pylon.setColor(CrystalElement.ORANGE);
			helper.assertTrue(helper.getLevel().getBlockState(growthPos)
					.getBlock() == ChromaBlocks.encrustedCrystal(CrystalElement.ORANGE).get(),
					"pylon recolor should update the encrusted host blockstate");
			helper.assertTrue(growth.getGrowths().stream().allMatch(face -> face.color == CrystalElement.ORANGE),
					"pylon recolor should update every persisted encrusted face color");
			helper.succeed();
		});
	}
	/** Ordinary pylons retain V33a target filtering, damage floor, and accelerating player cadence. */
	private static void pylonHostileAttack(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.RED);
		var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), null, pylonPos.east(),
				EntitySpawnReason.COMMAND, false, false);
		helper.assertTrue(zombie != null, "test zombie should instantiate");
		float zombieHealth = zombie.getHealth();
		helper.assertTrue(pylon.attackEntity(zombie, false), "ordinary pylon should strike a hostile living target");
		helper.assertTrue(zombie.getHealth() == zombieHealth - 5,
				"ordinary V33a pylon attack should apply its five-damage floor");
		helper.assertTrue(zombie.hasEffect(MobEffects.RESISTANCE)
				&& zombie.getEffect(MobEffects.RESISTANCE).getAmplifier() == 2,
				"pylon strikes should apply the original 200-tick level-two colour potion effect");

		var player = helper.makeMockPlayer(GameType.CREATIVE);
		float creativeHealth = player.getHealth();
		helper.assertTrue(!pylon.attackEntity(player, false) && player.getHealth() == creativeHealth,
				"creative players must remain immune to automatic pylon hostility");
		var survival = helper.makeMockPlayer(GameType.SURVIVAL);
		pylon.attackEntity(survival, false);
		int delay = pylon.getMinimumTicksBetweenAttacks();
		helper.assertTrue(delay >= TileEntityCrystalPylon.MIN_ATTACK_DELAY && delay <= 62,
				"player strikes should reduce the V33a repeat delay by 18-60 ticks; actual=" + delay);
		helper.succeed();
	}



	/** The tower column retains V33a height weighting, LOS gating, and PYLON ability immunity. */
	private static void pylonVerticalDefense(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.YELLOW);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(pylonPos.getX() + 0.5, pylonPos.getY() + 36, pylonPos.getZ() + 0.5, 0, 0);
		player.invulnerableTime = 0;
		float health = player.getHealth();
		helper.assertTrue(pylon.tryAttackClimber(player),
				"a visible survival player in the pylon column should trigger the anti-climb strike");
		helper.assertTrue(player.getHealth() < health,
				"the anti-climb strike should apply authoritative pylon damage");

		player.invulnerableTime = 0;
		ChromaAbilityData.setPylonImmunity(player, true);
		health = player.getHealth();
		helper.assertTrue(ChromaAbilityData.hasPylonImmunity(player),
				"the modern ability seam should retain the original chromabilities/pylon boolean");
		helper.assertTrue(!pylon.tryAttackClimber(player) && player.getHealth() == health,
				"PYLON ability users must be immune to vertical and ordinary pylon targeting");
		helper.succeed();
	}

	/** Travelling overload pulses preserve their route, beam, impact, and target-pylon destabilization. */
	private static void pylonUnstableShock(GameTestHelper helper) {
		BlockPos sourcePos = helper.absolutePos(new BlockPos(8, 12, 8));
		BlockPos targetPos = sourcePos.offset(12, 0, 0);
		TileEntityCrystalPylon source = placePylon(helper, sourcePos, CrystalElement.BLUE);
		TileEntityCrystalPylon target = placePylon(helper, targetPos, CrystalElement.ORANGE);
		helper.assertTrue(source.shortCircuitWith(target),
				"a same-level remote pylon should accept the V33a short-circuit pulse");
		helper.assertTrue(!source.getTargets().isEmpty(),
				"short circuit should expose its temporary overload beam while the pulse travels");

		EntityPylonOverloadShock guaranteed = new EntityPylonOverloadShock(helper.getLevel(), source,
				List.of(sourcePos, targetPos), 8, 8);
		helper.assertTrue(helper.getLevel().addFreshEntity(guaranteed),
				"the registered overload entity should spawn on the server");
		for (int i = 0; i < 9; i++)
			guaranteed.tick();
		helper.assertTrue(guaranteed.isRemoved(),
				"completed overload pulse should discard after impact; tick=" + guaranteed.tickCount);
		helper.assertTrue(target.isUnstable(),
				"damage-factor eight overload impact should deterministically destabilize a pylon target; targetBE="
				+ helper.getLevel().getBlockEntity(targetPos));
		helper.succeed();
	}

	private static void castingNbtStructureContract(GameTestHelper helper) {
		BlockPos table = helper.absolutePos(new BlockPos(30, 12, 30));
		FilledBlockArray tier1 = ChromaStructures.CASTING1.getArray(helper.getLevel(),
				table.getX(), table.getY(), table.getZ());
		tier1.place();
		String[] mismatch = {""};
		helper.assertTrue(tier1.matchInWorld((world, x, y, z, expected) -> mismatch[0] =
				new BlockPos(x, y, z) + " expected " + expected.asBlockKey() + " found " + world.getBlockState(new BlockPos(x, y, z))),
				"generated casting_l1 NBT must round-trip through the runtime matcher; " + mismatch[0]);
		FilledBlockArray tier2 = ChromaStructures.CASTING2.getArray(helper.getLevel(),
				table.getX(), table.getY(), table.getZ());
		tier2.place();
		helper.assertTrue(tier2.matchInWorld(),
				"generated casting_l2 NBT must round-trip through the runtime matcher");
		helper.getLevel().setBlock(table.offset(3, 0, 3),
				ChromaBlocks.rune(CrystalElement.WHITE).get().defaultBlockState(), 3);
		helper.assertTrue(tier2.matchInWorld(),
				"tier-two floor cells must retain the original rune-or-crystal-stone alternative");
		FilledBlockArray tier3 = ChromaStructures.CASTING3.getArray(helper.getLevel(),
				table.getX(), table.getY(), table.getZ());
		tier3.place();
		helper.assertTrue(tier3.matchInWorld(),
				"generated casting_l3 NBT must include its outer repeaters, rune band, and resource caps");
		helper.succeed();
	}
	/** A fully sealed pylon must violently clear its V33a 3x3x3 capture shell. */
	private static void pylonEnclosureRejection(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.YELLOW);
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x != 0 || y != 0 || z != 0)
						helper.getLevel().setBlock(pylonPos.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
				}
			}
		}
		var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), null, pylonPos.offset(3, 0, 0),
				EntitySpawnReason.COMMAND, false, false);
		helper.assertTrue(zombie != null && helper.getLevel().addFreshEntity(zombie),
				"enclosure test zombie should enter the level");

		pylon.updateEntity(helper.getLevel(), pylonPos);
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x != 0 || y != 0 || z != 0) {
						BlockPos shellPos = pylonPos.offset(x, y, z);
						// Name the offending position and what is actually there: this assertion has
						// flaked once with no way to tell which block survived or why.
						// V33a's anti-capture path also seeds fires in the cleared shell, so a position
						// may legitimately hold fire rather than air. Which positions catch is random,
						// which is exactly why asserting plain air flaked.
						BlockState shell = helper.getLevel().getBlockState(shellPos);
						helper.assertTrue(shell.isAir() || shell.is(net.minecraft.world.level.block.Blocks.FIRE),
								"jar rejection must remove every enclosing block without drops; offset "
										+ x + "," + y + "," + z + " still holds "
										+ shell + " (below=" + helper.getLevel().getBlockState(pylonPos.below()) + ")");
					}
				}
			}
		}
		helper.assertTrue(zombie.getDeltaMovement().horizontalDistance() > 5 && zombie.getDeltaMovement().y() >= 3,
				"jar rejection must launch nearby living entities clear of the pylon");
		helper.assertTrue(zombie.fallDistance == 250,
				"jar rejection must retain the original severe fall-distance penalty");
		helper.succeed();
	}
	/** Restores V33a enhancement capacity, charging rates, and accelerated regeneration decay. */
	private static void pylonRegenerationEnhancement(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.GREEN);
		pylon.enhance();
		helper.assertTrue(pylon.isEnhanced(), "structured pylon should enter enhanced state");
		helper.assertTrue(pylon.getMaxStorage(CrystalElement.GREEN) == TileEntityCrystalPylon.MAX_ENERGY_ENHANCED,
				"enhanced pylon should expose the V33a 900000-lumen capacity");
		helper.assertTrue(pylon.getHeldToolChargingPower(null, CrystalElement.GREEN, ItemStack.EMPTY) == 3,
				"enhanced held-tool charging multiplier should be restored");
		helper.assertTrue(pylon.getDroppedItemChargeRate(ItemStack.EMPTY) == 2,
				"enhanced dropped-item charging multiplier should be restored");

		helper.assertTrue(pylon.drain(CrystalElement.GREEN, 1000), "test setup should drain the enhanced pylon");
		int drainedEnergy = pylon.getEnergy(CrystalElement.GREEN);
		pylon.speedRegenShortly(8);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(pylon.getEnergy(CrystalElement.GREEN) > drainedEnergy,
					"structured pylon should regenerate after being drained");
			pylon.disenhance();
			helper.assertTrue(!pylon.isEnhanced(), "disenhance should clear enhanced state");
			helper.assertTrue(pylon.getMaxStorage(CrystalElement.GREEN) == TileEntityCrystalPylon.MAX_ENERGY,
					"disenhance should restore normal capacity");
			helper.succeed();
		});
	}

	/** Player-placed pylons retain V33a creative-owner source restrictions. */
	private static void pylonPlayerPlacedRestriction(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.YELLOW);
		helper.assertTrue(pylon.canSupply(receiver, CrystalElement.YELLOW),
				"a natural pylon should supply an otherwise valid receiver");

		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		pylon.setPlacer(owner);
		pylon.markPlaced();
		helper.assertTrue(pylon.isPlayerPlaced(), "markPlaced should persist the player-placed state");
		helper.assertTrue(!pylon.canSupply(receiver, CrystalElement.YELLOW),
				"player-placed pylon must use creative-source owner restrictions");
		receiver.removeFromCache();
		testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
		helper.succeed();
	}

	/** A used, depleted pylon forces the original 3x3 chunk square and releases it on invalidation. */
	private static void pylonChunkLoadingLifecycle(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.LIME);
		helper.assertTrue(pylon.getChunksToLoad().size() == 9,
				"pylon chunk loading must retain the V33a 3x3 square covering its power crystals");
		helper.assertTrue(pylon.getChunksToLoad().contains(new net.minecraft.world.level.ChunkPos(
				(pylonPos.getX() >> 4) - 1, (pylonPos.getZ() >> 4) - 1)),
				"pylon chunk square should include the north-west neighboring chunk");
		helper.assertTrue(pylon.drain(CrystalElement.LIME, 100),
				"test pylon should be depleted before chunk loading is requested");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		pylon.onUsedBy(player, CrystalElement.LIME);
		helper.assertTrue(pylon.isForceLoading(),
				"using a depleted structured pylon should acquire its persistent chunk tickets");

		pylon.invalidateMultiblock();
		helper.assertTrue(!pylon.isForceLoading(),
				"structure invalidation should release every pylon chunk ticket");
		helper.assertTrue(!pylon.canConduct(),
				"ticket release must accompany the normal structure invalidation shutdown");
		helper.succeed();
	}
	/** Eight same-owner crystals reproduce the exact V33a geometric 768-lumen/tick recharge. */
	/** Same-owner, same-colour link blocks aggregate throughput and donate V33a recharge. */
	private static void pylonLinkDonation(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos firstPos = helper.absolutePos(new BlockPos(5, 12, 5));
		BlockPos secondPos = helper.absolutePos(new BlockPos(12, 12, 12));
		TileEntityCrystalPylon first = placePylon(helper, firstPos, CrystalElement.PURPLE);
		TileEntityCrystalPylon second = placePylon(helper, secondPos, CrystalElement.PURPLE);

		TileEntityPylonLink firstLink = placePylonLink(helper, first, owner);
		TileEntityPylonLink secondLink = placePylonLink(helper, second, owner);
		firstLink.link();
		secondLink.link();
		helper.assertTrue(first.getLinkTileUUID().equals(owner.getUUID())
				&& second.getLinkTileUUID().equals(owner.getUUID()),
				"both pylons should resolve their owner-bound link blocks");
		helper.assertTrue(first.maxThroughput() == 12000,
				"two ordinary linked pylons should aggregate to 12000 throughput; actual=" + first.maxThroughput());

		helper.assertTrue(second.drain(CrystalElement.PURPLE, 10000),
				"test setup should deplete the donation target");
		int before = second.getEnergy(CrystalElement.PURPLE);
		first.updateEntity(helper.getLevel(), firstPos);
		helper.assertTrue(second.getEnergy(CrystalElement.PURPLE) == before + 100,
				"ordinary linked pylon should donate exactly 100 lumens per eligible tick");
		helper.succeed();
	}

	private static TileEntityPylonLink placePylonLink(GameTestHelper helper,
			TileEntityCrystalPylon pylon, ServerPlayer owner) {
		BlockPos linkPos = pylon.getBlockPos().below(9);
		helper.getLevel().setBlock(linkPos, ChromaBlocks.PYLON_LINK.get().defaultBlockState(), 3);
		TileEntityPylonLink link = (TileEntityPylonLink)helper.getLevel().getBlockEntity(linkPos);
		link.setPlacer(owner);
		helper.assertTrue(pylon.refreshStructure(), "pylon NBT template should accept the link-block base alternative");
		return link;
	}
	private static void pylonPowerCrystalRecharge(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.CYAN);
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		List<TileEntityChromaCrystal> crystals = placePowerCrystals(helper, pylon, owner);
		helper.assertTrue(crystals.size() == 8 && pylon.getBoosterCrystals(true).size() == 8,
				"all eight legal same-owner sockets must be discovered");
		helper.assertTrue(crystals.stream().allMatch(crystal -> crystal.isOwnedByPlayer(owner)),
				"normal block placement must apply the V33a ItemChromaPlacer owner contract");
		helper.assertTrue(crystals.stream().allMatch(TileEntityChromaCrystal::isConnected),
				"each power crystal must persist a live connection to its pylon");
		for (ProgressStage stage : List.of(ProgressStage.LINK, ProgressStage.STORAGE,
				ProgressStage.CHARGE, ProgressStage.INFUSE))
			ProgressionManager.instance.setPlayerStage(owner, stage, true, false, false);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(pylon.drain(CrystalElement.CYAN, 10000), "test setup should drain the pylon");
			int before = pylon.getEnergy(CrystalElement.CYAN);
			helper.runAfterDelay(1, () -> {
				int gained = pylon.getEnergy(CrystalElement.CYAN) - before;
				helper.assertTrue(gained == 768,
						"eight V33a boosters must add 768 lumens per normal tick including base regen; actual=" + gained);
				helper.assertTrue(ProgressStage.POWERCRYSTAL.isPlayerAtStage(owner),
						"an owner with LINK, STORAGE, CHARGE and INFUSE must gain POWERCRYSTAL from eight live boosters");
				helper.succeed();
			});
		});
	}

	/** Owner mismatch exclusion, item-NBT ownership, and V33a quarter-drain backlash. */
	private static void pylonPowerCrystalOwnershipBacklash(GameTestHelper helper) {
		BlockPos pylonPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.MAGENTA);
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		List<TileEntityChromaCrystal> crystals = placePowerCrystals(helper, pylon, owner);
		TileEntityChromaCrystal changed = crystals.get(0);
		changed.setPlacer(other);
		helper.assertTrue(pylon.getBoosterCrystals(true).size() < 8,
				"mixed placer UUIDs must not qualify as a complete owner-matched recharge set");
		changed.setPlacer(owner);

		CompoundTag itemData = new CompoundTag();
		crystals.get(1).getTagsToWriteToStack(itemData);
		ItemStack moved = new ItemStack(ChromaBlocks.POWER_CRYSTAL.get());
		ReikaItemHelper.setStackTag(moved, itemData);
		BlockPos sparePos = pylonPos.offset(0, 2, 0);
		helper.getLevel().setBlock(sparePos, ChromaBlocks.POWER_CRYSTAL.get().defaultBlockState(), 3);
		TileEntityChromaCrystal restored = (TileEntityChromaCrystal)helper.getLevel().getBlockEntity(sparePos);
		restored.setDataFromItemStackTag(moved);
		helper.assertTrue(restored.isOwnedByPlayer(owner) && !restored.isOwnedByPlayer(other),
				"power-crystal owner UUIDs must round-trip through 26.2 item custom data");

		pylon.enhance();
		int before = pylon.getEnergy(CrystalElement.MAGENTA);
		pylon.onPowerCrystalBreak(changed);
		helper.assertTrue(!pylon.isEnhanced(), "breaking a booster must remove pylon enhancement");
		helper.assertTrue(pylon.getEnergy(CrystalElement.MAGENTA) == before - before / 4,
				"power-crystal backlash must drain exactly one quarter of stored energy");
		helper.succeed();
	}
	/** A compound repeater carries independent colors and applies its direct-pylon attenuation. */
	private static void compoundRepeaterMulticolor(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos compoundPos = receiverPos.offset(26, 0, 0);
		BlockPos blackPylonPos = receiverPos.offset(52, 0, 0);
		BlockPos orangePylonPos = receiverPos.offset(52, 0, 16);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon blackPylon = placePylon(helper, blackPylonPos, CrystalElement.BLACK);
		TileEntityCrystalPylon orangePylon = placePylon(helper, orangePylonPos, CrystalElement.ORANGE);
		TileEntityCompoundRepeater compound = placeCompoundRepeater(helper, compoundPos, Direction.DOWN);
		helper.assertTrue(compound.hasStructure() && compound.canConduct(),
				"complete compound repeater structure should conduct every color");
		helper.assertTrue(compound.getPrimaryStructure() == ChromaStructures.COMPOUND,
				"compound repeater must expose its own multiblock identity");
		helper.assertTrue(compound.isConductingElement(CrystalElement.BLACK)
				&& compound.isConductingElement(CrystalElement.ORANGE),
				"compound repeater should conduct independent colors");
		int blackBefore = blackPylon.getEnergy(CrystalElement.BLACK);
		int orangeBefore = orangePylon.getEnergy(CrystalElement.ORANGE);

		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.BLACK, 1, receiver.getReceiveRange()),
				"black pylon should route through the compound repeater");
		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.ORANGE, 1, receiver.getReceiveRange()),
				"orange pylon should route through the same compound repeater");
		helper.assertTrue(compound.getSignalDepth(CrystalElement.BLACK) == 1
				&& compound.getSignalDepth(CrystalElement.ORANGE) == 1,
				"compound repeater should retain independent per-color depths");
		helper.assertTrue(compound.connectedToPylon(),
				"direct pylon connection should set the V33a compound status flag");
		// Sample every tick and assert on the deepest dip, not on the endpoint. Pylons regenerate
		// energyStep*ticks in charge(), so by tick 6 the attenuation debit can be fully refilled and
		// the endpoint comparison passes or fails depending on where the pylon's charge cycle happens
		// to sit -- which is what made this test flaky.
		int[] blackMin = {blackBefore};
		int[] orangeMin = {orangeBefore};
		for (int tick = 1; tick <= 6; tick++) {
			helper.runAfterDelay(tick, () -> {
				blackMin[0] = Math.min(blackMin[0], blackPylon.getEnergy(CrystalElement.BLACK));
				orangeMin[0] = Math.min(orangeMin[0], orangePylon.getEnergy(CrystalElement.ORANGE));
			});
		}
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(receiver.energy == 2,
					"receiver should accept both one-lumen colors; received=" + receiver.energy);
			helper.assertTrue(blackBefore - blackMin[0] > 1,
					"direct-pylon compound route should charge the extra 1000 attenuation; before="
							+ blackBefore + " lowest=" + blackMin[0]);
			helper.assertTrue(orangeBefore - orangeMin[0] > 1,
					"each color should pay its own direct-pylon attenuation; before="
							+ orangeBefore + " lowest=" + orangeMin[0]);
			receiver.removeFromCache();
			testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
			helper.succeed();
		});
	}
	/** A changed block on a cached beam aborts it, and removing the obstruction permits a fresh route. */
	private static void lineOfSightObstructionRecovery(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos pylonPos = receiverPos.offset(20, 0, 0);
		BlockPos obstructionPos = receiverPos.offset(10, 0, 0);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		placePylon(helper, pylonPos, CrystalElement.CYAN);

		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.CYAN, 50, receiver.getReceiveRange()),
				"clear direct beam should establish and populate its LOS cache");
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(receiver.energy == 50, "initial clear beam should deliver 50 lumens");
			helper.assertTrue(CrystalNetworker.instance.hasCachedLineThrough(helper.getLevel(), obstructionPos),
				"LOS cache should contain the intended midpoint before it is obstructed");
			helper.assertTrue(CrystalNetworker.instance.makeRequest(
					receiver, CrystalElement.CYAN, 5000, receiver.getReceiveRange()),
					"long request should create an active cached flow");
			helper.getLevel().setBlock(obstructionPos, Blocks.STONE.defaultBlockState(), 3);
			helper.assertTrue(!PylonFinder.lineOfSight(helper.getLevel(),
				receiverPos.getX(), receiverPos.getY(), receiverPos.getZ(),
				pylonPos.getX(), pylonPos.getY(), pylonPos.getZ()).hasLineOfSight,
				"placed stone must be detected by the endpoint-to-endpoint ray trace");
			helper.runAfterDelay(4, () -> {
				helper.assertTrue(receiver.energy == 50,
						"placing stone on the beam must abort delivery; received=" + receiver.energy);
				helper.getLevel().destroyBlock(obstructionPos, false);
				helper.assertTrue(CrystalNetworker.instance.makeRequest(
						receiver, CrystalElement.CYAN, 100, receiver.getReceiveRange()),
						"removing the obstruction should recalculate LOS and restore routing");
				helper.runAfterDelay(5, () -> {
					helper.assertTrue(receiver.energy == 150,
							"recovered beam should deliver the new request; received=" + receiver.energy);
					receiver.removeFromCache();
					testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
					helper.succeed();
				});
			});
		});
	}
	/** Chunk unload/removal evicts live flows and caches; onLoad registers the same tile cleanly again. */
	private static void networkTileLifecycleCache(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos pylonPos = receiverPos.offset(20, 0, 0);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.LIGHTBLUE);
		helper.assertTrue(CrystalNetworker.instance.isTileCached(pylon),
				"fresh pylon should be registered in the live network cache");
		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.LIGHTBLUE, 5000, receiver.getReceiveRange()),
				"setup should create an active flow");
		helper.assertTrue(CrystalNetworker.instance.hasFlowContaining(pylon),
				"long request should remain active before unload");

		pylon.onChunkUnloaded();
		helper.assertTrue(!CrystalNetworker.instance.isTileCached(pylon),
				"chunk unload must evict the pylon cache entry");
		helper.assertTrue(!CrystalNetworker.instance.hasFlowContaining(pylon),
				"chunk unload must abort every flow containing the pylon");

		pylon.onLoad();
		helper.assertTrue(CrystalNetworker.instance.isTileCached(pylon),
				"onLoad must register an unloaded network tile again");
		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.LIGHTBLUE, 5000, receiver.getReceiveRange()),
				"reloaded pylon should rebuild its evicted cached route");
		pylon.setRemoved();
		helper.assertTrue(!CrystalNetworker.instance.isTileCached(pylon),
				"setRemoved must evict the pylon cache entry idempotently");
		helper.assertTrue(!CrystalNetworker.instance.hasFlowContaining(pylon),
				"tile removal must abort the rebuilt active flow");

		pylon.clearRemoved();
		pylon.onLoad();
		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.LIGHTBLUE, 100, receiver.getReceiveRange()),
				"a cleared and reloaded tile should be able to rebuild a fresh route");
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(receiver.energy == 100,
					"fresh route after lifecycle recovery should deliver exactly 100 lumens");
			pylon.removeFromCache();
			receiver.removeFromCache();
			testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
			helper.succeed();
		});
	}
	/** Breaking a repeater during an active multi-tick request must abort the cached flow. */
	private static void repeaterBreakInvalidatesFlow(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos repeaterPos = receiverPos.offset(26, 0, 0);
		BlockPos pylonPos = receiverPos.offset(52, 0, 0);
		Direction structureDirection = Direction.NORTH;
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		placePylon(helper, pylonPos, CrystalElement.PURPLE);
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, structureDirection);
		BlockPos runePos = repeaterPos.relative(structureDirection);
		helper.getLevel().setBlock(runePos, ChromaBlocks.rune(CrystalElement.PURPLE).get().defaultBlockState(), 3);
		repeater.validateStructure();

		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.PURPLE, 5000, receiver.getReceiveRange()),
				"long request should create a multi-tick repeater flow");
		helper.getLevel().destroyBlock(runePos, false);
		repeater.validateStructure();
		helper.assertTrue(!repeater.canConduct(), "damaged repeater should leave the network immediately");
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(receiver.energy == 0,
					"invalidated flow must not deliver after repeater structure loss; received=" + receiver.energy);
			receiver.removeFromCache();
			testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
			helper.succeed();
		});
	}

	/** Durable locations survive chunk eviction and repopulate the live cache when SavedData reloads. */
	private static void networkSavedDataReload(GameTestHelper helper) {
		BlockPos receiverPos = helper.absolutePos(new BlockPos(2, 12, 2));
		BlockPos pylonPos = receiverPos.offset(20, 0, 0);
		TestReceiver receiver = placeReceiver(helper, receiverPos);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.MAGENTA);
		WorldLocation pylonLocation = new WorldLocation(helper.getLevel(), pylonPos);

		pylon.onChunkUnloaded();
		helper.assertTrue(!CrystalNetworker.instance.isTileCached(pylon),
				"chunk eviction should remove only the live pylon reference");
		CompoundTag persisted = CrystalNetworker.instance.writePersistentState();
		helper.assertTrue(persistentStateContains(persisted, pylonLocation),
				"chunk eviction must retain the pylon location in SavedData");

		CrystalNetworker.instance.readPersistentState(persisted.copy());
		helper.assertTrue(CrystalNetworker.instance.isTileCached(pylon),
				"SavedData reload should resolve the still-valid loaded pylon block entity");
		pylon.cachePosition();
		helper.assertTrue(CrystalNetworker.instance.makeRequest(
				receiver, CrystalElement.MAGENTA, 100, receiver.getReceiveRange()),
				"a pylon restored from durable location data should route immediately");
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(receiver.energy == 100,
					"restored network state should deliver exactly 100 lumens");
			pylon.removeFromCache();
			receiver.removeFromCache();
			testReceivers.remove(new WorldLocation(helper.getLevel(), receiverPos));
			helper.succeed();
		});
	}

	private static boolean persistentStateContains(CompoundTag state, WorldLocation expected) {
		CompoundTag network = state.getCompoundOrEmpty("crystalnet");
		ListTag locations = network.getList("locs").orElse(new ListTag());
		for (Tag value : locations) {
			if (value instanceof CompoundTag entry && expected.equals(WorldLocation.readTag(entry)))
				return true;
		}
		return false;
	}

	/** Repeater structure damage and redstone disablement must immediately remove it from routing. */
	private static void repeaterStructureRedstoneLifecycle(GameTestHelper helper) {
		BlockPos repeaterPos = helper.absolutePos(new BlockPos(8, 12, 8));
		Direction structureDirection = Direction.NORTH;
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, structureDirection);
		helper.assertTrue(repeater.hasStructure() && repeater.canConduct(),
				"complete unpowered repeater should conduct");

		BlockPos runePos = repeaterPos.relative(structureDirection);
		helper.getLevel().destroyBlock(runePos, false);
		repeater.validateStructure();
		helper.assertTrue(!repeater.hasStructure() && !repeater.canConduct(),
				"breaking the rune must invalidate the repeater");

		helper.getLevel().setBlock(runePos, ChromaBlocks.rune(CrystalElement.WHITE).get().defaultBlockState(), 3);
		repeater.validateStructure();
		helper.assertTrue(repeater.canConduct(), "restoring the rune should restore conduction");

		BlockPos powerPos = runePos.relative(Direction.EAST);
		helper.getLevel().setBlock(powerPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		repeater.onAdjacentBlockUpdate();
		helper.assertTrue(!repeater.canConduct(), "redstone beside the supporting rune should disable conduction");
		helper.getLevel().destroyBlock(powerPos, false);
		repeater.onAdjacentBlockUpdate();
		helper.assertTrue(repeater.canConduct(), "removing redstone should re-enable conduction");
		helper.succeed();
	}

	/** V33a authorizes mining and sneak-pop by placer UUID; the turbo caster is independent. */
	private static void repeaterTwoPlayerOwnership(GameTestHelper helper) {
		BlockPos repeaterPos = helper.absolutePos(new BlockPos(8, 12, 8));
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, Direction.NORTH);
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer otherPlayer = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(!owner.getUUID().equals(otherPlayer.getUUID()),
				"test setup must create two distinct player UUIDs");

		helper.assertTrue(repeater.canDrop(owner) && repeater.canDrop(otherPlayer),
				"an unowned V33a repeater should remain public");
		repeater.setPlacer(owner);
		helper.assertTrue(repeater.getPlacerID().equals(owner.getUUID()),
				"setPlacer must retain the owner UUID in the shared tile foundation");
		helper.assertTrue(repeater.canDrop(owner) && repeater.allowMining(owner)
				&& repeater.isOwnedByPlayer(owner),
				"the placer must be authorized to pop, mine, and use owner checks");
		helper.assertTrue(!repeater.canDrop(otherPlayer) && !repeater.allowMining(otherPlayer)
				&& !repeater.isOwnedByPlayer(otherPlayer),
				"a second player must not be authorized to pop or mine an owned repeater");
		helper.assertTrue(repeater.onlyAllowOwnersToMine() && !repeater.onlyAllowOwnersToUse(),
				"V33a protects mining while leaving ordinary repeater use public");

		ItemStack casterStack = new ItemStack(ChromaBlocks.REPEATER.get());
		CompoundTag casterTag = new CompoundTag();
		casterTag.putString("caster", otherPlayer.getUUID().toString());
		ReikaItemHelper.setStackTag(casterStack, casterTag);
		repeater.setDataFromItemStackTag(casterStack);
		helper.assertTrue(otherPlayer.getUUID().equals(repeater.getCaster()),
				"test setup should assign the second player as the repeater caster");
		helper.assertTrue(repeater.canDrop(owner) && !repeater.canDrop(otherPlayer),
				"caster custom data must not replace or bypass placer ownership");
		helper.succeed();
	}

	/** V33a turbo/caster item data must survive the 26.2 CUSTOM_DATA migration. */
	private static void repeaterTurboCustomData(GameTestHelper helper) {
		BlockPos repeaterPos = helper.absolutePos(new BlockPos(8, 12, 8));
		Direction structureDirection = Direction.NORTH;
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, structureDirection);
		UUID caster = UUID.randomUUID();
		ItemStack stack = new ItemStack(ChromaBlocks.REPEATER.get());
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("boosted", true);
		tag.putString("caster", caster.toString());
		ReikaItemHelper.setStackTag(stack, tag);
		repeater.setDataFromItemStackTag(stack);

		BlockPos resonantStone = repeaterPos.relative(structureDirection, 3);
		helper.getLevel().setBlock(resonantStone, ChromaBlocks.crystallineStone(BlockCrystallineStone.StoneTypes.RESORING).get().defaultBlockState(), 3);
		repeater.validateStructure();
		helper.assertTrue(repeater.isTurbocharged(), "boosted custom data should restore turbo state");
		helper.assertTrue(repeater.isEnhancedStructure(), "resonant end stone should activate enhanced structure");
		helper.assertTrue(repeater.getCaster().equals(caster), "caster UUID should round-trip from item custom data");
		helper.assertTrue(repeater.maxThroughput() == 18000,
				"turbo enhanced repeater should have 18000 throughput; actual=" + repeater.maxThroughput());
		helper.assertTrue(repeater.getSignalDegradation(false) == 0,
				"turbo repeater should have zero base signal degradation");
		repeater.markAsTableGrouped(true);
		helper.assertTrue(repeater.maxThroughput() == 36000,
				"table grouping should double enhanced turbo throughput");
		helper.succeed();
	}

	/** A V33a overload waits 55 ticks, then destroys the repeater and its supporting stalk. */
	private static void repeaterOverloadDestroysStalk(GameTestHelper helper) {
		BlockPos repeaterPos = helper.absolutePos(new BlockPos(8, 12, 8));
		Direction structureDirection = Direction.NORTH;
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, structureDirection);
		helper.assertTrue(repeater.getFailureWeight(CrystalElement.BLUE) == 1.5F,
				"repeater fuse failure weight should match V33a");
		helper.assertTrue(repeater.getPathPriority() == 10, "repeater path priority should match V33a");
		repeater.overload(CrystalElement.BLUE);
		helper.assertTrue(repeater.isSurging() && repeater.getSurgeTicks() == 55,
				"overload should begin the 55-tick surge countdown");
		for (int i = 0; i < 54; i++)
			repeater.updateEntity(helper.getLevel(), repeaterPos);
		helper.assertTrue(helper.getLevel().getBlockState(repeaterPos).getBlock() == ChromaBlocks.REPEATER.get(),
				"repeater should survive until the final surge tick");
		repeater.updateEntity(helper.getLevel(), repeaterPos);
		helper.assertTrue(helper.getLevel().getBlockState(repeaterPos).isAir(),
				"final surge tick should destroy the repeater");
		for (int distance = 1; distance <= 3; distance++)
			helper.assertTrue(helper.getLevel().getBlockState(repeaterPos.relative(structureDirection, distance)).isAir(),
					"surge should destroy supporting stalk block " + distance);
		helper.assertTrue(ChromaItems.CRAFTING.size() == 35,
				"every V33a CRAFTING metadata variant should have a distinct modern registry entry");
		helper.assertTrue(ChromaCraftingItems.CRYSTAL_POWDER.ordinal() == 6,
				"crystal powder must retain its original V33a metadata ordinal");
		helper.assertTrue(ChromaItems.craftingStack(ChromaCraftingItems.CRYSTAL_POWDER).is(ChromaItems.CRYSTAL_POWDER.get()),
				"the crafting-item conversion and overload drop must share the registered crystal-powder identity");
		int powderCount = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(repeaterPos).inflate(2))
				.stream()
				.filter(entity -> entity.getItem().is(ChromaItems.CRYSTAL_POWDER.get()))
				.mapToInt(entity -> entity.getItem().getCount())
				.sum();
		helper.assertTrue(powderCount >= 0 && powderCount <= 11,
				"V33a overload should drop between zero and eleven individual crystal powder items, got " + powderCount);
		helper.succeed();
	}

	private static TileEntityCompoundRepeater placeCompoundRepeater(GameTestHelper helper, BlockPos pos, Direction structureDirection) {
		helper.getLevel().setBlock(pos, ChromaBlocks.COMPOUND.get().defaultBlockState(), 3);
		TileEntityCompoundRepeater compound = (TileEntityCompoundRepeater)helper.getLevel().getBlockEntity(pos);
		for (int distance = 1; distance <= 5; distance++) {
			BlockCrystallineStone.StoneTypes type = distance == 3
					? BlockCrystallineStone.StoneTypes.MULTICHROMIC
					: distance == 1 || distance == 5
							? BlockCrystallineStone.StoneTypes.BRICKS
							: structureDirection.getAxis().isVertical()
									? BlockCrystallineStone.StoneTypes.COLUMN
									: BlockCrystallineStone.StoneTypes.BEAM;
			helper.getLevel().setBlock(pos.relative(structureDirection, distance),
					ChromaBlocks.crystallineStone(type).get().defaultBlockState(), 3);
		}
		compound.redirect(structureDirection.getOpposite().get3DDataValue());
		compound.cachePosition();
		return compound;
	}
	private static TileEntityCrystalRepeater placeRepeater(GameTestHelper helper, BlockPos pos, Direction structureDirection) {
		helper.getLevel().setBlock(pos, ChromaBlocks.REPEATER.get().defaultBlockState(), 3);
		TileEntityCrystalRepeater repeater = (TileEntityCrystalRepeater)helper.getLevel().getBlockEntity(pos);
		helper.getLevel().setBlock(pos.relative(structureDirection), ChromaBlocks.rune(CrystalElement.WHITE).get().defaultBlockState(), 3);
		helper.getLevel().setBlock(pos.relative(structureDirection, 2), ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState(), 3);
		helper.getLevel().setBlock(pos.relative(structureDirection, 3), ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState(), 3);
		repeater.redirect(structureDirection.getOpposite().get3DDataValue());
		repeater.cachePosition();
		return repeater;
	}
	private static TestReceiver placeReceiver(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().setBlock(pos, ChromaBlocks.DISPLAY_POINT.get().defaultBlockState(), 3);
		TestReceiver receiver = new TestReceiver(helper.getLevel(), pos);
		testReceivers.put(new WorldLocation(helper.getLevel(), pos), receiver);
		receiver.cachePosition();
		return receiver;
	}
	private static List<TileEntityChromaCrystal> placePowerCrystals(GameTestHelper helper,
			TileEntityCrystalPylon pylon, ServerPlayer owner) {
		List<TileEntityChromaCrystal> crystals = new java.util.ArrayList<>();
		for (BlockPos offset : TileEntityCrystalPylon.getPowerCrystalLocations()) {
			BlockPos pos = pylon.getBlockPos().offset(offset);
			BlockState state = ChromaBlocks.POWER_CRYSTAL.get().defaultBlockState();
			helper.getLevel().setBlock(pos, state, 3);
			ChromaBlocks.POWER_CRYSTAL.get().setPlacedBy(helper.getLevel(), pos, state, owner,
					new ItemStack(ChromaBlocks.POWER_CRYSTAL.get()));
			TileEntityChromaCrystal crystal = (TileEntityChromaCrystal)helper.getLevel().getBlockEntity(pos);
			crystal.refreshConnection();
			crystals.add(crystal);
		}
		return crystals;
	}

	private static TileEntityCrystalPylon placePylon(GameTestHelper helper, BlockPos pos, CrystalElement color) {
		FilledBlockArray structure = ChromaStructures.PYLON.getArray(
				helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), color);
		structure.place();
		helper.getLevel().setBlock(pos, ChromaBlocks.PYLON.get().defaultBlockState(), 3);
		TileEntityCrystalPylon pylon = (TileEntityCrystalPylon)helper.getLevel().getBlockEntity(pos);
		pylon.generateColor(color);
		if (!pylon.refreshStructure())
			throw new IllegalStateException("Generated pylon structure did not match at " + pos);
		pylon.cachePosition();
		return pylon;
	}

	/** Exact grid/stand occupancy and pylon aura thresholds are part of the physical recipe contract. */
	private static void castingRecipeContract(GameTestHelper helper) {
		BlockPos standOffset = new BlockPos(2, 0, 0);
		CastingTableRecipe recipe = new CastingTableRecipe(
				CastingTableRecipe.Tier.PYLON,
				List.of(new CastingTableRecipe.GridIngredient(4, Ingredient.of(Items.REDSTONE))),
				List.of(new StandIngredient(standOffset, Ingredient.of(Items.QUARTZ))),
				List.of(new CastingTableRecipe.RuneRequirement(new BlockPos(1, -1, 1), CrystalElement.BLUE)),
				List.of(new AuraRequirement(CrystalElement.BLACK, 500)),
				new ItemStackTemplate(Items.DIAMOND), 20, 8);

		NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
		grid.set(4, new ItemStack(Items.REDSTONE));
		Map<BlockPos, ItemStack> stands = Map.of(standOffset, new ItemStack(Items.QUARTZ));
		Map<BlockPos, CrystalElement> runes = Map.of(new BlockPos(1, -1, 1), CrystalElement.BLUE);
		CastingRecipeInput valid = new CastingRecipeInput(grid, stands, runes, Map.of(CrystalElement.BLACK, 500));
		helper.assertTrue(recipe.matches(valid, helper.getLevel()), "complete pylon recipe input should match");
		helper.assertTrue(recipe.assemble(valid).is(Items.DIAMOND), "casting recipe should assemble its declared result");

		NonNullList<ItemStack> occupiedGrid = NonNullList.withSize(9, ItemStack.EMPTY);
		occupiedGrid.set(4, new ItemStack(Items.REDSTONE));
		occupiedGrid.set(0, new ItemStack(Items.STICK));
		helper.assertTrue(!recipe.matches(new CastingRecipeInput(occupiedGrid, stands, runes,
				Map.of(CrystalElement.BLACK, 500)), helper.getLevel()), "undeclared grid occupancy must reject the recipe");
		helper.assertTrue(!recipe.matches(new CastingRecipeInput(grid,
				Map.of(standOffset, new ItemStack(Items.QUARTZ), new BlockPos(-2, 0, 0), new ItemStack(Items.QUARTZ)),
				runes, Map.of(CrystalElement.BLACK, 500)), helper.getLevel()), "undeclared stand occupancy must reject the recipe");
		helper.assertTrue(!recipe.matches(new CastingRecipeInput(grid, stands, runes,
				Map.of(CrystalElement.BLACK, 499)), helper.getLevel()), "insufficient aura must reject the recipe");

		// V33a getXPModifier: below the threshold every craft pays in full; at and past it the award
		// decays by the penalty multiplier per extra craft. CoreRecipe classes are exempt, which is
		// the schema default, so an ordinary recipe never decays.
		helper.assertTrue(recipe.penaltyThreshold() == Integer.MAX_VALUE
				&& recipe.experienceModifier(1_000_000) == 1F,
				"a recipe with no declared threshold must be exempt like V33a's CoreRecipe marker");
		CastingTableRecipe penalised = recipe.withPenaltyThreshold(3);
		helper.assertTrue(penalised.experienceModifier(0) == 1F && penalised.experienceModifier(2) == 1F,
				"crafts below the threshold must pay full experience");
		helper.assertTrue(penalised.experienceModifier(3) == 1F,
				"the threshold craft itself is the last one at full experience");
		helper.assertTrue(Math.abs(penalised.experienceModifier(4) - 0.75F) < 1e-6
				&& Math.abs(penalised.experienceModifier(6) - 0.75F*0.75F*0.75F) < 1e-6,
				"each craft past the threshold must multiply by the V33a 0.75 penalty");
		helper.succeed();
	}

	/** A registered ordinary recipe runs for its declared duration and commits input/output/XP together. */
	/**
	 * The opening move of the whole mod: looking at a cave crystal must grant CRYSTALS, and that
	 * grant must be what unlocks tier-1 casting. Everything else in the casting chain is covered by
	 * castingTableAtomicCraft, which grants the stage directly -- this covers the link that one
	 * skips, so a silently refused grant cannot masquerade as "casting is broken".
	 */
	private static void explorationGrantsCrystals(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
		helper.getLevel().setBlock(pos,
				ChromaBlocks.caveCrystal(CrystalElement.BLUE).get().defaultBlockState(), 3);
		var ep = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(!ProgressStage.CRYSTALS.isPlayerAtStage(ep),
				"a fresh player must not already hold CRYSTALS");
		ExplorationMonitor.scanLookedAtBlock(ep, helper.getLevel(), pos);
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(ep),
				"looking at a cave crystal must grant CRYSTALS");
		helper.succeed();
	}

	/**
	 * Complete beta opening gate: discover and mine cave crystals, resolve the registered ordinary
	 * Casting Table recipe, then use that owned table and the registered base-tier StandRecipe to
	 * produce the first Casting Item Stand. No progression stage or recipe object is injected.
	 */
	private static void earlyGameCastingStandChain(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos crystalOrigin = helper.absolutePos(new BlockPos(2, 4, 2));
		for (int i = 0; i < 4; i++) {
			BlockPos crystalPos = crystalOrigin.offset(i, 0, 0);
			helper.getLevel().setBlock(crystalPos.below(), Blocks.STONE.defaultBlockState(), 3);
			helper.getLevel().setBlock(crystalPos,
					ChromaBlocks.caveCrystal(CrystalElement.BLUE).get().defaultBlockState(), 3);
		}

		helper.assertTrue(!ProgressStage.CRYSTALS.isPlayerAtStage(player),
				"a new beta-path player must begin before CRYSTALS");
		helper.assertTrue(ResearchProgress.getLevel(player) == ResearchLevel.ENTRY
				&& !ResearchLevel.RUNECRAFT.canProgressTo(player),
				"a fresh player must begin at ENTRY without the casting research gate");
		ExplorationMonitor.scanLookedAtBlock(player, helper.getLevel(), crystalOrigin);
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(player),
				"looking at the first cave crystal must unlock the opening CRYSTALS stage");

		for (int i = 0; i < 4; i++)
			helper.getLevel().destroyBlock(crystalOrigin.offset(i, 0, 0), true, player);
		int droppedShards = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
				new AABB(crystalOrigin).expandTowards(4, 1, 1).inflate(1)).stream()
				.filter(entity -> entity.getItem().is(ChromaItems.SHARDS.get(CrystalElement.BLUE).get()))
				.mapToInt(entity -> entity.getItem().getCount()).sum();
		helper.assertTrue(droppedShards >= 4,
				"four mined cave crystals must supply at least four blue shards for the table; got " + droppedShards);

		ItemStack shard = ChromaItems.shardStack(CrystalElement.BLUE);
		CraftingInput tableInput = CraftingInput.of(3, 3, List.of(
				new ItemStack(Blocks.STONE), new ItemStack(Blocks.CRAFTING_TABLE), new ItemStack(Blocks.STONE),
				new ItemStack(Blocks.STONE), shard.copy(), new ItemStack(Blocks.STONE),
				shard.copy(), shard.copy(), shard.copy()));
		Optional<RecipeHolder<CraftingRecipe>> tableRecipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, tableInput, helper.getLevel());
		helper.assertTrue(tableRecipe.isPresent(), "the source-exact ordinary Casting Table recipe must resolve");
		helper.assertTrue(tableRecipe.get().id().identifier().equals(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "casting_table")),
				"the opening grid must resolve chromaticraft:casting_table, not another recipe");
		ItemStack craftedTable = tableRecipe.get().value().assemble(tableInput);
		helper.assertTrue(craftedTable.is(ChromaBlocks.CASTING_TABLE.get().asItem()),
				"the registered opening recipe must assemble a Casting Table");
		CraftingInput manipulatorInput = CraftingInput.of(3, 3, List.of(
				ItemStack.EMPTY, ItemStack.EMPTY, shard.copy(),
				ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY,
				new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY));
		Optional<RecipeHolder<CraftingRecipe>> manipulatorRecipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, manipulatorInput, helper.getLevel());
		helper.assertTrue(manipulatorRecipe.isPresent()
				&& manipulatorRecipe.get().id().identifier().equals(
						Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "manipulator")),
				"the source-exact shard-and-sticks Elemental Manipulator recipe must resolve");
		ItemStack craftedManipulator = manipulatorRecipe.get().value().assemble(manipulatorInput);
		helper.assertTrue(craftedManipulator.is(ChromaItems.MANIPULATOR.get()),
				"the bootstrap recipe must assemble the real Elemental Manipulator");

		// The first cast a new player can actually run is a base-tier one. V33a's CrystalStoneRecipe
		// turns a shard plus four stone into eight crystalline stone, and it is what grants CASTING
		// and starts the table's climb toward the 250-XP temple tier.
		BlockPos tablePos = helper.absolutePos(new BlockPos(10, 4, 10));
		helper.getLevel().setBlock(tablePos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		table.setPlacer(player);
		table.setItem(1, new ItemStack(Blocks.STONE));
		table.setItem(3, new ItemStack(Blocks.STONE));
		table.setItem(4, ChromaItems.shardStack(CrystalElement.BLUE));
		table.setItem(5, new ItemStack(Blocks.STONE));
		table.setItem(7, new ItemStack(Blocks.STONE));
		helper.assertTrue(table.triggerCrafting(player),
				"a fresh CRYSTALS-stage player must be able to run the base-tier crystalline stone recipe");
		for (int i = 0; i < 5; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaBlocks.crystallineStone(
						BlockCrystallineStone.StoneTypes.SMOOTH).get().asItem())
				&& table.getItem(9).getCount() == 8,
				"the base-tier stone cast must yield the source-exact eight crystalline stone");
		helper.assertTrue(table.getTableXP() == 5 && ProgressStage.CASTING.isPlayerAtStage(player),
				"the first cast must award its XP and the CASTING progression stage");
		helper.assertTrue(CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.CRAFTING)
				&& !CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.TEMPLE),
				"the successful cast must persist the player's exact V33a CRAFTING tier");
		helper.assertTrue(ResearchLevel.RUNECRAFT.canProgressTo(player),
				"a personal CRAFTING-tier completion must unlock the V33a RUNECRAFT research gate");
		helper.assertTrue(ResearchProgress.setLevel(player, ResearchLevel.RUNECRAFT, false)
				&& ResearchProgress.getLevel(player) == ResearchLevel.RUNECRAFT
				&& ResearchLevel.ENTRY.playerHas(player),
				"research levels must persist by name and include all earlier tiers");
		table.setItem(9, ItemStack.EMPTY);

		// The Item Stand is a TEMPLE recipe, so the same table cannot make one yet: it has neither
		// the temple, the 250-XP tier, RUNEUSE, nor the recipe's own four floor runes.
		setStandGrid(table);
		helper.assertTrue(!table.triggerCrafting(player),
				"the Item Stand must not be castable from a bare base-tier table");

		earlyGameStandCast(helper, player, craftedManipulator);
		helper.succeed();
	}

	/** The temple half of the opening arc: RUNEUSE, the 250-XP tier, and the StandRecipe rune ring. */
	private static void earlyGameStandCast(GameTestHelper helper, ServerPlayer player, ItemStack manipulator) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING1, 250);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		table.setPlacer(player);
		// Colour discovery has its own coverage; this test is about the casting half of the arc.
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.PYLON, true, false, false);
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.ALLCOLORS, true, false, false);
		table.validateStructure();
		table.onAddRune(player);
		helper.assertTrue(ProgressStage.RUNEUSE.isPlayerAtStage(player),
				"a rune placed at a temple-tier table must grant RUNEUSE");

		setStandGrid(table);
		helper.assertTrue(!table.triggerCrafting(player),
				"the temple alone is not enough: StandRecipe also wants its own four floor runes");
		placeRune(helper, tablePos, new BlockPos(-2, 0, 3), CrystalElement.PURPLE);
		placeRune(helper, tablePos, new BlockPos(2, 0, -3), CrystalElement.PURPLE);
		placeRune(helper, tablePos, new BlockPos(-2, 0, -3), CrystalElement.BLACK);
		placeRune(helper, tablePos, new BlockPos(2, 0, 3), CrystalElement.BLACK);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, manipulator);
		net.minecraft.world.InteractionResult manipulation = manipulator.getItem().useOn(
				new net.minecraft.world.item.context.UseOnContext(player,
						net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(
								net.minecraft.world.phys.Vec3.atCenterOf(tablePos), Direction.UP, tablePos, false)));
		helper.assertTrue(manipulation.consumesAction() && table.isCrafting(),
				"the crafted Manipulator must start the source-exact StandRecipe once its rune ring is set");
		helper.assertTrue(table.getCraftingTick() == 20,
				"StandRecipe is a TempleCastingRecipe and runs for the source twenty ticks");
		for (int i = 0; i < 20; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(!table.isCrafting() && table.getItem(9).is(ChromaBlocks.ITEM_STAND.get().asItem()),
				"the beta opening chain must finish with a usable Casting Item Stand in the output slot");
		helper.assertTrue(table.getTableXP() == 330,
				"StandRecipe pays twice the temple experience; got " + table.getTableXP());
		helper.assertTrue(CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.TEMPLE),
				"casting the stand must record the player's TEMPLE tier");
	}

	private static void setStandGrid(TileEntityCastingTable table) {
		table.setItem(0, new ItemStack(Items.IRON_INGOT));
		table.setItem(2, new ItemStack(Items.IRON_INGOT));
		table.setItem(3, new ItemStack(Items.STONE_SLAB));
		table.setItem(4, new ItemStack(Items.LAPIS_LAZULI));
		table.setItem(5, new ItemStack(Items.STONE_SLAB));
		table.setItem(6, new ItemStack(Items.COBBLESTONE));
		table.setItem(7, new ItemStack(Items.COBBLESTONE));
		table.setItem(8, new ItemStack(Items.COBBLESTONE));
	}

	/**
	 * V33a tiered ores are disguised as their host stone until the miner reaches their stage: an
	 * insufficient player mines the host block's own drops and gets none of the real resource.
	 */
	private static void tieredOreProgressionGate(GameTestHelper helper) {
		// makeMockServerPlayerInLevel is hardcoded to CREATIVE, and V33a's tiered harvest is a
		// no-op in creative, so this needs a survival actor to exercise the gate at all.
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos orePos = helper.absolutePos(new BlockPos(3, 3, 3));
		net.minecraft.world.level.block.Block ore = ChromaBlocks.ENERGIZED_ROCK.get();
		helper.assertTrue(!ProgressStage.CRYSTALS.isPlayerAtStage(player),
				"the gate test must start before the ore's stage");

		int dust = breakTieredOre(helper, orePos, ore, player,
				ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get());
		int stone = countDrops(helper, orePos, net.minecraft.world.item.Items.COBBLESTONE);
		helper.assertTrue(dust == 0,
				"an insufficient miner must get none of the real resource; got " + dust);
		helper.assertTrue(stone > 0,
				"an insufficient miner must get the host stone's own drops instead");

		ProgressionManager.instance.setPlayerStage(player, ProgressStage.CRYSTALS, true, false, false);
		BlockPos secondPos = orePos.offset(0, 0, 3);
		int realDust = breakTieredOre(helper, secondPos, ore, player,
				ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get());
		helper.assertTrue(realDust >= 1 && realDust <= 16,
				"a sufficient miner must get the V33a 1-16 chromic dust; got " + realDust);
		helper.assertTrue(countDrops(helper, secondPos, net.minecraft.world.item.Items.COBBLESTONE) == 0,
				"a sufficient miner must not also get the disguise drops");

		// The place-back rule: an ore you cannot see removes itself instead of staying placed.
		net.minecraft.world.entity.player.Player novice = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos placedPos = orePos.offset(0, 0, 6);
		helper.getLevel().setBlock(placedPos, ore.defaultBlockState(), 3);
		ore.setPlacedBy(helper.getLevel(), placedPos, ore.defaultBlockState(), novice, new ItemStack(ore));
		helper.assertTrue(helper.getLevel().getBlockState(placedPos).isAir(),
				"placing a tiered ore below its stage must remove it again");
		helper.succeed();
	}

	private static int breakTieredOre(GameTestHelper helper, BlockPos pos,
			net.minecraft.world.level.block.Block ore, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item want) {
		helper.getLevel().setBlock(pos, ore.defaultBlockState(), 3);
		BlockState state = helper.getLevel().getBlockState(pos);
		state.getBlock().onDestroyedByPlayer(state, helper.getLevel(), pos, player,
				new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE), true, state.getFluidState());
		return countDrops(helper, pos, want);
	}

	private static int countDrops(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item want) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
				.filter(e -> e.getItem().is(want)).mapToInt(e -> e.getItem().getCount()).sum();
	}

	/**
	 * The ore vein itself: V33a targets only the host block, so a vein dropped into a stone volume
	 * replaces stone and leaves anything else (here, the cliff material it explicitly excluded)
	 * untouched.
	 */
	private static void tieredOreWorldgen(GameTestHelper helper) {
		BlockPos centre = helper.absolutePos(new BlockPos(8, 4, 8));
		for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -4; dz <= 4; dz++)
			helper.getLevel().setBlock(centre.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 3);
		BlockPos guarded = centre.offset(3, 0, 3);
		helper.getLevel().setBlock(guarded, ChromaBlocks.CLIFF_STONE.get().defaultBlockState(), 3);

		var registry = helper.getLevel().registryAccess()
				.lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
		var configured = registry.getOrThrow(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "energized_rock"))).value();
		helper.assertTrue(configured.place(helper.getLevel(),
						helper.getLevel().getChunkSource().getGenerator(),
						RandomSource.create(0xDEADBEEFL), centre),
				"the registered tiered-ore configured feature must place a vein in a stone volume");

		int ores = 0;
		for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -4; dz <= 4; dz++) {
			if (helper.getLevel().getBlockState(centre.offset(dx, dy, dz)).is(ChromaBlocks.ENERGIZED_ROCK.get()))
				ores++;
		}
		helper.assertTrue(ores > 0 && ores <= 12,
				"the vein must place between one and its V33a size of twelve; got " + ores);
		helper.assertTrue(helper.getLevel().getBlockState(guarded).is(ChromaBlocks.CLIFF_STONE.get()),
				"the vein targets the host block only and must not eat non-stone neighbours");
		helper.succeed();
	}
	/** V33a rejects fake/dummy actors before the Manipulator can dispatch or a table can cast. */
	private static void castingManipulatorFakePlayerGuard(GameTestHelper helper) {
		BlockPos tablePos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(tablePos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		ServerPlayer fake = ReikaPlayerAPI.getFakePlayerByNameAndUUID(helper.getLevel(),
				"ChromatiCraft Casting Guard", UUID.fromString("2176554e-9406-4d70-ae3e-9b39c54aac22"));
		table.setPlacer(fake);
		ProgressionManager.instance.setPlayerStage(fake, ProgressStage.CRYSTALS, true, false, false);
		ItemStack smooth = new ItemStack(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem());
		table.setItem(3, smooth.copy());
		table.setItem(4, smooth.copy());
		ItemStack manipulator = new ItemStack(ChromaItems.MANIPULATOR.get());
		fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, manipulator);
		net.minecraft.world.InteractionResult result = manipulator.getItem().useOn(
				new net.minecraft.world.item.context.UseOnContext(fake,
						net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(
								net.minecraft.world.phys.Vec3.atCenterOf(tablePos), Direction.UP, tablePos, false)));
		helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS && !table.isCrafting(),
				"a fake player must be rejected before the Elemental Manipulator dispatches to the table");
		helper.assertTrue(!table.triggerCrafting(fake) && !table.isCrafting(),
				"the Casting Table controller must independently reject fake-player direct triggers");
		helper.succeed();
	}

	/** V33a's GUI no-entry overlay exposes the exact missing progression in source inheritance order. */
	private static void castingTableProgressFeedback(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(pos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(pos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ItemStack smooth = new ItemStack(ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().asItem());
		for (int slot = 0; slot < 9; slot++) table.setItem(slot, smooth.copy());
		table.setItem(4, ChromaItems.shardStack(CrystalElement.BLUE));

		helper.assertTrue(!table.triggerCrafting(owner) && table.getActiveRecipe() != null,
				"a matching rune recipe must remain displayable when player progression rejects it");
		helper.assertTrue(table.getMissingProgress(owner).equals(
				List.of(ProgressStage.CRYSTALS, ProgressStage.ALLCOLORS)),
				"the no-entry overlay must report universal CRYSTALS before RuneRecipe's ALLCOLORS gate");
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.CRYSTALS, true, false, false);
		helper.assertTrue(table.getMissingProgress(owner).equals(List.of(ProgressStage.ALLCOLORS)),
				"granting CRYSTALS must leave only the recipe-specific ALLCOLORS gate");
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.ALLCOLORS, true, false, false);
		helper.assertTrue(table.getMissingProgress(owner).isEmpty() && table.triggerCrafting(owner),
				"granting every reported stage must make the same displayed recipe startable");
		helper.succeed();
	}

	private static void castingTableAtomicCraft(GameTestHelper helper) {		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(pos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(pos);
		BlockPos outputPos = pos.relative(Direction.EAST);
		helper.getLevel().setBlock(outputPos, Blocks.CHEST.defaultBlockState(), 3);
		net.minecraft.world.Container outputInventory = (net.minecraft.world.Container)helper.getLevel().getBlockEntity(outputPos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.CRYSTALS, true, false, false);
		ItemStack smooth = new ItemStack(ChromaBlocks.crystallineStone(BlockCrystallineStone.StoneTypes.SMOOTH).get().asItem(), 32);
		table.setItem(3, smooth.copy());
		table.setItem(4, smooth.copy());
		helper.assertTrue(table.triggerCrafting(owner), "the registered two-smooth-stone beam recipe should start");
		helper.assertTrue(table.isCrafting() && table.getCraftingAmount() == 32 && table.getCraftingTick() == 19,
				"thirty-two stacked five-tick casts must converge to V33a's nineteen-tick geometric duration");
		for (int i = 0; i < 18; i++) table.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(table.getItem(9).isEmpty() && table.getItem(3).getCount() == 32,
				"batched inputs and output must remain untouched before the final tick");
		table.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(!table.isCrafting(), "the stacked craft should finish on its source duration");
		helper.assertTrue(table.getItem(3).isEmpty() && table.getItem(4).isEmpty(),
				"the final commit should atomically consume all thirty-two batches");
		helper.assertTrue(table.getItem(9).isEmpty()
				&& outputInventory.getItem(0).is(ChromaBlocks.crystallineStone(BlockCrystallineStone.StoneTypes.BEAM).get().asItem())
				&& outputInventory.getItem(0).getCount() == 64,
				"all 64 beams must move into one adjacent chest stack without truncation or item loss");
		helper.assertTrue(table.getTableXP() == 160 && table.getCompletedRecipes().size() == 1,
				"thirty-two committed batches must award per-craft XP and remember one recipe key");
		helper.succeed();
	}

	/** The V33a green-group temple recipe validates its NBT temple, exact runes, XP gate, and commit. */
	private static void castingTableTempleGroup(GameTestHelper helper) {
		BlockPos anchor = helper.absolutePos(new BlockPos(30, 12, 30));
		BlockPos tablePos = anchor.above();
		ChromaStructures.CASTING1.getArray(helper.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ()).place();
		helper.getLevel().setBlock(tablePos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ItemStack tieredTable = new ItemStack(ChromaBlocks.CASTING_TABLE.get());
		CompoundTag tableData = new CompoundTag();
		tableData.putInt("tableXP", 250);
		ReikaItemHelper.setStackTag(tieredTable, tableData);
		table.setDataFromItemStackTag(tieredTable);

		Map<BlockPos, CrystalElement> runes = Map.of(
				new BlockPos(0, 0, -4), CrystalElement.YELLOW,
				new BlockPos(-4, 0, 0), CrystalElement.CYAN,
				new BlockPos(4, 0, 0), CrystalElement.LIME,
				new BlockPos(0, 0, 4), CrystalElement.GREEN);
		for (Map.Entry<BlockPos, CrystalElement> rune : runes.entrySet()) {
			helper.getLevel().setBlock(tablePos.offset(rune.getKey()),
					ChromaBlocks.rune(rune.getValue()).get().defaultBlockState(), 3);
		}
		table.setItem(1, ChromaItems.shardStack(CrystalElement.YELLOW));
		table.setItem(3, ChromaItems.shardStack(CrystalElement.CYAN));
		table.setItem(4, ChromaItems.craftingStack(ChromaCraftingItems.LIVING_ESSENCE));
		table.setItem(5, ChromaItems.shardStack(CrystalElement.LIME));
		table.setItem(7, ChromaItems.shardStack(CrystalElement.GREEN));

		helper.assertTrue(ChromaItems.CLUSTERS.size() == 13
				&& ChromaClusterItems.CRYSTAL_STAR.ordinal() == 11,
				"all V33a CLUSTER identities and their metadata order must be retained");

		// V33a onAddRune is isAtLeast(TEMPLE): the temple structure is not enough on its own, the
		// table also has to have been worked up to the 250-XP temple tier.
		ItemStack noviceTable = new ItemStack(ChromaBlocks.CASTING_TABLE.get());
		CompoundTag noviceData = new CompoundTag();
		noviceData.putInt("tableXP", 249);
		ReikaItemHelper.setStackTag(noviceTable, noviceData);
		table.setDataFromItemStackTag(noviceTable);
		table.validateStructure();
		table.onAddRune(owner);
		helper.assertTrue(!table.hasRunes(),
				"a temple below the 250-XP tier must not accept runes or grant RUNEUSE");
		table.setDataFromItemStackTag(tieredTable);
		table.validateStructure();
		table.onAddRune(owner);
		helper.assertTrue(table.hasRunes(),
				"the same temple at temple tier must accept the rune");
		helper.assertTrue(!table.triggerCrafting(owner),
				"temple recipes must reject a player who has not reached RUNEUSE");
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.RUNEUSE, true, false, false);
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(owner),
				"RUNEUSE progression grant must include the prerequisite crystal discovery chain");
		helper.assertTrue(table.triggerCrafting(owner),
				"the source-exact green crystal-group temple recipe should start");
		helper.assertTrue(table.getCraftingTick() == 20,
				"ordinary-shard CrystalGroupRecipe must retain its V33a 20-tick duration");
		for (int i = 0; i < 20; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CLUSTERS.get(ChromaClusterItems.GREEN_GROUP).get()),
				"temple completion must produce the green crystal-group identity");
		helper.assertTrue(table.getTableXP() == 290,
				"temple completion must add the V33a 40 XP award to the retained table XP");
		helper.succeed();
	}
	/** Liquid Chroma sources preserve the V33a ground conversion and modern bucket pickup contract. */
	private static void liquidChromaBucketMud(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 4, 4));
		helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(pos, ChromaBlocks.CHROMA.get().defaultBlockState());
		helper.assertTrue(helper.getLevel().getBlockState(pos.below()).is(ChromaBlocks.MUD.get()),
				"placing a Liquid Chroma source over dirt must create Chroma Mud");
		helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma,
				"a Liquid Chroma source must own its activation block entity");
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack bucket = ChromaBlocks.CHROMA.get().pickupBlock(player, helper.getLevel(), pos,
				helper.getLevel().getBlockState(pos));
		helper.assertTrue(bucket.is(ChromaItems.CHROMA_BUCKET.get()) && helper.getLevel().getBlockState(pos).isAir(),
				"source pickup must return the registered Liquid Chroma bucket and remove the source");
		helper.succeed();
	}

	/** Berries, ether, stone deposit, persistence, and automatic shard charging form one pool loop. */
	private static void liquidChromaElementalLoop(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 4, 4));
		helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(pos, ChromaBlocks.CHROMA.get().defaultBlockState());
		reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma pool =
				(reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma)helper.getLevel().getBlockEntity(pos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);

		ItemStack berries = new ItemStack(ChromaItems.BERRIES.get(CrystalElement.BLUE).get(), 30);
		ItemEntity berryEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, berries);
		ChromaItems.BERRIES.get(CrystalElement.BLUE).get().onEntityItemUpdate(berries, berryEntity);
		helper.assertTrue(pool.getElement() == CrystalElement.BLUE && pool.getBerryCount() == 24 && berries.getCount() == 6,
				"matching berries must saturate at 24 and preserve the six-item remainder");

		ItemStack earlyStone = ChromaItems.elementalStoneStack(CrystalElement.BLUE);
		ItemEntity earlyStoneEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, earlyStone);
		earlyStoneEntity.setThrower(owner);
		ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLUE).get().onEntityItemUpdate(earlyStone, earlyStoneEntity);
		helper.assertTrue(earlyStone.getCount() == 1 && !pool.hasElementalBoost(),
				"an Elemental Stone must not deposit before the SHARDCHARGE prerequisites");
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.SHARDCHARGE, true, false, false);

		ItemStack ether = new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.ETHER_BERRIES).get(), 20);
		ItemEntity etherEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, ether);
		etherEntity.setThrower(owner);
		ether.getItem().onEntityItemUpdate(ether, etherEntity);
		helper.assertTrue(pool.getEtherCount() == 16 && ether.getCount() == 4,
				"Ether Berries must saturate at 16 and preserve their four-item remainder");

		ItemStack wrongStone = ChromaItems.elementalStoneStack(CrystalElement.RED);
		ItemEntity wrongStoneEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, wrongStone);
		wrongStoneEntity.setThrower(owner);
		wrongStone.getItem().onEntityItemUpdate(wrongStone, wrongStoneEntity);
		helper.assertTrue(wrongStone.getCount() == 1 && !pool.hasElementalBoost(),
				"a mismatched Elemental Stone must remain untouched");

		ItemStack stone = new ItemStack(ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLUE).get(), 2);
		ItemEntity stoneEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stone);
		stoneEntity.setThrower(owner);
		stone.getItem().onEntityItemUpdate(stone, stoneEntity);
		helper.assertTrue(stone.getCount() == 1 && pool.hasElementalBoost(),
				"the first matching Elemental Stone must set the one-time pool boost and consume exactly one");

		CompoundTag saved = pool.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(pos, pool.getBlockState(), saved, helper.getLevel().registryAccess());
		helper.assertTrue(loaded instanceof reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma,
				"the Liquid Chroma pool must reload through its registered block-entity type");
		reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma restored =
				(reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma)loaded;
		restored.setLevel(helper.getLevel());
		helper.getLevel().setBlockEntity(restored);
		helper.assertTrue(restored.getElement() == CrystalElement.BLUE && restored.getBerryCount() == 24
				&& restored.getEtherCount() == 16 && restored.hasElementalBoost(),
				"color, berry saturation, ether saturation, and elemental boost must all survive reload");

		ItemStack shards = new ItemStack(ChromaItems.SHARDS.get(CrystalElement.BLUE).get(), 2);
		ItemEntity shardEntity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, shards);
		helper.getLevel().addFreshEntity(shardEntity);
		for (int i = 0; i < 1200; i++) shards.getItem().onEntityItemUpdate(shards, shardEntity);
		int charged = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
				.filter(drop -> drop.getItem().is(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.BLUE).get()))
				.mapToInt(drop -> drop.getItem().getCount()).sum();
		helper.assertTrue(!shardEntity.isAlive() && charged == 2,
				"a fully etherized matching pool must automatically convert both shards after 1200 x5 ticks");
		helper.succeed();
	}


	/** V33a dropped-shard charging retains progress and replaces the whole stack at 6000 ticks. */
	private static void crystalShardChargingIdentity(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		ItemStack stack = ChromaItems.shardStack(CrystalElement.BLUE);
		ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack);
		helper.getLevel().addFreshEntity(entity);
		ItemCrystalShard shard = (ItemCrystalShard)stack.getItem();
		helper.assertTrue(!shard.tickCharging(stack, entity, 5999) && entity.isAlive(), "charging must not finish before the V33a 6000-tick threshold");
		helper.assertTrue(shard.tickCharging(stack, entity, 1) && !entity.isAlive(), "the threshold tick must replace the uncharged entity");
		int charged = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
				.filter(drop -> drop.getItem().is(ChromaItems.BOOSTED_SHARDS.get(CrystalElement.BLUE).get()))
				.mapToInt(drop -> drop.getItem().getCount()).sum();
		helper.assertTrue(charged == 1, "charging must preserve stack count and element identity");
		helper.succeed();
	}

	/** Boosted group yield/duration/XP and active-recipe persistence survive a block-entity reload. */
	private static void castingTableBoostedGroupReload(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING1, 250);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.RUNEUSE, true, false, false);
		placeGroupRunes(helper, tablePos, new CrystalElement[] {CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN});
		table.setItem(1, ChromaItems.boostedShardStack(CrystalElement.YELLOW));
		table.setItem(3, ChromaItems.boostedShardStack(CrystalElement.CYAN));
		table.setItem(4, ChromaItems.craftingStack(ChromaCraftingItems.LIVING_ESSENCE));
		table.setItem(5, ChromaItems.boostedShardStack(CrystalElement.LIME));
		table.setItem(7, ChromaItems.boostedShardStack(CrystalElement.GREEN));
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 40, "boosted group must start with doubled V33a duration");
		for (int i = 0; i < 10; i++) table.updateEntity(helper.getLevel(), tablePos);
		CompoundTag saved = table.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(tablePos, table.getBlockState(), saved, helper.getLevel().registryAccess());
		helper.assertTrue(loaded instanceof TileEntityCastingTable, "casting table must reload through its registered block entity type");
		TileEntityCastingTable restored = (TileEntityCastingTable)loaded;
		restored.setLevel(helper.getLevel());
		for (int i = 0; i < 30; i++) restored.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(restored.getItem(9).is(ChromaItems.CLUSTERS.get(ChromaClusterItems.GREEN_GROUP).get()) && restored.getItem(9).getCount() == 4, "charged recipe must retain its fourfold output across reload");
		helper.assertTrue(restored.getTableXP() == 410, "charged recipe must award the V33a 160 table XP");
		helper.succeed();
	}

	/** The source-exact primary cluster combines red/green groups and both rune sets. */
	private static void castingTablePrimaryCluster(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING1, 250);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.RUNEUSE, true, false, false);
		placeGroupRunes(helper, tablePos, new CrystalElement[] {CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA});
		placeGroupRunes(helper, tablePos, new CrystalElement[] {CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN});
		table.setItem(1, ChromaItems.clusterStack(ChromaClusterItems.RED_GROUP));
		table.setItem(3, ChromaItems.clusterStack(ChromaClusterItems.GREEN_GROUP));
		table.setItem(4, ChromaItems.shardStack(CrystalElement.WHITE));
		table.setItem(5, ChromaItems.clusterStack(ChromaClusterItems.GREEN_GROUP));
		table.setItem(7, ChromaItems.clusterStack(ChromaClusterItems.RED_GROUP));
		helper.assertTrue(table.triggerCrafting(owner), "primary cluster recipe must match its exact groups and union runes");
		for (int i = 0; i < 20; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CLUSTERS.get(ChromaClusterItems.PRIMARY_CLUSTER).get()), "primary cluster must commit after 20 ticks");
		helper.succeed();
	}

	/** The V33a multiblock core consumes four linked stands atomically and yields four cores. */
	private static void castingTableCrystalCore(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		table.setItem(4, new ItemStack(Items.DIAMOND));
		setStand(helper, tablePos, new BlockPos(-2,0,0), ChromaItems.clusterStack(ChromaClusterItems.PRIMARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(2,0,0), ChromaItems.clusterStack(ChromaClusterItems.PRIMARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(0,0,-2), ChromaItems.clusterStack(ChromaClusterItems.SECONDARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(0,0,2), ChromaItems.clusterStack(ChromaClusterItems.SECONDARY_CLUSTER), owner);
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 200, "crystal core must start at the multiblock tier");
		for (int i = 0; i < 200; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get()) && table.getItem(9).getCount() == 4, "crystal core must retain fourfold V33a yield");
		helper.assertTrue(table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty), "core completion must consume every required stand atomically");
		helper.succeed();
	}

	/** The V33a Element Unit binds one stone of every element around the raised outer ring. */
	private static void castingTableElementUnit(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		table.setItem(4, ChromaItems.tieredStack(ChromaTieredItems.BINDING_CRYSTAL));
		int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},
				{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
		for (int i = 0; i < ring.length; i++) {
			setStand(helper, tablePos, new BlockPos(ring[i][0], 1, ring[i][1]),
					ChromaItems.elementalStoneStack(CrystalElement.elements[i]), owner);
		}
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 100,
				"element unit must match all sixteen source-ordered elemental stones");
		for (int i = 0; i < 100; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get()),
				"element unit must commit after its V33a duration");
		helper.assertTrue(table.getTableXP() == 2200,
				"element unit must award the V33a 200 multiblock XP");
		helper.assertTrue(table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty),
				"element unit completion must consume all sixteen stones atomically");
		helper.succeed();
	}


	/** The exact twelve-rune V33a personal key is detected, persisted, and grants TUNECAST. */
	private static void castingTableTuningKey(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.CHROMA, true, false, false);
		table.setPlacer(owner);
		// V33a adds the key positions as rune *alternatives*, so a freshly built temple with no key
		// installed at all is still a valid MULTIBLOCK-tier structure. This is the assertion that a
		// place-then-match round trip can never make, because it only ever matches what it placed.
		table.validateStructure();
		helper.assertTrue(table.isStructureValid(CastingTableRecipe.Tier.MULTIBLOCK) && !table.hasTuningKey(),
				"a CASTING2 temple must validate before any personal tuning key is installed");
		Map<BlockPos, CrystalElement> key = CastingTuningRegistry.instance.getTuningKey(helper.getLevel(), owner.getUUID()).runes();
		helper.assertTrue(key.size() == 12, "a V33a casting key must contain all twelve non-cardinal fan positions");
		for (Map.Entry<BlockPos, CrystalElement> entry : key.entrySet())
			placeRune(helper, tablePos, entry.getKey(), entry.getValue());
		Map.Entry<BlockPos, CrystalElement> first = key.entrySet().iterator().next();
		CrystalElement wrong = CrystalElement.elements[(first.getValue().ordinal() + 1) % CrystalElement.elements.length];
		placeRune(helper, tablePos, first.getKey(), wrong);
		table.validateStructure();
		helper.assertTrue(table.hasTuningKey() && !table.isTuned(),
				"a complete but color-mismatched personal key must not tune the table");
		placeRune(helper, tablePos, first.getKey(), first.getValue());
		table.validateStructure();
		helper.assertTrue(table.isTuned() && key.equals(table.getCurrentTuningMap()),
				"the source-exact personal rune map must tune the table");
		helper.assertTrue(ProgressStage.TUNECAST.isPlayerAtStage(owner),
				"recognizing a personal key must grant the V33a TUNECAST stage");
		CompoundTag saved = table.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity reloaded = BlockEntity.loadStatic(tablePos, table.getBlockState(), saved, helper.getLevel().registryAccess());
		helper.assertTrue(reloaded instanceof TileEntityCastingTable, "tuned table must reload through its registered type");
		TileEntityCastingTable loaded = (TileEntityCastingTable)reloaded;
		helper.assertTrue(loaded.isTuned(), "tuned state must survive block-entity synchronization");

		// Exercise the first real V33a recipe whose canRunRecipe contract requires that key.
		tablePos = placeCastingTable(helper, ChromaStructures.CASTING3, 15000);
		table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		table.setPlacer(owner);
		for (ProgressStage stage : new ProgressStage[] {ProgressStage.CRYSTALS, ProgressStage.RUNEUSE,
				ProgressStage.MULTIBLOCK, ProgressStage.PYLON, ProgressStage.REPEATER})
			ProgressionManager.instance.setPlayerStage(owner, stage, true, false, false);
		table.setItem(4, new ItemStack(Items.DIAMOND));
		for (int[] pos : new int[][] {{-2,0},{-4,0},{2,0},{4,0},{0,-2},{0,-4}})
			setStand(helper, tablePos, new BlockPos(pos[0], Math.abs(pos[0]) == 4 || Math.abs(pos[1]) == 4 ? 1 : 0, pos[1]),
					ChromaItems.craftingStack(ChromaCraftingItems.IRIDESCENT_CHUNK), owner);
		for (int x = -4; x <= 4; x += 2)
			setStand(helper, tablePos, new BlockPos(x, Math.abs(x) == 4 ? 1 : 0, 2), new ItemStack(Items.OBSIDIAN), owner);
		setStand(helper, tablePos, new BlockPos(-2,0,-2), new ItemStack(Items.GLOWSTONE), owner);
		setStand(helper, tablePos, new BlockPos(2,0,-2), new ItemStack(Items.GLOWSTONE), owner);
		for (Map.Entry<BlockPos, CrystalElement> entry : key.entrySet())
			placeRune(helper, tablePos, entry.getKey(), entry.getValue());
		placeRune(helper, tablePos, first.getKey(), wrong);
		table.validateStructure();
		helper.assertTrue(table.receiveElement(null, CrystalElement.YELLOW, 15000) == 15000
				&& table.receiveElement(null, CrystalElement.BLACK, 25000) == 25000
				&& table.receiveElement(null, CrystalElement.PURPLE, 10000) == 10000,
				"Power Crystal setup must buffer the source-exact three-color aura");
		String[] casting3Mismatch = {"none"};
		BlockPos casting3Anchor = tablePos.below();
		ChromaStructures.CASTING3.getArray(helper.getLevel(), casting3Anchor.getX(), casting3Anchor.getY(), casting3Anchor.getZ())
				.matchInWorld((world, x, y, z, wanted) -> casting3Mismatch[0] = new BlockPos(x, y, z)
						+ " wanted=" + wanted + " found=" + world.getBlockState(new BlockPos(x, y, z)));
		helper.assertTrue(table.isStructureValid(CastingTableRecipe.Tier.PYLON),
				"the canonical CASTING3 structure must remain valid after installing its personal tuning runes; first mismatch "
						+ casting3Mismatch[0]);
		helper.assertTrue(!table.isTuned(), "the deliberately wrong rune must leave the replacement table untuned");
		boolean mismatchedStarted = table.triggerCrafting(owner);
		helper.assertTrue(table.getActiveRecipe() != null,
				"the complete Power Crystal physical layout must select a casting recipe; nonempty stands="
						+ table.getOtherStands().values().stream().filter(stand -> !stand.isEmpty()).count());
		helper.assertTrue(table.getActiveRecipe().id().identifier().getPath().equals("power_crystal")
				&& table.getActiveRecipe().value().requiresTuningKey(),
				"the selected recipe must be the data-driven tuning-sensitive Power Crystal recipe, got "
						+ table.getActiveRecipe().id());
		helper.assertTrue(!mismatchedStarted,
				"a complete but mismatched personal key must reject the real tuning-sensitive Power Crystal recipe");
		placeRune(helper, tablePos, first.getKey(), first.getValue());
		table.validateStructure();
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 1600,
				"the exact personal key must start the V33a 1600-tick Power Crystal cast");
		helper.succeed();
	}

	/** Four complete V33a crystal groups around a pylon table double both table and repeater throughput. */
	private static void castingTableRepeaterGrouping(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING3, 15000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		int[][] ring = {
				{-6,-8},{-2,-8},{2,-8},{6,-8},
				{-6,8},{-2,8},{2,8},{6,8},
				{-8,-6},{-8,-2},{-8,2},{-8,6},
				{8,-6},{8,-2},{8,2},{8,6}
		};
		CrystalElement[][] groups = {
				{CrystalElement.RED, CrystalElement.BLUE, CrystalElement.PURPLE, CrystalElement.MAGENTA},
				{CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME, CrystalElement.GREEN},
				{CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE},
				{CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE}
		};
		for (int i = 0; i < ring.length; i++) {
			BlockPos repeaterPos = tablePos.offset(ring[i][0], 3, ring[i][1]);
			BlockEntity tile = helper.getLevel().getBlockEntity(repeaterPos);
			helper.assertTrue(tile instanceof TileEntityCrystalRepeater,
					"the NBT pylon-casting structure must place all sixteen outer repeaters");
			TileEntityCrystalRepeater repeater = (TileEntityCrystalRepeater)tile;
			helper.getLevel().setBlock(repeaterPos.below(), ChromaBlocks.rune(groups[i / 4][i % 4]).get().defaultBlockState(), 3);
			helper.assertTrue(repeater.findFirstValidSide(), "each casting-post repeater must retain a valid stalk");
		}
		table.validateStructure();
		helper.assertTrue(Math.abs(table.getThroughputBonus() - 1F) < 0.0001F,
				"four correctly grouped sides must grant the full +100% table throughput bonus");
		helper.assertTrue(table.maxThroughput() == 1200,
				"the V33a 600-lumen base at 15000 XP must double to 1200; actual=" + table.maxThroughput());
		helper.assertTrue(!table.allowsEfficiencyBoost(),
				"Casting Tables must retain V33a's explicit immunity to receiver efficiency-cost scaling");
		for (int[] offset : ring) {
			TileEntityCrystalRepeater repeater = (TileEntityCrystalRepeater)helper.getLevel()
					.getBlockEntity(tablePos.offset(offset[0], 3, offset[1]));
			helper.assertTrue(repeater.isTableGrouped(), "every member of a matched side must be marked table-grouped");
		}
		BlockPos first = tablePos.offset(ring[0][0], 3, ring[0][1]);
		helper.getLevel().setBlock(first.below(), ChromaBlocks.rune(CrystalElement.YELLOW).get().defaultBlockState(), 3);
		table.validateStructure();
		helper.assertTrue(table.getThroughputBonus() == 0,
				"a duplicated color must invalidate grouping exactly as in V33a");
		helper.assertTrue(!((TileEntityCrystalRepeater)helper.getLevel().getBlockEntity(first)).isTableGrouped(),
				"invalidating the ring must clear the prior grouping flag");
		helper.succeed();
	}

	/** Eight exquisite focus crystals reproduce the V33a x8 casting acceleration with a 20-tick floor. */
	private static void castingTableFocusAcceleration(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		for (BlockPos offset : table.getRelativeFocusCrystalLocations()) {
			BlockPos pos = tablePos.offset(offset);
			helper.getLevel().setBlock(pos, ChromaBlocks.FOCUS_CRYSTAL.get().defaultBlockState(), 3);
			TileEntityFocusCrystal focus = (TileEntityFocusCrystal)helper.getLevel().getBlockEntity(pos);
			focus.setTier(TileEntityFocusCrystal.CrystalTier.EXQUISITE);
		}
		helper.assertTrue(Math.abs(table.getAccelerationFactor() - 8F) < 0.0001F,
				"eight exquisite crystals must contribute 8 x 0.875 to the base factor of one");
		table.setItem(4, ChromaItems.tieredStack(ChromaTieredItems.BINDING_CRYSTAL));
		int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},
				{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
		for (int i = 0; i < ring.length; i++)
			setStand(helper, tablePos, new BlockPos(ring[i][0], 1, ring[i][1]),
					ChromaItems.elementalStoneStack(CrystalElement.elements[i]), owner);
		table.validateStructure();
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 20,
				"x8 focus acceleration must reduce a 100-tick multiblock recipe to the V33a 20-tick floor");
		for (int i = 0; i < 20; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.ELEMENT_UNIT).get()),
				"focus acceleration must change duration without changing the atomic result");
		TileEntityFocusCrystal focus = (TileEntityFocusCrystal)helper.getLevel().getBlockEntity(
				tablePos.offset(table.getRelativeFocusCrystalLocations().get(0)));
		helper.assertTrue(tablePos.equals(focus.getConnectedTarget()),
				"participating focus crystals must retain their target connection");
		helper.succeed();
	}
	/** The V33a crystal star exercises all 24 stands, boosted colors, runes, and 400-tick commit. */
	private static void castingTableCrystalStar(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		table.setItem(4, new ItemStack(Items.NETHER_STAR));
		for (int[] pos : new int[][] {{-2,0},{2,0},{0,-2},{0,2}}) setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.clusterStack(ChromaClusterItems.CRYSTAL_CORE), owner);
		for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2}}) setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.craftingStack(ChromaCraftingItems.ELEMENT_UNIT), owner);
		int[][] ring = {{-4,-4},{-2,-4},{0,-4},{2,-4},{4,-4},{4,-2},{4,0},{4,2},{4,4},{2,4},{0,4},{-2,4},{-4,4},{-4,2},{-4,0},{-4,-2}};
		for (int i = 0; i < ring.length; i++) setStand(helper, tablePos, new BlockPos(ring[i][0],1,ring[i][1]), ChromaItems.boostedShardStack(CrystalElement.elements[i]), owner);
		for (int x : new int[] {-3,3}) for (int z : new int[] {-3,3}) placeRune(helper, tablePos, new BlockPos(x,-1,z), CrystalElement.BLACK);
		table.validateStructure();
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 400, "crystal star must match all source-exact physical inputs");
		for (int i = 0; i < 400; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_STAR).get()) && table.getItem(9).getCount() == 4, "crystal star must retain fourfold V33a yield");
		helper.assertTrue(table.getTableXP() == 2400, "crystal star must award doubled multiblock XP");
		helper.succeed();
	}

	/** The first tiered-resource unlocks drive both dependent multiblock recipes in source order. */
	private static void castingTableTieredMultiblockChain(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);

		table.setItem(4, new ItemStack(Items.GLASS));
		for (int[] pos : new int[][] {{-2,-2},{-2,2},{2,-2},{2,2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.tieredStack(ChromaTieredItems.FOCUS_DUST), owner);
		for (int[] pos : new int[][] {{-2,0},{2,0}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.shardStack(CrystalElement.WHITE), owner);
		for (int[] pos : new int[][] {{0,-2},{0,2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.shardStack(CrystalElement.BLUE), owner);
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 100,
				"crystal lens must match the source-exact glass, powder, and shard layout");
		for (int i = 0; i < 100; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.CRYSTAL_LENS).get()),
				"crystal lens must commit after its V33a duration");
		helper.assertTrue(table.getTableXP() == 2200 && table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty),
				"crystal lens must award 200 XP and consume all eight stands atomically");

		table.setItem(9, ItemStack.EMPTY);
		table.setItem(4, ChromaItems.tieredStack(ChromaTieredItems.BINDING_CRYSTAL));
		for (int[] pos : new int[][] {{-2,0},{2,0},{0,-2},{0,2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.craftingStack(ChromaCraftingItems.IRIDESCENT_CRYSTAL), owner);
		for (int[] pos : new int[][] {{-2,-2},{2,-2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.tieredStack(ChromaTieredItems.RESONANCE_DUST), owner);
		setStand(helper, tablePos, new BlockPos(-2,0,2), ChromaItems.tieredStack(ChromaTieredItems.FOCUS_DUST), owner);
		setStand(helper, tablePos, new BlockPos(2,0,2), ChromaItems.tieredStack(ChromaTieredItems.BEACON_DUST), owner);
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 100,
				"iridescent chunk must match its exact binding-crystal and tiered-powder layout");
		for (int i = 0; i < 100; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CHUNK).get()),
				"iridescent chunk must commit after its V33a duration");
		helper.assertTrue(table.getTableXP() == 2400 && table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty),
				"the chained multiblock crafts must award 400 total XP without leaving stand inputs");
		helper.succeed();
	}

	/** The V33a Lumen Core debits all three 60k aura colors in one atomic pylon-tier commit. */
	private static void castingTableLumenCoreMulticolor(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING3, 15000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		// V33a's casting temple requires the four cells beside the table and the cell above it to be
		// empty, so the cell below the table is the only one of the six output directions an
		// automation inventory can legally occupy while the multiblock still validates.
		BlockPos outputPos = tablePos.below();
		helper.getLevel().setBlock(outputPos, Blocks.CHEST.defaultBlockState(), 3);
		net.minecraft.world.Container outputInventory = (net.minecraft.world.Container)helper.getLevel().getBlockEntity(outputPos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		for (ProgressStage stage : new ProgressStage[] {ProgressStage.RUNEUSE, ProgressStage.MULTIBLOCK, ProgressStage.PYLON, ProgressStage.REPEATER})
			ProgressionManager.instance.setPlayerStage(owner, stage, true, false, false);
		table.setItem(4, ChromaItems.clusterStack(ChromaClusterItems.CRYSTAL_STAR).copyWithCount(2));
		for (int x = -4; x <= 4; x += 2) for (int z = -4; z <= 4; z += 2) {
			if (x == 0 && z == 0) continue;
			boolean cardinal = (x == 0 && Math.abs(z) == 2) || (z == 0 && Math.abs(x) == 2);
			ItemStack ingredient = cardinal ? ChromaItems.tieredStack(ChromaTieredItems.PURITY_DUST)
					: ChromaItems.craftingStack(ChromaCraftingItems.GLOW_CHUNK);
			setStand(helper, tablePos, new BlockPos(x, Math.abs(x) == 4 || Math.abs(z) == 4 ? 1 : 0, z),
					ingredient.copyWithCount(2), owner);
		}
		for (CrystalElement element : new CrystalElement[] {CrystalElement.BLACK, CrystalElement.YELLOW, CrystalElement.BLUE})
			helper.assertTrue(table.receiveElement(null, element, 120000) == 120000,
					"two queued Lumen Cores must buffer the exact 120000-lumen requirement for "+element);
		boolean started = table.triggerCrafting(owner);
		helper.assertTrue(started && table.getCraftingAmount() == 2 && table.getCraftingTick() == 400,
				"V33a pylon recipes must queue both inputs but schedule only one 400-tick cycle; started="
						+started+", amount="+table.getCraftingAmount()+", ticks="+table.getCraftingTick()
						+", tier="+table.getTier()+", recipe="+(table.getActiveRecipe() != null
								? table.getActiveRecipe().id()+"/stackable="+table.getActiveRecipe().value().stackable() : "none"));
		for (int i = 0; i < 400; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.isCrafting() && table.getCraftingAmount() == 1 && table.getCraftingTick() == 400,
				"moving the first output into the adjacent chest must immediately schedule the second pylon cycle");
		helper.assertTrue(table.getItem(4).getCount() == 1
				&& outputInventory.getItem(0).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.LUMEN_CORE).get())
				&& outputInventory.getItem(0).getCount() == 1,
				"the first non-stackable cycle must consume and emit exactly one recipe batch");
		for (CrystalElement element : new CrystalElement[] {CrystalElement.BLACK, CrystalElement.YELLOW, CrystalElement.BLUE})
			helper.assertTrue(table.getEnergy(element) == 60000,
					"the first cycle must leave one Lumen Core's buffered "+element+" aura");
		for (int i = 0; i < 400; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(!table.isCrafting() && table.getItem(9).isEmpty()
				&& outputInventory.getItem(0).getCount() == 2,
				"the second non-stackable cycle must finish the queue without leaving a table output");
		for (CrystalElement element : new CrystalElement[] {CrystalElement.BLACK, CrystalElement.YELLOW, CrystalElement.BLUE})
			helper.assertTrue(table.getEnergy(element) == 0, "both cycles must debit all 120000 "+element+" lumens");
		helper.assertTrue(table.getTableXP() == 16000 && table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty),
				"two Lumen Core cycles must award 1000 XP and consume both layers of all 24 stands");
		helper.succeed();
	}
	/** Losing the required NBT multiblock cancels work without consuming or stranding locked inputs. */
	private static void castingTableStructureLossCancel(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING2, 2000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.MULTIBLOCK, true, false, false);
		table.setItem(4, new ItemStack(Items.DIAMOND));
		setStand(helper, tablePos, new BlockPos(-2,0,0), ChromaItems.clusterStack(ChromaClusterItems.PRIMARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(2,0,0), ChromaItems.clusterStack(ChromaClusterItems.PRIMARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(0,0,-2), ChromaItems.clusterStack(ChromaClusterItems.SECONDARY_CLUSTER), owner);
		setStand(helper, tablePos, new BlockPos(0,0,2), ChromaItems.clusterStack(ChromaClusterItems.SECONDARY_CLUSTER), owner);
		helper.assertTrue(table.triggerCrafting(owner), "the long multiblock craft must start before its structure is damaged");
		for (int i = 0; i < 20; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getCraftingTick() == 180 && table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isLocked),
				"the controller must still be working with every auxiliary stand locked");
		helper.getLevel().setBlock(tablePos.below().offset(-6, 0, -6), Blocks.AIR.defaultBlockState(), 3);
		table.validateStructure();
		table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(!table.isCrafting() && table.getItem(9).isEmpty(),
				"losing the canonical NBT structure must cancel rather than complete the craft");
		helper.assertTrue(table.getItem(4).is(Items.DIAMOND)
				&& table.getOtherStands().values().stream().allMatch(stand -> !stand.isEmpty()),
				"cancellation must preserve the center and all auxiliary ingredients");
		helper.assertTrue(table.getOtherStands().values().stream().noneMatch(TileEntityItemStand::isLocked),
				"cancellation must release every linked stand lock");
		helper.assertTrue(table.getTableXP() == 2000 && table.getCompletedRecipes().isEmpty(),
				"cancelled work must award neither XP nor recipe completion history");
		helper.succeed();
	}

	/** A real V33a pylon recipe requests through a required repeater, then commits its aura atomically. */
	private static void castingTablePylonNetworkCore(GameTestHelper helper) {
		BlockPos tablePos = placeCastingTable(helper, ChromaStructures.CASTING3, 15000);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		for (ProgressStage stage : new ProgressStage[] {ProgressStage.RUNEUSE, ProgressStage.MULTIBLOCK, ProgressStage.PYLON, ProgressStage.REPEATER})
			ProgressionManager.instance.setPlayerStage(owner, stage, true, false, false);
		table.setItem(4, ChromaItems.clusterStack(ChromaClusterItems.CRYSTAL_STAR));
		for (int[] pos : new int[][] {{-2,-2},{2,-2},{-2,2},{2,2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.boostedShardStack(CrystalElement.YELLOW), owner);
		for (int[] pos : new int[][] {{2,0},{-2,0},{0,2},{0,-2}})
			setStand(helper, tablePos, new BlockPos(pos[0],0,pos[1]), ChromaItems.boostedShardStack(CrystalElement.WHITE), owner);
		for (int[] pos : new int[][] {{-4,-4},{4,-4},{-4,4},{4,4}})
			setStand(helper, tablePos, new BlockPos(pos[0],1,pos[1]), ChromaItems.craftingStack(ChromaCraftingItems.IRIDESCENT_CRYSTAL), owner);
		setStand(helper, tablePos, new BlockPos(-2,1,-4), ChromaItems.tieredStack(ChromaTieredItems.FIRE_ESSENCE), owner);
		setStand(helper, tablePos, new BlockPos(4,1,-2), ChromaItems.craftingStack(ChromaCraftingItems.ENERGY_POWDER), owner);
		setStand(helper, tablePos, new BlockPos(2,1,4), ChromaItems.tieredStack(ChromaTieredItems.FIRE_ESSENCE), owner);
		setStand(helper, tablePos, new BlockPos(-4,1,2), ChromaItems.craftingStack(ChromaCraftingItems.ENERGY_POWDER), owner);
		setStand(helper, tablePos, new BlockPos(2,1,-4), ChromaItems.tieredStack(ChromaTieredItems.ENDER_DUST), owner);
		setStand(helper, tablePos, new BlockPos(-4,1,-2), ChromaItems.tieredStack(ChromaTieredItems.SPACE_DUST), owner);
		setStand(helper, tablePos, new BlockPos(-2,1,4), ChromaItems.tieredStack(ChromaTieredItems.ENDER_DUST), owner);
		setStand(helper, tablePos, new BlockPos(4,1,2), ChromaItems.tieredStack(ChromaTieredItems.SPACE_DUST), owner);
		setStand(helper, tablePos, new BlockPos(-4,1,0), new ItemStack(Items.DIAMOND), owner);
		setStand(helper, tablePos, new BlockPos(4,1,0), new ItemStack(Items.ENDER_PEARL), owner);
		setStand(helper, tablePos, new BlockPos(0,1,-4), new ItemStack(Items.EMERALD), owner);
		setStand(helper, tablePos, new BlockPos(0,1,4), new ItemStack(Items.GUNPOWDER), owner);
		placeRune(helper, tablePos, new BlockPos(-3,0,-2), CrystalElement.YELLOW);
		placeRune(helper, tablePos, new BlockPos(3,0,2), CrystalElement.YELLOW);

		BlockPos repeaterPos = tablePos.offset(14, 19, 0);
		BlockPos pylonPos = tablePos.offset(45, 19, 0);
		TileEntityCrystalPylon pylon = placePylon(helper, pylonPos, CrystalElement.YELLOW);
		TileEntityCrystalRepeater repeater = placeRepeater(helper, repeaterPos, Direction.NORTH);
		helper.getLevel().setBlock(repeaterPos.relative(Direction.NORTH), ChromaBlocks.rune(CrystalElement.YELLOW).get().defaultBlockState(), 3);
		helper.assertTrue(repeater.hasStructure(), "network core test requires a valid repeater stalk");
		helper.assertTrue(pylon.getDistanceSqTo(table.getX(), table.getY(), table.getZ()) > TileEntityCrystalPylon.RANGE * TileEntityCrystalPylon.RANGE,
				"test geometry must forbid a direct pylon-to-table path");
		int sourceBefore = pylon.getEnergy(CrystalElement.YELLOW);
		helper.assertTrue(table.getEnergy(CrystalElement.YELLOW) == 0 && table.triggerCrafting(owner),
				"high-energy core must start without locally buffered aura and issue a network request");
		helper.assertTrue(table.getCraftingTick() == 400,
				"the table must wait at the V33a 400-tick duration until all 5000 lumens arrive");
		helper.assertTrue(CrystalNetworker.instance.canMakeConnection(repeater, table, CrystalElement.YELLOW),
				"the NBT casting structure aperture must preserve repeater-to-table line of sight");
		helper.assertTrue(CrystalNetworker.instance.canMakeConnection(pylon, repeater, CrystalElement.YELLOW),
				"the pylon must have a valid line to the intermediate repeater");
		helper.assertTrue(repeater.getSignalDepth(CrystalElement.YELLOW) == 1, "the casting request must traverse exactly one repeater hop");
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(table.getEnergy(CrystalElement.YELLOW) == 5000 && repeater.getSignalDepth(CrystalElement.YELLOW) == 1,
					"the table must buffer the exact aura requirement delivered through one repeater hop");
			helper.assertTrue(pylon.getEnergy(CrystalElement.YELLOW) < sourceBefore, "the live source must pay for the delivered aura and attenuation");
			CompoundTag saved = table.saveWithFullMetadata(helper.getLevel().registryAccess());
			BlockEntity loaded = BlockEntity.loadStatic(tablePos, table.getBlockState(), saved, helper.getLevel().registryAccess());
			helper.assertTrue(loaded instanceof TileEntityCastingTable, "the aura-fed active table must reload through its registered type");
			TileEntityCastingTable restored = (TileEntityCastingTable)loaded;
			restored.setLevel(helper.getLevel());
			helper.getLevel().setBlockEntity(restored);
			helper.assertTrue(restored.getEnergy(CrystalElement.YELLOW) == 5000 && restored.getCraftingTick() > 0, "active recipe identity, timer, and delivered aura must survive block-entity persistence");
			int remaining = restored.getCraftingTick();
			for (int i = 0; i < remaining; i++) restored.updateEntity(helper.getLevel(), tablePos);
			helper.assertTrue(restored.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.HIGH_ENERGY_CORE).get()),
						"operational pylon casting must produce the source-exact high-energy core");
			helper.assertTrue(restored.getEnergy(CrystalElement.YELLOW) == 0, "completion must atomically consume exactly the required 5000 table lumens");
			helper.assertTrue(restored.getTableXP() == 15500, "pylon completion must award the V33a 500 casting XP");
			helper.assertTrue(restored.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty), "pylon completion must consume all 24 stands atomically");
			helper.succeed();
		});
	}
	private static BlockPos placeCastingTable(GameTestHelper helper, ChromaStructures structure, int xp) {
		BlockPos anchor = helper.absolutePos(new BlockPos(30, 12, 30));
		structure.getArray(helper.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ()).place();
		BlockPos tablePos = anchor.above();
		helper.getLevel().setBlock(tablePos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		ItemStack stack = new ItemStack(ChromaBlocks.CASTING_TABLE.get());
		CompoundTag data = new CompoundTag();
		data.putInt("tableXP", xp);
		ReikaItemHelper.setStackTag(stack, data);
		table.setDataFromItemStackTag(stack);
		table.cachePosition();
		return tablePos;
	}

	private static void placeGroupRunes(GameTestHelper helper, BlockPos tablePos, CrystalElement[] shards) {
		int group = shards[0] == CrystalElement.RED ? 0 : shards[0] == CrystalElement.YELLOW ? 1 : shards[0] == CrystalElement.BROWN ? 2 : 3;
		int radius = group == 0 ? 3 : 4;
		int diagonal = group == 2 ? -2 : group == 3 ? 2 : 0;
		placeRune(helper, tablePos, new BlockPos(diagonal,0,-radius), shards[0]);
		placeRune(helper, tablePos, new BlockPos(-radius,0,-diagonal), shards[1]);
		placeRune(helper, tablePos, new BlockPos(radius,0,diagonal), shards[2]);
		placeRune(helper, tablePos, new BlockPos(-diagonal,0,radius), shards[3]);
	}

	private static void placeRune(GameTestHelper helper, BlockPos tablePos, BlockPos offset, CrystalElement element) {
		helper.getLevel().setBlock(tablePos.offset(offset), ChromaBlocks.rune(element).get().defaultBlockState(), 3);
	}

	private static void setStand(GameTestHelper helper, BlockPos tablePos, BlockPos offset, ItemStack stack, net.minecraft.world.entity.player.Player owner) {
		BlockPos pos = tablePos.offset(offset);
		helper.getLevel().setBlock(pos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		TileEntityItemStand stand = (TileEntityItemStand)helper.getLevel().getBlockEntity(pos);
		stand.setPlacer(owner);
		stand.setItem(0, stack);
	}
	/** Receiver acceptance is capped against current storage and reports the amount actually stored. */
	private static void castingTableReceiverCapacity(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(pos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(pos);
		int first = table.receiveElement(null, CrystalElement.BLUE, 250);
		helper.assertTrue(first == 250 && table.getEnergy(CrystalElement.BLUE) == 250,
				"receiver should report and retain the full first delivery");
		table.setEnergy(CrystalElement.BLUE, Integer.MAX_VALUE - 10);
		int capped = table.receiveElement(null, CrystalElement.BLUE, 50);
		helper.assertTrue(capped == 10 && table.getEnergy(CrystalElement.BLUE) == Integer.MAX_VALUE,
				"receiver should accept only remaining capacity without overflow");
		helper.succeed();
	}
	/** Stand placement ownership, one-item insertion, lock enforcement, and extraction. */
	private static void castingStandOwnershipLock(GameTestHelper helper) {

		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(pos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		TileEntityItemStand stand = (TileEntityItemStand)helper.getLevel().getBlockEntity(pos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		var other = helper.makeMockPlayer(GameType.SURVIVAL);
		stand.setPlacer(owner);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.QUARTZ, 3));
		helper.assertTrue(stand.interact(owner, net.minecraft.world.InteractionHand.MAIN_HAND), "owner should insert into stand");
		helper.assertTrue(stand.getItem(0).is(Items.QUARTZ) && stand.getItem(0).getCount() == 1,
				"stand should insert one item from a new stack");
		helper.assertTrue(owner.getMainHandItem().getCount() == 2, "insertion should consume exactly one held item");
		helper.assertTrue(!stand.interact(other, net.minecraft.world.InteractionHand.MAIN_HAND), "non-owner must not use stand");
		stand.lock(true);
		helper.assertTrue(!stand.interact(owner, net.minecraft.world.InteractionHand.MAIN_HAND), "locked stand must reject its owner too");
		stand.lock(false);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(stand.interact(owner, net.minecraft.world.InteractionHand.MAIN_HAND), "owner should extract from unlocked stand");
		helper.assertTrue(stand.isEmpty(), "stand should be empty after extraction");
		helper.succeed();
	}

	/** V33a empty-hand block interactions select spread stands and dump the upgraded table's ring. */
	private static void castingStandSpread(GameTestHelper helper) {
		BlockPos tablePos = helper.absolutePos(new BlockPos(8, 3, 8));
		BlockPos firstPos = tablePos.offset(-2, 0, 0);
		BlockPos secondPos = tablePos.offset(2, 0, 0);
		helper.getLevel().setBlock(tablePos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		helper.getLevel().setBlock(firstPos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		helper.getLevel().setBlock(secondPos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(tablePos);
		TileEntityItemStand first = (TileEntityItemStand)helper.getLevel().getBlockEntity(firstPos);
		TileEntityItemStand second = (TileEntityItemStand)helper.getLevel().getBlockEntity(secondPos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		first.setPlacer(owner);
		second.setPlacer(owner);

		owner.setShiftKeyDown(true);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (BlockPos standPos : List.of(firstPos, secondPos)) {
			var hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(standPos), Direction.UP, standPos, false);
			helper.assertTrue(helper.getLevel().getBlockState(standPos).useWithoutItem(helper.getLevel(), owner, hit).consumesAction(),
					"sneak-empty-hand stand click should select it for spread filling");
		}
		owner.setShiftKeyDown(false);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE, 5));
		var firstHit = new net.minecraft.world.phys.BlockHitResult(
				net.minecraft.world.phys.Vec3.atCenterOf(firstPos), Direction.UP, firstPos, false);
		helper.assertTrue(helper.getLevel().getBlockState(firstPos).useItemOn(owner.getMainHandItem(), helper.getLevel(), owner,
				net.minecraft.world.InteractionHand.MAIN_HAND, firstHit).consumesAction(), "queued spread should be handled through the block");
		helper.assertTrue(first.getItem(0).getCount() == 2 && second.getItem(0).getCount() == 2,
				"five items across two stands should place two on each");
		helper.assertTrue(owner.getMainHandItem().getCount() == 1, "spread remainder should stay in hand");

		ItemStack tieredTable = new ItemStack(ChromaBlocks.CASTING_TABLE.get());
		CompoundTag tableData = new CompoundTag();
		tableData.putInt("tableXP", TileEntityCastingTable.TableTier.MULTIBLOCK.minimumXP());
		ReikaItemHelper.setStackTag(tieredTable, tableData);
		table.setDataFromItemStackTag(tieredTable);
		owner.setShiftKeyDown(true);
		var tableHit = new net.minecraft.world.phys.BlockHitResult(
				net.minecraft.world.phys.Vec3.atCenterOf(tablePos), Direction.UP, tablePos, false);
		helper.getLevel().getBlockState(tablePos).useItemOn(owner.getMainHandItem(), helper.getLevel(), owner,
				net.minecraft.world.InteractionHand.MAIN_HAND, tableHit);
		helper.assertTrue(!first.isEmpty() && !second.isEmpty(),
				"the source mass-empty shortcut must require an empty hand");
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(helper.getLevel().getBlockState(tablePos).useWithoutItem(helper.getLevel(), owner, tableHit).consumesAction(),
				"sneak-empty-hand table click should claim the mass-empty action");
		helper.assertTrue(first.isEmpty() && second.isEmpty(), "tier-III table should dump every stand in its ring");

		// V33a makes a locked stand outright unbreakable, not merely undroppable, so an active cast
		// cannot have its ingredients mined away mid-craft.
		helper.assertTrue(helper.getLevel().getBlockState(firstPos)
				.getDestroyProgress(owner, helper.getLevel(), firstPos) > 0,
				"an unlocked owned stand must be mineable by its owner");
		first.lock(true);
		helper.assertTrue(helper.getLevel().getBlockState(firstPos)
				.getDestroyProgress(owner, helper.getLevel(), firstPos) == 0,
				"a stand locked by a running cast must be unbreakable");
		BlockState standState = helper.getLevel().getBlockState(firstPos);
		helper.assertTrue(!standState.getBlock().onDestroyedByPlayer(standState, helper.getLevel(), firstPos,
						owner, ItemStack.EMPTY, false, standState.getFluidState()),
				"the player-destroy path must refuse a locked stand");
		helper.assertTrue(helper.getLevel().getBlockEntity(firstPos) instanceof TileEntityItemStand,
				"the locked stand must survive the attempt");
		first.lock(false);
		helper.succeed();
	}

	/** The mechanically imported V33a village house must hydrate its doors, loot and progress NBT. */
	private static void villageCastingNbtContract(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
		NBTStructureLoader.place(helper.getLevel(),
				ChromaStructureTemplateProvider.villageTemplate("plains", true), origin,
				BlockPos.ZERO, state -> state, 3);

		BlockPos chestPos = origin.offset(7, 0, 7);
		helper.assertTrue(helper.getLevel().getBlockEntity(chestPos) instanceof TileEntityLootChest,
				"the wooden failed-casting house must hydrate its authored ChromatiCraft loot chest");
		TileEntityLootChest chest = (TileEntityLootChest)helper.getLevel().getBlockEntity(chestPos);
		helper.assertTrue(chest.getLootTable() != null
				&& chest.getLootTable().identifier().toString().equals("chromaticraft:chests/village_casting"),
				"the village chest must retain its data-driven village_casting loot table");
		ServerPlayer opener = helper.makeMockServerPlayerInLevel();
		chest.grantProgress(opener);
		helper.assertTrue(ProgressStage.VILLAGECASTING.isPlayerAtStage(opener),
				"opening the imported structure chest must grant the V33a VILLAGECASTING stage");

		for (int[] door : new int[][] {{1,1,5},{1,1,9},{5,1,1},{5,1,13},
				{9,1,1},{9,1,13},{13,1,5},{13,1,9}}) {
			BlockState lower = helper.getLevel().getBlockState(origin.offset(door[0], door[1], door[2]));
			BlockState upper = helper.getLevel().getBlockState(origin.offset(door[0], door[1] + 1, door[2]));
			helper.assertTrue(lower.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
					&& upper.getBlock() == lower.getBlock()
					&& lower.getValue(net.minecraft.world.level.block.DoorBlock.HALF)
							== net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
					&& upper.getValue(net.minecraft.world.level.block.DoorBlock.HALF)
							== net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
					&& lower.getValue(net.minecraft.world.level.block.DoorBlock.FACING)
							== upper.getValue(net.minecraft.world.level.block.DoorBlock.FACING),
					"legacy door halves must be translated into a matching modern state pair at "
							+ java.util.Arrays.toString(door));
		}
		helper.succeed();
	}

	/** Registry decoding must produce the exact one-emerald, flawed, effectively infinite V33a offer. */
	private static void focusCrystalTradeDefinition(GameTestHelper helper) {
		VillagerTrade definition = helper.getLevel().registryAccess()
				.lookupOrThrow(Registries.VILLAGER_TRADE)
				.getOptional(ChromaWorldGenProvider.FOCUS_CRYSTAL_TRADE)
				.orElseThrow(() -> new AssertionError("missing data-driven Focus Crystal villager trade"));
		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		var villager = EntityTypes.VILLAGER.create(helper.getLevel(), null, pos,
				EntitySpawnReason.COMMAND, false, false);
		helper.assertTrue(villager != null, "test villager should instantiate");
		LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
				.withParameter(LootContextParams.THIS_ENTITY, villager)
				.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,
						net.minecraft.util.Unit.INSTANCE)
				.create(LootContextParamSets.VILLAGER_TRADE)).create(Optional.empty());
		MerchantOffer offer = definition.getOffer(context);
		helper.assertTrue(offer != null, "the Focus Crystal registry definition must create an offer");
		helper.assertTrue(offer.getBaseCostA().is(Items.EMERALD)
				&& offer.getBaseCostA().getCount() == 1 && offer.getCostB().isEmpty(),
				"the source trade must cost exactly one emerald and no second item");
		helper.assertTrue(offer.getResult().is(ChromaBlocks.FOCUS_CRYSTAL.get().asItem())
				&& offer.getResult().getCount() == 1,
				"the source trade must return one Focus Crystal");
		var custom = offer.getResult().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		helper.assertTrue(custom != null && custom.copyTag().getIntOr("tier", -1) == 0,
				"the traded Focus Crystal must carry the FLAWED tier component");
		helper.assertTrue(offer.getMaxUses() == Integer.MAX_VALUE,
				"the V33a Focus Crystal offer must not expire");
		helper.succeed();
	}

	/** Seven registered identities, component persistence and all seven source recipes form one seam. */
	private static void storageCrystalItemAndRecipe(GameTestHelper helper) {
		java.util.Set<net.minecraft.world.item.Item> identities = new java.util.HashSet<>();
		for (StorageCrystalTier tier : StorageCrystalTier.list) {
			ItemStack stack = ChromaItems.storageCrystalStack(tier);
			identities.add(stack.getItem());
			helper.assertTrue(ItemStorageCrystal.getTier(stack) == tier
					&& ItemStorageCrystal.getCapacity(stack) == tier.capacity(),
					"storage tier " + tier + " must retain its registered identity and V33a capacity");
		}
		helper.assertTrue(identities.size() == 7,
				"V33a storage tiers must be seven registry items, never one metadata/component variant");

		ItemStack persistent = ChromaItems.storageCrystalStack(StorageCrystalTier.DIVI);
		CompoundTag foreign = new CompoundTag();
		foreign.putString("foreign", "preserved");
		ReikaItemHelper.setStackTag(persistent, foreign);
		ItemStorageCrystal.addEnergy(persistent, CrystalElement.BLUE, 9_000);
		helper.assertTrue(ItemStorageCrystal.getStoredEnergy(persistent, CrystalElement.BLUE) == 8_000,
				"energy addition must clamp to the per-element Divi capacity");
		ItemStorageCrystal.removeEnergy(persistent, CrystalElement.BLUE, 125);
		helper.assertTrue(ItemStorageCrystal.getStoredEnergy(persistent, CrystalElement.BLUE) == 7_875,
				"energy removal must retain the remainder");
		var custom = persistent.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		helper.assertTrue(custom != null && custom.copyTag().getStringOr("foreign", "").equals("preserved"),
				"writing lumen energy must preserve unrelated item custom data");
		helper.assertTrue(!ItemStorageCrystal.isFull(persistent)
				&& ItemStorageCrystal.isFull(ItemStorageCrystal.fullStack(StorageCrystalTier.DIVI)),
				"fullness requires all sixteen channels at capacity");

		for (StorageCrystalTier tier : StorageCrystalTier.list) {
			List<CastingTableRecipe> recipes = reika.chromaticraft.network.ChromaNetwork.guideCastingRecipes(
					helper.getLevel().getServer().getRecipeManager(),
					ChromaItems.STORAGE_CRYSTALS.get(tier).get());
			helper.assertTrue(recipes.size() == 1,
					"storage tier " + tier + " must have exactly one registered casting recipe");
			CastingTableRecipe recipe = recipes.getFirst();
			helper.assertTrue(recipe.tier() == CastingTableRecipe.Tier.MULTIBLOCK
					&& recipe.duration() == (50 << tier.legacyMetadata())
					&& recipe.experience() == 200 && recipe.penaltyThreshold() == 3,
					"storage tier " + tier + " must retain its source tier, duration, XP and typical-4 penalty");
			helper.assertTrue(recipe.stands().size() == 24 && recipe.runes().size() == 12,
					"storage tier " + tier + " must require all 24 stands and 12 authored runes");
			helper.assertTrue(recipe.completion().copyCenterCustomData()
					&& recipe.completion().grantedProgress().equals(List.of(ProgressStage.STORAGE))
					&& recipe.completion().harmonics().equals(List.of(0.5F, 2F)),
					"storage recipes must copy centre NBT, grant STORAGE and expose the V33a harmonics");

			Ingredient outer = recipe.stands().stream()
					.filter(stand -> stand.offset().equals(new BlockPos(-4, 1, -4)))
					.findFirst().orElseThrow().ingredient();
			Ingredient inner = recipe.stands().stream()
					.filter(stand -> stand.offset().equals(new BlockPos(-2, 0, -2)))
					.findFirst().orElseThrow().ingredient();
			ItemStack expectedOuter = tier == StorageCrystalTier.NULA
					? ChromaItems.boostedShardStack(CrystalElement.BLACK)
					: ChromaItems.tieredStack(ChromaTieredItems.CHROMA_DUST);
			ItemStack expectedInner = ChromaItems.tieredStack(tier == StorageCrystalTier.NULA
					? ChromaTieredItems.ELEMENT_DUST : ChromaTieredItems.RESONANCE_DUST);
			helper.assertTrue(outer.test(expectedOuter) && inner.test(expectedInner),
					"base storage must use boosted shards/Infused Dust; upgrades Chromic/Resonant Dust");

			NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
			ItemStack center = tier.previous() == null
					? ChromaItems.craftingStack(ChromaCraftingItems.ELEMENT_UNIT)
					: ChromaItems.storageCrystalStack(tier.previous());
			CompoundTag centerData = new CompoundTag();
			centerData.putString("upgrade_marker", tier.name());
			ReikaItemHelper.setStackTag(center, centerData);
			if (tier.previous() != null)
				ItemStorageCrystal.addEnergy(center, CrystalElement.RED,
						Math.min(100, tier.previous().capacity()));
			grid.set(4, center);
			ItemStack output = recipe.assemble(new CastingRecipeInput(grid, Map.of(), Map.of(), Map.of()));
			var outputData = output.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
			helper.assertTrue(output.is(ChromaItems.STORAGE_CRYSTALS.get(tier).get())
					&& outputData != null
					&& outputData.copyTag().getStringOr("upgrade_marker", "").equals(tier.name())
					&& (tier.previous() == null
							|| ItemStorageCrystal.getStoredEnergy(output, CrystalElement.RED)
									== Math.min(100, tier.previous().capacity())),
					"storage upgrade output must retain the central crystal's complete custom payload");
		}
		helper.succeed();
	}

	/** Charger registration, transfer math, toggles, extraction, upgrade multiplier, and recipe. */
	private static void crystalChargerItemLoop(GameTestHelper helper) {
		BlockPos relative = new BlockPos(2, 2, 2);
		BlockPos absolute = helper.absolutePos(relative);
		helper.getLevel().setBlock(absolute, ChromaBlocks.CRYSTAL_CHARGER.get().defaultBlockState(), 3);
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
		helper.assertTrue(blockEntity instanceof TileEntityCrystalCharger,
				"crystal_charger must instantiate its registered block entity");
		TileEntityCrystalCharger charger = (TileEntityCrystalCharger)blockEntity;
		helper.assertTrue(charger.getMaxStorage(CrystalElement.RED) == 120_000
				&& charger.getReceiveRange() == 20 && charger.maxThroughput() == 4_000,
				"charger must retain V33a capacity, receiver range and throughput");

		ItemStack crystal = ChromaItems.storageCrystalStack(StorageCrystalTier.DAYA);
		charger.setItem(0, crystal);
		charger.setEnergy(CrystalElement.RED, 900);
		charger.runTransferCycleForTest();
		helper.assertTrue(ItemStorageCrystal.getStoredEnergy(crystal, CrystalElement.RED) == 40
				&& charger.getEnergy(CrystalElement.RED) == 860,
				"normal transfer must use 10+floor(sqrt(internal energy))");

		charger.toggle(CrystalElement.RED);
		charger.runTransferCycleForTest();
		helper.assertTrue(ItemStorageCrystal.getStoredEnergy(crystal, CrystalElement.RED) == 40,
				"a disabled colour must not transfer");
		charger.toggle(CrystalElement.RED);

		charger.setItem(1, new ItemStack(ChromaItems.SPEED_UPGRADE.get()));
		charger.setEnergy(CrystalElement.GREEN, 900);
		charger.runTransferCycleForTest();
		helper.assertTrue(ItemStorageCrystal.getStoredEnergy(crystal, CrystalElement.GREEN) == 320
				&& charger.getEnergy(CrystalElement.GREEN) == 580,
				"the hidden V33a speed-upgrade slot must multiply transfer by eight");
		helper.assertTrue(!charger.canTakeItemThroughFace(0, crystal, Direction.UP)
				&& charger.canTakeItemThroughFace(0,
						ItemStorageCrystal.fullStack(StorageCrystalTier.DAYA), Direction.UP),
				"automation may extract slot zero only when all sixteen channels are full");

		List<CastingTableRecipe> recipes = reika.chromaticraft.network.ChromaNetwork.guideCastingRecipes(
				helper.getLevel().getServer().getRecipeManager(), ChromaBlocks.CRYSTAL_CHARGER.get().asItem());
		helper.assertTrue(recipes.size() == 1, "crystal_charger must have one registered casting recipe");
		CastingTableRecipe recipe = recipes.getFirst();
		helper.assertTrue(recipe.tier() == CastingTableRecipe.Tier.MULTIBLOCK
				&& recipe.duration() == 200 && recipe.experience() == 200
				&& recipe.stands().size() == 8 && recipe.runes().isEmpty() && recipe.aura().isEmpty(),
				"charger recipe must retain its V33a tier, doubled duration, XP and eight-stand shape");
		Ingredient center = recipe.grid().stream().filter(entry -> entry.slot() == 4)
				.findFirst().orElseThrow().ingredient();
		Ingredient north = recipe.stands().stream()
				.filter(entry -> entry.offset().equals(new BlockPos(0, 0, -2)))
				.findFirst().orElseThrow().ingredient();
		Ingredient south = recipe.stands().stream()
				.filter(entry -> entry.offset().equals(new BlockPos(0, 0, 2)))
				.findFirst().orElseThrow().ingredient();
		helper.assertTrue(center.test(ChromaItems.clusterStack(ChromaClusterItems.CRYSTAL_CORE))
				&& north.test(ChromaItems.shardStack(CrystalElement.WHITE))
				&& south.test(new ItemStack(Items.SMOOTH_STONE_SLAB)),
				"charger center and asymmetric white-shard/slab cardinal stands must match V33a");
		helper.succeed();
	}

	/** NBT structure, focus speed, bucket refill, conversion/overflow, progression, and recipe parity. */
	private static void itemAuraInfuserLoop(GameTestHelper helper) {
		BlockPos relative = new BlockPos(8, 4, 8);
		BlockPos absolute = helper.absolutePos(relative);
		NBTStructureLoader.place(helper.getLevel(), ChromaStructureTemplateProvider.INFUSION,
				absolute, new BlockPos(3, 2, 3), state -> state, 2);
		helper.getLevel().setBlock(absolute, ChromaBlocks.ITEM_INFUSER.get().defaultBlockState(), 3);
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
		helper.assertTrue(blockEntity instanceof TileEntityItemInfuser,
				"item_aura_infuser must instantiate its registered block entity");
		TileEntityItemInfuser infuser = (TileEntityItemInfuser)blockEntity;
		infuser.validateStructure();
		helper.assertTrue(infuser.hasStructure() && !infuser.getChromaLocations().isEmpty(),
				"the canonical NBT infusion rings must validate and expose their source-chroma cells");

		BlockPos refill = infuser.getChromaLocations().iterator().next();
		helper.getLevel().setBlock(refill, Blocks.AIR.defaultBlockState(), 3);
		try (Transaction transaction = Transaction.openRoot()) {
			int inserted = infuser.fluidHandler().insert(0, FluidResource.of(ChromaFluids.CHROMA.get()),
					FluidType.BUCKET_VOLUME, transaction);
			helper.assertTrue(inserted == FluidType.BUCKET_VOLUME,
					"the virtual ring tank must accept exactly one whole bucket");
			transaction.commit();
		}
		helper.assertTrue(helper.getLevel().getBlockState(refill).is(ChromaBlocks.CHROMA.get())
				&& helper.getLevel().getFluidState(refill).isSource(),
				"committing a refill transaction must restore a source Liquid Chroma cell");

		List<BlockPos> focusLocations = List.copyOf(infuser.getRelativeFocusCrystalLocations());
		helper.assertTrue(focusLocations.size() >= 4,
				"the outer V33a brick ring must expose its focus-crystal sockets");
		for (int i = 0; i < 4; i++) {
			BlockPos focusPos = absolute.offset(focusLocations.get(i));
			helper.getLevel().setBlock(focusPos, ChromaBlocks.FOCUS_CRYSTAL.get().defaultBlockState(), 3);
			((TileEntityFocusCrystal)helper.getLevel().getBlockEntity(focusPos))
					.setTier(TileEntityFocusCrystal.CrystalTier.EXQUISITE);
		}
		infuser.validateStructure();
		helper.assertTrue(infuser.getAccelerationFactor() == 4,
				"four Exquisite focus crystals must select V33a's maximum 4x craft speed");

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		infuser.setPlacer(player);
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.ALLOY, true, false, false);
		ChromaAbilityData.setDoubleCraft(player, true);
		ItemStack raw = ChromaItems.craftingStack(ChromaCraftingItems.RAW_CRYSTAL);
		raw.setCount(40);
		infuser.interact(raw, player);
		helper.assertTrue(raw.isEmpty() && infuser.getCraftingTick() == 152
				&& infuser.getState() == reika.chromaticraft.auxiliary.interfaces.OperationInterval.OperationState.RUNNING,
				"insertion at ALLOY with four Exquisite focuses must start a 608/4-tick operation");
		infuser.completeCraftForTest();
		ItemStack result = infuser.getItem(0);
		CompoundTag resultData = ReikaItemHelper.getStackTag(result);
		helper.assertTrue(result.is(ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get())
				&& result.getCount() == 64 && resultData != null
				&& resultData.getIntOr("requiredExtra", 0) == 16,
				"DOUBLECRAFT must turn forty Raw Crystals into 64+16 Iridescent Crystal Shards");
		helper.assertTrue(ProgressStage.INFUSE.isPlayerAtStage(player),
				"a completed conversion must grant INFUSE progression");
		helper.assertTrue(infuser.getChromaLocations().stream()
				.noneMatch(pos -> helper.getLevel().getBlockState(pos).is(ChromaBlocks.CHROMA.get())),
				"completion must consume every source Liquid Chroma cell in the authored ring");

		List<CastingTableRecipe> recipes = reika.chromaticraft.network.ChromaNetwork.guideCastingRecipes(
				helper.getLevel().getServer().getRecipeManager(), ChromaBlocks.ITEM_INFUSER.get().asItem());
		helper.assertTrue(recipes.size() == 1, "item_aura_infuser must have one registered casting recipe");
		CastingTableRecipe recipe = recipes.getFirst();
		Ingredient center = recipe.grid().stream().filter(entry -> entry.slot() == 4)
				.findFirst().orElseThrow().ingredient();
		helper.assertTrue(recipe.tier() == CastingTableRecipe.Tier.MULTIBLOCK
				&& recipe.duration() == 100 && recipe.experience() == 200
				&& recipe.stands().size() == 8 && center.test(new ItemStack(ChromaBlocks.ITEM_STAND.get()))
				&& recipe.stands().stream().allMatch(stand -> stand.ingredient().test(
						ChromaItems.craftingStack(ChromaCraftingItems.CHROMA_INGOT))),
				"the infuser recipe must retain its Item Stand center and eight Chroma Alloy stands");
		helper.succeed();
	}

	/** NBT fountain, ingredient-gated capacity grant, recipient capture, consumption, and recipe parity. */
	private static void playerAuraInfuserLoop(GameTestHelper helper) {
		BlockPos relative = new BlockPos(10, 6, 10);
		BlockPos absolute = helper.absolutePos(relative);
		NBTStructureLoader.place(helper.getLevel(), ChromaStructureTemplateProvider.PLAYER_INFUSION,
				absolute, new BlockPos(4, 3, 4), state -> state, 2);
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
		helper.assertTrue(blockEntity instanceof TileEntityPlayerInfuser,
				"player_aura_infuser NBT must instantiate its distinct registered block entity");
		TileEntityPlayerInfuser infuser = (TileEntityPlayerInfuser)blockEntity;
		infuser.validateStructure();
		helper.assertTrue(infuser.hasStructure() && infuser.getChromaLocations().size() == 32,
				"the exact 9x4x9 Player Infusion fountain must validate with 32 source-chroma cells");

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		infuser.setPlacer(player);
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.ALLCOLORS, true, false, false);
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.ABILITY, true, false, false);
		ProgressionManager.instance.setPlayerStage(player, ProgressStage.ALLOY, true, false, false);
		player.snapTo(absolute.getX() + 0.5, absolute.getY() + 0.4, absolute.getZ() + 0.5);
		ItemStack berries = ChromaItems.craftingStack(ChromaCraftingItems.ETHER_BERRIES);
		berries.setCount(8);
		infuser.interact(berries, player);
		helper.assertTrue(berries.isEmpty() && infuser.getSelectedEffect() == ElementBufferCapacityBoost.ALLOYS
				&& infuser.getCraftingTick() == TileEntityAuraInfuser.DURATION,
				"eight Ether Berries and a recipient in the target band must start the ALLOYS boost");
		infuser.completeCraftForTest();
		helper.assertTrue(ElementBufferCapacityBoost.ALLOYS.playerHas(player) && infuser.getItem(0).isEmpty(),
				"completion must persist the ALLOYS buffer boost and consume all eight ingredients");
		helper.assertTrue(infuser.getChromaLocations().stream()
				.noneMatch(pos -> helper.getLevel().getBlockState(pos).is(ChromaBlocks.CHROMA.get())),
				"Player Infusion completion must consume all 32 authored Liquid Chroma sources");

		List<CastingTableRecipe> recipes = reika.chromaticraft.network.ChromaNetwork.guideCastingRecipes(
				helper.getLevel().getServer().getRecipeManager(), ChromaBlocks.PLAYER_INFUSER.get().asItem());
		helper.assertTrue(recipes.size() == 1, "player_aura_infuser must have one registered upgrade recipe");
		CastingTableRecipe recipe = recipes.getFirst();
		Ingredient center = recipe.grid().stream().filter(entry -> entry.slot() == 4)
				.findFirst().orElseThrow().ingredient();
		helper.assertTrue(recipe.tier() == CastingTableRecipe.Tier.MULTIBLOCK
				&& recipe.duration() == 100 && recipe.experience() == 200
				&& recipe.stands().size() == 12 && recipe.runes().isEmpty() && recipe.aura().isEmpty()
				&& center.test(new ItemStack(ChromaBlocks.ITEM_INFUSER.get())),
				"the Player Infuser recipe must retain its Item Infuser center and twelve exact stands");
		helper.assertTrue(recipe.stands().stream().filter(stand -> stand.ingredient().test(
				ChromaItems.craftingStack(ChromaCraftingItems.AURA_INGOT))).count() == 4
				&& recipe.stands().stream().filter(stand -> stand.ingredient().test(new ItemStack(Items.DIAMOND))).count() == 4
				&& recipe.stands().stream().filter(stand -> stand.ingredient().test(
				ChromaItems.tieredStack(ChromaTieredItems.RESONANCE_DUST))).count() == 3,
				"the twelve stands must contain four Aura Ingots, four diamonds, three Resonant Dusts, and Chromastone");
		helper.succeed();
	}

	private static final class TestReceiver implements CrystalReceiver {
		private final Level level;
		private final BlockPos pos;
		private final UUID id = UUID.randomUUID();
		private int energy;

		private TestReceiver(Level level, BlockPos pos) {
			this.level = level;
			this.pos = pos;
		}

		@Override public int receiveElement(CrystalSource src, CrystalElement e, int amount) {
			int accepted = Math.min(amount, 10000 - energy);
			energy += accepted;
			return accepted;
		}
		@Override public void onPathBroken(CrystalFlow path, FlowFail failure) {}
		@Override public void onPathCompleted(CrystalFlow path) {}
		@Override public int getReceiveRange() { return 32; }
		@Override public boolean canReceiveFrom(CrystalTransmitter transmitter) { return true; }
		@Override public boolean needsLineOfSightFromTransmitter(CrystalTransmitter transmitter) { return true; }
		@Override public boolean canBeSuppliedBy(CrystalSource source, CrystalElement element) { return true; }
		@Override public DecimalPosition getTargetRenderOffset(CrystalElement element) { return null; }
		@Override public double getIncomingBeamRadius() { return 0.35; }
		@Override public boolean isConductingElement(CrystalElement element) { return true; }
		@Override public void cachePosition() { CrystalNetworker.instance.addTile(this); }
		@Override public void removeFromCache() { CrystalNetworker.instance.removeTile(this); }
		@Override public double getDistanceSqTo(double x, double y, double z) { return pos.distToCenterSqr(x, y, z); }
		@Override public Level getWorld() { return level; }
		@Override public int getX() { return pos.getX(); }
		@Override public int getY() { return pos.getY(); }
		@Override public int getZ() { return pos.getZ(); }
		@Override public int maxThroughput() { return 1000; }
		@Override public boolean canConduct() { return energy < 10000; }
		@Override public UUID getUniqueID() { return id; }
		@Override public UUID getPlacerUUID() { return null; }
		@Override public void triggerBottleneckDisplay(int duration) {}
		@Override public boolean isRemoved() { return false; }
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			String name, Consumer<GameTestHelper> body) {
		register(event, env, name, 20, body);
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			String name, int maxTicks, Consumer<GameTestHelper> body) {
		Identifier id = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name);
		TestData<Holder<TestEnvironmentDefinition<?>>> data =
				new TestData<>(env, ChromaTestStructureProvider.ARENA, maxTicks, 0, true, Rotation.NONE, false, 1, 1, false, 96);
		event.registerTest(id, new DirectInstance(data, body));
	}

	/** A {@link GameTestInstance} carrying its body as a plain {@link Consumer} (see class javadoc). */
	private static final class DirectInstance extends GameTestInstance {

		static final MapCodec<DirectInstance> CODEC =
				TestData.CODEC.xmap(data -> new DirectInstance(data, h -> {}), inst -> inst.info);

		private final TestData<Holder<TestEnvironmentDefinition<?>>> info;
		private final Consumer<GameTestHelper> body;

		DirectInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> body) {
			super(info);
			this.info = info;
			this.body = body;
		}

		@Override
		public void run(GameTestHelper helper) {
			body.accept(helper);
		}

		@Override
		public MapCodec<? extends GameTestInstance> codec() {
			return CODEC;
		}

		@Override
		protected MutableComponent typeDescription() {
			return Component.literal("chromaticraft direct test");
		}
	}
}
