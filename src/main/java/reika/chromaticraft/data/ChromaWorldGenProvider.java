package reika.chromaticraft.data;

import java.util.List;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
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
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.world.PylonGridPlacement;
import reika.chromaticraft.world.biome.ChromaBiomes;

/** Datapack registry objects for ChromatiCraft's active 26.2 worldgen features. */
public final class ChromaWorldGenProvider {

    public static final ResourceKey<VillagerTrade> FOCUS_CRYSTAL_TRADE = ResourceKey.create(
            Registries.VILLAGER_TRADE, id("focus_crystal"));

    private static final Identifier CAVE_CRYSTAL = id("cave_crystal");
    private static final Identifier NATURAL_PYLON = id("natural_pylon");
    private static final Identifier DATA_TOWER = id("data_tower");
    private static final Identifier TURBOCHARGED_PYLON = id("turbocharged_pylon");
    private static final List<Identifier> CASTING_TEMPLES =
            List.of(id("casting_temple_l1"), id("casting_temple_l2"), id("casting_temple_l3"));
    private static final Identifier POWER_CRYSTAL_BOOSTED_PYLON = id("power_crystal_boosted_pylon");
    private static final Identifier LUMINOUS_CLIFFS_TERRAIN = id("luminous_cliffs_terrain");
    private static final Identifier LUMA_PATCH = id("luma_patch");
    private static final Identifier LUMINOUS_ISLAND = id("luminous_island");
    private static final Identifier LUMINOUS_FLORA = id("luminous_flora");
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

    private static Identifier coloredPylon(CrystalElement element) {
        return id("pylon_" + element.getEnglishName());
    }

