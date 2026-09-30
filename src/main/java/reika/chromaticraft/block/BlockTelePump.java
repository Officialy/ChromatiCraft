package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;

/**
 * V33a TELEPUMP on {@code TILEENTITY} (rock, hardness 5): right-clicking opens its GUI, but not while it is still
 * scanning ({@code isPlayerAccessible}), and only for a player it lets use it.
 */
public final class BlockTelePump extends BlockChromaticTile {

	public BlockTelePump(Properties props) {
		super(props, ChromaTiles.TELEPUMP);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileEntityTeleportationPump te))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer sp && te.isPlayerAccessible(player)
				&& (!te.onlyAllowOwnersToUse() || te.isOwnedByPlayer(player)))
			sp.openMenu(te, pos);
		return InteractionResult.SUCCESS;
	}

}
