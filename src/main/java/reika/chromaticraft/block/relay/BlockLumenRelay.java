package reika.chromaticraft.block.relay;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a Lumen Relay. The sixteen elemental forms and the multichromic form are independent 26.2
 * registry identities; {@link #FACING} is only the support/visual orientation.
 */
public final class BlockLumenRelay extends BlockRelayBase {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	private static final VoxelShape WEST = box(2, 6, 6, 16, 10, 10);
	private static final VoxelShape EAST = box(0, 6, 6, 14, 10, 10);
	private static final VoxelShape NORTH = box(6, 6, 2, 10, 10, 16);
	private static final VoxelShape SOUTH = box(6, 6, 0, 10, 10, 14);
	private static final VoxelShape UP = box(6, 0, 6, 10, 14, 10);
	private static final VoxelShape DOWN = box(6, 2, 6, 10, 16, 10);

	private final @Nullable CrystalElement element;
	private final MapCodec<BlockLumenRelay> codec = MapCodec.unit(this);

	/** A null element is V33a metadata 16: the multichromic relay. */
	public BlockLumenRelay(BlockBehaviour.Properties properties, @Nullable CrystalElement element) {
		super(properties);
		this.element = element;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override public MapCodec<? extends BlockLumenRelay> codec() { return codec; }
	public boolean isMultichromic() { return element == null; }
	public @Nullable CrystalElement getElement() { return element; }

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
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
			ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof TileEntityLumenRelay relay)
			relay.setInput(state.getValue(FACING).getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case WEST -> WEST;
			case EAST -> EAST;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case UP -> UP;
			case DOWN -> DOWN;
		};
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (level.isClientSide())
			ChromaParticle.spawnLumenRelay(level, pos, state.getValue(FACING), element, random);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLumenRelay(pos, state);
	}

	public static final class TileEntityLumenRelay extends TileRelayBase {
		public TileEntityLumenRelay(BlockPos pos, BlockState state) {
			super(ChromaBlockEntities.LUMEN_RELAY.get(), pos, state);
		}

		@Override
		public boolean canTransmit(CrystalElement requested) {
			return getBlockState().getBlock() instanceof BlockLumenRelay relay
					&& (relay.isMultichromic() || relay.element == requested);
		}

		public boolean isMultichromic() {
			return getBlockState().getBlock() instanceof BlockLumenRelay relay
					&& relay.isMultichromic();
		}

		public CrystalElement getColor() {
			return getBlockState().getBlock() instanceof BlockLumenRelay relay && relay.element != null
					? relay.element : CrystalElement.WHITE;
		}
	}
}
