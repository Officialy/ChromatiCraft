package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;

/** V33a Chroma Collector interaction and inventory-drop behavior. */
public final class BlockCollector extends BlockChromaticTile {
	public BlockCollector(Properties properties) { super(properties, ChromaTiles.COLLECTOR); }

	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof TileEntityCollector collector
				&& FluidUtil.interactWithFluidHandler(player, hand, pos, collector.fluidHandler(), null))
			return InteractionResult.SUCCESS;
		return this.open(level, pos, player);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		return this.open(level, pos, player);
	}
	private InteractionResult open(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityCollector collector)) return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(collector, pos);
		return InteractionResult.SUCCESS;
	}

	@Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityCollector collector)
			for (int slot = 0; slot < collector.getContainerSize(); slot++)
				if (!collector.getItem(slot).isEmpty()) drops.add(collector.getItem(slot).copy());
		return drops;
	}
}
