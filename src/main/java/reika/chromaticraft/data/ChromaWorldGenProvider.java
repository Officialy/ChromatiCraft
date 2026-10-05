package reika.chromaticraft.data;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.*;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.world.PylonGridPlacement;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.libraries.RandomTagSingleStateProvider;

import java.util.List;

/** Datapack registry objects for ChromatiCraft's active 26.2 worldgen features. */
public final class ChromaWorldGenProvider {

    public static final ResourceKey<VillagerTrade> FOCUS_CRYSTAL_TRADE = ResourceKey.create(
            Registries.VILLAGER_TRADE, id("focus_crystal"));
    /** V33a {@code UATrades.UATrade}: a villager buys an Unknown Artefact. */
    public static final ResourceKey<VillagerTrade> UA_TRADE = ResourceKey.create(
            Registries.VILLAGER_TRADE, id("unknown_artefact"));
    /** V33a {@code UATrades.MetaAlloyTrade}: a villager buys a Lumen Pod. */
    public static final ResourceKey<VillagerTrade> META_ALLOY_TRADE = ResourceKey.create(
            Registries.VILLAGER_TRADE, id("meta_alloy"));

    private static final Identifier CAVE_CRYSTAL = id("cave_crystal");
    private static final Identifier NATURAL_PYLON = id("natural_pylon");
    private static final Identifier DATA_TOWER = id("data_tower");
    private static final Identifier RUNIC_ALTAR = id("runic_altar");
    private static final Identifier TURBOCHARGED_PYLON = id("turbocharged_pylon");
    private static final Identifier POWER_CRYSTAL_BOOSTED_PYLON = id("power_crystal_boosted_pylon");
    private static final Identifier LUMINOUS_CLIFFS_TERRAIN = id("luminous_cliffs_terrain");
    private static final Identifier LUMA_PATCH = id("luma_patch");
    private static final Identifier NETHER_ROOF_STRUCTURE = id("nether_roof_structure");
    private static final Identifier NETHER_LAVA_RIVER = id("nether_lava_river");
    private static final Identifier CRYSTAL_SHRUB = id("crystal_shrub");
    private static final Identifier FLOATSTONE = id("floatstone");
    private static final Identifier CRYSTAL_TREE = id("crystal_tree");
    private static final Identifier GLOW_TREE = id("glow_tree");
    private static final Identifier TREE_CLUSTER = id("tree_cluster");
    private static final Identifier FIRE_JET = id("fire_jet");
    private static final Identifier MINI_ALTAR = id("mini_altar");
    private static final Identifier FISSURE = id("fissure");
    private static final Identifier GLOWING_CRACKS = id("glowing_cracks");
    private static final Identifier LIGHTED_SHRUB = id("lighted_shrub");
    private static final Identifier CRYSTAL_PIT = id("crystal_pit");
    private static final Identifier AURORAE = id("aurorae");
    private static final List<Identifier> NETHER_ROOF_STRUCTURES =
            List.of(id("nether_hut"), id("nether_temple"), id("nether_maze"),
                    id("nether_spiral"), id("nether_diorama"));
    private static final Identifier NATURAL_CAVERN = id("natural_cavern");
    private static final Identifier CAVERN = id("cavern");
    private static final Identifier NATURAL_BURROW = id("natural_burrow");
    private static final Identifier BURROW = id("burrow");
    private static final Identifier NATURAL_OCEAN = id("natural_ocean");
    private static final Identifier OCEAN = id("ocean");
    private static final Identifier NATURAL_DESERT = id("natural_desert");
    private static final Identifier DESERT = id("desert");
	private static final Identifier NATURAL_SNOW = id("natural_snow");
	private static final Identifier SNOW = id("snow");
	private static final Identifier NATURAL_BIOME_FRAGMENT = id("natural_biome_fragment");
	private static final Identifier BIOME_FRAGMENT = id("biome_fragment");
    /** V33a TieredOres: {genChance one-in-N per chunk, veinCount attempts, veinSize}. */
    private record TieredOre(String name, java.util.function.Supplier<net.minecraft.world.level.block.Block> block,
            net.minecraft.world.level.block.Block host, int genChance, int veinCount, int veinSize, boolean deepBand) {}

    private static final List<TieredOre> TIERED_ORES = List.of(
            new TieredOre("energized_rock", () -> ChromaBlocks.ENERGIZED_ROCK.get(),
                    net.minecraft.world.level.block.Blocks.STONE, 1, 4, 12, false),
            new TieredOre("elemental_stones", () -> ChromaBlocks.ELEMENTAL_STONES.get(),
                    net.minecraft.world.level.block.Blocks.STONE, 1, 4, 8, false),
			new TieredOre("fused_crystals", () -> ChromaBlocks.FUSED_CRYSTALS.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 4, 8, false),
			new TieredOre("radiant_stone", () -> ChromaBlocks.RADIANT_STONE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 4, 8, false),
			new TieredOre("ender_stone", () -> ChromaBlocks.ENDER_STONE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 2, 8, false),
			new TieredOre("fluid_stone", () -> ChromaBlocks.FLUID_STONE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 2, 2, 8, false),
			new TieredOre("firaxite_ore", () -> ChromaBlocks.FIRAXITE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 2, 8, false),
			new TieredOre("glowing_rock", () -> ChromaBlocks.GLOWING_ROCK.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 1, 8, false),
			new TieredOre("echostone", () -> ChromaBlocks.ECHOSTONE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 2, 1, 8, false),
			new TieredOre("vibrant_crystals", () -> ChromaBlocks.VIBRANT_CRYSTALS.get(),
					net.minecraft.world.level.block.Blocks.END_STONE, 1, 8, 8, true),
			new TieredOre("spacerift_stone", () -> ChromaBlocks.SPACERIFT_STONE.get(),
					net.minecraft.world.level.block.Blocks.END_STONE, 2, 9, 8, true),
			new TieredOre("lumenite_ore", () -> ChromaBlocks.LUMENITE_ORE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 1, 2, 8, true),
			new TieredOre("firestone", () -> ChromaBlocks.FIRESTONE.get(),
					net.minecraft.world.level.block.Blocks.NETHERRACK, 3, 2, 16, true),
			new TieredOre("thermitic_rock", () -> ChromaBlocks.THERMITIC_ROCK.get(),
					net.minecraft.world.level.block.Blocks.NETHERRACK, 3, 2, 16, true),
			new TieredOre("avolite_ore", () -> ChromaBlocks.AVOLITE_ORE.get(),
					net.minecraft.world.level.block.Blocks.STONE, 2, 1, 18, true));

    public static final ResourceKey<PlacedFeature> LUMINOUS_CLIFFS_TERRAIN_PLACED = placedKey("luminous_cliffs_terrain");
    public static final ResourceKey<PlacedFeature> LUMA_PATCH_PLACED = placedKey("luma_patch");
    public static final ResourceKey<Feature> RAINBOW_TREE = featureKey("rainbow_tree");
    public static final ResourceKey<Feature> GLOWING_TREE = featureKey("glowing_tree");
    public static final ResourceKey<PlacedFeature> ENDER_FOREST_TREE_PLACED = placedKey("ender_forest_tree");
    public static final ResourceKey<PlacedFeature> ENDER_POOL_PLACED = placedKey("ender_pool");
    public static final ResourceKey<PlacedFeature> ENDER_FOREST_FLOWER_PLACED = placedKey("ender_forest_flower");
    public static final ResourceKey<PlacedFeature> ENDER_FOREST_DEFAULT_FLOWERS_PLACED =
            placedKey("ender_forest_default_flowers");
    /** The bone-meal feature; see {@code EnderForestFlowerFeature} and {@code ChromaConfiguredFeatureTagProvider}. */
    public static final ResourceKey<Feature> ENDER_FOREST_FLOWER =
            featureKey("ender_forest_flower");

