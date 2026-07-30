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
                ChromaBiomes.LUMINOUS_CLIFFS, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
    }
}
