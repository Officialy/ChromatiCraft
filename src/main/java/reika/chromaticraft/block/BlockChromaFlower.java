package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a {@code BlockChromaPlantTile} metadata 0, the Chromatic Flower: no collision, a full-block selection box, and
 * it stands only on dirt or grass (1.7.10 {@code ReikaPlantHelper.FLOWER}) where the light is at least 8 or it sees
 * the sky, breaking to its item otherwise. {@code ChromaFlowerRenderer} draws it.
 */
public final class BlockChromaFlower extends BlockChromaticTile {

	public BlockChromaFlower(Properties properties) {
		super(properties, ChromaTiles.CHROMAFLOWER);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.below());
		// 1.7.10 isDirtType: dirt (every metadata, so coarse dirt and podzol too) or grass.
		boolean soil = below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.PODZOL)
				|| below.is(Blocks.GRASS_BLOCK);
		boolean light = world.getRawBrightness(pos, 0) >= 8 || world.canSeeSky(pos);
		return soil && light;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (!this.canSurvive(state, world, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, world, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

}
