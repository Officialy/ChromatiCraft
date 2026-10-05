package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/** V33a's block-only redstone requirement permits every power level and connection shape. */
final class VoidRedstoneCheck implements BlockCheck {
    static final VoidRedstoneCheck INSTANCE=new VoidRedstoneCheck();
    private static final BlockKey DISPLAY=new BlockKey(Blocks.REDSTONE_WIRE);
    private VoidRedstoneCheck() {}
    public boolean matchInWorld(Level level,BlockPos pos) { return match(level.getBlockState(pos)); }
    public boolean match(BlockState state) { return state.is(Blocks.REDSTONE_WIRE); }
    public boolean match(BlockCheck check) { return match(check.asBlockKey().blockID); }
    public void place(Level level,BlockPos pos,int flags) { DISPLAY.place(level,pos,flags); }
    public ItemStack asItemStack() { return DISPLAY.asItemStack(); }
    public ItemStack getDisplay() { return DISPLAY.getDisplay(); }
    public BlockKey asBlockKey() { return DISPLAY; }
}
