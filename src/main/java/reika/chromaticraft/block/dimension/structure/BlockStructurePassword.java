package reika.chromaticraft.block.dimension.structure;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.tileentity.TileEntityStructurePassword;

/** V33a structure-password bypass terminal. The eight crystal slots live in its block entity. */
public final class BlockStructurePassword extends Block implements EntityBlock {

	private final MapCodec<BlockStructurePassword> codec = MapCodec.unit(this);

	public BlockStructurePassword(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override public MapCodec<? extends BlockStructurePassword> codec() { return codec; }

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityStructurePassword(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityStructurePassword password))
			return InteractionResult.PASS;
		if (!password.checkPassword(player) && player instanceof ServerPlayer server)
			server.openMenu(password, pos);
		return InteractionResult.SUCCESS;
	}
}
