package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;

/** Player interaction, ownership, teardown, and inventory drops for the casting controller. */
public final class BlockCastingTable extends BlockChromaticTile {
    public BlockCastingTable(Properties properties) { super(properties, ChromaTiles.TABLE); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityCastingTable table)
            table.setPlacer(player);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) { return this.activate(level, pos, player); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) { return this.activate(level, pos, player); }

    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCastingTable table)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!table.isOwnedByPlayer(player)) return InteractionResult.FAIL;
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(table, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof TileEntityCastingTable table) table.breakBlock();
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof TileEntityCastingTable table) {
            for (int slot = 0; slot < table.getContainerSize(); slot++)
                if (!table.getItem(slot).isEmpty()) drops.add(table.getItem(slot).copy());
        }
        return drops;
    }
}
