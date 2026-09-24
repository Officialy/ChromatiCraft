package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.biome.ChromaBiomes;

/** Biome classification and feature-target tags for the Rainbow Forest pair. */
public final class ChromaBiomeTagProvider extends TagsProvider<Biome> {

    public static final TagKey<Biome> RAINBOW_FOREST = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "rainbow_forest"));
    public static final TagKey<Biome> LUMINOUS_CLIFFS = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "luminous_cliffs"));

    /** V33a {@code DyeTreeBlocker}: biomes that never grow dye trees. Empty; for packs and addons. */
    public static final TagKey<Biome> DYE_TREES_BLOCKED = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "dye_trees/blocked"));
    /** {@code c:is_dry} biomes that Forge 1.7.10 did not call DRY; see {@code TreeShaper.canGenerateTree}. */
    public static final TagKey<Biome> DYE_TREES_NOT_DRY = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "dye_trees/not_dry"));

    /** Biomes for which V33a {@code ColorTreeGenerator.getTreeCount} came to {@code trees}. */
    public static TagKey<Biome> dyeTreeCount(int trees) {
        return TagKey.create(Registries.BIOME,
                Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "dye_trees/count_" + trees));
    }

    public ChromaBiomeTagProvider(PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BIOME, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(RAINBOW_FOREST).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.RAINBOW_STREAM);
        this.tag(LUMINOUS_CLIFFS).add(ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        this.tag(BiomeTags.IS_FOREST).add(ChromaBiomes.RAINBOW_FOREST);
        this.tag(Tags.Biomes.IS_FOREST).add(ChromaBiomes.RAINBOW_FOREST);
        this.tag(BiomeTags.IS_RIVER).add(ChromaBiomes.RAINBOW_STREAM);
        this.tag(Tags.Biomes.IS_RIVER).add(ChromaBiomes.RAINBOW_STREAM);
        this.tag(BiomeTags.IS_MOUNTAIN).add(ChromaBiomes.LUMINOUS_CLIFFS);
        this.tag(Tags.Biomes.IS_MOUNTAIN).add(ChromaBiomes.LUMINOUS_CLIFFS);
        this.tag(BiomeTags.IS_OVERWORLD).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.RAINBOW_STREAM,
                ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES, ChromaBiomes.ENDER_FOREST);
        this.tag(Tags.Biomes.IS_OVERWORLD).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.RAINBOW_STREAM,
                ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES, ChromaBiomes.ENDER_FOREST);

        // V33a BiomeDictionary.registerBiomeType, whose successor is NeoForge's c: biome tags. The
        // Ender Forest had been left out of every tag, including is_overworld, which quietly kept every
        // ChromatiCraft #is_overworld generator -- cave crystals, tiered ores, pylons -- out of it.
        //   rainbowforest:     FOREST, MAGICAL, HILLS
        //   rainbowRiver:      FOREST, MAGICAL, RIVER
        //   enderforest:       FOREST, MAGICAL
        //   glowingcliffs(+Edge): MOUNTAIN, LUSH, MAGICAL, BEACH, WET
        this.tag(BiomeTags.IS_FOREST).add(ChromaBiomes.ENDER_FOREST);
        this.tag(Tags.Biomes.IS_FOREST).add(ChromaBiomes.RAINBOW_STREAM, ChromaBiomes.ENDER_FOREST);
        this.tag(Tags.Biomes.IS_MAGICAL).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.RAINBOW_STREAM,
                ChromaBiomes.ENDER_FOREST, ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        this.tag(Tags.Biomes.IS_HILL).add(ChromaBiomes.RAINBOW_FOREST);
        this.tag(Tags.Biomes.IS_MOUNTAIN).add(ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        this.tag(Tags.Biomes.IS_LUSH).add(ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        this.tag(Tags.Biomes.IS_BEACH).add(ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        this.tag(Tags.Biomes.IS_WET).add(ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);

        // V33a BiomeManager.addVillageBiome(biome, true) -- 1.7.10 had one village style, the plains one.
        this.tag(BiomeTags.HAS_VILLAGE_PLAINS).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.ENDER_FOREST);
        // V33a BiomeManager.addStrongholdBiome: the Rainbow Forest, the Ender Forest and the Cliffs, but
        // not the Cliffs' edge child.
        this.tag(BiomeTags.STRONGHOLD_BIASED_TO).add(ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.ENDER_FOREST,
                ChromaBiomes.LUMINOUS_CLIFFS);

        this.addDyeTreeTags();
    }

    /**
     * V33a {@code ColorTreeGenerator.getTreeCount} for each 26.2 biome whose 1.7.10 counterpart is known,
     * read from the 1.7.10 decompiled biome classes and Forge 10.13.4's {@code BiomeDictionary}:
     * the decorator's {@code treesPerChunk} (negative counting as 0), plus 2 for Plains, 6 for Forest and
     * Forest Hills, 3 for Extreme Hills (and Edge), Ice Plains (and Mountains), Jungle and Swampland. A zero
     * fell back to the biome's dictionary types, largest of Forest 16, Extreme Hills 3, Jungle 53, Forest
     * Hills 16, Ice Plains 3, Plains 2, Swampland 5 -- which is how Roofed Forest, Frozen River and Cold
     * Beach got trees. Unregistered mutated biomes (Birch Forest M, Extreme Hills M, Ice Plains Spikes)
     * were typed by Forge's best guess from their raised height variation: HILLS, so 16.
     *
     * <p>Biomes 1.7.10 did not have (meadow, grove, peaks, cherry grove, pale garden, mangrove swamp,
     * bamboo jungle...) get the same type fallback through their c: tags in the configured feature.
     */
    private void addDyeTreeTags() {
        this.tag(DYE_TREES_BLOCKED);
        // Savanna / Savanna Plateau were HOT SAVANNA PLAINS SPARSE, Mesa and Mesa Plateau F MESA SANDY.
        this.tag(DYE_TREES_NOT_DRY).add(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.BADLANDS, Biomes.WOODED_BADLANDS);

        // Rivers, beaches, stone beaches and the plain Mesa: nothing, and no fallback type that grows any.
        this.tag(dyeTreeCount(0)).add(Biomes.RIVER, Biomes.BEACH, Biomes.STONY_SHORE, Biomes.BADLANDS);
        // Savanna, Savanna Plateau: treesPerChunk 1.
        this.tag(dyeTreeCount(1)).add(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU);
        // Plains -999 + 2; Sunflower Plains (a plain BiomeGenPlains, -999) falls back to PLAINS;
        // Jungle Edge 2.
        this.tag(dyeTreeCount(2)).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SPARSE_JUNGLE);
        // Ice Plains 0 + 3; Extreme Hills 0 + 3; Extreme Hills+ 3; Frozen River and Cold Beach fall back
        // to SNOWY.
        this.tag(dyeTreeCount(3)).add(Biomes.SNOWY_PLAINS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.FROZEN_RIVER, Biomes.SNOWY_BEACH);
        // GlowingCliffsDecorator treesPerChunk 4, the Edge sharing it.
        this.tag(dyeTreeCount(4)).add(ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        // Swampland 2 + 3; Mesa Plateau F 5.
        this.tag(dyeTreeCount(5)).add(Biomes.SWAMP, Biomes.WOODED_BADLANDS);
        // Flower Forest: BiomeGenForest type 1, 6.
        this.tag(dyeTreeCount(6)).add(Biomes.FLOWER_FOREST);
        // BiomeEnderForest: the Forest's 10, times 0.7.
        this.tag(dyeTreeCount(7)).add(ChromaBiomes.ENDER_FOREST);
        // Birch Forest 10; Taiga, Cold Taiga, Mega Taiga, Mega Spruce Taiga 10.
        this.tag(dyeTreeCount(10)).add(Biomes.BIRCH_FOREST, Biomes.TAIGA, Biomes.SNOWY_TAIGA,
                Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        // Forest 10 + 6; Roofed Forest -999 -> FOREST; Birch Forest M, Extreme Hills M and Ice Plains
        // Spikes -> HILLS; Rainbow Forest (-999) and its river -> FOREST.
        this.tag(dyeTreeCount(16)).add(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.ICE_SPIKES, ChromaBiomes.RAINBOW_FOREST, ChromaBiomes.RAINBOW_STREAM);
        // Jungle 50 + 3.
        this.tag(dyeTreeCount(53)).add(Biomes.JUNGLE);
    }
}
