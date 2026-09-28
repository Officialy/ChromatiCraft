package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.FeatureTags;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature tags.
 *
 * <p>{@code minecraft:can_spawn_from_bone_meal} is how 26.2 decides what bone meal grows on grass: on one
 * try in eight it picks one of the biome's tagged configured features and places it. That is the modern
 * seat of 1.7.10's {@code BiomeGenBase.plantFlower}, which V33a's Ender Forest overrode.
 */
public final class ChromaConfiguredFeatureTagProvider extends TagsProvider<Feature> {

    public ChromaConfiguredFeatureTagProvider(PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.FEATURE, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL).add(ChromaWorldGenProvider.ENDER_FOREST_FLOWER,
                ChromaWorldGenProvider.RAINBOW_FOREST_FLOWER, ChromaWorldGenProvider.LUMINOUS_CLIFFS_FLOWER);
    }
}
