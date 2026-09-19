package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;

public final class BlockRitualTable extends BlockChromaticTile {

	public static final BooleanProperty ENHANCED = BooleanProperty.create("enhanced");

	public BlockRitualTable(Properties properties) {
		super(properties, ChromaTiles.RITUAL);
		registerDefaultState(stateDefinition.any().setValue(ENHANCED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ENHANCED);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof reika.chromaticraft.item.ItemManipulator)
			return InteractionResult.PASS;
		return activate(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		return activate(level, pos, player);
	}

	private InteractionResult activate(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityRitualTable table))
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		if (!table.isOwnedByPlayer(player))
			return InteractionResult.FAIL;
		table.initEnhancementCheck(player);
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(table, pos);
		return InteractionResult.SUCCESS;
	}

	public static void setEnhanced(Level level, BlockPos pos, boolean enhanced) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof BlockRitualTable && state.getValue(ENHANCED) != enhanced)
			level.setBlock(pos, state.setValue(ENHANCED, enhanced), Block.UPDATE_CLIENTS);
	}
}