    // V33a RainbowForestDecorator / DecoratorRainbowRiver. Every Rainbow placed feature is ChromatiCraft's
    // own key, so no vanilla feature's relative order is shared with another biome (FeatureSorter).
    // V33a GlowingCliffsDecorator, one key each so no vanilla feature order is shared (FeatureSorter).
    public static final ResourceKey<PlacedFeature> LUMINOUS_GRAVEL_ORE_PLACED = placedKey("luminous_gravel_ore");
    public static final ResourceKey<PlacedFeature> LUMINOUS_DISK_SAND_PLACED = placedKey("luminous_disk_sand");
    public static final ResourceKey<PlacedFeature> LUMINOUS_DISK_CLAY_PLACED = placedKey("luminous_disk_clay");
    public static final ResourceKey<PlacedFeature> LUMINOUS_GRAVEL_PATCH_PLACED = placedKey("luminous_gravel_patch");
    public static final ResourceKey<PlacedFeature> LUMINOUS_TREES_PLACED = placedKey("luminous_trees");
    public static final ResourceKey<PlacedFeature> LUMINOUS_FLOWERS_PLACED = placedKey("luminous_flowers");
    public static final ResourceKey<PlacedFeature> LUMINOUS_GRASS_PLACED = placedKey("luminous_grass");
    public static final ResourceKey<PlacedFeature> LUMINOUS_LILY_PAD_PLACED = placedKey("luminous_lily_pad");
    public static final ResourceKey<PlacedFeature> LUMINOUS_BROWN_MUSHROOM_PLACED = placedKey("luminous_brown_mushroom");
    public static final ResourceKey<PlacedFeature> LUMINOUS_RED_MUSHROOM_PLACED = placedKey("luminous_red_mushroom");
    public static final ResourceKey<PlacedFeature> LUMINOUS_REEDS_PLACED = placedKey("luminous_reeds");
    public static final ResourceKey<PlacedFeature> LUMINOUS_SPRING_WATER_PLACED = placedKey("luminous_spring_water");
    public static final ResourceKey<PlacedFeature> LUMINOUS_SPRING_LAVA_PLACED = placedKey("luminous_spring_lava");
    public static final ResourceKey<PlacedFeature> LUMINOUS_CLIFFS_FLOWER_PLACED = placedKey("luminous_cliffs_flower");
    /** V33a {@code getUndergroundTreeGen}: the cavern trees the aux pass grows from the shaper's saplings. */
    public static final ResourceKey<Feature> UNDERGROUND_GLOWING_TREE_SMALL =
            featureKey("underground_glowing_tree_small");
    public static final ResourceKey<Feature> UNDERGROUND_GLOWING_TREE_BIG =
            featureKey("underground_glowing_tree_big");
    /** Bone meal's flower list; see {@code ChromaConfiguredFeatureTagProvider}. */
    public static final ResourceKey<Feature> LUMINOUS_CLIFFS_FLOWER = featureKey("luminous_cliffs_flower");

    public static final ResourceKey<PlacedFeature> RAINBOW_FOREST_TREE_PLACED = placedKey("rainbow_forest_tree");
    public static final ResourceKey<PlacedFeature> DYE_FLOWER_PATCH_PLACED = placedKey("dye_flower_patch");
    public static final ResourceKey<PlacedFeature> RAINBOW_FOREST_UNDERGROUND_PLACED = placedKey("rainbow_forest_underground");
    public static final ResourceKey<PlacedFeature> RAINBOW_FOREST_FLOWER_PLACED = placedKey("rainbow_forest_flower");
    /** The bone-meal feature; see {@code RainbowForestFlowerFeature}. */
    public static final ResourceKey<Feature> RAINBOW_FOREST_FLOWER = featureKey("rainbow_forest_flower");

    /** Per-biome counts of the Rainbow auxiliary decoration: forest and stream. */
    public record RainbowDecoration(String prefix, int sandDisks, int clayDisks, int lateSandDisks,
            int dandelions, boolean roses, int grass, int reeds) {
        public ResourceKey<PlacedFeature> sandDisk() { return placedKey(prefix + "_disk_sand"); }
        public ResourceKey<PlacedFeature> clayDisk() { return placedKey(prefix + "_disk_clay"); }
        public ResourceKey<PlacedFeature> lateSandDisk() { return placedKey(prefix + "_disk_sand_late"); }
        public ResourceKey<PlacedFeature> dandelion() { return placedKey(prefix + "_dandelion"); }
        public ResourceKey<PlacedFeature> rose() { return placedKey(prefix + "_rose"); }
        public ResourceKey<PlacedFeature> grassPatch() { return placedKey(prefix + "_grass"); }
        public ResourceKey<PlacedFeature> reedPatch() { return placedKey(prefix + "_reeds"); }
    }

    /**
     * V33a RainbowForestDecorator: sandPerChunk2 3, clayPerChunk 1, sandPerChunk 1 (both sand loops use
     * the sand generator), 16 dandelion attempts, grass 8, reeds 50 plus a further 10.
     */
    public static final RainbowDecoration RAINBOW_FOREST_DECORATION =
            new RainbowDecoration("rainbow_forest", 3, 1, 1, 16, false, 8, 50 + 10);
    /**
     * V33a DecoratorRainbowRiver: sand2 /2 = 1, clay x9 = 9, sand x8 = 8, flowers x6 = 96 with a rose
     * one time in four, grass x0.4 = 3, reeds x15 = 750 plus the same further 10.
     */
    public static final RainbowDecoration RAINBOW_STREAM_DECORATION =
            new RainbowDecoration("rainbow_stream", 1, 9, 8, 96, true, 3, 750 + 10);

    private ChromaWorldGenProvider() {}

    public static ResourceKey<Feature> dyeTree(CrystalElement element) {
        return featureKey("dye_tree_" + element.getEnglishName());
    }

    private static Identifier coloredPylon(CrystalElement element) {
        return id("pylon_" + element.getEnglishName());
    }

