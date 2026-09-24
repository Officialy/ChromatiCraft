package reika.chromaticraft.world.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
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
    /** Vanilla's untinted water, which is what a 1.7.10 biome with no water multiplier rendered. */
    public static final int DEFAULT_WATER_COLOR = 0x3f76e4;
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

    /**
     * V33a {@code BiomeRainbowForest} and {@code RainbowRiver}, decorated by {@code RainbowForestDecorator}
     * and {@code DecoratorRainbowRiver}.
     *
     * <p>Upstream replaced the whole 1.7.10 decorator, so nothing vanilla decorates these biomes beyond
     * the terrain passes every overworld biome shared (ores, lakes, dungeons, springs). There are no
     * vanilla trees, bushes, mushrooms, pumpkins or mixed flowers: only dye trees, dye flowers,
     * dandelions (and in the stream, poppies), grass, sugar cane, sand and clay, the sparkling ground and
     * lights underground, and the odd rainbow tree. The stream uses the same decorator with its own
     * counts, and no lava springs.
     */
    private static Biome create(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean stream) {
        MobSpawnSettings.Builder mobs = sourceSpawns();
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        addGlobalOverworldGeneration(generation, !stream);
        BiomeDefaultFeatures.addDefaultOres(generation);
        ChromaWorldGenProvider.RainbowDecoration deco = stream
                ? ChromaWorldGenProvider.RAINBOW_STREAM_DECORATION : ChromaWorldGenProvider.RAINBOW_FOREST_DECORATION;
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, features.getOrThrow(deco.sandDisk()));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, features.getOrThrow(deco.clayDisk()));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, features.getOrThrow(deco.lateSandDisk()));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.RAINBOW_FOREST_UNDERGROUND_PLACED));
        // V33a genDecorations order: dye flowers, then the auxiliary flowers, grass and reeds, and the
        // trees last of all.
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.DYE_FLOWER_PATCH_PLACED));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(deco.dandelion()));
        if (deco.roses())
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(deco.rose()));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(deco.grassPatch()));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(deco.reedPatch()));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.RAINBOW_FOREST_TREE_PLACED));
        // Bone meal's plantFlower; count zero, never run by world generation.
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.RAINBOW_FOREST_FLOWER_PLACED));

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
        // V33a BiomeEnderForest extends BiomeGenForest(id, 0) and clears only the *monster* list. Every
        // other list is the 1.7.10 forest's: BiomeGenBase's farm animals, squid and bats, plus the wolf a
        // type-0 forest adds. Emitting only the monsters left the biome with no animals at all.
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.CREATURE, 12, new MobSpawnSettings.SpawnerData(EntityTypes.SHEEP, 4, 4))
                .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.PIG, 4, 4))
                .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.CHICKEN, 4, 4))
                .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.COW, 4, 4))
                .addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityTypes.WOLF, 4, 4))
                .addSpawn(MobCategory.WATER_CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 4, 4))
                .addSpawn(MobCategory.AMBIENT, 10, new MobSpawnSettings.SpawnerData(EntityTypes.BAT, 8, 8))
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
        // Vanilla's flower_default under an untagged ChromatiCraft key, so that bone meal here sees only
        // the biome's own plantFlower (ender_forest_flower, listed with a count of zero just below).
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.ENDER_FOREST_DEFAULT_FLOWERS_PLACED));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.ENDER_FOREST_FLOWER_PLACED));
        BiomeDefaultFeatures.addForestGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
        // V33a DecoratorEnderForest calls super.genDecorations first and only then its pool, so the pool
        // is the last thing the biome decorates.
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.ENDER_POOL_PLACED));

        return new Biome.BiomeBuilder()
                // V33a setDisableRain().
                .hasPrecipitation(false)
                .temperature(0.7F)
                .downfall(0.8F)
                // BiomeEnderForest overrides neither sky nor base water, so both are the 1.7.10 forest's:
                // the temperature-derived sky (the same formula 26.2 uses) and untinted water, which is
                // vanilla's default water colour. The Rainbow Forest's cyan and its 100,140,255 sky had
                // been copied here by mistake. ENDERCOLORING's pink water is applied at render time.
                .setAttribute(EnvironmentAttributes.SKY_COLOR,
                        net.minecraft.data.worldgen.biome.OverworldBiomes.calculateSkyColor(0.7F))
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(DEFAULT_WATER_COLOR)
                        // The forest end of V33a's per-block forest/ice-plains blend; EnderForestColors
                        // applies the blend over this at render time.
                        .grassColorOverride(FOREST_GRASS)
                        .foliageColorOverride(FOREST_FOLIAGE)
                        .build())
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }

    private static Biome createLuminousCliffs(HolderGetter<PlacedFeature> features,
            HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean shore) {
        // V33a BiomeGlowingCliffs clears and rebuilds only the monster and cave lists; the creature and
        // water lists are BiomeGenBase's farm animals and squid. Its monsters come in groups of four.
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.CREATURE, 12, new MobSpawnSettings.SpawnerData(EntityTypes.SHEEP, 4, 4))
                .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.PIG, 4, 4))
                .addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.CHICKEN, 4, 4))
                .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.COW, 4, 4))
                .addSpawn(MobCategory.WATER_CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 4, 4))
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SPIDER, 4, 4))
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE, 4, 4))
                .addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(EntityTypes.SKELETON, 4, 4))
                .addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SLIME, 4, 4))
                .addSpawn(MobCategory.MONSTER, 2, new MobSpawnSettings.SpawnerData(EntityTypes.ENDERMAN, 1, 4))
                .addSpawn(MobCategory.AMBIENT, 10, new MobSpawnSettings.SpawnerData(EntityTypes.BAT, 8, 8))
                // V33a BiomeGlowingCliffs.glowCloudList: SpawnListEntry(EntityGlowCloud, weight 30,
                // min 1, max 1), returned from a custom EnumCreatureType ("glowcloud", cap 24) rather
                // than a vanilla category. CREATURE is the closest existing MobCategory (V33a
                // registered it with isPeacefulCreature=true); the custom 24-instance world cap and
                // EntityGlowCloud.getMaxSpawnedInChunk()==8 chunk cap have no direct MobCategory
                // equivalent and are not reproduced here.
                .addSpawn(MobCategory.CREATURE, 30,
                        new MobSpawnSettings.SpawnerData(ChromaEntityTypes.GLOW_CLOUD.get(), 1, 1));

        // V33a GlowingCliffsDecorator replaced the whole 1.7.10 decorator; see
        // ChromaWorldGenProvider.registerLuminousPlacements for its passes. The shared terrain passes are
        // vanilla's, except that the ordinary gravel ore gives way to the cliffs' own non-floating veins
        // and the springs are the decorator's doubled ones.
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIRT);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_GRAVEL_ORE_PLACED));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_GRANITE_UPPER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_GRANITE_LOWER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIORITE_UPPER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIORITE_LOWER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_ANDESITE_UPPER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_ANDESITE_LOWER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, net.minecraft.data.worldgen.placement.OrePlacements.ORE_TUFF);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, net.minecraft.data.worldgen.placement.CavePlacements.GLOW_LICHEN);
        generation.addFeature(GenerationStep.Decoration.FLUID_SPRINGS,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_SPRING_WATER_PLACED));
        generation.addFeature(GenerationStep.Decoration.FLUID_SPRINGS,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_SPRING_LAVA_PLACED));
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        // The floating islands are a structure (LuminousIslandStructure), too large for a feature.
        BiomeDefaultFeatures.addDefaultOres(generation);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_DISK_SAND_PLACED));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_DISK_CLAY_PLACED));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                features.getOrThrow(ChromaWorldGenProvider.LUMINOUS_GRAVEL_PATCH_PLACED));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION,
                features.getOrThrow(ChromaWorldGenProvider.LUMA_PATCH_PLACED));
        for (var key : java.util.List.of(ChromaWorldGenProvider.LUMINOUS_TREES_PLACED,
                ChromaWorldGenProvider.LUMINOUS_FLOWERS_PLACED, ChromaWorldGenProvider.LUMINOUS_GRASS_PLACED,
                ChromaWorldGenProvider.LUMINOUS_LILY_PAD_PLACED, ChromaWorldGenProvider.LUMINOUS_BROWN_MUSHROOM_PLACED,
                ChromaWorldGenProvider.LUMINOUS_RED_MUSHROOM_PLACED, ChromaWorldGenProvider.LUMINOUS_REEDS_PLACED,
                ChromaWorldGenProvider.LUMINOUS_CLIFFS_FLOWER_PLACED))
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, features.getOrThrow(key));

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.75F)
                .downfall(0.85F)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xd0a0ff)
                .setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC,
                        new BackgroundMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES))
                .specialEffects(new BiomeSpecialEffects.Builder()
                        // V33a getWaterColorMultiplier(). The per-position teal/pink/clear variation of
                        // getWaterColor() is applied at render time by ChromaWaterColors, through
                        // DragonAPI's WaterColorEvent.
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
                // V33a getSpawningChance at the default ANIMALSPAWN of 6; RainbowForestSpawnDensity
                // replaces it with the configured value when the server loads.
                .creatureGenerationProbability(RainbowForestSpawnDensity.spawningChance(6))
				.addSpawn(MobCategory.MONSTER, 5,
						new MobSpawnSettings.SpawnerData(ChromaEntityTypes.BALL_LIGHTNING.get(), 1, 1))
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
        addGlobalOverworldGeneration(generation, true);
    }

    /**
     * 1.7.10's decorator made 50 water and 20 lava springs per chunk, vanilla's modern springs are its
     * translation of those; {@code DecoratorRainbowRiver.generateLavaLakes} turns the lava ones off.
     */
    private static void addGlobalOverworldGeneration(BiomeGenerationSettings.Builder generation, boolean lavaSprings) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        if (lavaSprings)
            BiomeDefaultFeatures.addDefaultSprings(generation);
        else
            generation.addFeature(GenerationStep.Decoration.FLUID_SPRINGS,
                    net.minecraft.data.worldgen.placement.MiscOverworldPlacements.SPRING_WATER);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME,
                Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
    }
}
