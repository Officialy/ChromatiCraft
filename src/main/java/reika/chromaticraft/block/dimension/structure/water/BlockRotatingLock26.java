package reika.chromaticraft.block.dimension.structure.water;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.TileEntityRotatingLock;

/**
 * V33a {@code BlockRotatingLock} (WATERLOCK). Metadata 0 was the lock itself, a tile-bearing block drawn
 * with the animated {@code dimstruct/waterlock} texture under its turning model; metadata 1 was a "pad",
 * an invisible solid block that fills every closed channel cell and plugs a turning lock. The two are
 * content identities, so {@link #PAD} keeps them apart; only the lock has a tile.
 */
public final class BlockRotatingLock26 extends Block implements EntityBlock {

	public static final BooleanProperty PAD = BooleanProperty.create("pad");

	public BlockRotatingLock26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PAD, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PAD);
	}

	/** V33a drew the pad with the TRANSPARENT icon: present, solid and invisible. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(PAD) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PAD) ? null : new TileEntityRotatingLock(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(PAD) || type != ChromaBlockEntities.ROTATING_LOCK.get())
			return null;
		return (world, pos, current, entity) -> TileEntityRotatingLock.serverTick(world, pos, current,
				(TileEntityRotatingLock)entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && !state.getValue(PAD)
				&& level.getBlockEntity(pos) instanceof TileEntityRotatingLock lock)
			lock.startRotating(player.isShiftKeyDown());
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			@Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
		if (!state.getValue(PAD) && level.getBlockEntity(pos) instanceof TileEntityRotatingLock lock)
			lock.updateHasFluidState();
	}
}
