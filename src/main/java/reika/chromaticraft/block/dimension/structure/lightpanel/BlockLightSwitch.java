package reika.chromaticraft.block.dimension.structure.lightpanel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.tileentity.TileEntityLightSwitch;

/** V33a panel switch. Its target is persisted by the accompanying block entity. */
public final class BlockLightSwitch extends Block implements EntityBlock {

	public static final BooleanProperty UP = BooleanProperty.create("up");
	private final MapCodec<BlockLightSwitch> codec = MapCodec.unit(this);

	public BlockLightSwitch(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(UP, false));
	}

	@Override public MapCodec<? extends BlockLightSwitch> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UP);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLightSwitch(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		boolean up = !state.getValue(UP);
		level.setBlock(pos, state.setValue(UP, up), Block.UPDATE_ALL);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityLightSwitch panel)
			panel.sendState(player, up);
		level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS,
				1, up ? 0.875F : 0.75F);
		return InteractionResult.SUCCESS;
	}

	public static boolean isSwitchUp(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof BlockLightSwitch && state.getValue(UP);
	}
}
