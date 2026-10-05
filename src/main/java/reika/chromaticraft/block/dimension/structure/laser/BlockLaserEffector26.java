package reika.chromaticraft.block.dimension.structure.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.TileEntityLaserEffector;

/** The twelve V33a Chromatic Beams devices, with type and heading visible in block state. */
public final class BlockLaserEffector26 extends Block implements EntityBlock {

	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 11);
	public static final IntegerProperty DIRECTION = IntegerProperty.create("direction", 0, 7);
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	public static final BooleanProperty FULL_BLOCK = BooleanProperty.create("full_block");
	/** V33a {@code LaserEffectTile.isRotateable()}, which the renderer shows as a chiseled plinth. */
	public static final BooleanProperty ROTATEABLE = BooleanProperty.create("rotateable");
	private static final VoxelShape HALF = Block.box(0, 0, 0, 16, 8, 16);

	public BlockLaserEffector26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, 0).setValue(DIRECTION, 0).setValue(COLOR, 7)
				.setValue(LIT, false).setValue(FULL_BLOCK, false).setValue(ROTATEABLE, false));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE, DIRECTION, COLOR, LIT, FULL_BLOCK, ROTATEABLE);
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return state.getValue(FULL_BLOCK) ? Shapes.block() : HALF;
	}

	@Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext
				&& !(entityContext.getEntity() instanceof Player) ? Shapes.empty()
				: getShape(state, level, pos, context);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLaserEffector(pos, state);
	}

	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
			BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ChromaBlockEntities.LASER_EFFECTOR.get()) return null;
		return (world, pos, current, entity) -> TileEntityLaserEffector.serverTick(
				world, pos, current, (TileEntityLaserEffector)entity);
	}

	/** Forwards the target-hit FX event to the tile, as {@code BaseEntityBlock} does. */
	@Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity tile = level.getBlockEntity(pos);
		return tile != null && tile.triggerEvent(id, param);
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityLaserEffector effector)
			effector.rotate(player.isShiftKeyDown());
		return InteractionResult.SUCCESS;
	}
}