    public static RegistrySetBuilder buildRegistrySet() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.VILLAGER_TRADE, bootstrap -> bootstrap.register(FOCUS_CRYSTAL_TRADE,
                new VillagerTrade(
                        new TradeCost(Items.EMERALD, 1),
						flawedFocusCrystal(),
                        Integer.MAX_VALUE,
                        1,
                        0,
                        java.util.Optional.empty(),
                        java.util.List.of())));
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
        builder.add(Registries.CONFIGURED_FEATURE, bootstrap -> {
            HolderGetter<Feature<?>> features = bootstrap.lookup(Registries.FEATURE);
            registerConfigured(bootstrap, features, CAVE_CRYSTAL);
            registerConfigured(bootstrap, features, id("cave_indicator"));
            registerConfigured(bootstrap, features, id("unknown_artefact"));
            registerConfigured(bootstrap, features, id("warp_node"));
            registerConfigured(bootstrap, features, id("skypeater"));
            registerConfigured(bootstrap, features, id("ender_forest_tree"));
            registerConfigured(bootstrap, features, id("rainbow_tree"));
            for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
                registerConfigured(bootstrap, features, id(flower.registryName()));
            for (ChromaTieredPlants plant : ChromaTieredPlants.list)
                registerConfigured(bootstrap, features, id(plant.registryName()));
            registerConfigured(bootstrap, features, NATURAL_PYLON);
            for (CrystalElement element : CrystalElement.elements)
                registerConfigured(bootstrap, features, coloredPylon(element));
            registerConfigured(bootstrap, features, DATA_TOWER);
            registerConfigured(bootstrap, features, TURBOCHARGED_PYLON);
            for (Identifier temple : CASTING_TEMPLES)
                registerConfigured(bootstrap, features, temple);
            registerConfigured(bootstrap, features, POWER_CRYSTAL_BOOSTED_PYLON);
            registerConfigured(bootstrap, features, LUMINOUS_CLIFFS_TERRAIN);
            registerConfigured(bootstrap, features, LUMA_PATCH);
            registerConfigured(bootstrap, features, LUMINOUS_ISLAND);
            registerConfigured(bootstrap, features, LUMINOUS_FLORA);
            registerConfigured(bootstrap, features, NETHER_ROOF_STRUCTURE);
            registerConfigured(bootstrap, features, NETHER_LAVA_RIVER);
            registerConfigured(bootstrap, features, CRYSTAL_SHRUB);
            registerConfigured(bootstrap, features, FLOATSTONE);
            registerConfigured(bootstrap, features, CRYSTAL_TREE);
            registerConfigured(bootstrap, features, GLOW_TREE);
            registerConfigured(bootstrap, features, TREE_CLUSTER);
            registerConfigured(bootstrap, features, FIRE_JET);
            registerConfigured(bootstrap, features, MINI_ALTAR);
            registerConfigured(bootstrap, features, FISSURE);
            registerConfigured(bootstrap, features, GLOWING_CRACKS);
            registerConfigured(bootstrap, features, LIGHTED_SHRUB);
            registerConfigured(bootstrap, features, CRYSTAL_PIT);
            registerConfigured(bootstrap, features, AURORAE);
            for (Identifier structure : NETHER_ROOF_STRUCTURES)
                registerConfigured(bootstrap, features, structure);
            registerConfigured(bootstrap, features, NATURAL_CAVERN);
            registerConfigured(bootstrap, features, CAVERN);
            registerConfigured(bootstrap, features, NATURAL_BURROW);
            registerConfigured(bootstrap, features, BURROW);
            registerConfigured(bootstrap, features, NATURAL_OCEAN);
            registerConfigured(bootstrap, features, OCEAN);
            registerConfigured(bootstrap, features, NATURAL_DESERT);
            registerConfigured(bootstrap, features, DESERT);
			registerConfigured(bootstrap, features, NATURAL_SNOW);
			registerConfigured(bootstrap, features, SNOW);
			registerConfigured(bootstrap, features, NATURAL_BIOME_FRAGMENT);
			registerConfigured(bootstrap, features, BIOME_FRAGMENT);
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
            bootstrap.register(GLOWING_TREE, new ConfiguredFeature<>(Feature.TREE,
                    glowingTreeConfiguration(biomes)));
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
        });
        builder.add(Registries.STRUCTURE_SET, bootstrap -> {
            var structures = bootstrap.lookup(Registries.STRUCTURE);
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
        });
        builder.add(Registries.PLACED_FEATURE, bootstrap -> {
            HolderGetter<ConfiguredFeature<?, ?>> configured = bootstrap.lookup(Registries.CONFIGURED_FEATURE);
            registerPlaced(bootstrap, configured, CAVE_CRYSTAL);
            registerPlaced(bootstrap, configured, id("cave_indicator"), List.of(BiomeFilter.biome()));
            // The feature owns its own tower-ring filter and 1-in-40 roll; no count modifiers.
            registerPlaced(bootstrap, configured, id("unknown_artefact"), List.of(BiomeFilter.biome()));
            // The feature owns its own 2048-chunk grid and biome roll; no count modifiers.
            registerPlaced(bootstrap, configured, id("warp_node"), List.of(BiomeFilter.biome()));
            // The feature owns its own 4x4 shuffled lattice; no count modifiers.
            registerPlaced(bootstrap, configured, id("skypeater"), List.of(BiomeFilter.biome()));
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
            registerPlaced(bootstrap, configured, NATURAL_PYLON);
            for (CrystalElement element : CrystalElement.elements)
                registerPlaced(bootstrap, configured, coloredPylon(element));
            registerPlaced(bootstrap, configured, DATA_TOWER);
            registerPlaced(bootstrap, configured, TURBOCHARGED_PYLON);
            // Command-only, like the pylon variants: no biome modifier names these.
            for (Identifier temple : CASTING_TEMPLES)
                registerPlaced(bootstrap, configured, temple);
            registerPlaced(bootstrap, configured, POWER_CRYSTAL_BOOSTED_PYLON);
            registerPlaced(bootstrap, configured, LUMINOUS_CLIFFS_TERRAIN, List.of(BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, LUMA_PATCH,
                    List.of(CountPlacement.of(1), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, LUMINOUS_ISLAND,
                    List.of(RarityFilter.onAverageOnceEvery(4), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, LUMINOUS_FLORA, List.of(BiomeFilter.biome()));
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
            // writes are dropped in silence. Anchored at the chunk's corner the whole spread stays
            // inside it, and upstream's own scatter is untouched.
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
            // A 144-block source noise scale is approximately one candidate per 9x9 chunk area.
            // The feature then performs V33a's exact y roll and enclosed-cavern/tunnel tests.
            registerPlaced(bootstrap, configured, NATURAL_CAVERN, List.of(
                    RarityFilter.onAverageOnceEvery(81), InSquarePlacement.spread(), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, CAVERN);
            // V33a Burrows use a 240-block noise scale, approximately one candidate per 15x15 chunks.
            registerPlaced(bootstrap, configured, NATURAL_BURROW, List.of(
                    RarityFilter.onAverageOnceEvery(225), InSquarePlacement.spread(), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, BURROW);
            // V33a Ocean cells use a 640-block noise scale: one candidate per 40x40 chunks.
            registerPlaced(bootstrap, configured, NATURAL_OCEAN, List.of(
                    RarityFilter.onAverageOnceEvery(1600), InSquarePlacement.spread(), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, OCEAN);
            registerPlaced(bootstrap, configured, NATURAL_DESERT, List.of(
                    RarityFilter.onAverageOnceEvery(756), InSquarePlacement.spread(), BiomeFilter.biome()));
            registerPlaced(bootstrap, configured, DESERT);
			// V33a Snow cells use a 480-block source noise scale: one candidate per 30x30 chunks.
			registerPlaced(bootstrap, configured, NATURAL_SNOW, List.of(
					RarityFilter.onAverageOnceEvery(900), InSquarePlacement.spread(), BiomeFilter.biome()));
			registerPlaced(bootstrap, configured, SNOW);
			// V33a Biome Fragment cells use a 640-block scale: one candidate per 40x40 chunks.
			registerPlaced(bootstrap, configured, NATURAL_BIOME_FRAGMENT, List.of(
					RarityFilter.onAverageOnceEvery(1600), InSquarePlacement.spread(), BiomeFilter.biome()));
			registerPlaced(bootstrap, configured, BIOME_FRAGMENT);
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

	private static ItemStackTemplate flawedFocusCrystal() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("tier", 0); // TileEntityFocusCrystal.CrystalTier.FLAWED
		return new ItemStackTemplate(ChromaTiles.FOCUSCRYSTAL.getBlock().asItem(),
				DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build());
	}

    private static TreeConfiguration dyeTreeConfiguration(CrystalElement element, HolderGetter<Biome> biomes) {
        return new TreeConfiguration.TreeConfigurationBuilder(randomOverworldLog(),
                new StraightTrunkPlacer(5, 2, 0),
                BlockStateProvider.simple(ChromaBlocks.dyeLeaves(element).get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1),
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
                id.equals(NATURAL_PYLON) ? List.of(PylonGridPlacement.INSTANCE) : List.of());
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
