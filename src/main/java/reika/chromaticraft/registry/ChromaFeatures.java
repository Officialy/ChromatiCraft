package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.CrystalFeature;
import reika.chromaticraft.world.PylonFeature;
import reika.chromaticraft.world.CaveIndicatorFeature;
import reika.chromaticraft.world.DecoFlowerFeature;
import reika.chromaticraft.world.EnderForestTreeFeature;
import reika.chromaticraft.world.UnknownArtefactFeature;
import reika.chromaticraft.world.TieredPlantFeature;
import reika.chromaticraft.world.luminous.LumaPatchFeature;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature;
import reika.chromaticraft.world.luminous.LuminousIslandFeature;
import reika.chromaticraft.world.luminous.LuminousFloraFeature;

/** Runtime feature registry for ChromatiCraft's data-driven 26.2 world generation. */
public final class ChromaFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ChromatiCraft.MODID);

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

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> UNKNOWN_ARTEFACT =
            FEATURES.register("unknown_artefact", UnknownArtefactFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CAVE_INDICATOR =
            FEATURES.register("cave_indicator", CaveIndicatorFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PYLON =
            FEATURES.register("pylon", () -> new PylonFeature());
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

    private ChromaFeatures() {}
}
