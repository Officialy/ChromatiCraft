package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;

/**
 * V33a's Aura Point: what the monument becomes when its ritual completes, and thereafter a standing
 * area effect keyed to the player who made it.
 *
 * <p>Drawn entirely by its block entity, like the other locus points, so the world model exists only to
 * name a particle sprite.
 */
public class BlockAuraPoint extends BaseEntityBlock {

	public static final MapCodec<BlockAuraPoint> CODEC = simpleCodec(BlockAuraPoint::new);

	public BlockAuraPoint(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityAuraPoint(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		// Both halves: the base lifecycle, then this tile's own body.
		return (world, pos, blockState, be) -> {
			if (be instanceof TileEntityAuraPoint point) {
				point.updateEntity();
				point.updateEntity(world, pos);
			}
		};
	}
}
