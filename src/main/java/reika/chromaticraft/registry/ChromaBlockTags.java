package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ChromaBlockTags {
    public static final TagKey<Block> NATURAL_STONE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath("chromaticraft", "natural_stone"));

    private ChromaBlockTags() {}
}
