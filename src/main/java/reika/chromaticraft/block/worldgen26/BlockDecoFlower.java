package reika.chromaticraft.block.worldgen26;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaDecoFlowers;

/**
 * V33a {@code BlockDecoFlower}, as one registered block per flower.
 *
 * <p>Support comes from {@code Flowers.canPlantAt}: Luma Lotus stands on dirt or grass, Ether Berries
 * hang beneath fully-grown jungle leaves, Void Reeds stand on another reed or on sugarcane-legal
 * ground, and Aura Ivy clings to any horizontally adjacent solid rock face.
 */
public class BlockDecoFlower extends Block {

	private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);
	private static final VoxelShape[] IVY_SHAPES = new VoxelShape[16];
	public static final Direction[] IVY_FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	static {
		VoxelShape[] faces = {box(0, 0, 0, 16, 16, 1), box(15, 0, 0, 16, 16, 16),
				box(0, 0, 15, 16, 16, 16), box(0, 0, 0, 1, 16, 16)};
		for (int mask = 0; mask < IVY_SHAPES.length; mask++) {
			VoxelShape shape = Shapes.empty();
			for (int i = 0; i < faces.length; i++)
				if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, faces[i]);
			IVY_SHAPES[mask] = shape;
		}
	}

	private final ChromaDecoFlowers flower;


	public BlockDecoFlower(BlockBehaviour.Properties properties, ChromaDecoFlowers flower) {
		super(properties);
		this.flower = flower;
	}


	public ChromaDecoFlowers getFlower() {
		return flower;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return flower.siting() == ChromaDecoFlowers.Siting.IVY
				? IVY_SHAPES[ivyFaces(level, pos)] : SHAPE;
	}

	/** Wall faces, inherited down a hanging run. No saved properties, so existing ivy updates too. */
	public static int ivyFaces(BlockGetter level, BlockPos pos) {
		int mask = 0;
		BlockPos.MutableBlockPos cursor = pos.mutable();
		do {
			for (int i = 0; i < IVY_FACES.length; i++)
				if (level.getBlockState(cursor.relative(IVY_FACES[i])).isSolidRender()) mask |= 1 << i;
			cursor.move(Direction.UP);
			BlockState above = level.getBlockState(cursor);
			if (!(above.getBlock() instanceof BlockDecoFlower block)
					|| block.flower.siting() != ChromaDecoFlowers.Siting.IVY) break;
		} while (cursor.getY() < level.getMaxY() && mask != 15);
		return mask;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** V33a canPlantAt. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return switch (flower.siting()) {
			case GROUND -> {
				BlockState below = level.getBlockState(pos.below());
				yield below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK);
			}
			// V33a requires leaves at metadata%4 == 3, which is a fully-grown jungle leaf.
			case JUNGLE_LEAVES -> level.getBlockState(pos.above()).is(Blocks.JUNGLE_LEAVES);
			case REED -> {
				BlockState below = level.getBlockState(pos.below());
				if (below.is(this))
					yield true;
				boolean ground = below.is(BlockTags.DIRT) || below.is(BlockTags.SAND);
				if (!ground)
					yield false;
				// ReikaPlantHelper.SUGARCANE: ground with water directly beside it.
				for (Direction dir : Direction.Plane.HORIZONTAL)
					if (level.getFluidState(pos.below().relative(dir)).is(Fluids.WATER))
						yield true;
				yield false;
			}
			case IVY -> ivyFaces(level, pos) != 0;
		};
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
			BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}
}
