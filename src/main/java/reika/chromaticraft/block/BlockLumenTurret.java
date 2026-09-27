package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;

/** V33a TURRET, a {@code BlockModelledChromaTile} entry: its bounds are 0.25-0.75 across and 0-0.625 up, drawn by its renderer. */
public final class BlockLumenTurret extends BlockChromaticTile {

	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 10, 12);

	public BlockLumenTurret(Properties properties) {
		super(properties, ChromaTiles.TURRET);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

}
