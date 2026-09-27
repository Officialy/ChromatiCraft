package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;

/**
 * V33a ENCHANTER, a {@code BlockChromaTile} entry: opens its GUI, drops its two items with itself, and shows its side
 * variant texture while an enchanting table sits on top (V33a {@code VariableTexture}, now the ASSISTED state).
 */
public final class BlockAutoEnchanter extends BlockChromaticTile {

	public static final BooleanProperty ASSISTED = BooleanProperty.create("assisted");

	public BlockAutoEnchanter(Properties properties) {
		super(properties, ChromaTiles.ENCHANTER);
		this.registerDefaultState(this.stateDefinition.any().setValue(ASSISTED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ASSISTED);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(ASSISTED, isTable(context.getLevel().getBlockState(context.getClickedPos().above())));
	}

	private static boolean isTable(BlockState state) {
		return state.getBlock() instanceof EnchantingTableBlock;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP)
			state = state.setValue(ASSISTED, isTable(neighborState));
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	private InteractionResult open(Level level, BlockPos pos, Player player) {
		if (player.isShiftKeyDown())
			return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityAutoEnchanter te))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer sp && te.isPlayerAccessible(player))
			sp.openMenu(te, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityAutoEnchanter te) {
			for (int slot = 0; slot < te.getContainerSize(); slot++) {
				ItemStack stack = te.getItem(slot);
				if (!stack.isEmpty()) drops.add(stack.copy());
			}
		}
		return drops;
	}

}
