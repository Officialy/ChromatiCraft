package reika.chromaticraft.block.dimension.structure.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.tileentity.TileEntityLaserController;

/** The V33a room data tile immediately above its light-panel switch. */
public final class BlockLaserController extends Block implements EntityBlock {
	public BlockLaserController(BlockBehaviour.Properties properties) { super(properties); }

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLaserController(pos, state);
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityLaserController controller)
			controller.toggle();
		return InteractionResult.SUCCESS;
	}
}
