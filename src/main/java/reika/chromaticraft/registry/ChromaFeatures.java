package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.CrystalFeature;
import reika.chromaticraft.world.PylonFeature;
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
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PYLON =
            FEATURES.register("pylon", () -> new PylonFeature());
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TURBOCHARGED_PYLON =
            FEATURES.register("turbocharged_pylon",
                    () -> new PylonFeature(PylonFeature.Variant.TURBOCHARGED));
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> POWER_CRYSTAL_BOOSTED_PYLON =
            FEATURES.register("power_crystal_boosted_pylon",
                    () -> new PylonFeature(PylonFeature.Variant.POWER_CRYSTAL_BOOSTED));

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
