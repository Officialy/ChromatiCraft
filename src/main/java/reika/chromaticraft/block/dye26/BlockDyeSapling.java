package reika.chromaticraft.block.dye26;

import java.util.Optional;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.CrystalElement;

/** A concrete registry identity for one legacy dye-sapling metadata colour. */
public final class BlockDyeSapling extends SaplingBlock {
    private final CrystalElement element;

    public BlockDyeSapling(BlockBehaviour.Properties properties, CrystalElement element) {
        super(new TreeGrower("chromaticraft_dye_" + element.name().toLowerCase(java.util.Locale.ROOT),
                Optional.empty(), Optional.of(ChromaWorldGenProvider.dyeTree(element)), Optional.empty()), properties);
        this.element = element;
    }

    public CrystalElement getElement() {
        return element;
    }

    public int getTintColor() {
        return 0xFF000000 | element.getColor();
    }
}