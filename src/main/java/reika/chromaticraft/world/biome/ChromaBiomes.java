package reika.chromaticraft.world.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.CrystalElement;

/** Datapack-backed definitions of the V33a Rainbow Forest and its Rainbow Stream river. */
public final class ChromaBiomes {

    public static final ResourceKey<Biome> RAINBOW_FOREST = key("rainbow_forest");
    public static final ResourceKey<Biome> RAINBOW_STREAM = key("rainbow_stream");
    public static final ResourceKey<Biome> LUMINOUS_CLIFFS = key("luminous_cliffs");
    public static final ResourceKey<Biome> LUMINOUS_CLIFFS_SHORES = key("luminous_cliffs_shores");
    public static final ResourceKey<Biome> ENDER_FOREST = key("ender_forest");

    /** Shared with the Proxima biomes, whose base derives from the Rainbow Forest's water. */
    public static final int WATER_COLOR = 0x00ffff;
    private static final int SKY_COLOR = 0x648cff;

    /**
     * Vanilla forest's grass/foliage colours. Both V33a Rainbow Forest and V33a Luminous Cliffs
     * derive their palette from {@code BiomeGenBase.forest}, so this is the shared base rather than
     * a per-biome choice.
     */
    public static final int FOREST_GRASS = 0x79c05a;
    private static final int FOREST_FOLIAGE = 0x59ae30;

