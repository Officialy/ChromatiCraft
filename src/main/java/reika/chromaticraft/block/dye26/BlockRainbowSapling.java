package reika.chromaticraft.block.dye26;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import reika.chromaticraft.data.ChromaWorldGenProvider;

/** Sapling for the distinct rainbow-tree family. */
public final class BlockRainbowSapling extends SaplingBlock {
    public BlockRainbowSapling(BlockBehaviour.Properties properties) {
        super(new TreeGrower("chromaticraft_rainbow", WeightedList.of(ChromaWorldGenProvider.RAINBOW_TREE),
                WeightedList.of(), WeightedList.of(), ChromaWorldGenProvider.RAINBOW_TREE), properties);
    }
}
