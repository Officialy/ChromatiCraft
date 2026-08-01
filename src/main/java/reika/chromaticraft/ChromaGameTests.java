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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
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
import reika.chromaticraft.data.ChromaTestStructureProvider;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockCrystallineStone;
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
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityDisplayPoint;
import reika.chromaticraft.tileentity.networking.TileEntityCompoundRepeater;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.chromaticraft.world.CrystalFeature;
import reika.chromaticraft.world.PylonFeature;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

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
		register(event, env, "pylon_worldgen_nbt_contract", ChromaGameTests::pylonWorldgenNbtContract);
        register(event, env, "pylon_feature_variants", 40, ChromaGameTests::pylonFeatureVariants);
		register(event, env, "glow_cloud_spherical_movement", ChromaGameTests::glowCloudSphericalMovement);
		register(event, env, "manipulator_repeater_dispatch", ChromaGameTests::manipulatorRepeaterDispatch);
		register(event, env, "pylon_worldgen_grid_density", ChromaGameTests::pylonWorldgenGridDensity);
		register(event, env, "casting_table_menu_grid", ChromaGameTests::castingTableMenuGrid);
	}

	/** The V33a spherical velocity must pass through 26.2 LivingEntity travel and entity tracking. */
	private static void glowCloudSphericalMovement(GameTestHelper helper) {
		EntityGlowCloud cloud = ChromaEntityTypes.GLOW_CLOUD.get().create(
				helper.getLevel(), EntitySpawnReason.COMMAND);
		helper.assertTrue(cloud != null, "glow cloud should instantiate from its registered entity type");
		cloud.setCustomName(Component.literal("movement test"));
		cloud.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8))));
		var start = cloud.position();
		helper.assertTrue(helper.getLevel().addFreshEntity(cloud), "glow cloud should enter the test level");
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(cloud.position().distanceToSqr(start) > 0.01,
					"glow cloud must travel under its source-faithful spherical velocity");
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

	/**
	 * The NBT broadcast monument retains its V33a stone geometry and fails closed until the actual
	 * chroma-fluid block is registered; structure-void markers must never become substitute blocks.
	 */
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
		helper.assertTrue(helper.getLevel().getBlockState(pylonPos.offset(2, -9, 4)).isAir(),
				"deferred chroma-fluid cells must remain empty rather than silently accepting a substitute");
		helper.assertTrue(!monument.matchInWorld(),
				"broadcast monument must not validate while chromaticraft:chroma is unregistered");
		helper.assertTrue(!pylon.refreshBroadcastUpgrade() && !pylon.hasBroadcastUpgrade(),
				"missing chroma fluid must keep the pylon broadcast upgrade disabled");
		helper.assertTrue(pylon.needsLineOfSightToReceiver(null),
				"an inactive broadcast monument must preserve normal receiver line-of-sight requirements");
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
		helper.assertTrue(crystals.stream().allMatch(TileEntityChromaCrystal::isConnected),
				"each power crystal must persist a live connection to its pylon");

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(pylon.drain(CrystalElement.CYAN, 10000), "test setup should drain the pylon");
			int before = pylon.getEnergy(CrystalElement.CYAN);
			helper.runAfterDelay(1, () -> {
				int gained = pylon.getEnergy(CrystalElement.CYAN) - before;
				helper.assertTrue(gained == 768,
						"eight V33a boosters must add 768 lumens per normal tick including base regen; actual=" + gained);
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
			helper.getLevel().setBlock(pos, ChromaBlocks.POWER_CRYSTAL.get().defaultBlockState(), 3);
			TileEntityChromaCrystal crystal = (TileEntityChromaCrystal)helper.getLevel().getBlockEntity(pos);
			crystal.setPlacer(owner);
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

	private static void castingTableAtomicCraft(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
		helper.getLevel().setBlock(pos, ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 3);
		TileEntityCastingTable table = (TileEntityCastingTable)helper.getLevel().getBlockEntity(pos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		ProgressionManager.instance.setPlayerStage(owner, ProgressStage.CRYSTALS, true, false, false);
		ItemStack smooth = new ItemStack(ChromaBlocks.crystallineStone(BlockCrystallineStone.StoneTypes.SMOOTH).get().asItem());
		table.setItem(3, smooth.copy());
		table.setItem(4, smooth.copy());
		helper.assertTrue(table.triggerCrafting(owner), "the registered two-smooth-stone beam recipe should start");
		helper.assertTrue(table.isCrafting() && table.getCraftingTick() == 5,
				"ordinary recipe should begin its five-tick work period");
		for (int i = 0; i < 4; i++) table.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(table.getItem(9).isEmpty() && !table.getItem(3).isEmpty(),
				"inputs and output must remain untouched before the final tick");
		table.updateEntity(helper.getLevel(), pos);
		helper.assertTrue(!table.isCrafting(), "craft should finish on the declared duration");
		helper.assertTrue(table.getItem(3).isEmpty() && table.getItem(4).isEmpty(),
				"the final commit should consume both declared grid inputs");
		helper.assertTrue(table.getItem(9).is(ChromaBlocks.crystallineStone(BlockCrystallineStone.StoneTypes.BEAM).get().asItem()) && table.getItem(9).getCount() == 2,
				"the final commit should produce the V33a beam output");
		helper.assertTrue(table.getTableXP() == 5 && table.getCompletedRecipes().size() == 1,
				"completion should award recipe XP and remember the recipe key");
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
		helper.assertTrue(table.maxThroughput() == 200,
				"the V33a 100-lumen base at 15000 XP must double to 200; actual=" + table.maxThroughput());
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
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		table.setPlacer(owner);
		for (ProgressStage stage : new ProgressStage[] {ProgressStage.RUNEUSE, ProgressStage.MULTIBLOCK, ProgressStage.PYLON, ProgressStage.REPEATER})
			ProgressionManager.instance.setPlayerStage(owner, stage, true, false, false);
		table.setItem(4, ChromaItems.clusterStack(ChromaClusterItems.CRYSTAL_STAR));
		for (int x = -4; x <= 4; x += 2) for (int z = -4; z <= 4; z += 2) {
			if (x == 0 && z == 0) continue;
			boolean cardinal = (x == 0 && Math.abs(z) == 2) || (z == 0 && Math.abs(x) == 2);
			ItemStack ingredient = cardinal ? ChromaItems.tieredStack(ChromaTieredItems.PURITY_DUST)
					: ChromaItems.craftingStack(ChromaCraftingItems.GLOW_CHUNK);
			setStand(helper, tablePos, new BlockPos(x, Math.abs(x) == 4 || Math.abs(z) == 4 ? 1 : 0, z), ingredient, owner);
		}
		for (CrystalElement element : new CrystalElement[] {CrystalElement.BLACK, CrystalElement.YELLOW, CrystalElement.BLUE})
			helper.assertTrue(table.receiveElement(null, element, 60000) == 60000,
					"Lumen Core setup must accept the exact 60000-lumen requirement for "+element);
		helper.assertTrue(table.triggerCrafting(owner) && table.getCraftingTick() == 400,
				"Lumen Core must start only with all 24 stands and all three aura colors present");
		for (int i = 0; i < 400; i++) table.updateEntity(helper.getLevel(), tablePos);
		helper.assertTrue(table.getItem(9).is(ChromaItems.CRAFTING.get(ChromaCraftingItems.LUMEN_CORE).get()),
				"Lumen Core must commit its source-exact output after 400 ticks");
		for (CrystalElement element : new CrystalElement[] {CrystalElement.BLACK, CrystalElement.YELLOW, CrystalElement.BLUE})
			helper.assertTrue(table.getEnergy(element) == 0, "Lumen Core completion must debit all 60000 "+element+" lumens");
		helper.assertTrue(table.getTableXP() == 15500 && table.getOtherStands().values().stream().allMatch(TileEntityItemStand::isEmpty),
				"Lumen Core completion must award 500 XP and consume all 24 stands atomically");
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

	/** V33a spread-fill balances the held stack across every queued empty stand. */
	private static void castingStandSpread(GameTestHelper helper) {
		BlockPos firstPos = helper.absolutePos(new BlockPos(4, 3, 4));
		BlockPos secondPos = firstPos.offset(2, 0, 0);
		helper.getLevel().setBlock(firstPos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		helper.getLevel().setBlock(secondPos, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 3);
		TileEntityItemStand first = (TileEntityItemStand)helper.getLevel().getBlockEntity(firstPos);
		TileEntityItemStand second = (TileEntityItemStand)helper.getLevel().getBlockEntity(secondPos);
		var owner = helper.makeMockPlayer(GameType.SURVIVAL);
		first.setPlacer(owner);
		second.setPlacer(owner);
		first.queueSpread(owner);
		second.queueSpread(owner);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE, 5));
		helper.assertTrue(first.interact(owner, net.minecraft.world.InteractionHand.MAIN_HAND), "queued spread should be handled");
		helper.assertTrue(first.getItem(0).getCount() == 2 && second.getItem(0).getCount() == 2,
				"five items across two stands should place two on each");
		helper.assertTrue(owner.getMainHandItem().getCount() == 1, "spread remainder should stay in hand");
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
