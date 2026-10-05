package reika.chromaticraft.block.dimension.structure.water;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.TileEntityEverFluid;

/**
 * V33a {@code BlockEverFluid}: Channeled Flow's fluid. It is not a vanilla liquid; each cell is a block with
 * a tile naming its source. On every tick a cell that is no longer connected (face to face, through other
 * cells) to its source vanishes, which is how turning a lock drains everything downstream of it. A
 * connected cell falls straight down when it can; when it rests on a lock it drops through it as a new
 * source one floor lower; otherwise it spreads sideways into air.
 *
 * <p>Metadata 0 (the source) is {@link #SOURCE}. V33a's tick rate starts at 4 and becomes 3 the first time
 * any cell flows, as it was a field mutated on the block singleton. Its {@code Material.water} gave it
 * vanilla's swimming physics; this block has no fluid state, so entities pass through it without
 * swimming, as through any other non-colliding block. That difference is deliberate: a vanilla water
 * fluid state would also spread and draw vanilla water over the V33a geometry.
 */
public final class BlockEverFluid26 extends Block implements EntityBlock {

	public static final BooleanProperty SOURCE = BooleanProperty.create("source");

	private int tickRate = 4;

	public BlockEverFluid26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SOURCE, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SOURCE);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityEverFluid(pos, state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/**
	 * V33a {@code canCollideCheck(meta, fullHit)} answered true only for liquid-hitting ray traces on a source,
	 * so ordinary clicks always passed through to the lock below.
	 */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
		return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.EMPTY.defaultFluidState();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (oldState.is(this))
			return;
		level.scheduleTick(pos, this, tickRate);
		// "liquid.water" is the vanilla water ambience.
		RandomSource rand = level.getRandom();
		level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS,
				rand.nextFloat() * 0.25F + 0.75F, rand.nextFloat() + 0.5F);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			@Nullable Orientation orientation, boolean movedByPiston) {
		level.scheduleTick(pos, this, tickRate);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		this.tick(state, level, pos, random);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityEverFluid te) || te.sourceLocation() == null) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			return;
		}
		BlockPos source = te.sourceLocation();
		if (!connectedCells(level, pos).contains(source)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockState(pos.below()).is(ChromaBlocks.ROTATING_LOCK.get())
					&& level.getBlockState(pos.below(2)).is(this))
				level.setBlock(pos.below(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		else if (isSourceBlock(level, pos) || isSourceBlock(level, source)) {
			trySpread(level, pos, te);
		}
		else if (!level.getBlockState(source).is(this)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** DragonAPI {@code BlockArray.recursiveAdd}: every cell face-connected to {@code start}. */
	private Set<BlockPos> connectedCells(Level level, BlockPos start) {
		Set<BlockPos> found = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		found.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Direction dir : Direction.values()) {
				BlockPos next = at.relative(dir);
				if (!found.contains(next) && level.isLoaded(next) && level.getBlockState(next).is(this)) {
					found.add(next);
					queue.add(next);
				}
			}
		}
		return found;
	}

	private void trySpread(Level level, BlockPos pos, TileEntityEverFluid te) {
		if (flowInto(level, pos, te, Direction.DOWN)) {
			// do not flow outwards
		}
		else if (!level.getBlockState(pos.below()).is(this)) {
			for (Direction dir : Direction.Plane.HORIZONTAL)
				flowInto(level, pos, te, dir);
		}
	}

	public boolean isSourceBlock(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(this) && state.getValue(SOURCE);
	}

	private boolean flowInto(Level level, BlockPos pos, TileEntityEverFluid src, Direction dir) {
		BlockPos to = pos.relative(dir);
		tickRate = 3;
		boolean placeSource = false;
		if (to.getY() > level.getMinY() && pos.getY() > level.getMinY() && dir == Direction.DOWN
				&& level.getBlockState(pos.below()).is(ChromaBlocks.ROTATING_LOCK.get())) {
			to = to.below();
			placeSource = true;
		}
		if (canFlowInto(level, to)) {
			if (placeSource) {
				placeSource(level, to);
			}
			else {
				level.setBlock(to, defaultBlockState().setValue(SOURCE, false), Block.UPDATE_ALL);
			}
			if (level.getBlockEntity(to) instanceof TileEntityEverFluid te) {
				if (!placeSource)
					te.setSource(src.sourceLocation());
				te.setFloor(placeSource ? src.floor() + 1 : src.floor());
			}
			return true;
		}
		return isBlockingBlock(level, to);
	}

	private static boolean isBlockingBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !(state.getBlock() instanceof BlockStructureShield) && !state.is(ChromaBlocks.ROTATING_LOCK.get());
	}

	private boolean canFlowInto(LevelAccessor level, BlockPos pos) {
		if (pos.getY() < level.getMinY())
			return false;
		BlockState state = level.getBlockState(pos);
		if (state.isAir())
			return true;
		if (state.is(this))
			return false;
		return !(state.isSolid() || !state.getFluidState().isEmpty()
				|| state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock);
	}

	/** V33a {@code placeSource}: a source cell whose source is itself. */
	public static void placeSource(LevelAccessor level, BlockPos pos) {
		BlockState source = ChromaBlocks.EVER_FLUID.get().defaultBlockState();
		if (!level.getBlockState(pos).is(source.getBlock()))
			level.setBlock(pos, source, Block.UPDATE_ALL);
		// V33a called onBlockAdded explicitly; a worldgen region never runs onPlace, so schedule here too.
		level.scheduleTick(pos, source.getBlock(), 4);
		if (level.getBlockEntity(pos) instanceof TileEntityEverFluid te)
			te.setSource(pos);
	}
}
