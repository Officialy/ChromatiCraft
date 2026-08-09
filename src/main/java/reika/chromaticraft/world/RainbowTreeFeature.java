package reika.chromaticraft.world;

import java.util.List;
import java.util.stream.StreamSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a small Rainbow Tree silhouette.
 *
 * <p>The generic blob-tree configuration previously used here changed both defining properties: it
 * sampled a new tagged log for every trunk block and replaced the source's rising/falling diamond
 * crown with vanilla blob foliage. This feature chooses one overworld log for the whole tree, then
 * reproduces {@code tryGenerateSmallRainbowTree(..., 1)} layer for layer.
 */
public final class RainbowTreeFeature extends Feature<NoneFeatureConfiguration> {

	public RainbowTreeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		BlockState log = chooseLog(random);
		BlockState leaves = ChromaBlocks.RAINBOW_LEAVES.get().defaultBlockState();
		int lowerCrown = 2 + random.nextInt(3);
		int height = lowerCrown + Mth.ceil(5 + random.nextInt(7));

		// V33a shortens the tree at the first occupied trunk cell, then keeps trimming while the
		// four-block cap clearance is obstructed. A tree of height three or less is rejected.
		for (int i = 1; i <= height; i++) {
			if (!world.getBlockState(origin.above(i)).isAir()) {
				height = i;
				break;
			}
		}
		while (height > 2 && !world.getBlockState(origin.above(height + 4)).isAir())
			height--;
		if (height <= 3)
			return false;
		lowerCrown = Math.min(lowerCrown, height / 2);

		int width = 0;
		for (int layer = 0; layer < height; layer++) {
			BlockPos core = origin.above(layer);
			// The V33a routine writes the trunk unconditionally. Site/soil selection belongs to the
			// placed-feature/caller layer; adding a second gate here makes valid source sites fail.
			world.setBlock(core, log, 2);
			if (layer < lowerCrown)
				continue;

			boolean canGrow = width < 5 && height - layer - 1 > width;
			boolean canShrink = width > 1;
			boolean grow = random.nextBoolean() && canGrow || !canShrink;
			width += grow ? 1 : -1;
			for (int x = -width; x <= width; x++) {
				for (int z = -width; z <= width; z++) {
					if ((x != 0 || z != 0) && Math.abs(x) + Math.abs(z) <= width + 1)
						placeLeaf(world, core.offset(x, 0, z), leaves);
				}
			}
		}

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (x == 0 || z == 0)
					placeLeaf(world, origin.offset(x, height, z), leaves);
			}
		}
		placeLeaf(world, origin.above(height + 1), leaves);
		return true;
	}

	private static BlockState chooseLog(RandomSource random) {
		List<BlockState> logs = StreamSupport.stream(
				BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.OVERWORLD_NATURAL_LOGS).spliterator(), false)
				.map(Holder::value)
				.map(Block::defaultBlockState)
				.toList();
		return logs.isEmpty() ? Blocks.OAK_LOG.defaultBlockState() : logs.get(random.nextInt(logs.size()));
	}

	private static void placeLeaf(WorldGenLevel world, BlockPos pos, BlockState leaves) {
		if (canReplace(world, pos))
			world.setBlock(pos, leaves, 2);
	}

	private static boolean canReplace(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES);
	}
}
