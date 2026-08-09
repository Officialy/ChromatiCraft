package reika.chromaticraft.block.decoration;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.tileentity.TileEntityDataNode;

/**
 * V33a Meta-Alloy plant. The former low three metadata bits are the attachment direction and bit 3
 * is represented by the explicit {@link #POD} property. A new plant starts leaf-only, grows its
 * luminous pod on the original one-in-100 random tick, and a left click harvests that pod without
 * removing the plant.
 */
public final class BlockMetaAlloyLamp extends Block {

	public static final int COLOR1 = 0xA1E56A;
	public static final int COLOR2 = 0x75DAFF;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POD = BooleanProperty.create("pod");
	private static final VoxelShape HALF = box(0, 0, 0, 16, 8, 16);
	private static final MapCodec<BlockMetaAlloyLamp> CODEC = simpleCodec(BlockMetaAlloyLamp::new);

	public BlockMetaAlloyLamp(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(POD, false));
	}

	@Override
	protected MapCodec<? extends BlockMetaAlloyLamp> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POD);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getClickedFace().getOpposite();
		BlockState state = this.defaultBlockState().setValue(FACING, facing);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction direction = state.getValue(FACING);
		BlockPos support = pos.relative(direction);
		BlockState supporting = level.getBlockState(support);
		return supporting.isFaceSturdy(level, support, direction.getOpposite())
				&& !supporting.is(Blocks.GLASS) && !supporting.is(Blocks.ICE);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POD) && random.nextInt(100) == 0)
			level.setBlock(pos, state.setValue(POD, true), Block.UPDATE_ALL);
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (!level.isClientSide() && state.getValue(POD)) {
			level.setBlock(pos, state.setValue(POD, false), Block.UPDATE_ALL);
			popResource(level, pos, new ItemStack(this));
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide())
			TileEntityDataNode.removeMetaAlloy(level, pos);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(POD) ? super.getShape(state, level, pos, context) : HALF;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.getShape(state, level, pos, context);
	}
}
