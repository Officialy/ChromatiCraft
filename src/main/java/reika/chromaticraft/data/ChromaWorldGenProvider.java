package reika.chromaticraft.data;

import java.util.List;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import net.minecraft.tags.BlockTags;
import reika.dragonapi.libraries.RandomTagSingleStateProvider;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.registry.ChromaDecoFlowers;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.world.PylonGridPlacement;
import reika.chromaticraft.world.biome.ChromaBiomes;

/** Datapack registry objects for ChromatiCraft's active 26.2 worldgen features. */
public final class ChromaWorldGenProvider {

    private static final Identifier CAVE_CRYSTAL = id("cave_crystal");
    private static final Identifier PYLON = id("pylon");
    private static final Identifier TURBOCHARGED_PYLON = id("turbocharged_pylon");
    private static final List<Identifier> CASTING_TEMPLES =
            List.of(id("casting_temple_l1"), id("casting_temple_l2"), id("casting_temple_l3"));
    private static final Identifier POWER_CRYSTAL_BOOSTED_PYLON = id("power_crystal_boosted_pylon");
    private static final Identifier LUMINOUS_CLIFFS_TERRAIN = id("luminous_cliffs_terrain");
    private static final Identifier LUMA_PATCH = id("luma_patch");
    private static final Identifier LUMINOUS_ISLAND = id("luminous_island");
    private static final Identifier LUMINOUS_FLORA = id("luminous_flora");
    /** V33a TieredOres: {genChance one-in-N per chunk, veinCount attempts, veinSize}. */
    private record TieredOre(String name, java.util.function.Supplier<net.minecraft.world.level.block.Block> block,
            net.minecraft.world.level.block.Block host, int genChance, int veinCount, int veinSize, boolean deepBand) {}

    private static final List<TieredOre> TIERED_ORES = List.of(
            new TieredOre("energized_rock", () -> ChromaBlocks.ENERGIZED_ROCK.get(),
                    net.minecraft.world.level.block.Blocks.STONE, 1, 4, 12, false),
            new TieredOre("elemental_stones", () -> ChromaBlocks.ELEMENTAL_STONES.get(),
                    net.minecraft.world.level.block.Blocks.STONE, 1, 4, 8, false),
            new TieredOre("firestone", () -> ChromaBlocks.FIRESTONE.get(),
                    net.minecraft.world.level.block.Blocks.NETHERRACK, 3, 2, 16, true));

