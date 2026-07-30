package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;

/** Runtime-semantic tags for ChromatiCraft blocks whose behaviour depends on vanilla families. */
public final class ChromaBlockTagsProvider extends BlockTagsProvider {
    public ChromaBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, ChromatiCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var leaves = tag(BlockTags.LEAVES);
        ChromaBlocks.DYE_LEAVES.forEach(block -> leaves.add(block.getKey()));
        leaves.add(ChromaBlocks.RAINBOW_LEAVES.getKey(), ChromaBlocks.GLOWING_LEAVES.getKey());
        tag(BlockTags.SMALL_FLOWERS).add(ChromaBlocks.GLOW_DAISY.getKey());
    }
}