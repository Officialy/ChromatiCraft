package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
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

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** V33a direct insert/eject interaction and inventory drops for the Lumen Relay source. */
public final class BlockRelaySource extends BlockChromaticTile {

	public BlockRelaySource(Properties properties) {
		super(properties, ChromaTiles.RELAYSOURCE);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return this.interact(stack, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		return this.interact(ItemStack.EMPTY, level, pos, player);
	}

	private InteractionResult interact(ItemStack held, Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityRelaySource source))
			return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		ItemStack previous = source.removeItemNoUpdate(0);
		if (!previous.isEmpty())
			ReikaItemHelper.dropItem(level, pos.getX() + 0.5, pos.getY() + 0.75,
					pos.getZ() + 0.5, previous);
		if (source.canPlaceItem(0, held)) {
			source.setItem(0, held.copyWithCount(1));
			if (!player.getAbilities().instabuild) held.shrink(1);
		}
		else source.setChanged();
		source.syncAllData(true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityRelaySource source && !source.getItem(0).isEmpty())
			drops.add(source.getItem(0).copy());
		return drops;
	}
}
