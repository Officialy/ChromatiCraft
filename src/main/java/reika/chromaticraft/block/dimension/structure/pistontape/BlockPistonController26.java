package reika.chromaticraft.block.dimension.structure.pistontape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.TileEntityPistonController;

/**
 * V33a {@code BlockPistonController} (PISTONCONTROL, light 12). {@link #TYPE} is its metadata: 0 playback
 * (front: {@code musicmemory_front}), 1 fire ({@code piston_control_fire}), 2 step
 * ({@code piston_control_step}), 3 display (plain); every other face is {@code piston_control}. Only a click on
 * the front face does anything.
 */
public final class BlockPistonController26 extends Block implements EntityBlock {

	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 3);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public BlockPistonController26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, 0).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE, FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityPistonController(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(TYPE) != 0 || type != ChromaBlockEntities.PISTON_CONTROLLER.get())
			return null;
		return (world, pos, current, entity) -> TileEntityPistonController.serverTick(world, pos, current,
				(TileEntityPistonController)entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && hit.getDirection() == state.getValue(FACING)
				&& level.getBlockEntity(pos) instanceof TileEntityPistonController te)
			te.onRightClick();
		return InteractionResult.SUCCESS;
	}
}
