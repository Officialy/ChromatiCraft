package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;

import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;

/**
 * V33a's Dimension Core block.
 *
 * <p>The block itself is deliberately thin: everything that makes a core a core — its colour, which
 * structure it belongs to, whether it is sealed — lives on the block entity, because upstream's single
 * block carried all sixteen colours through its tile rather than through metadata.
 *
 * <p>{@link #getDestroyProgress} is the seal. A core inside an unsolved puzzle structure returns zero,
 * which is how vanilla expresses "this cannot be mined at all" without a special case at every break
 * site; the same answer covers explosions and other mods' miners, which a break event alone would not.
 */
public class BlockDimensionCore extends BaseEntityBlock {

	public static final MapCodec<BlockDimensionCore> CODEC = simpleCodec(BlockDimensionCore::new);

	public BlockDimensionCore(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityDimensionCore(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// Drawn by its block entity renderer, as upstream draws it.
		return RenderShape.INVISIBLE;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		// Both halves are required: updateEntity() runs the base lifecycle -- first tick, sync,
		// callbacks -- and updateEntity(level, pos) is this tile's own body.
		return (world, pos, blockState, be) -> {
			if (be instanceof TileEntityDimensionCore core) {
				core.updateEntity();
				core.updateEntity(world, pos);
			}
		};
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof TileEntityDimensionCore core && !core.isBreakable(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}
}