    public static RegistrySetBuilder buildReloadableRegistrySet() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.LOOT_TABLE, new ChromaLootProvider());
        builder.add(Registries.ADVANCEMENT, new ChromaAdvancementProvider());
        builder.add(net.minecraft.data.recipes.RecipeProvider.asBootstrap((recipes, advancements) ->
                new net.minecraft.data.recipes.RecipeProvider(recipes, advancements) {
                    @Override
                    protected void buildRecipes() {
                        ChromaCastingRecipeProvider.bootstrap(recipes, advancements);
                        ChromaPoolRecipeProvider.bootstrap(recipes, advancements);
                        ChromaCobbleGeneratorRecipeProvider.bootstrap(recipes, advancements);
                        ChromaRecipeProvider.bootstrap(recipes, advancements);
                    }
                }));
        return builder;
    }

    public static RegistrySetBuilder buildRegistrySet() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.DAMAGE_TYPE, context -> {
            reika.chromaticraft.modinterface.voidritual.VoidRitualDamageTypes.bootstrap(context);
            reika.chromaticraft.auxiliary.PylonDamage.bootstrap(context);
            reika.chromaticraft.auxiliary.LumenTurretDamage.bootstrap(context);
        });
        builder.add(Registries.ENCHANTMENT, reika.chromaticraft.registry.ChromaEnchants::bootstrap);
        builder.add(Registries.VILLAGER_TRADE, bootstrap -> {
            bootstrap.register(FOCUS_CRYSTAL_TRADE,
                VillagerTrade.builder(
                        new TradeCost(Items.EMERALD, 1),
						flawedFocusCrystal(),
                        Integer.MAX_VALUE,
                        1,
                        0).build());
            // V33a UATypeTrade: the villager pays calcValue(commodity) emeralds, and never tires of it.
            bootstrap.register(UA_TRADE, VillagerTrade.builder(
                    new TradeCost(ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get(), 1),
                    new net.minecraft.world.item.ItemStackTemplate(Items.EMERALD,
                            reika.chromaticraft.magic.artefact.UATrades.calcValue("Thargoid Sensor")),
                    Integer.MAX_VALUE, 1, 0).build());
            bootstrap.register(META_ALLOY_TRADE, VillagerTrade.builder(
                    new TradeCost(ChromaBlocks.META_ALLOY_LAMP.get().asItem(), 1),
                    new net.minecraft.world.item.ItemStackTemplate(Items.EMERALD,
                            reika.chromaticraft.magic.artefact.UATrades.calcValue("Meta-Alloys")),
                    Integer.MAX_VALUE, 1, 0).build());
        });
        builder.add(Registries.MATERIAL_RULE,
                reika.chromaticraft.world.dimension.ProximaNoiseSettings::bootstrapMaterialRule);
        builder.add(Registries.NOISE_SETTINGS,
                reika.chromaticraft.world.dimension.ProximaNoiseSettings::bootstrap);
        builder.add(Registries.DIMENSION_TYPE,
                reika.chromaticraft.world.dimension.ProximaDimension::bootstrapType);
        builder.add(Registries.LEVEL_STEM,
                reika.chromaticraft.world.dimension.ProximaDimension::bootstrapStem);
        builder.add(Registries.BIOME, bootstrap -> {
            ChromaBiomes.bootstrap(bootstrap);
            reika.chromaticraft.world.dimension.biome.ProximaBiomeDefinitions.bootstrap(bootstrap);
        });
        builder.add(Registries.FEATURE, bootstrap -> {
            registerFeature(bootstrap, CAVE_CRYSTAL, new reika.chromaticraft.world.CrystalFeature());
            registerFeature(bootstrap, id("cave_indicator"), new reika.chromaticraft.world.CaveIndicatorFeature());
            registerFeature(bootstrap, id("unknown_artefact"), new reika.chromaticraft.world.UnknownArtefactFeature());
            registerFeature(bootstrap, id("warp_node"), new reika.chromaticraft.world.WarpNodeFeature());
            registerFeature(bootstrap, id("skypeater"), new reika.chromaticraft.world.SkypeaterFeature());
            registerFeature(bootstrap, id("ender_forest_tree"), new reika.chromaticraft.world.EnderForestTreeFeature());
            registerFeature(bootstrap, id("ender_pool"), new reika.chromaticraft.world.EnderPoolFeature());
            registerFeature(bootstrap, id("ender_forest_flower"), new reika.chromaticraft.world.EnderForestFlowerFeature());
            // Vanilla's flower_default placement under ChromatiCraft's name, so that it is *not* in
            // minecraft:can_spawn_from_bone_meal, with the flower mix corrected to V33a's. The Ender Forest's world-generation flowers stay exactly
            // vanilla's, while bone meal in that biome sees only ender_forest_flower -- the V33a
            // plantFlower override -- instead of choosing between the two.
            bootstrap.register(featureKey("ender_forest_default_flowers"),
                    new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                                    new net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider(
                                            net.minecraft.util.random.WeightedList.<net.minecraft.world.level.block.state.BlockState>builder()
                                                    // V33a BiomeGenForest (type 0) leaves func_150572_a to
                                                    // BiomeGenBase: a dandelion two times in three. 26.2's
                                                    // flower_default flipped that to mostly poppies.
                                                    .add(net.minecraft.world.level.block.Blocks.DANDELION.defaultBlockState(), 2)
                                                    .add(net.minecraft.world.level.block.Blocks.POPPY.defaultBlockState(), 1))));
            registerFeature(bootstrap, id("rainbow_tree"), new reika.chromaticraft.world.RainbowTreeFeature());
            bootstrap.register(featureKey("color_tree"), new reika.chromaticraft.world.ColorTreeFeature(colorTreeConfig()));
            registerFeature(bootstrap, id("no_floating_gravel"), new reika.chromaticraft.world.luminous.NoFloatingGravelFeature());
            registerFeature(bootstrap, id("luminous_cliffs_aux"), new reika.chromaticraft.world.luminous.LuminousCliffsAuxFeature());
            // V33a BiomeGenBase.func_150572_a, which the Luminous Cliffs do not override: a dandelion
            // two times in three, otherwise a poppy.
            bootstrap.register(featureKey("luminous_flowers"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new WeightedStateProvider(WeightedList.<BlockState>builder()
                                    .add(Blocks.DANDELION.defaultBlockState(), 2)
                                    .add(Blocks.POPPY.defaultBlockState(), 1))));
            // V33a BiomeGlowingCliffs' Forge flower list, which is what bone meal's plantFlower draws
            // from: BiomeGenBase's defaults (dandelion 20, poppy 10) plus every red-flower metadata and
            // the dandelion again at 20 each.
            bootstrap.register(LUMINOUS_CLIFFS_FLOWER, new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new WeightedStateProvider(WeightedList.<BlockState>builder()
                                    .add(Blocks.DANDELION.defaultBlockState(), 40)
                                    .add(Blocks.POPPY.defaultBlockState(), 30)
                                    .add(Blocks.BLUE_ORCHID.defaultBlockState(), 20)
                                    .add(Blocks.ALLIUM.defaultBlockState(), 20)
                                    .add(Blocks.AZURE_BLUET.defaultBlockState(), 20)
                                    .add(Blocks.RED_TULIP.defaultBlockState(), 20)
                                    .add(Blocks.ORANGE_TULIP.defaultBlockState(), 20)
                                    .add(Blocks.WHITE_TULIP.defaultBlockState(), 20)
                                    .add(Blocks.PINK_TULIP.defaultBlockState(), 20)
                                    .add(Blocks.OXEYE_DAISY.defaultBlockState(), 20))));
            bootstrap.register(featureKey("luminous_lily_pad"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.LILY_PAD.defaultBlockState())));
            bootstrap.register(featureKey("luminous_brown_mushroom"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.BROWN_MUSHROOM.defaultBlockState())));
            bootstrap.register(featureKey("luminous_red_mushroom"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.RED_MUSHROOM.defaultBlockState())));
            registerFeature(bootstrap, id("rainbow_forest_tree"), new reika.chromaticraft.world.RainbowForestTreeFeature());
            registerFeature(bootstrap, id("dye_flower_patch"), new reika.chromaticraft.world.DyeFlowerPatchFeature());
            registerFeature(bootstrap, id("rainbow_forest_underground"), new reika.chromaticraft.world.RainbowForestUndergroundFeature());
            registerFeature(bootstrap, id("rainbow_forest_flower"), new reika.chromaticraft.world.RainbowForestFlowerFeature());
            // V33a WorldGenFlowers(yellow_flower), WorldGenFlowers(red_flower) and WorldGenTallGrass
            // (tallgrass meta 1): single blocks, each only where it can stand (SimpleBlockFeature).
            bootstrap.register(featureKey("rainbow_dandelion"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.DANDELION.defaultBlockState())));
            bootstrap.register(featureKey("rainbow_rose"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.POPPY.defaultBlockState())));
            bootstrap.register(featureKey("rainbow_grass"), new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                            new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.SHORT_GRASS.defaultBlockState())));
            for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
                registerFeature(bootstrap, id(flower.registryName()), new reika.chromaticraft.world.DecoFlowerFeature(flower));
            registerFeature(bootstrap, id("glow_daisy"), new reika.chromaticraft.world.CliffsFlowerFeature(false));
            registerFeature(bootstrap, id("glow_root"), new reika.chromaticraft.world.CliffsFlowerFeature(true));
            for (ChromaTieredPlants plant : ChromaTieredPlants.list)
                registerFeature(bootstrap, id(plant.registryName()), new reika.chromaticraft.world.TieredPlantFeature(plant,
                        () -> ChromaBlocks.tieredPlant(plant).get().defaultBlockState()));
            registerFeature(bootstrap, NATURAL_PYLON, new reika.chromaticraft.world.PylonFeature());
            for (CrystalElement element : CrystalElement.elements)
                registerFeature(bootstrap, coloredPylon(element), new reika.chromaticraft.world.PylonFeature(element));
            registerFeature(bootstrap, DATA_TOWER, new reika.chromaticraft.world.DataTowerFeature());
            registerFeature(bootstrap, RUNIC_ALTAR, new reika.chromaticraft.world.RunicAltarFeature());
            registerFeature(bootstrap, TURBOCHARGED_PYLON, new reika.chromaticraft.world.PylonFeature(reika.chromaticraft.world.PylonFeature.Variant.TURBOCHARGED));
            registerFeature(bootstrap, POWER_CRYSTAL_BOOSTED_PYLON, new reika.chromaticraft.world.PylonFeature(reika.chromaticraft.world.PylonFeature.Variant.POWER_CRYSTAL_BOOSTED));
            registerFeature(bootstrap, LUMINOUS_CLIFFS_TERRAIN, new reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature());
            registerFeature(bootstrap, LUMA_PATCH, new reika.chromaticraft.world.luminous.LumaPatchFeature());
            registerFeature(bootstrap, NETHER_ROOF_STRUCTURE, new reika.chromaticraft.world.NetherRoofStructureFeature());
            registerFeature(bootstrap, NETHER_LAVA_RIVER, new reika.chromaticraft.world.NetherLavaRiverFeature());
            registerFeature(bootstrap, CRYSTAL_SHRUB, new reika.chromaticraft.world.dimension.CrystalShrubFeature());
            registerFeature(bootstrap, FLOATSTONE, new reika.chromaticraft.world.dimension.FloatstoneFeature());
            registerFeature(bootstrap, CRYSTAL_TREE, new reika.chromaticraft.world.dimension.CrystalTreeFeature());
            registerFeature(bootstrap, GLOW_TREE, new reika.chromaticraft.world.dimension.GlowTreeFeature());
            registerFeature(bootstrap, TREE_CLUSTER, new reika.chromaticraft.world.dimension.TreeClusterFeature());
            registerFeature(bootstrap, FIRE_JET, new reika.chromaticraft.world.dimension.FireJetFeature());
            registerFeature(bootstrap, MINI_ALTAR, new reika.chromaticraft.world.dimension.MiniAltarFeature());
            registerFeature(bootstrap, FISSURE, new reika.chromaticraft.world.dimension.FissureFeature());
            registerFeature(bootstrap, GLOWING_CRACKS, new reika.chromaticraft.world.dimension.GlowingCracksFeature());
            registerFeature(bootstrap, LIGHTED_SHRUB, new reika.chromaticraft.world.dimension.LightedShrubFeature());
            registerFeature(bootstrap, CRYSTAL_PIT, new reika.chromaticraft.world.dimension.CrystalPitFeature());
            registerFeature(bootstrap, AURORAE, new reika.chromaticraft.world.dimension.AuroraeFeature());
            for (Identifier structure : NETHER_ROOF_STRUCTURES)
                registerFeature(bootstrap, structure, new reika.chromaticraft.world.NetherRoofStructureFeature(
                        reika.chromaticraft.world.NetherRoofStructureFeature.Type.valueOf(
                                structure.getPath().substring("nether_".length()).toUpperCase(java.util.Locale.ROOT))));
            registerFeature(bootstrap, NATURAL_CAVERN, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.CAVERN, true));
            registerFeature(bootstrap, CAVERN, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.CAVERN, false));
            registerFeature(bootstrap, NATURAL_BURROW, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.BURROW, true));
            registerFeature(bootstrap, BURROW, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.BURROW, false));
            registerFeature(bootstrap, NATURAL_OCEAN, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.OCEAN, true));
            registerFeature(bootstrap, OCEAN, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.OCEAN, false));
            registerFeature(bootstrap, NATURAL_DESERT, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.DESERT, true));
            registerFeature(bootstrap, DESERT, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.DESERT, false));
			registerFeature(bootstrap, NATURAL_SNOW, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.SNOW, true));
			registerFeature(bootstrap, SNOW, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.SNOW, false));
			registerFeature(bootstrap, NATURAL_BIOME_FRAGMENT, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.BIOME_FRAGMENT, true));
			registerFeature(bootstrap, BIOME_FRAGMENT, new reika.chromaticraft.world.OverworldStructureFeature(reika.chromaticraft.world.OverworldStructureFeature.Type.BIOME_FRAGMENT, false));
            for (TieredOre ore : TIERED_ORES) {
                // V33a BlockExcludingOreVein targets only the host block, which is what makes these
                // ores replace stone/netherrack and never the cliff material it explicitly excluded.
                bootstrap.register(featureKey(ore.name()), new net.minecraft.world.level.levelgen.feature.OreFeature(
                        new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(ore.host()),
                        ore.block().get().defaultBlockState(), ore.veinSize()));
            }
            var belowTrunk = bootstrap.lookup(Registries.BLOCK_STATE_PROVIDER)
                    .getOrThrow(net.minecraft.data.worldgen.BlockStateProviders.SOIL_BENEATH_TREE);
            // V33a BlockDyeSapling grows a TreeShaper tree, not a vanilla blob tree.
            for (CrystalElement element : CrystalElement.elements) {
                bootstrap.register(dyeTree(element), new reika.chromaticraft.world.DyeTreeFeature(
                        reika.chromaticraft.world.DyeTreeFeature.Config.sapling(element)));
            }
            bootstrap.register(GLOWING_TREE, glowingTreeConfiguration(belowTrunk));
            // V33a setGlowChance(10): one leaf in ten glows. The small tree is DragonAPI's
            // ModifiableSmallTrees, 1.7.10's plain oak, whose 26.2 form is vanilla's oak shape.
            bootstrap.register(UNDERGROUND_GLOWING_TREE_SMALL,
                    new net.minecraft.world.level.levelgen.feature.TreeFeature.Builder(new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.OAK_LOG.defaultBlockState()),
                            new StraightTrunkPlacer(4, 2, 0), glowingFoliage(9),
                            new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                            new TwoLayersFeatureSize(1, 0, 1),
                            belowTrunk).ignoreVines().build());
            bootstrap.register(UNDERGROUND_GLOWING_TREE_BIG,
                    fancyGlowingTree(glowingFoliage(9), belowTrunk));
        });
        // Proxima's oversized decoration generates as structures rather than features, so its pieces
        // can be written chunk by chunk with a clipped box. See GlassCliffPiece.
        builder.add(Registries.STRUCTURE, bootstrap -> {
            var biomes = bootstrap.lookup(Registries.BIOME);
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GLASS_CLIFF,
                    new reika.chromaticraft.world.dimension.structure.GlassCliffStructure(
                            new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                                    // V33a generateIn: Glass Cliffs are the Plains biome's alone.
                                    net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
                                            reika.chromaticraft.world.dimension.biome.ProximaBiomes.PLAINS.biomeKey())),
                                    java.util.Map.of(),
                                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                                    // The cliff lays its own terrain; vanilla must not also beard it.
                                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
            // V33a GlowingCliffsAuxGenerator's islands: every Luminous Cliffs chunk rolls for one.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LUMINOUS_ISLAND,
                    new reika.chromaticraft.world.luminous.LuminousIslandStructure(
                            new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                                    net.minecraft.core.HolderSet.direct(
                                            biomes.getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS),
                                            biomes.getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS_SHORES)),
                                    java.util.Map.of(),
                                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
            // The glowing caves. V33a generateIn: the Sanctuary, the Glowing Forest and the Crystal
            // Plains. A cave wanders far past a feature's write window, which is why it is here at all.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GLOW_CAVE,
                    new reika.chromaticraft.world.dimension.structure.GlowCaveStructure(
                            new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                                    net.minecraft.core.HolderSet.direct(
                                            biomes.getOrThrow(reika.chromaticraft.world.dimension.biome
                                                    .ProximaBiomes.CENTER.biomeKey()),
                                            biomes.getOrThrow(reika.chromaticraft.world.dimension.biome
                                                    .ProximaBiomes.FOREST.biomeKey()),
                                            // V33a tests b.biomeType == FOREST, which includes the
                                            // Crystal Forest sub-biome rather than only its parent.
                                            biomes.getOrThrow(reika.chromaticraft.world.dimension.biome
                                                    .ProximaSubBiomes.CRYSFOREST.biomeKey()),
                                            biomes.getOrThrow(reika.chromaticraft.world.dimension.biome
                                                    .ProximaBiomes.PLAINS.biomeKey())),
                                    java.util.Map.of(),
                                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
                                    // The cave hollows itself out of terrain that is already there;
                                    // bearding it would fill the mouth back in.
                                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
            // The monument. Its biome is the Monument Field, which BiomeDistributor paints around the
            // ring's centre for exactly this reason -- so the one place the monument may stand is the
            // one place that biome exists.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.MONUMENT,
                    new reika.chromaticraft.world.dimension.structure.ProximaMonumentStructure(
                            new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                                    net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
                                            reika.chromaticraft.world.dimension.biome.ProximaBiomes.MONUMENT.biomeKey())),
                                    java.util.Map.of(),
                                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                                    // The piece hollows its own clearing and lays its own floor, so
                                    // vanilla must not also raise terrain to meet it.
                                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LIGHT_PANEL,
					new reika.chromaticraft.world.dimension.structure.ProximaLightPanelStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.THREE_D_MAZE,
					new reika.chromaticraft.world.dimension.structure.ProximaThreeDMazeStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.MUSIC,
					new reika.chromaticraft.world.dimension.structure.ProximaMusicStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GOL,
					new reika.chromaticraft.world.dimension.structure.ProximaGOLStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LASER,
					new reika.chromaticraft.world.dimension.structure.ProximaLaserStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.WATER,
					new reika.chromaticraft.world.dimension.structure.ProximaWaterStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.PISTONTAPE,
					new reika.chromaticraft.world.dimension.structure.ProximaPistonTapeStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.direct(biomes.getOrThrow(
											reika.chromaticraft.world.dimension.biome.ProximaBiomes.STRUCTURE.biomeKey())),
									java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
            // Player-built and command-only: no structure set points here, but registering the
            // assembly as a Structure gives it vanilla's /place structure semantics and lets its NBT
            // template span chunks without unsafe feature writes.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.PORTAL,
                    new reika.chromaticraft.world.dimension.structure.PortalStructureCommand(
                            new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
                                    net.minecraft.core.HolderSet.empty(), java.util.Map.of(),
                                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
                                    net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
			registerCastingTemple(bootstrap,
					reika.chromaticraft.world.dimension.structure.ProximaStructures.CASTING_TEMPLE_L1, 1);
			registerCastingTemple(bootstrap,
					reika.chromaticraft.world.dimension.structure.ProximaStructures.CASTING_TEMPLE_L2, 2);
			registerCastingTemple(bootstrap,
					reika.chromaticraft.world.dimension.structure.ProximaStructures.CASTING_TEMPLE_L3, 3);
			// Full command inspection form of the NBT-backed Burrow. Natural placement remains the
			// configured feature because V33a rolls its annexes and validates its host terrain there.
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.BURROW,
					new reika.chromaticraft.world.dimension.structure.BurrowCommandStructure(
							new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
									net.minecraft.core.HolderSet.empty(), java.util.Map.of(),
									net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
									net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE)));
        });
        builder.add(Registries.STRUCTURE_SET, bootstrap -> {
            var structures = bootstrap.lookup(Registries.STRUCTURE);
            // V33a rolls every chunk (generateIslands runs per chunk), so every chunk is a candidate
            // and the structure's own roll does the thinning.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LUMINOUS_ISLAND_SET,
                    new net.minecraft.world.level.levelgen.structure.StructureSet(
                            structures.getOrThrow(
                                    reika.chromaticraft.world.dimension.structure.ProximaStructures.LUMINOUS_ISLAND),
                            new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(
                                    1, 0,
                                    net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR,
                                    0x1A1A4D)));
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GLASS_CLIFF_SET,
                    new net.minecraft.world.level.levelgen.structure.StructureSet(
                            structures.getOrThrow(
                                    reika.chromaticraft.world.dimension.structure.ProximaStructures.GLASS_CLIFF),
                            // V33a getGenerationChance is 0.015 per chunk, so roughly one per eight
                            // chunks squared; a spacing of 8 with separation 3 is that density with the
                            // spread modern structure placement needs to keep them apart.
                            new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(
                                    8, 3,
                                    net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR,
                                    0x6C1FF)));
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GLOW_CAVE_SET,
                    new net.minecraft.world.level.levelgen.structure.StructureSet(
                            structures.getOrThrow(
                                    reika.chromaticraft.world.dimension.structure.ProximaStructures.GLOW_CAVE),
                            // V33a getGenerationChance is 0.0025 in the Sanctuary and 0.00125 elsewhere
                            // -- one cave per four hundred chunks at best, which is what makes finding
                            // one an event. A spacing of 24 with separation 12 is that density.
                            new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(
                                    24, 12,
                                    net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR,
                                    0x91C4E)));
            // One monument, at the position the layout chose. MonumentPlacement names that single
            // chunk; there is nothing to space or scatter.
            bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.MONUMENT_SET,
                    new net.minecraft.world.level.levelgen.structure.StructureSet(
                            structures.getOrThrow(
                                    reika.chromaticraft.world.dimension.structure.ProximaStructures.MONUMENT),
                            new reika.chromaticraft.world.dimension.structure.MonumentPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LIGHT_PANEL_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.LIGHT_PANEL),
							new reika.chromaticraft.world.dimension.structure.LightPanelPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.THREE_D_MAZE_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.THREE_D_MAZE),
							new reika.chromaticraft.world.dimension.structure.ThreeDMazePlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.MUSIC_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.MUSIC),
							new reika.chromaticraft.world.dimension.structure.MusicPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.GOL_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.GOL),
							new reika.chromaticraft.world.dimension.structure.GOLPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.LASER_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.LASER),
							new reika.chromaticraft.world.dimension.structure.LaserPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.WATER_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.WATER),
							new reika.chromaticraft.world.dimension.structure.WaterPlacement()));
			bootstrap.register(reika.chromaticraft.world.dimension.structure.ProximaStructures.PISTONTAPE_SET,
					new net.minecraft.world.level.levelgen.structure.StructureSet(
							structures.getOrThrow(
									reika.chromaticraft.world.dimension.structure.ProximaStructures.PISTONTAPE),
							new reika.chromaticraft.world.dimension.structure.PistonTapePlacement()));
        });
        builder.add(Registries.PLACED_FEATURE, bootstrap -> {
            HolderGetter<Feature> configured = bootstrap.lookup(Registries.FEATURE);
            registerPlaced(bootstrap, configured, CAVE_CRYSTAL);
            registerPlaced(bootstrap, configured, id("cave_indicator"), List.of(BiomeFilter.biome()));
            // The feature owns its own tower-ring filter and 1-in-40 roll; no count modifiers.
            registerPlaced(bootstrap, configured, id("unknown_artefact"), List.of(BiomeFilter.biome()));
            // The feature owns its own 2048-chunk grid and biome roll; no count modifiers.
            registerPlaced(bootstrap, configured, id("warp_node"), List.of(BiomeFilter.biome()));
            // The feature owns its own 4x4 shuffled lattice; no count modifiers.
            registerPlaced(bootstrap, configured, id("skypeater"), List.of(BiomeFilter.biome()));
            // V33a ColorTreeGenerator: once per chunk at the chunk corner, which is where it read the
            // biome. No biome filter: the feature reads the surface biome itself, where a filter would
            // test the origin's (bottom-of-world) biome.
            registerPlaced(bootstrap, configured, id("color_tree"), List.of());
            // V33a thins the biome's trees to 0.7x; vanilla forest is 10 per chunk, so 7 attempts,
            // and the selector's own "no tree" entry does the rest of the thinning per position.
            registerPlaced(bootstrap, configured, id("ender_forest_tree"), List.of(
                    CountPlacement.of(7),
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                            .onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING),
                    BiomeFilter.biome()));
            // V33a DecoratorEnderForest runs after the biome's own decoration, one attempt per chunk at
            // the surface; the ENDERPOOLS chance is rolled inside the feature from the live option.
            registerPlaced(bootstrap, configured, id("ender_pool"), List.of(
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                            .onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // Bone meal only: bone meal finds the configured feature through the biome's list, but this
            // must never run during world generation, where upstream did not use plantFlower at all.
            registerPlaced(bootstrap, configured, id("ender_forest_flower"),
                    List.of(CountPlacement.of(0), BiomeFilter.biome()));
            // vanilla VegetationPlacements.FLOWER_DEFAULT's modifiers, unchanged.
            registerPlaced(bootstrap, configured, id("ender_forest_default_flowers"), List.of(
                    RarityFilter.onAverageOnceEvery(32),
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                            .onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING),
                    BiomeFilter.biome(),
                    CountPlacement.of(64),
                    net.minecraft.world.level.levelgen.placement.OffsetPlacement.ofTriangle(7, 3),
                    net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                            net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.ONLY_IN_AIR_PREDICATE)));
            for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
                registerPlaced(bootstrap, configured, id(flower.registryName()), List.of(
                        RarityFilter.onAverageOnceEvery(flower.generationChance()),
                        BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, id("glow_daisy"), List.of(RarityFilter.onAverageOnceEvery(2), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, id("glow_root"), List.of(BiomeFilter.biome()));
            for (ChromaTieredPlants plant : ChromaTieredPlants.list)
                registerPlaced(bootstrap, configured, id(plant.registryName()), List.of(
                        RarityFilter.onAverageOnceEvery(plant.generationChance()),
                        CountPlacement.of(plant.generationCount()),
                        InSquarePlacement.spread(),
                        BiomeFilter.biome()));
			// V33a Rock Flower changes from one-in-two chunks to every chunk in dimension 1.
			bootstrap.register(placedKey("rock_flower_end"), new PlacedFeature(
					configured.getOrThrow(featureKey(ChromaTieredPlants.ROCK_FLOWER.registryName())),
					List.of(CountPlacement.of(ChromaTieredPlants.ROCK_FLOWER.generationCount()),
							InSquarePlacement.spread(), BiomeFilter.biome())));
            for (TieredOre ore : TIERED_ORES) {
                // V33a: `if (rand.nextInt(genChance) == 0) for (k < veinCount)` at a random column in
                // the chunk, each attempt rolling its own y. A genChance of one is every chunk.
                List<PlacementModifier> modifiers = new java.util.ArrayList<>();
                if (ore.genChance() > 1)
                    modifiers.add(net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(ore.genChance()));
                modifiers.add(net.minecraft.world.level.levelgen.placement.CountPlacement.of(ore.veinCount()));
                modifiers.add(net.minecraft.world.level.levelgen.placement.InSquarePlacement.spread());
                modifiers.add(new reika.chromaticraft.world.TieredOreHeightPlacement(ore.deepBand()));
                modifiers.add(net.minecraft.world.level.levelgen.placement.BiomeFilter.biome());
                registerPlaced(bootstrap, configured, id(ore.name()), modifiers);
            }
            registerPlaced(bootstrap, configured, NATURAL_PYLON);
            for (CrystalElement element : CrystalElement.elements)
                registerPlaced(bootstrap, configured, coloredPylon(element));
            registerPlaced(bootstrap, configured, DATA_TOWER);
            registerPlaced(bootstrap, configured, RUNIC_ALTAR);
            registerPlaced(bootstrap, configured, TURBOCHARGED_PYLON);
            // Command-only, like the pylon variants: no biome modifier names these.
            registerPlaced(bootstrap, configured, POWER_CRYSTAL_BOOSTED_PYLON);
            registerPlaced(bootstrap, configured, LUMINOUS_CLIFFS_TERRAIN, List.of(BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, LUMA_PATCH,
                    List.of(CountPlacement.of(1), BiomeFilter.biome()));
            // V33a BASE_GEN_FACTOR = 1/64 per Nether chunk; the feature retains its weighted type roll.
            registerPlaced(bootstrap, configured, NETHER_ROOF_STRUCTURE, List.of(
                    RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(), BiomeFilter.biome()));
            // V33a runs the lava rivers for every Nether chunk, unconditionally: no rarity gate and no
            // in-square spread, because the feature walks its own chunk's sixteen by sixteen columns
            // itself and needs the origin to stay on the chunk corner.
            registerPlaced(bootstrap, configured, NETHER_LAVA_RIVER, List.of(BiomeFilter.biome()));
            // V33a's decorator gives the crystal shrub a generation chance of 1 per chunk and rolls
            // the size itself, refusing most attempts, so the placement is one attempt per chunk on
            // the surface rather than a rarity gate here.
            // V33a getGenerationChance for the aurorae is 0.03125/4, so one chunk in a hundred and
            // twenty-eight. One placement lays a whole display of up to twelve ribbons.
            registerPlaced(bootstrap, configured, AURORAE, List.of(
                    RarityFilter.onAverageOnceEvery(128), InSquarePlacement.spread(), BiomeFilter.biome()));
            // V33a getGenerationChance for the geode is 0.01875 per chunk outside the central region,
            // so about one in fifty-three; its own site check refuses many of those again.
            registerPlaced(bootstrap, configured, CRYSTAL_PIT, List.of(
                    RarityFilter.onAverageOnceEvery(53), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the crystal tree is 0.5 per chunk, and its own space check
            // refuses most attempts once a stand has grown in.
            registerPlaced(bootstrap, configured, CRYSTAL_TREE, List.of(
                    RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the glowing tree is per biome: 0.8 in the Iridescent
            // Archipelago, 0.5 in the Luminescent Sanctuary and by default, 0.1 in the Sparkling Sands
            // and 0.05 on the Crystal Plains. A placed feature carries one rate, so the default is the
            // one expressed -- it is the rate the Sanctuary itself wants, and it is what upstream falls
            // back to for every biome that does not name its own. The site check refuses most attempts
            // once a stand has grown in, which is what keeps a one-in-two chunk chance from producing a
            // solid forest.
            registerPlaced(bootstrap, configured, GLOW_TREE, List.of(
                    RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the fire jet is 0.1 per chunk, and its own site check --
            // two blocks of water above the bed -- refuses most of those again, so the jets end up
            // only in real pools.
            registerPlaced(bootstrap, configured, FIRE_JET, List.of(
                    RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the tree cluster is a flat 0.67 per chunk, so two chunks in
            // three. Deliberately no InSquarePlacement: the cluster scatters its own trees sixteen
            // blocks about the anchor and a giant reaches five more, so randomising the anchor inside
            // the chunk as well would push the far side outside the feature write window, where the
            // writes are dropped in silence. The feature moves this unsquared chunk-minimum origin
            // to the chunk centre before applying upstream's own scatter.
            registerPlaced(bootstrap, configured, TREE_CLUSTER, List.of(
                    RarityFilter.onAverageOnceEvery(3),
                    net.minecraft.world.level.levelgen.placement.CountPlacement.of(2),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the glowing bush is 4 per chunk in the Glowing Forest, 2 in
            // most biomes and 0.25 in the Sparkling Sands. This is ground cover, not a rarity: two per
            // chunk is the figure expressed, since it is what all but two biomes get, and CountPlacement
            // is what a chance above one means. Its own site check -- grass directly beneath -- is what
            // thins it on broken ground.
            registerPlaced(bootstrap, configured, LIGHTED_SHRUB, List.of(
                    net.minecraft.world.level.levelgen.placement.CountPlacement.of(2),
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the cracks is a flat 0.125 per chunk -- one in eight, the
            // most common thing in this list -- and its own site check wants nine by nine of unbroken
            // grass, which is what actually makes them rare outside flat open country.
            registerPlaced(bootstrap, configured, GLOWING_CRACKS, List.of(
                    RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the fissure is 0.05 on the Crystal Plains and 0.01 elsewhere.
            // The rarer rate is the one expressed, as with the other per-biome splits here. No
            // InSquarePlacement: the footprint wanders up to a dozen blocks from its anchor in each of
            // up to four directions, and randomising the anchor within the chunk as well would push the
            // far side past the feature write window, where the writes are dropped in silence.
            registerPlaced(bootstrap, configured, FISSURE, List.of(
                    RarityFilter.onAverageOnceEvery(100),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance for the altar is a flat 0.025 per chunk -- once every forty --
            // and its own site check refuses anything that is not seven by seven of open grass, so the
            // altars end up rare and always on level ground. InSquarePlacement is safe here: the whole
            // build is a seven-by-seven platform with a five-by-five burrow under it, so even anchored
            // at the far corner of a chunk it stays well inside the write window.
            registerPlaced(bootstrap, configured, MINI_ALTAR, List.of(
                    RarityFilter.onAverageOnceEvery(40), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // V33a getGenerationChance: 0.1 per chunk in Skylands and Voidlands, 0.02 elsewhere. The
            // rarer case is the one expressed here; the dimension's own biome gating decides the rest.
            registerPlaced(bootstrap, configured, FLOATSTONE, List.of(
                    RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, CRYSTAL_SHRUB, List.of(
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                    BiomeFilter.biome()));
            // Named command/debug seams do not carry a rarity gate.
            for (Identifier structure : NETHER_ROOF_STRUCTURES)
                registerPlaced(bootstrap, configured, structure);
            // V33a DungeonGenerator zones: once per chunk, and the feature asks DungeonZones whether this
            // chunk is the planned one for its Voronoi zone (noise scale 144/240/640/440/480/640 blocks,
            // exclusion 64/64/512/256/256/440) before making its V33a tries.
            registerPlaced(bootstrap, configured, NATURAL_CAVERN, List.of());
            registerPlaced(bootstrap, configured, CAVERN);
            registerPlaced(bootstrap, configured, NATURAL_BURROW, List.of());
            registerPlaced(bootstrap, configured, BURROW);
            registerPlaced(bootstrap, configured, NATURAL_OCEAN, List.of());
            registerPlaced(bootstrap, configured, OCEAN);
            registerPlaced(bootstrap, configured, NATURAL_DESERT, List.of());
            registerPlaced(bootstrap, configured, DESERT);
			registerPlaced(bootstrap, configured, NATURAL_SNOW, List.of());
			registerPlaced(bootstrap, configured, SNOW);
			registerPlaced(bootstrap, configured, NATURAL_BIOME_FRAGMENT, List.of());
			registerPlaced(bootstrap, configured, BIOME_FRAGMENT);
            registerRainbowPlacements(bootstrap, configured);
            registerLuminousPlacements(bootstrap, configured);
            // Attached to every overworld biome, like the terrain pass; it checks each column itself.
            registerPlaced(bootstrap, configured, id("luminous_cliffs_aux"), List.of(BiomeFilter.biome()));
        });
        return builder;
    }

    /**
     * The Rainbow Forest's decoration. The whole-chunk passes are placed at the chunk's surface so the
     * biome filter reads the surface biome, which is what V33a's 2D biome lookup meant.
     */
    private static void registerRainbowPlacements(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<Feature> configured) {
        PlacementModifier surface = net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                .onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        // V33a: nine attempts per chunk, each standing on its column's top solid block.
        registerPlaced(bootstrap, configured, id("rainbow_forest_tree"), List.of(
                CountPlacement.of(9), InSquarePlacement.spread(), surface, BiomeFilter.biome()));
        registerPlaced(bootstrap, configured, id("dye_flower_patch"), List.of(surface, BiomeFilter.biome()));
        registerPlaced(bootstrap, configured, id("rainbow_forest_underground"), List.of(surface, BiomeFilter.biome()));
        // Bone meal only; see ender_forest_flower.
        registerPlaced(bootstrap, configured, id("rainbow_forest_flower"), List.of(CountPlacement.of(0), BiomeFilter.biome()));
        for (RainbowDecoration deco : List.of(RAINBOW_FOREST_DECORATION, RAINBOW_STREAM_DECORATION))
            registerRainbowDecoration(bootstrap, configured, deco);
    }

    /**
     * V33a {@code RainbowForestDecorator.auxDeco}, with 1.7.10's own generators reproduced in placement
     * modifiers:
     * <ul>
     * <li>disks are vanilla's (WorldGenSand(sand, 7) and WorldGenClay(4) are its radius 2-6 and 2-3
     * disks, placed on the top solid block and only under water), with upstream's counts;</li>
     * <li>flowers, grass and reeds start at {@code rand(128)}, a random height below 128, and succeed only
     * where that lands near ground, which is why 1.7.10 needed so many attempts;</li>
     * <li>WorldGenFlowers is 64 tries spread {@code rand(8)-rand(8)} across and {@code rand(4)-rand(4)}
     * up, into air, where the flower can stand: a triangle(7, 3) offset exactly;</li>
     * <li>WorldGenTallGrass first sinks through air and leaves to the ground, then makes 128 such tries;
     * </li>
     * <li>WorldGenReed is 20 tries spread {@code rand(4)-rand(4)} at the same height, into air beside
     * water, each a column of 2 + rand(rand(3)+1); vanilla's sugar cane configured feature is that
     * column exactly.</li>
     * </ul>
     */
    private static void registerRainbowDecoration(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<Feature> configured, RainbowDecoration deco) {
        var sand = configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.DISK_SAND);
        var clay = configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.DISK_CLAY);
        bootstrap.register(deco.sandDisk(), new PlacedFeature(sand, disk(deco.sandDisks())));
        bootstrap.register(deco.clayDisk(), new PlacedFeature(clay, disk(deco.clayDisks())));
        bootstrap.register(deco.lateSandDisk(), new PlacedFeature(sand, disk(deco.lateSandDisks())));

        PlacementModifier legacyHeight = net.minecraft.world.level.levelgen.placement.HeightRangePlacement.uniform(
                net.minecraft.world.level.levelgen.VerticalAnchor.absolute(0),
                net.minecraft.world.level.levelgen.VerticalAnchor.absolute(127));
        PlacementModifier intoAir = net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.ONLY_IN_AIR_PREDICATE);
        PlacementModifier flowerSpread = net.minecraft.world.level.levelgen.placement.OffsetPlacement.ofTriangle(7, 3);

        bootstrap.register(deco.dandelion(), new PlacedFeature(configured.getOrThrow(featureKey("rainbow_dandelion")),
                List.of(CountPlacement.of(deco.dandelions()), InSquarePlacement.spread(), legacyHeight, BiomeFilter.biome(),
                        CountPlacement.of(64), flowerSpread, intoAir)));
        if (deco.roses())
            bootstrap.register(deco.rose(), new PlacedFeature(configured.getOrThrow(featureKey("rainbow_rose")),
                    List.of(CountPlacement.of(deco.dandelions()), RarityFilter.onAverageOnceEvery(4),
                            InSquarePlacement.spread(), legacyHeight, BiomeFilter.biome(),
                            CountPlacement.of(64), flowerSpread, intoAir)));

        PlacementModifier groundBelow = reika.chromaticraft.world.SinkThroughAirPlacement.INSTANCE;
        bootstrap.register(deco.grassPatch(), new PlacedFeature(configured.getOrThrow(featureKey("rainbow_grass")),
                List.of(CountPlacement.of(deco.grass()), InSquarePlacement.spread(), legacyHeight, groundBelow,
                        BiomeFilter.biome(), CountPlacement.of(128), flowerSpread, intoAir)));

        bootstrap.register(deco.reedPatch(), new PlacedFeature(
                configured.getOrThrow(net.minecraft.data.worldgen.features.VegetationFeatures.SUGAR_CANE),
                List.of(CountPlacement.of(deco.reeds()), InSquarePlacement.spread(), legacyHeight, BiomeFilter.biome(),
                        CountPlacement.of(20),
                        net.minecraft.world.level.levelgen.placement.OffsetPlacement.ofTriangle(3, 0),
                        net.minecraft.data.worldgen.features.VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE))));
    }

    /**
     * V33a {@code GlowingCliffsDecorator.genDecorations}, with 1.7.10's generators reproduced in
     * placement modifiers as for the Rainbow Forest (see {@link #registerRainbowDecoration}):
     * <ul>
     * <li>ten gravel veins anywhere from y 0 to 255, which refuse cliff stone and unsupported cells;</li>
     * <li>six sand disks and one clay disk under water, then two gravel veins on the top solid block;</li>
     * <li>four trees, five one chunk in ten: a big glowing tree one time in twenty, else a big oak one
     * time in ten, else an oak;</li>
     * <li>eight flower attempts at {@code rand(height+32)}, three grass, two lily pads and twenty sugar
     * cane at {@code rand(height*2)}, and a brown mushroom patch one chunk in four and a red one in
     * eight;</li>
     * <li>twice the ordinary springs: 100 water at {@code rand(rand(248)+8)} and 40 lava at
     * {@code rand(rand(rand(240)+8)+8)}, heights vanilla's biased providers reproduce exactly.</li>
     * </ul>
     */
    private static void registerLuminousPlacements(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<Feature> configured) {
        var gravel = configured.getOrThrow(featureKey("no_floating_gravel"));
        bootstrap.register(LUMINOUS_GRAVEL_ORE_PLACED, new PlacedFeature(gravel, List.of(
                CountPlacement.of(10), InSquarePlacement.spread(),
                net.minecraft.world.level.levelgen.placement.HeightRangePlacement.uniform(
                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(0),
                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(255)),
                BiomeFilter.biome())));
        var sand = configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.DISK_SAND);
        var clay = configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.DISK_CLAY);
        bootstrap.register(LUMINOUS_DISK_SAND_PLACED, new PlacedFeature(sand, disk(6)));
        bootstrap.register(LUMINOUS_DISK_CLAY_PLACED, new PlacedFeature(clay, disk(1)));
        bootstrap.register(LUMINOUS_GRAVEL_PATCH_PLACED, new PlacedFeature(gravel, List.of(
                CountPlacement.of(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome())));

        var trees = new net.minecraft.world.level.levelgen.feature.RandomSelectorFeature(List.of(
                        new net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature(
                                PlacementUtils.inlinePlaced(configured.getOrThrow(GLOWING_TREE)), 0.05F),
                        new net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature(
                                bootstrap.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                                        .getOrThrow(net.minecraft.data.worldgen.placement.TreePlacements.FANCY_OAK_CHECKED), 0.1F)),
                        bootstrap.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                                .getOrThrow(net.minecraft.data.worldgen.placement.TreePlacements.OAK_CHECKED));
        bootstrap.register(LUMINOUS_TREES_PLACED, new PlacedFeature(net.minecraft.core.Holder.direct(trees),
                VegetationPlacements.treePlacement(PlacementUtils.countExtra(4, 0.1F, 1))));

        PlacementModifier intoAir = net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.ONLY_IN_AIR_PREDICATE);
        PlacementModifier spread = net.minecraft.world.level.levelgen.placement.OffsetPlacement.ofTriangle(7, 3);
        bootstrap.register(LUMINOUS_FLOWERS_PLACED, new PlacedFeature(configured.getOrThrow(featureKey("luminous_flowers")),
                List.of(CountPlacement.of(8), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(1, 32), BiomeFilter.biome(),
                        CountPlacement.of(64), spread, intoAir)));
        bootstrap.register(LUMINOUS_GRASS_PLACED, new PlacedFeature(configured.getOrThrow(featureKey("rainbow_grass")),
                List.of(CountPlacement.of(3), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(2, 0),
                        reika.chromaticraft.world.SinkThroughAirPlacement.INSTANCE, BiomeFilter.biome(),
                        CountPlacement.of(128), spread, intoAir)));
        // WorldGenWaterlily: ten tries spread rand(8)-rand(8) across and rand(4)-rand(4) up.
        bootstrap.register(LUMINOUS_LILY_PAD_PLACED, new PlacedFeature(configured.getOrThrow(featureKey("luminous_lily_pad")),
                List.of(CountPlacement.of(2), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(2, 0),
                        reika.chromaticraft.world.SinkThroughAirPlacement.ABOVE_GROUND, BiomeFilter.biome(),
                        CountPlacement.of(10), spread, intoAir)));
        bootstrap.register(LUMINOUS_BROWN_MUSHROOM_PLACED, new PlacedFeature(configured.getOrThrow(featureKey("luminous_brown_mushroom")),
                List.of(RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(2, 0), BiomeFilter.biome(),
                        CountPlacement.of(64), spread, intoAir)));
        bootstrap.register(LUMINOUS_RED_MUSHROOM_PLACED, new PlacedFeature(configured.getOrThrow(featureKey("luminous_red_mushroom")),
                List.of(RarityFilter.onAverageOnceEvery(8), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(2, 0), BiomeFilter.biome(),
                        CountPlacement.of(64), spread, intoAir)));
        bootstrap.register(LUMINOUS_REEDS_PLACED, new PlacedFeature(
                configured.getOrThrow(net.minecraft.data.worldgen.features.VegetationFeatures.SUGAR_CANE),
                List.of(CountPlacement.of(10 + 10), InSquarePlacement.spread(),
                        new reika.chromaticraft.world.LegacyHeightmapScaledPlacement(2, 0), BiomeFilter.biome(),
                        CountPlacement.of(20),
                        net.minecraft.world.level.levelgen.placement.OffsetPlacement.ofTriangle(3, 0),
                        net.minecraft.data.worldgen.features.VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE))));

        bootstrap.register(LUMINOUS_SPRING_WATER_PLACED, new PlacedFeature(
                configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.SPRING_WATER),
                List.of(CountPlacement.of(2 * 50), InSquarePlacement.spread(),
                        net.minecraft.world.level.levelgen.placement.HeightRangePlacement.of(
                                net.minecraft.world.level.levelgen.heightproviders.BiasedToBottomHeight.of(
                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(0),
                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(255), 8)),
                        BiomeFilter.biome())));
        bootstrap.register(LUMINOUS_SPRING_LAVA_PLACED, new PlacedFeature(
                configured.getOrThrow(net.minecraft.data.worldgen.features.MiscOverworldFeatures.SPRING_LAVA_OVERWORLD),
                List.of(CountPlacement.of(2 * 20), InSquarePlacement.spread(),
                        net.minecraft.world.level.levelgen.placement.HeightRangePlacement.of(
                                net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight.of(
                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(0),
                                        net.minecraft.world.level.levelgen.VerticalAnchor.absolute(247), 8)),
                        BiomeFilter.biome())));
        // Bone meal only.
        bootstrap.register(LUMINOUS_CLIFFS_FLOWER_PLACED, new PlacedFeature(configured.getOrThrow(LUMINOUS_CLIFFS_FLOWER),
                List.of(CountPlacement.of(0), BiomeFilter.biome())));
    }

    /** Vanilla's disk placement (MiscOverworldPlacements) with V33a's count. */
    private static List<PlacementModifier> disk(int count) {
        return List.of(CountPlacement.of(count), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID,
                net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                        net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.matchesFluids(
                                net.minecraft.world.level.material.Fluids.WATER)),
                BiomeFilter.biome());
    }

	private static ItemStackTemplate flawedFocusCrystal() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("tier", 0); // TileEntityFocusCrystal.CrystalTier.FLAWED
		return new ItemStackTemplate(ChromaTiles.FOCUSCRYSTAL.getBlock().asItem(),
				DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build());
	}

    /**
     * V33a {@code BiomeGlowingCliffs.GlowingTreeGenerator}: DragonAPI's {@code ModifiableBigTree}, which is
     * 1.7.10's big oak, with one leaf in sixteen swapped for a Glowing Leaf. Vanilla's fancy oak is the
     * big oak's 26.2 form, so this is fancy oak with the leaf mix changed. The wood is oak, as the
     * DragonAPI generator's default log is.
     */
    private static net.minecraft.world.level.levelgen.feature.TreeFeature glowingTreeConfiguration(
            net.minecraft.core.Holder<BlockStateProvider> belowTrunk) {
        return fancyGlowingTree(glowingFoliage(15), belowTrunk);
    }

    /** Oak leaves with one in {@code oneIn} a Glowing Leaf: V33a {@code selectGlowingTreeLeaf}. */
    private static WeightedStateProvider glowingFoliage(int oneIn) {
        return new WeightedStateProvider(WeightedList.<BlockState>builder()
                .add(Blocks.OAK_LEAVES.defaultBlockState(), oneIn - 1)
                .add(ChromaBlocks.GLOWING_LEAVES.get().defaultBlockState(), 1));
    }

    private static net.minecraft.world.level.levelgen.feature.TreeFeature fancyGlowingTree(WeightedStateProvider foliage,
            net.minecraft.core.Holder<BlockStateProvider> belowTrunk) {
        return new net.minecraft.world.level.levelgen.feature.TreeFeature.Builder(new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.OAK_LOG.defaultBlockState()),
                new net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer(3, 11, 0),
                foliage,
                new net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer(
                        ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, java.util.OptionalInt.of(4)),
                belowTrunk).ignoreVines().build();
    }

    /**
     * V33a picked the trunk wood from its whole tree registry — every vanilla overworld wood plus
     * whatever modded woods were present — so a hardcoded six-entry weighted list was wrong twice
     * over: it fixed the set at datagen time, and it silently excluded every modded log.
     *
     * <p>{@link RandomTagSingleStateProvider} resolves {@code minecraft:overworld_natural_logs}
     * lazily at generation time and picks uniformly, so modded woods that join the tag are included
     * and nether stems are not. Vanilla's {@code WeightedStateProvider} cannot express this: it needs
     * every state enumerated with a weight when the feature is built, before datapack tags resolve.
     */
    private static BlockStateProvider randomOverworldLog() {
        return new RandomTagSingleStateProvider(BlockTags.OVERWORLD_NATURAL_LOGS);
    }

	private static void registerCastingTemple(
			BootstrapContext<net.minecraft.world.level.levelgen.structure.Structure> bootstrap,
			ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key, int tier) {
		var settings = new net.minecraft.world.level.levelgen.structure.Structure.StructureSettings(
				net.minecraft.core.HolderSet.empty(), java.util.Map.of(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES,
				net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE);
		bootstrap.register(key,
				new reika.chromaticraft.world.dimension.structure.CastingTempleStructure(settings, tier));
	}

    @SuppressWarnings({"rawtypes", "unchecked"})
    /**
     * V33a {@code ColorTreeGenerator}'s constants and tree-count table. The known-biome counts are the
     * {@code chromaticraft:dye_trees/count_<n>} tags (see {@link ChromaBiomeTagProvider#addDyeTreeTags});
     * the fallback is V33a's {@code BiomeDictionary} type table, each type's count being
     * {@code getTreeCount} of the vanilla biome it pointed at.
     */
    private static reika.chromaticraft.world.ColorTreeFeature.Config colorTreeConfig() {
        List<reika.chromaticraft.world.ColorTreeFeature.Count> counts = java.util.stream.IntStream
                .of(0, 1, 2, 3, 4, 5, 6, 7, 10, 16, 53)
                .mapToObj(n -> new reika.chromaticraft.world.ColorTreeFeature.Count(ChromaBiomeTagProvider.dyeTreeCount(n), n))
                .toList();
        List<reika.chromaticraft.world.ColorTreeFeature.Count> fallback = List.of(
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_FOREST, 16),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_MOUNTAIN, 3),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_JUNGLE, 53),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_HILL, 16),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_SNOWY, 3),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_PLAINS, 2),
                new reika.chromaticraft.world.ColorTreeFeature.Count(net.neoforged.neoforge.common.Tags.Biomes.IS_SWAMP, 5));
        return new reika.chromaticraft.world.ColorTreeFeature.Config(counts, fallback, 128, 32);
    }

    private static void registerFeature(BootstrapContext<Feature> bootstrap, Identifier id, Feature feature) {
        bootstrap.register(ResourceKey.create(Registries.FEATURE, id), feature);
    }

    private static void registerPlaced(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<Feature> configured, Identifier id) {
        registerPlaced(bootstrap, configured, id,
                id.equals(NATURAL_PYLON) ? List.of(PylonGridPlacement.INSTANCE) : List.of());
    }

    private static void registerPlaced(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<Feature> configured, Identifier id, List<PlacementModifier> modifiers) {
        ResourceKey<Feature> featureKey = ResourceKey.create(Registries.FEATURE, id);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(Registries.PLACED_FEATURE, id);
        bootstrap.register(placedKey, new PlacedFeature(configured.getOrThrow(featureKey), modifiers));
    }

    private static ResourceKey<Feature> featureKey(String path) {
        return ResourceKey.create(Registries.FEATURE, id(path));
    }

    private static ResourceKey<PlacedFeature> placedKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }
}
