package reika.chromaticraft.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;

/** V33a's connected, UUID-keyed Chroma Door, expressed with explicit 26.2 state flags. */
public final class BlockChromaDoor extends Block implements EntityBlock {

	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	public static final BooleanProperty DAMAGE = BooleanProperty.create("damage");
	public static final BooleanProperty CONSUME_KEY = BooleanProperty.create("consume_key");
	public static final BooleanProperty STAY_OPEN = BooleanProperty.create("stay_open");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
	private final MapCodec<BlockChromaDoor> codec = MapCodec.unit(this);

	public BlockChromaDoor(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(DAMAGE, false)
				.setValue(CONSUME_KEY, false).setValue(STAY_OPEN, false)
				.setValue(UP, false).setValue(DOWN, false).setValue(NORTH, false)
				.setValue(SOUTH, false).setValue(EAST, false).setValue(WEST, false));
	}

	@Override public MapCodec<? extends BlockChromaDoor> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN, DAMAGE, CONSUME_KEY, STAY_OPEN, UP, DOWN, NORTH, SOUTH, EAST, WEST);
	}

	public static BlockState state(boolean open, boolean damage, boolean consume, boolean stay) {
		return reika.chromaticraft.registry.ChromaBlocks.CHROMA_DOOR.get().defaultBlockState()
				.setValue(OPEN, open).setValue(DAMAGE, damage)
				.setValue(CONSUME_KEY, consume).setValue(STAY_OPEN, stay);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityChromaDoor(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || type != ChromaBlockEntities.CHROMA_DOOR.get()) return null;
		return (tickLevel, pos, tickState, entity) ->
				TileEntityChromaDoor.serverTick(tickLevel, pos, tickState, (TileEntityChromaDoor)entity);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		return state.setValue(property(direction), connects(level, neighbourPos, direction.getOpposite()));
	}

	/** Resolves all six multipart arms after bulk/NBT placement has installed the whole component. */
	public static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction direction : Direction.values())
			state = state.setValue(property(direction), connects(level, pos.relative(direction), direction.getOpposite()));
		return state;
	}

	private static boolean connects(BlockGetter level, BlockPos pos, Direction face) {
		BlockState neighbour = level.getBlockState(pos);
		return neighbour.is(reika.chromaticraft.registry.ChromaBlocks.CHROMA_DOOR.get())
				|| neighbour.isFaceSturdy(level, pos, face);
	}

	private static BooleanProperty property(Direction direction) {
		return switch (direction) {
			case UP -> UP;
			case DOWN -> DOWN;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
		};
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
			ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityChromaDoor door)
			door.setPlacer(player.getUUID());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		if (state.getValue(UP)) shape = Shapes.or(shape, box(6, 10, 6, 10, 16, 10));
		if (state.getValue(DOWN)) shape = Shapes.or(shape, box(6, 0, 6, 10, 6, 10));
		if (state.getValue(EAST)) shape = Shapes.or(shape, box(10, 6, 6, 16, 10, 10));
		if (state.getValue(WEST)) shape = Shapes.or(shape, box(0, 6, 6, 6, 10, 10));
		if (state.getValue(SOUTH)) shape = Shapes.or(shape, box(6, 6, 10, 10, 10, 16));
		if (state.getValue(NORTH)) shape = Shapes.or(shape, box(6, 6, 0, 10, 10, 6));
		return shape;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, context);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof TileEntityChromaDoor door && !door.isOwner(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof TileEntityChromaDoor door)
			door.close();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean intersects) {
		if (!state.getValue(OPEN) && state.getValue(DAMAGE) && level instanceof ServerLevel server) {
			entity.hurtServer(server, server.damageSources().magic(), 5);
			double dx = entity.getX() - (pos.getX() + 0.5);
			double dz = entity.getZ() - (pos.getZ() + 0.5);
			double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
			entity.push(dx / len * 2, 0.03125, dz / len * 2);
			ChromaSounds.DISCHARGE.playSoundAtBlock(level, pos, 0.5F, 2F);
		}
		super.entityInside(state, level, pos, entity, effects, intersects);
	}
}
