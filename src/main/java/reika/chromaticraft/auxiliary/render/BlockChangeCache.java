package reika.chromaticraft.auxiliary.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.base.ItemBlockChangingWand;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

import java.util.List;

/** The original targeted-wand preview cache, refreshed when its selection or surrounding world changes. */
public final class BlockChangeCache {
    private Level level;
    private BlockPos origin;
    private BlockState state;
    private ItemStack item = ItemStack.EMPTY;
    private boolean sneaking;
    private int depth;
    private long tick = Long.MIN_VALUE;
    private List<BlockPos> overlay = List.of();

    public List<BlockPos> get(Level level, BlockPos pos, Player player, ItemStack stack) {
        ItemBlockChangingWand wand = (ItemBlockChangingWand)stack.getItem();
        int range = wand.getDepth(player, level, pos);
        BlockState target = level.getBlockState(pos);
        if (this.level != level || !pos.equals(origin) || target != state || sneaking != player.isShiftKeyDown()
                || !ItemStack.isSameItemSameComponents(stack, item) || depth != range || tick != level.getGameTime()) {
            BlockArray blocks = new BlockArray();
            blocks.maxDepth = range-1;
            wand.getSpreadBlocks(level, pos, blocks, player, stack);
            var positions = new java.util.ArrayList<BlockPos>();
            for (int i = 0; i < blocks.getSize(); i++) positions.add(blocks.getNthBlock(i).immutable());
            overlay = List.copyOf(positions);
            this.level = level; origin = pos.immutable(); state = target; sneaking = player.isShiftKeyDown();
            item = stack.copy(); depth = range; tick = level.getGameTime();
        }
        return overlay;
    }

    public void clearCache() {
        level = null; item = ItemStack.EMPTY; overlay = List.of(); tick = Long.MIN_VALUE;
    }
}