    private ChromaBiomes() {}

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(RAINBOW_FOREST, create(features, carvers, false));
        context.register(RAINBOW_STREAM, create(features, carvers, true));
        context.register(LUMINOUS_CLIFFS, createLuminousCliffs(features, carvers, false));
        context.register(LUMINOUS_CLIFFS_SHORES, createLuminousCliffs(features, carvers, true));
        context.register(ENDER_FOREST, createEnderForest(features, carvers));
    }

    private static Biome create(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean stream) {
        MobSpawnSettings.Builder mobs = sourceSpawns();
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addGlobalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addForestFlowers(generation);
        if (stream) {
            BiomeDefaultFeatures.addWaterTrees(generation);
        } else {
            for (CrystalElement element : CrystalElement.elements) {
                generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                        features.getOrThrow(ChromaWorldGenProvider.dyeTreePlaced(element)));
            }
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                    features.getOrThrow(ChromaWorldGenProvider.RAINBOW_TREE_PLACED));
        }
        BiomeDefaultFeatures.addBushes(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addForestGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        if (stream) {
            // Keep the shared river features in vanilla River's order. FeatureSorter compares
            // their relative order across every biome; moving seagrass ahead of bushes/flowers
            // creates a River <-> Rainbow Stream cycle during worldgen bootstrap.
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
        }

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(stream ? 0.6375F : 0.7F)
                .downfall(0.8F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, SKY_COLOR)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(WATER_COLOR)
                        .grassColorOverride(FOREST_GRASS)
                        .foliageColorOverride(FOREST_FOLIAGE)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }

    /**
     * V33a {@code BiomeEnderForest}: a rainless forest whose monster list is cleared and rebuilt
     * around endermen.
     *
     * <p>Source spawn weights are Enderman 10 against Creeper, Spider and Skeleton at 1 each, all in
     * groups of 1-4, which is what makes the biome feel like an enderman wood rather than an ordinary
     * one. Rain is disabled outright.
     *
     * <p>V33a also thins trees to 0.7x and picks between vanilla oak, vanilla big oak and three Ender
     * Oak variants through a noise-driven weighted table (a {@code Simplex3DGenerator} at frequency
     * 1/30, with each entry's weight shifted by the local noise value and a "no tree" entry
     * competing alongside them). That selector has no vanilla equivalent and is not expressible as a
     * biome tree feature, so it is deliberately absent here rather than approximated with a plain
     * weighted list: the biome currently carries vanilla forest trees, and the real selector lands
     * with the Ender Oak feature. See TODO.md.
     */
    private static Biome createEnderForest(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.ENDERMAN, 1, 4))
                .addSpawn(MobCategory.MONSTER, 1, new MobSpawnSettings.SpawnerData(EntityTypes.CREEPER, 1, 4))
                .addSpawn(MobCategory.MONSTER, 1, new MobSpawnSettings.SpawnerData(EntityTypes.SPIDER, 1, 4))
                .addSpawn(MobCategory.MONSTER, 1, new MobSpawnSettings.SpawnerData(EntityTypes.SKELETON, 1, 4));
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addGlobalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addForestFlowers(generation);
        // V33a's own tree selector, not a vanilla tree list -- see EnderForestTreeFeature.
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.ENDER_FOREST_TREE_PLACED));
        BiomeDefaultFeatures.addBushes(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addForestGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);

        return new Biome.BiomeBuilder()
                // V33a setDisableRain().
                .hasPrecipitation(false)
                .temperature(0.7F)
                .downfall(0.8F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, SKY_COLOR)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(WATER_COLOR)
                        .grassColorOverride(FOREST_GRASS)
                        .foliageColorOverride(FOREST_FOLIAGE)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }

    private static Biome createLuminousCliffs(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean shore) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SPIDER, 1, 4))
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE, 1, 4))
                .addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(EntityTypes.SKELETON, 1, 4))
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SLIME, 1, 4))
                .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityTypes.ENDERMAN, 1, 4))
                .addSpawn(MobCategory.AMBIENT, 10, new MobSpawnSettings.SpawnerData(EntityTypes.BAT, 1, 4))
                // V33a BiomeGlowingCliffs.glowCloudList: SpawnListEntry(EntityGlowCloud, weight 30,
                // min 1, max 1), returned from a custom EnumCreatureType ("glowcloud", cap 24) rather
                // than a vanilla category. CREATURE is the closest existing MobCategory (V33a
                // registered it with isPeacefulCreature=true); the custom 24-instance world cap and
                // EntityGlowCloud.getMaxSpawnedInChunk()==8 chunk cap have no direct MobCategory
                // equivalent and are not reproduced here.
                .addSpawn(MobCategory.CREATURE, 30,
                        new MobSpawnSettings.SpawnerData(ChromaEntityTypes.GLOW_CLOUD.get(), 1, 1));

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addGlobalOverworldGeneration(generation);
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_CLIFFS_TERRAIN_PLACED));
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_ISLAND_PLACED));
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.LUMA_PATCH_PLACED));
        BiomeDefaultFeatures.addMountainForestTrees(generation);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.GLOWING_TREE_PLACED));
        BiomeDefaultFeatures.addForestFlowers(generation);
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addForestGrass(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_FLORA_PLACED));

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.75F)
                .downfall(0.85F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xd0a0ff)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        new BackgroundMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        // V33a getWaterColorMultiplier(). The per-position teal/pink/clear variation
                        // in getWaterColor() cannot be hooked in 26.2 (FluidRenderer picks the
                        // FluidModel per FluidState, never per position) -- see ISSUES_2026-07-30 E2.
                        .waterColor(0x22ffbb)
                        // V33a resolves grass/foliage as forest's colour *then* shifts hue by
                        // position and brightness by altitude (BiomeGlowingCliffs#getBiomeGrassColor
                        // -> BiomeGenBase.forest.getBiomeGrassColor + shiftHue + shiftBrightness).
                        // These overrides are therefore forest's exact palette -- the base the shift
                        // is applied to -- and LuminousCliffsColors does the shifting client-side.
                        // They are NOT the final in-world colour; do not "brighten" them here.
                        .grassColorOverride(FOREST_GRASS)
                        .foliageColorOverride(FOREST_FOLIAGE)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }

    /** V33a explicitly cleared every vanilla list before adding these entries. */
    private static MobSpawnSettings.Builder sourceSpawns() {
        return new MobSpawnSettings.Builder()
                .creatureGenerationProbability(0.1F)
                .addSpawn(MobCategory.MONSTER, 8,
                        new MobSpawnSettings.SpawnerData(EntityTypes.SLIME, 4, 4))
                .addSpawn(MobCategory.CREATURE, 3,
                        new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 4))
                .addSpawn(MobCategory.CREATURE, 6,
                        new MobSpawnSettings.SpawnerData(EntityTypes.SHEEP, 4, 4))
                .addSpawn(MobCategory.CREATURE, 7,
                        new MobSpawnSettings.SpawnerData(EntityTypes.PIG, 4, 4))
                .addSpawn(MobCategory.CREATURE, 6,
                        new MobSpawnSettings.SpawnerData(EntityTypes.CHICKEN, 4, 4))
                .addSpawn(MobCategory.CREATURE, 6,
                        new MobSpawnSettings.SpawnerData(EntityTypes.COW, 4, 4))
                .addSpawn(MobCategory.CREATURE, 3,
                        new MobSpawnSettings.SpawnerData(EntityTypes.HORSE, 1, 3))
                .addSpawn(MobCategory.WATER_CREATURE, 10,
                        new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 4, 4));
    }

    private static void addGlobalOverworldGeneration(BiomeGenerationSettings.Builder generation) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME,
                Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
    }
}