    public static final ResourceKey<PlacedFeature> LUMINOUS_CLIFFS_TERRAIN_PLACED = placedKey("luminous_cliffs_terrain");
    public static final ResourceKey<PlacedFeature> LUMA_PATCH_PLACED = placedKey("luma_patch");
    public static final ResourceKey<PlacedFeature> LUMINOUS_ISLAND_PLACED = placedKey("luminous_island");
    public static final ResourceKey<PlacedFeature> LUMINOUS_FLORA_PLACED = placedKey("luminous_flora");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINBOW_TREE = configuredKey("rainbow_tree");
    public static final ResourceKey<PlacedFeature> RAINBOW_TREE_PLACED = placedKey("rainbow_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GLOWING_TREE = configuredKey("glowing_tree");
    public static final ResourceKey<PlacedFeature> ENDER_FOREST_TREE_PLACED = placedKey("ender_forest_tree");
    public static final ResourceKey<PlacedFeature> GLOWING_TREE_PLACED = placedKey("glowing_tree");

    private ChromaWorldGenProvider() {}

    public static ResourceKey<ConfiguredFeature<?, ?>> dyeTree(CrystalElement element) {
        return configuredKey("dye_tree_" + element.getEnglishName());
    }

    public static ResourceKey<PlacedFeature> dyeTreePlaced(CrystalElement element) {
        return placedKey("dye_tree_" + element.getEnglishName());
    }

    public static RegistrySetBuilder buildRegistrySet() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.BIOME, ChromaBiomes::bootstrap);
        builder.add(Registries.CONFIGURED_FEATURE, bootstrap -> {
            HolderGetter<Feature<?>> features = bootstrap.lookup(Registries.FEATURE);
            registerConfigured(bootstrap, features, CAVE_CRYSTAL);
            registerConfigured(bootstrap, features, id("cave_indicator"));
            registerConfigured(bootstrap, features, id("unknown_artefact"));
            registerConfigured(bootstrap, features, id("ender_forest_tree"));
            for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
                registerConfigured(bootstrap, features, id(flower.registryName()));
            for (ChromaTieredPlants plant : ChromaTieredPlants.list)
                registerConfigured(bootstrap, features, id(plant.registryName()));
            registerConfigured(bootstrap, features, PYLON);
            registerConfigured(bootstrap, features, TURBOCHARGED_PYLON);
            for (Identifier temple : CASTING_TEMPLES)
                registerConfigured(bootstrap, features, temple);
            registerConfigured(bootstrap, features, POWER_CRYSTAL_BOOSTED_PYLON);
            registerConfigured(bootstrap, features, LUMINOUS_CLIFFS_TERRAIN);
            registerConfigured(bootstrap, features, LUMA_PATCH);
            registerConfigured(bootstrap, features, LUMINOUS_ISLAND);
            registerConfigured(bootstrap, features, LUMINOUS_FLORA);
            for (TieredOre ore : TIERED_ORES) {
                // V33a BlockExcludingOreVein targets only the host block, which is what makes these
                // ores replace stone/netherrack and never the cliff material it explicitly excluded.
                bootstrap.register(configuredKey(ore.name()), new ConfiguredFeature<>(Feature.ORE,
                        new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(
                                List.of(net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.target(
                                        new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(ore.host()),
                                        ore.block().get().defaultBlockState())),
                                ore.veinSize())));
            }
            HolderGetter<Biome> biomes = bootstrap.lookup(Registries.BIOME);
            for (CrystalElement element : CrystalElement.elements) {
                bootstrap.register(dyeTree(element), new ConfiguredFeature<>(Feature.TREE,
                        dyeTreeConfiguration(element, biomes)));
            }
            bootstrap.register(RAINBOW_TREE, new ConfiguredFeature<>(Feature.TREE,
                    rainbowTreeConfiguration(biomes)));
            bootstrap.register(GLOWING_TREE, new ConfiguredFeature<>(Feature.TREE,
                    glowingTreeConfiguration(biomes)));
        });
        builder.add(Registries.PLACED_FEATURE, bootstrap -> {
            HolderGetter<ConfiguredFeature<?, ?>> configured = bootstrap.lookup(Registries.CONFIGURED_FEATURE);
            registerPlaced(bootstrap, configured, CAVE_CRYSTAL);
            registerPlaced(bootstrap, configured, id("cave_indicator"), List.of(BiomeFilter.biome()));
            // The feature owns its own tower-ring filter and 1-in-40 roll; no count modifiers.
            registerPlaced(bootstrap, configured, id("unknown_artefact"), List.of(BiomeFilter.biome()));
            // V33a thins the biome's trees to 0.7x; vanilla forest is 10 per chunk, so 7 attempts,
            // and the selector's own "no tree" entry does the rest of the thinning per position.
            registerPlaced(bootstrap, configured, id("ender_forest_tree"), List.of(
                    CountPlacement.of(7),
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                            .onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING),
                    BiomeFilter.biome()));
            for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
                registerPlaced(bootstrap, configured, id(flower.registryName()), List.of(
                        RarityFilter.onAverageOnceEvery(flower.generationChance()),
                        BiomeFilter.biome()));
            for (ChromaTieredPlants plant : ChromaTieredPlants.list)
                registerPlaced(bootstrap, configured, id(plant.registryName()), List.of(
                        RarityFilter.onAverageOnceEvery(plant.generationChance()),
                        CountPlacement.of(plant.generationCount()),
                        InSquarePlacement.spread(),
                        BiomeFilter.biome()));
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
            registerPlaced(bootstrap, configured, PYLON);
            registerPlaced(bootstrap, configured, TURBOCHARGED_PYLON);
            // Command-only, like the pylon variants: no biome modifier names these.
            for (Identifier temple : CASTING_TEMPLES)
                registerPlaced(bootstrap, configured, temple);
            registerPlaced(bootstrap, configured, POWER_CRYSTAL_BOOSTED_PYLON);
            registerPlaced(bootstrap, configured, LUMINOUS_CLIFFS_TERRAIN, List.of());
            registerPlaced(bootstrap, configured, LUMA_PATCH,
                    List.of(CountPlacement.of(1)));
            registerPlaced(bootstrap, configured, LUMINOUS_ISLAND,
                    List.of(RarityFilter.onAverageOnceEvery(4)));
            registerPlaced(bootstrap, configured, LUMINOUS_FLORA, List.of());
            for (CrystalElement element : CrystalElement.elements) {
                bootstrap.register(dyeTreePlaced(element), new PlacedFeature(configured.getOrThrow(dyeTree(element)),
                        VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(2),
                                ChromaBlocks.dyeSapling(element).get())));
            }
            bootstrap.register(RAINBOW_TREE_PLACED, new PlacedFeature(configured.getOrThrow(RAINBOW_TREE),
                    VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(10),
                            ChromaBlocks.RAINBOW_SAPLING.get())));
            bootstrap.register(GLOWING_TREE_PLACED, new PlacedFeature(configured.getOrThrow(GLOWING_TREE),
                    VegetationPlacements.treePlacement(RarityFilter.onAverageOnceEvery(5), Blocks.OAK_SAPLING)));
        });
        return builder;
    }

    private static TreeConfiguration dyeTreeConfiguration(CrystalElement element, HolderGetter<Biome> biomes) {
        return new TreeConfiguration.TreeConfigurationBuilder(randomOverworldLog(),
                new StraightTrunkPlacer(5, 2, 0),
                BlockStateProvider.simple(ChromaBlocks.dyeLeaves(element).get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1),
                TreeConfiguration.defaultPlaceBelowTreeTrunkProvider(biomes)).ignoreVines().build();
    }

    private static TreeConfiguration rainbowTreeConfiguration(HolderGetter<Biome> biomes) {
        return new TreeConfiguration.TreeConfigurationBuilder(randomOverworldLog(),
                new StraightTrunkPlacer(9, 4, 2),
                BlockStateProvider.simple(ChromaBlocks.RAINBOW_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(3), ConstantInt.of(1), 4),
                new TwoLayersFeatureSize(1, 0, 2),
                TreeConfiguration.defaultPlaceBelowTreeTrunkProvider(biomes)).ignoreVines().build();
    }

    private static TreeConfiguration glowingTreeConfiguration(HolderGetter<Biome> biomes) {
        WeightedStateProvider foliage = new WeightedStateProvider(WeightedList.<BlockState>builder()
                .add(Blocks.OAK_LEAVES.defaultBlockState(), 15)
                .add(ChromaBlocks.GLOWING_LEAVES.get().defaultBlockState(), 1));
        return new TreeConfiguration.TreeConfigurationBuilder(randomOverworldLog(),
                new StraightTrunkPlacer(6, 3, 2),
                foliage,
                new BlobFoliagePlacer(ConstantInt.of(3), ConstantInt.of(1), 4),
                new TwoLayersFeatureSize(1, 0, 2),
                TreeConfiguration.defaultPlaceBelowTreeTrunkProvider(biomes)).ignoreVines().build();
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerConfigured(BootstrapContext<ConfiguredFeature<?, ?>> bootstrap,
            HolderGetter<Feature<?>> features, Identifier id) {
        ResourceKey<Feature<?>> featureKey = ResourceKey.create(Registries.FEATURE, id);
        ResourceKey<ConfiguredFeature<?, ?>> configuredKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, id);
        bootstrap.register(configuredKey, new ConfiguredFeature(
                (Feature)features.getOrThrow(featureKey).value(), NoneFeatureConfiguration.INSTANCE));
    }

    private static void registerPlaced(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<ConfiguredFeature<?, ?>> configured, Identifier id) {
        registerPlaced(bootstrap, configured, id,
                id.equals(PYLON) ? List.of(PylonGridPlacement.INSTANCE) : List.of());
    }

    private static void registerPlaced(BootstrapContext<PlacedFeature> bootstrap,
            HolderGetter<ConfiguredFeature<?, ?>> configured, Identifier id, List<PlacementModifier> modifiers) {
        ResourceKey<ConfiguredFeature<?, ?>> configuredKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, id);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(Registries.PLACED_FEATURE, id);
        bootstrap.register(placedKey, new PlacedFeature(configured.getOrThrow(configuredKey), modifiers));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(path));
    }

    private static ResourceKey<PlacedFeature> placedKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }
}