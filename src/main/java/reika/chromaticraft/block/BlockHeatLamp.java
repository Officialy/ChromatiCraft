package reika.chromaticraft.block;

import com.mojang.serialization.MapCodec;

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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.TileEntityHeatLamp;

/** V33a's six-way attachable thermal lamp; hot and cold are separate modern block identities. */
public final class BlockHeatLamp extends Block implements EntityBlock {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	private static final VoxelShape DOWN = box(4, 12, 4, 12, 16, 12);
	private static final VoxelShape UP = box(4, 0, 4, 12, 4, 12);
	private static final VoxelShape NORTH = box(4, 4, 12, 12, 12, 16);
	private static final VoxelShape SOUTH = box(4, 4, 0, 12, 12, 4);
	private static final VoxelShape WEST = box(12, 4, 4, 16, 12, 12);
	private static final VoxelShape EAST = box(0, 4, 4, 4, 12, 12);

	private final boolean cold;
	private final MapCodec<BlockHeatLamp> codec = MapCodec.unit(this);

	public BlockHeatLamp(BlockBehaviour.Properties properties, boolean cold) {
		super(properties);
		this.cold = cold;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override public MapCodec<? extends BlockHeatLamp> codec() { return codec; }
	public boolean isCold() { return cold; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos support = pos.relative(facing.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		return state.canSurvive(level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case DOWN -> DOWN;
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityHeatLamp(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || type != ChromaBlockEntities.HEAT_LAMP.get()) return null;
		return (tickLevel, pos, tickState, entity) ->
				TileEntityHeatLamp.serverTick(tickLevel, pos, tickState, (TileEntityHeatLamp)entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer server
				&& level.getBlockEntity(pos) instanceof TileEntityHeatLamp lamp) {
			server.openMenu(lamp, pos);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
