package reika.chromaticraft.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.base.BlockTEBase;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** Generic block backing one modern one-block-per-tile {@link ChromaTiles} entry. */
public class BlockChromaticTile extends BlockTEBase {

    private final ChromaTiles tile;

    public BlockChromaticTile(Properties props, ChromaTiles tile) {
        super(props);
        this.tile = tile;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tile.createBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof NBTTile nbtTile)
            nbtTile.setDataFromItemStackTag(stack);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof NBTTile nbtTile && !drops.isEmpty()) {
            CompoundTag customData = new CompoundTag();
            nbtTile.getTagsToWriteToStack(customData);
            if (!customData.isEmpty())
                ReikaItemHelper.setStackTag(drops.get(0), customData);
        }
        return drops;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof BlockEntityBase base)
                    base.updateEntity();
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof BlockEntityBase base) {
                base.updateEntity();
                base.updateEntity(lvl, pos);
            }
        };
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof OwnedTile owned && owned.onlyAllowOwnersToMine()
                && !owned.isOwnedByPlayer(player))
            return false;
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }
}
