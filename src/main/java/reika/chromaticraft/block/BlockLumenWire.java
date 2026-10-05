package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.TileEntityLumenWire;

/**
 * V33a LUMENWIRE, a {@code BlockModelledChromaTile} entry placed against a block face ({@code SidePlacedTile}): it
 * points out of the face it was placed on, has no collision (intangible) and a quarter-thick half-square hitbox against
 * its support, drops itself when that support can no longer hold it, and is a redstone source while active.
 */
public final class BlockLumenWire extends BlockChromaticTile {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public BlockLumenWire(Properties properties) {
		super(properties, ChromaTiles.LUMENWIRE);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getClickedFace());
		return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** V33a {@code checkLocationValidity}: the block behind must be solid on the face the wire sits on. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos support = pos.relative(facing.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == state.getValue(FACING).getOpposite() && !this.canSurvive(state, level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch(state.getValue(FACING).getOpposite()) {
			case DOWN -> Block.box(4, 0, 4, 12, 4, 12);
			case UP -> Block.box(4, 12, 4, 12, 16, 12);
			case EAST -> Block.box(12, 4, 4, 16, 12, 12);
			case WEST -> Block.box(0, 4, 4, 4, 12, 12);
			case NORTH -> Block.box(4, 4, 0, 12, 12, 4);
			case SOUTH -> Block.box(4, 4, 12, 12, 12, 16);
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof TileEntityLumenWire te ? te.getWeakPower(level, pos, direction) : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof TileEntityLumenWire te ? te.getStrongPower(level, pos, direction) : 0;
	}

}
