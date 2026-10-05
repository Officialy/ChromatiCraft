/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;

import java.util.EnumMap;

/**
 * V33a FLUIDRELAY on {@code TILEMODELLED3}: the Fluid Link Anchor. It is placed against a face, which it then holds
 * (V33a {@code SidePlacedTile.placeOnSide}: facing = the clicked side's opposite, toward the tank). It needs that
 * neighbour to present a full face on the touching side ({@code checkLocationValidity}: a solid block whose bounds
 * reach that face), and when it no longer does the anchor pops off as an item ({@code drop}). Its hitbox is V33a's
 * {@code getHitbox}: the block contracted by 5/16 on every side, then pushed 5/16 toward the tank.
 */
public final class BlockFluidRelay extends BlockChromaticTile {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
	static {
		double d = 5/16D;
		for (Direction dir : Direction.values()) {
			double ox = d*dir.getStepX();
			double oy = d*dir.getStepY();
			double oz = d*dir.getStepZ();
			SHAPES.put(dir, Shapes.box(d+ox, d+oy, d+oz, 1-d+ox, 1-d+oy, 1-d+oz));
		}
	}

	public BlockFluidRelay(Properties properties) {
		super(properties, ChromaTiles.FLUIDRELAY);
		this.registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction dir = state.getValue(FACING);
		BlockPos support = pos.relative(dir);
		return level.getBlockState(support).isFaceSturdy(level, support, dir.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		// V33a drop(): the anchor comes off as an item once its support is gone.
		if (direction == state.getValue(FACING) && !state.canSurvive(level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** V33a {@code BlockChromaTile.onBlockActivated}: the tile's GUI. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityFluidRelay relay))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(relay, pos);
		return InteractionResult.SUCCESS;
	}

}
