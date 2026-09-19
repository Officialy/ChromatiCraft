package reika.chromaticraft.block.dimension.structure.gol;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.tileentity.TileEntityGOLController;

/** V33a play/stop control for the Cellular Automata puzzle. */
public final class BlockGOLController extends Block implements EntityBlock {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private final MapCodec<BlockGOLController> codec = MapCodec.unit(this);

	public BlockGOLController(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override public MapCodec<? extends BlockGOLController> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityGOLController(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || type != ChromaBlockEntities.GOL_CONTROLLER.get()) return null;
		return (tickLevel, pos, tickState, entity) ->
				TileEntityGOLController.serverTick(tickLevel, pos, tickState,
						(TileEntityGOLController)entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityGOLController controller)
			controller.toggleSimulation();
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityGOLController controller)
			controller.toggleSimulation();
		super.attack(state, level, pos, player);
	}
}
