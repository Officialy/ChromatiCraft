package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;

/** Collision and outline for the V33a Energy Focal Point's Techne body and four arms. */
public final class BlockPylonTurboCharger extends BlockChromaticTile {

	private static final VoxelShape BODY = Shapes.or(
			box(5, 0, 5, 11, 3, 11),
			box(6, 3, 6, 10, 5, 10),
			box(6.5, 5, 6.5, 9.5, 6.2, 9.5),
			box(7, 6, 7, 9, 8, 9),
			box(3, 0, 7, 5, 2, 9),
			box(1, 0, 7, 3, 1, 9),
			box(11, 0, 7, 13, 2, 9),
			box(13, 0, 7, 15, 1, 9),
			box(7, 0, 3, 9, 2, 5),
			box(7, 0, 1, 9, 1, 3),
			box(7, 0, 11, 9, 2, 13),
			box(7, 0, 13, 9, 1, 15));

	public BlockPylonTurboCharger(Properties properties) {
		super(properties, ChromaTiles.PYLONTURBO);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return BODY;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return BODY;
	}
}
