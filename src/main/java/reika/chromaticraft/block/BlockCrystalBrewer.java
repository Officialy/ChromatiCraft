package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;

/** V33a BREWER, a full-cube {@code BlockChromaTile} entry: opens its GUI and drops its contents with itself. */
public final class BlockCrystalBrewer extends BlockChromaticTile {

	public BlockCrystalBrewer(Properties properties) {
		super(properties, ChromaTiles.BREWER);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityCrystalBrewer te)
			te.setPlacer(player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	private InteractionResult open(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityCrystalBrewer te))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(te, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityCrystalBrewer te) {
			for (int slot = 0; slot < te.getContainerSize(); slot++) {
				ItemStack stack = te.getItem(slot);
				if (!stack.isEmpty()) drops.add(stack.copy());
			}
		}
		return drops;
	}
}
