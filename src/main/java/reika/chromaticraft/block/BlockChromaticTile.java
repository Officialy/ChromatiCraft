package reika.chromaticraft.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.base.BlockTEBase;

/**
 * Generic block backing one {@link ChromaTiles} tile (the 26.2 one-block-per-BlockEntity model,
 * mirroring ReactorCraft). Creates the tile's BlockEntity and drives its server tick.
 */
public class BlockChromaticTile extends BlockTEBase {

	private final ChromaTiles tile;

	public BlockChromaticTile(Properties props, ChromaTiles tile) {
		super(props);
		this.tile = tile;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return tile.createBlockEntity(pos, state);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide())
			return null;
		return (lvl, pos, st, be) -> {
			if (be instanceof BlockEntityBase base) {
				base.updateEntity();
				base.updateEntity(lvl, pos);
			}
		};
	}
}
