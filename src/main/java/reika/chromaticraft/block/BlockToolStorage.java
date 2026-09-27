package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.storage.TileEntityToolStorage;

/**
 * V33a TOOLSTORAGE, a {@code BlockChromaTile2} entry: right-click with an item it accepts to put that item in, with an
 * empty hand to take the last one out (a pop either way). Sneaking does nothing.
 */
public final class BlockToolStorage extends BlockChromaticTile {

	public BlockToolStorage(Properties properties) {
		super(properties, ChromaTiles.TOOLSTORAGE);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack is, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileEntityToolStorage ts))
			return InteractionResult.PASS;
		if (ts.canPlaceItem(0, is)) {
			if (!level.isClientSide()) {
				ts.setItem(0, is.copy());
				player.setItemInHand(hand, ItemStack.EMPTY);
				level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1, 0.7F);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileEntityToolStorage ts))
			return InteractionResult.PASS;
		if (!player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		ItemStack take = ts.removeLastItem();
		if (!take.isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, take);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1, 1);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

}
