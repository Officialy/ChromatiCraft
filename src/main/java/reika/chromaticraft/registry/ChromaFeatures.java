package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

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

/** Runtime feature registry for ChromatiCraft's data-driven 26.2 world generation. */
public final class ChromaFeatures {

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends Feature>> FEATURES =
            DeferredRegister.create(Registries.FEATURE_TYPE, ChromatiCraft.MODID);

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
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.densityfunction.DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ChromatiCraft.MODID);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.densityfunction.DensityFunction>,
            com.mojang.serialization.MapCodec<reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction>> PROXIMA_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("proxima_terrain",
                    () -> reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CAVE_CRYSTAL =
            FEATURES.register("cave_crystal", () -> CrystalFeature.CODEC);
    /**
     * The plant half of V33a's TieredWorldGenerator, one feature per plant identity. Each resolves
     * its own registered block lazily so registration order does not matter.
     */
    public static final java.util.Map<ChromaTieredPlants, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> TIERED_PLANTS =
            registerTieredPlants();

    private static java.util.Map<ChromaTieredPlants, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> registerTieredPlants() {
        java.util.EnumMap<ChromaTieredPlants, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> map =
                new java.util.EnumMap<>(ChromaTieredPlants.class);
        for (ChromaTieredPlants plant : ChromaTieredPlants.list)
            map.put(plant, FEATURES.register(plant.registryName(), () -> TieredPlantFeature.CODEC));
        return map;
    }

    public static final java.util.Map<ChromaDecoFlowers, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> DECO_FLOWERS =
            registerDecoFlowers();

    private static java.util.Map<ChromaDecoFlowers, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> registerDecoFlowers() {
        java.util.EnumMap<ChromaDecoFlowers, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> map =
                new java.util.EnumMap<>(ChromaDecoFlowers.class);
        for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
            map.put(flower, FEATURES.register(flower.registryName(), () -> DecoFlowerFeature.CODEC));
        return map;
    }

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> ENDER_FOREST_TREE =
            FEATURES.register("ender_forest_tree", () -> EnderForestTreeFeature.CODEC);

    /** V33a EnderPoolGenerator; the chunk chance is rolled from the live ENDERPOOLS option. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> ENDER_POOL =
            FEATURES.register("ender_pool", () -> reika.chromaticraft.world.EnderPoolFeature.CODEC);

    /** V33a BiomeEnderForest.plantFlower; bone meal's only choice in the Ender Forest. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> ENDER_FOREST_FLOWER =
            FEATURES.register("ender_forest_flower", () -> reika.chromaticraft.world.EnderForestFlowerFeature.CODEC);

    /** V33a TreeShaper as a configured feature: what dye saplings grow into. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> DYE_TREE =
            FEATURES.register("dye_tree", () -> reika.chromaticraft.world.DyeTreeFeature.CODEC);

    /** V33a ColorTreeGenerator: the odd dye or rainbow tree across the ordinary overworld. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> COLOR_TREE =
            FEATURES.register("color_tree", () -> reika.chromaticraft.world.ColorTreeFeature.CODEC);

    /** V33a RainbowForestGenerator: one Rainbow Forest tree attempt. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> RAINBOW_FOREST_TREE =
            FEATURES.register("rainbow_forest_tree", () -> reika.chromaticraft.world.RainbowForestTreeFeature.CODEC);

    /** V33a RainbowForestDecorator.generateDyeFlowers. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> DYE_FLOWER_PATCH =
            FEATURES.register("dye_flower_patch", () -> reika.chromaticraft.world.DyeFlowerPatchFeature.CODEC);

    /** V33a RainbowForestDecorator.generateUndergroundLight. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> RAINBOW_FOREST_UNDERGROUND =
            FEATURES.register("rainbow_forest_underground", () -> reika.chromaticraft.world.RainbowForestUndergroundFeature.CODEC);

    /** V33a BiomeRainbowForest.plantFlower, reached only by bone meal. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> RAINBOW_FOREST_FLOWER =
            FEATURES.register("rainbow_forest_flower", () -> reika.chromaticraft.world.RainbowForestFlowerFeature.CODEC);

    /** V33a GlowingCliffsDecorator.NoFloatingGravelVein. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NO_FLOATING_GRAVEL =
            FEATURES.register("no_floating_gravel", () -> reika.chromaticraft.world.luminous.NoFloatingGravelFeature.CODEC);

    /** V33a GlowingCliffsAuxGenerator: cavern sapling growth and cleanup. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> LUMINOUS_CLIFFS_AUX =
            FEATURES.register("luminous_cliffs_aux", () -> reika.chromaticraft.world.luminous.LuminousCliffsAuxFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> RAINBOW_TREE =
            FEATURES.register("rainbow_tree", () -> reika.chromaticraft.world.RainbowTreeFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> SKYPEATER =
            FEATURES.register("skypeater", () -> SkypeaterFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> WARP_NODE =
            FEATURES.register("warp_node", () -> WarpNodeFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> UNKNOWN_ARTEFACT =
            FEATURES.register("unknown_artefact", () -> UnknownArtefactFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CAVE_INDICATOR =
            FEATURES.register("cave_indicator", () -> CaveIndicatorFeature.CODEC);

    /** Natural worldgen keeps the source random colour roll under an explicitly natural-only id. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_PYLON =
            FEATURES.register("natural_pylon", () -> PylonFeature.CODEC);
    /** Command/debug features: /place feature chromaticraft:pylon_<vanilla dye name>. */
    public static final java.util.Map<CrystalElement, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> COLORED_PYLONS =
            registerColoredPylons();

    private static java.util.Map<CrystalElement, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> registerColoredPylons() {
        java.util.EnumMap<CrystalElement, DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> map =
                new java.util.EnumMap<>(CrystalElement.class);
        for (CrystalElement element : CrystalElement.elements)
            map.put(element, FEATURES.register("pylon_" + element.getEnglishName(),
                    () -> PylonFeature.CODEC));
        return java.util.Map.copyOf(map);
    }
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> DATA_TOWER =
            FEATURES.register("data_tower", () -> DataTowerFeature.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> RUNIC_ALTAR =
            FEATURES.register("runic_altar", () -> reika.chromaticraft.world.RunicAltarFeature.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> TURBOCHARGED_PYLON =
            FEATURES.register("turbocharged_pylon",
                    () -> PylonFeature.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> POWER_CRYSTAL_BOOSTED_PYLON =
            FEATURES.register("power_crystal_boosted_pylon",
                    () -> PylonFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> LUMINOUS_CLIFFS_TERRAIN =
            FEATURES.register("luminous_cliffs_terrain", () -> LuminousCliffsTerrainFeature.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> LUMA_PATCH =
            FEATURES.register("luma_patch", () -> LumaPatchFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NETHER_ROOF_STRUCTURE =
            FEATURES.register("nether_roof_structure", () -> NetherRoofStructureFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> AURORAE =
            FEATURES.register("aurorae",
                    () -> reika.chromaticraft.world.dimension.AuroraeFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CRYSTAL_PIT =
            FEATURES.register("crystal_pit",
                    () -> reika.chromaticraft.world.dimension.CrystalPitFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CRYSTAL_TREE =
            FEATURES.register("crystal_tree",
                    () -> reika.chromaticraft.world.dimension.CrystalTreeFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> FLOATSTONE =
            FEATURES.register("floatstone",
                    () -> reika.chromaticraft.world.dimension.FloatstoneFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> GLOW_TREE =
            FEATURES.register("glow_tree",
                    () -> reika.chromaticraft.world.dimension.GlowTreeFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> TREE_CLUSTER =
            FEATURES.register("tree_cluster",
                    () -> reika.chromaticraft.world.dimension.TreeClusterFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> FIRE_JET =
            FEATURES.register("fire_jet",
                    () -> reika.chromaticraft.world.dimension.FireJetFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> GLOWING_CRACKS =
            FEATURES.register("glowing_cracks",
                    () -> reika.chromaticraft.world.dimension.GlowingCracksFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> FISSURE =
            FEATURES.register("fissure",
                    () -> reika.chromaticraft.world.dimension.FissureFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> MINI_ALTAR =
            FEATURES.register("mini_altar",
                    () -> reika.chromaticraft.world.dimension.MiniAltarFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> LIGHTED_SHRUB =
            FEATURES.register("lighted_shrub",
                    () -> reika.chromaticraft.world.dimension.LightedShrubFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CRYSTAL_SHRUB =
            FEATURES.register("crystal_shrub",
                    () -> reika.chromaticraft.world.dimension.CrystalShrubFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NETHER_LAVA_RIVER =
            FEATURES.register("nether_lava_river",
                    () -> reika.chromaticraft.world.NetherLavaRiverFeature.CODEC);
    /** Command/debug variants: /place feature chromaticraft:nether_hut, ... */
    public static final java.util.Map<NetherRoofStructureFeature.Type,
            DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> NETHER_ROOF_STRUCTURES =
            registerNetherRoofStructures();

    private static java.util.Map<NetherRoofStructureFeature.Type,
            DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> registerNetherRoofStructures() {
        java.util.EnumMap<NetherRoofStructureFeature.Type,
                DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>>> map =
                new java.util.EnumMap<>(NetherRoofStructureFeature.Type.class);
        for (NetherRoofStructureFeature.Type type : NetherRoofStructureFeature.Type.values())
            map.put(type, FEATURES.register("nether_" + type.name().toLowerCase(java.util.Locale.ROOT),
                    () -> NetherRoofStructureFeature.CODEC));
        return java.util.Map.copyOf(map);
    }

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_CAVERN =
            FEATURES.register("natural_cavern", () -> OverworldStructureFeature.CODEC);
    /** Command/debug seam: /place feature chromaticraft:cavern. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> CAVERN =
            FEATURES.register("cavern", () -> OverworldStructureFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_BURROW =
            FEATURES.register("natural_burrow", () -> OverworldStructureFeature.CODEC);
    /**
     * Command/debug seam: /place feature chromaticraft:burrow, with the command position as controller
     * and both optional annexes forced so the lamps, key and locked cache can be inspected together.
     */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> BURROW =
            FEATURES.register("burrow", () -> OverworldStructureFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_OCEAN =
            FEATURES.register("natural_ocean", () -> OverworldStructureFeature.CODEC);
    /** Command/debug seam: /place feature chromaticraft:ocean, with the command position as controller. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> OCEAN =
            FEATURES.register("ocean", () -> OverworldStructureFeature.CODEC);

    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_DESERT =
            FEATURES.register("natural_desert", () -> OverworldStructureFeature.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> DESERT =
            FEATURES.register("desert", () -> OverworldStructureFeature.CODEC);

	public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_SNOW =
			FEATURES.register("natural_snow", () -> OverworldStructureFeature.CODEC);
	public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> SNOW =
			FEATURES.register("snow", () -> OverworldStructureFeature.CODEC);
	public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> NATURAL_BIOME_FRAGMENT =
			FEATURES.register("natural_biome_fragment", () -> OverworldStructureFeature.CODEC);
	/** Command/debug seam: /place feature chromaticraft:biome_fragment. */
	public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends Feature>, com.mojang.serialization.MapCodec<? extends Feature>> BIOME_FRAGMENT =
			FEATURES.register("biome_fragment", () -> OverworldStructureFeature.CODEC);

    private ChromaFeatures() {}
}
