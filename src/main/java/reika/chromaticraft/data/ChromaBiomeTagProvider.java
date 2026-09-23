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
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.biome.ChromaBiomes;

/** Biome classification and feature-target tags for the Rainbow Forest pair. */
public final class ChromaBiomeTagProvider extends TagsProvider<Biome> {

    public static final TagKey<Biome> RAINBOW_FOREST = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "rainbow_forest"));
    public static final TagKey<Biome> LUMINOUS_CLIFFS = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "luminous_cliffs"));

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
    }
}
