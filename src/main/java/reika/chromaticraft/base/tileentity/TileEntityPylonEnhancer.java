package reika.chromaticraft.base.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.dragonapi.interfaces.blockentity.BreakAction;

import java.util.List;

/** Shared V33a multi-owner and item-NBT behavior for pylon enhancement blocks. */
public abstract class TileEntityPylonEnhancer extends TileEntityChromaticBase implements OwnedTile, BreakAction {


    protected TileEntityPylonEnhancer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Override
    public boolean onlyAllowOwnersToMine() {
        return true;
    }

    @Override
    public boolean onlyAllowOwnersToUse() {
        return false;
    }

    @Override
    public void getTagsToWriteToStack(CompoundTag tag) {
        this.writeOwnerData(tag);
    }

    @Override
    public void setDataFromItemStackTag(ItemStack stack) {
        this.readOwnerData(stack);
    }

    @Override
    public void addTooltipInfo(List li, boolean shift) {
    }
}
