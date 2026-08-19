package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.CrystalFeature;
import reika.chromaticraft.world.DataTowerFeature;
import reika.chromaticraft.world.PylonFeature;
import reika.chromaticraft.world.CaveIndicatorFeature;
import reika.chromaticraft.world.DecoFlowerFeature;
import reika.chromaticraft.world.EnderForestTreeFeature;
import reika.chromaticraft.world.UnknownArtefactFeature;
import reika.chromaticraft.world.WarpNodeFeature;
import reika.chromaticraft.world.SkypeaterFeature;
import reika.chromaticraft.world.TieredPlantFeature;
import reika.chromaticraft.world.NetherRoofStructureFeature;
import reika.chromaticraft.world.OverworldStructureFeature;
import reika.chromaticraft.world.luminous.LumaPatchFeature;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature;
import reika.chromaticraft.world.luminous.LuminousIslandFeature;
import reika.chromaticraft.world.luminous.LuminousFloraFeature;

/** Runtime feature registry for ChromatiCraft's data-driven 26.2 world generation. */
public final class ChromaFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ChromatiCraft.MODID);

    /**
     * Proxima answers biome queries from its own painted map rather than from climate parameters, so
     * its level stem names a mod biome source and that source's codec has to be registered like any
     * other worldgen type.
     */
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, ChromatiCraft.MODID);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>,
            com.mojang.serialization.MapCodec<reika.chromaticraft.world.dimension.biome.ProximaBiomeSource>> PROXIMA_BIOME_SOURCE =
            BIOME_SOURCES.register("proxima",
                    () -> reika.chromaticraft.world.dimension.biome.ProximaBiomeSource.CODEC);

    /**
     * V33a's radial terrain profile is the only part of its generator that was not stock vanilla, so
     * it enters the modern pipeline as a density function and everything else - noise, interpolation,
     * cell lattice, surface pass - stays vanilla's.
     */
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ChromatiCraft.MODID);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>,
            com.mojang.serialization.MapCodec<reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction>> PROXIMA_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("proxima_terrain",
                    () -> reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction.CODEC);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CAVE_CRYSTAL =
            FEATURES.register("cave_crystal", CrystalFeature::new);
    /**
     * The plant half of V33a's TieredWorldGenerator, one feature per plant identity. Each resolves
     * its own registered block lazily so registration order does not matter.
     */
    public static final java.util.Map<ChromaTieredPlants, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> TIERED_PLANTS =
            registerTieredPlants();

    private static java.util.Map<ChromaTieredPlants, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> registerTieredPlants() {
        java.util.EnumMap<ChromaTieredPlants, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> map =
                new java.util.EnumMap<>(ChromaTieredPlants.class);
        for (ChromaTieredPlants plant : ChromaTieredPlants.list)
            map.put(plant, FEATURES.register(plant.registryName(), () -> new TieredPlantFeature(plant,
                    () -> ChromaBlocks.tieredPlant(plant).get().defaultBlockState())));
        return map;
    }

    public static final java.util.Map<ChromaDecoFlowers, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> DECO_FLOWERS =
            registerDecoFlowers();

    private static java.util.Map<ChromaDecoFlowers, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> registerDecoFlowers() {
        java.util.EnumMap<ChromaDecoFlowers, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> map =
                new java.util.EnumMap<>(ChromaDecoFlowers.class);
        for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
            map.put(flower, FEATURES.register(flower.registryName(), () -> new DecoFlowerFeature(flower)));
        return map;
    }

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ENDER_FOREST_TREE =
            FEATURES.register("ender_forest_tree", EnderForestTreeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RAINBOW_TREE =
            FEATURES.register("rainbow_tree", reika.chromaticraft.world.RainbowTreeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SKYPEATER =
            FEATURES.register("skypeater", SkypeaterFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> WARP_NODE =
            FEATURES.register("warp_node", WarpNodeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> UNKNOWN_ARTEFACT =
            FEATURES.register("unknown_artefact", UnknownArtefactFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CAVE_INDICATOR =
            FEATURES.register("cave_indicator", CaveIndicatorFeature::new);

    /** Natural worldgen keeps the source random colour roll under an explicitly natural-only id. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_PYLON =
            FEATURES.register("natural_pylon", () -> new PylonFeature());
    /** Command/debug features: /place feature chromaticraft:pylon_<vanilla dye name>. */
    public static final java.util.Map<CrystalElement, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> COLORED_PYLONS =
            registerColoredPylons();

    private static java.util.Map<CrystalElement, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> registerColoredPylons() {
        java.util.EnumMap<CrystalElement, DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> map =
                new java.util.EnumMap<>(CrystalElement.class);
        for (CrystalElement element : CrystalElement.elements)
            map.put(element, FEATURES.register("pylon_" + element.getEnglishName(),
                    () -> new PylonFeature(element)));
        return java.util.Map.copyOf(map);
    }
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DATA_TOWER =
            FEATURES.register("data_tower", DataTowerFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TURBOCHARGED_PYLON =
            FEATURES.register("turbocharged_pylon",
                    () -> new PylonFeature(PylonFeature.Variant.TURBOCHARGED));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> POWER_CRYSTAL_BOOSTED_PYLON =
            FEATURES.register("power_crystal_boosted_pylon",
                    () -> new PylonFeature(PylonFeature.Variant.POWER_CRYSTAL_BOOSTED));

    /** Command-only: /place feature chromaticraft:casting_temple_l1 (and _l2, _l3). */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CASTING_TEMPLE_L1 =
            FEATURES.register("casting_temple_l1",
                    () -> new reika.chromaticraft.world.CastingTempleFeature(1));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CASTING_TEMPLE_L2 =
            FEATURES.register("casting_temple_l2",
                    () -> new reika.chromaticraft.world.CastingTempleFeature(2));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CASTING_TEMPLE_L3 =
            FEATURES.register("casting_temple_l3",
                    () -> new reika.chromaticraft.world.CastingTempleFeature(3));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LUMINOUS_CLIFFS_TERRAIN =
            FEATURES.register("luminous_cliffs_terrain", LuminousCliffsTerrainFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LUMA_PATCH =
            FEATURES.register("luma_patch", LumaPatchFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LUMINOUS_ISLAND =
            FEATURES.register("luminous_island", LuminousIslandFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LUMINOUS_FLORA =
            FEATURES.register("luminous_flora", LuminousFloraFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NETHER_ROOF_STRUCTURE =
            FEATURES.register("nether_roof_structure", () -> new NetherRoofStructureFeature());

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> AURORAE =
            FEATURES.register("aurorae",
                    reika.chromaticraft.world.dimension.AuroraeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CRYSTAL_PIT =
            FEATURES.register("crystal_pit",
                    reika.chromaticraft.world.dimension.CrystalPitFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CRYSTAL_TREE =
            FEATURES.register("crystal_tree",
                    reika.chromaticraft.world.dimension.CrystalTreeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FLOATSTONE =
            FEATURES.register("floatstone",
                    reika.chromaticraft.world.dimension.FloatstoneFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GLOW_TREE =
            FEATURES.register("glow_tree",
                    reika.chromaticraft.world.dimension.GlowTreeFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TREE_CLUSTER =
            FEATURES.register("tree_cluster",
                    reika.chromaticraft.world.dimension.TreeClusterFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FIRE_JET =
            FEATURES.register("fire_jet",
                    reika.chromaticraft.world.dimension.FireJetFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MINI_ALTAR =
            FEATURES.register("mini_altar",
                    reika.chromaticraft.world.dimension.MiniAltarFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CRYSTAL_SHRUB =
            FEATURES.register("crystal_shrub",
                    reika.chromaticraft.world.dimension.CrystalShrubFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NETHER_LAVA_RIVER =
            FEATURES.register("nether_lava_river",
                    reika.chromaticraft.world.NetherLavaRiverFeature::new);
    /** Command/debug variants: /place feature chromaticraft:nether_hut, ... */
    public static final java.util.Map<NetherRoofStructureFeature.Type,
            DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> NETHER_ROOF_STRUCTURES =
            registerNetherRoofStructures();

    private static java.util.Map<NetherRoofStructureFeature.Type,
            DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> registerNetherRoofStructures() {
        java.util.EnumMap<NetherRoofStructureFeature.Type,
                DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>> map =
                new java.util.EnumMap<>(NetherRoofStructureFeature.Type.class);
        for (NetherRoofStructureFeature.Type type : NetherRoofStructureFeature.Type.values())
            map.put(type, FEATURES.register("nether_" + type.name().toLowerCase(java.util.Locale.ROOT),
                    () -> new NetherRoofStructureFeature(type)));
        return java.util.Map.copyOf(map);
    }

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_CAVERN =
            FEATURES.register("natural_cavern", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.CAVERN, true));
    /** Command/debug seam: /place feature chromaticraft:cavern. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CAVERN =
            FEATURES.register("cavern", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.CAVERN, false));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_BURROW =
            FEATURES.register("natural_burrow", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.BURROW, true));
    /** Command/debug seam: /place feature chromaticraft:burrow, with the command position as controller. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BURROW =
            FEATURES.register("burrow", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.BURROW, false));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_OCEAN =
            FEATURES.register("natural_ocean", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.OCEAN, true));
    /** Command/debug seam: /place feature chromaticraft:ocean, with the command position as controller. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> OCEAN =
            FEATURES.register("ocean", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.OCEAN, false));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_DESERT =
            FEATURES.register("natural_desert", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.DESERT, true));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DESERT =
            FEATURES.register("desert", () -> new OverworldStructureFeature(
                    OverworldStructureFeature.Type.DESERT, false));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_SNOW =
			FEATURES.register("natural_snow", () -> new OverworldStructureFeature(
					OverworldStructureFeature.Type.SNOW, true));
	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SNOW =
			FEATURES.register("snow", () -> new OverworldStructureFeature(
					OverworldStructureFeature.Type.SNOW, false));
	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> NATURAL_BIOME_FRAGMENT =
			FEATURES.register("natural_biome_fragment", () -> new OverworldStructureFeature(
					OverworldStructureFeature.Type.BIOME_FRAGMENT, true));
	/** Command/debug seam: /place feature chromaticraft:biome_fragment. */
	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BIOME_FRAGMENT =
			FEATURES.register("biome_fragment", () -> new OverworldStructureFeature(
					OverworldStructureFeature.Type.BIOME_FRAGMENT, false));

    private ChromaFeatures() {}
}
