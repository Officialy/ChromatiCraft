package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/** Structure alternative that accepts every concrete CrystalElement rune registry identity. */
public final class RuneBlockCheck implements BlockCheck {

    public static final RuneBlockCheck INSTANCE = new RuneBlockCheck();

    private RuneBlockCheck() {}

    @Override
    public boolean matchInWorld(Level level, BlockPos pos) {
        return this.match(level.getBlockState(pos));
    }

    @Override
    public boolean match(BlockState state) {
        return ChromaBlocks.isRune(state);
    }

    @Override
    public boolean match(BlockCheck other) {
        return other instanceof RuneBlockCheck;
    }

    @Override
    public void place(Level level, BlockPos pos, int flags) {
        level.setBlock(pos, ChromaBlocks.rune(CrystalElement.WHITE).get().defaultBlockState(), flags);
    }

    @Override
    public ItemStack asItemStack() {
        return new ItemStack(ChromaBlocks.rune(CrystalElement.WHITE).get());
    }

    @Override
    public ItemStack getDisplay() {
        return this.asItemStack();
    }

    @Override
    public BlockKey asBlockKey() {
        return new BlockKey(ChromaBlocks.rune(CrystalElement.WHITE).get());
    }

    @Override
    public String toString() {
        return "[Any crystal rune]";
    }
}