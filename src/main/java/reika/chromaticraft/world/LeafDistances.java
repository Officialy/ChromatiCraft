package reika.chromaticraft.world;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Gives feature-placed leaves their real distance to a log.
 *
 * <p>A 26.2 leaf decays when its {@code DISTANCE} is 7, and its default state is 7. In a live level the
 * neighbour updates of placing a tree fix that; during world generation there are none, which is why
 * vanilla's own {@code TreeFeature} runs a distance pass after every tree. A custom tree feature has to
 * do the same or its leaves rot away on their first random tick — which 1.7.10 never had to worry
 * about, because its leaves checked for a log only when they decayed. This is that pass: a breadth-first
 * walk out from the logs through connected leaves, lowering each leaf's distance, stopping at 7.
 */
public final class LeafDistances {

	private static final int MAX = 7;

	private LeafDistances() {}

	/** Walks out from the given logs. */
	public static void update(WorldGenLevel level, Collection<BlockPos> logs) {
		Map<BlockPos, Integer> best = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos log : logs) {
			BlockPos p = log.immutable();
			best.put(p, 0);
			queue.add(p);
		}
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			int next = best.get(pos) + 1;
			if (next >= MAX)
				continue;
			for (Direction dir : Direction.values()) {
				BlockPos side = pos.relative(dir);
				Integer known = best.get(side);
				if (known != null && known <= next)
					continue;
				BlockState state = level.getBlockState(side);
				OptionalInt current = LeavesBlock.getOptionalDistanceAt(state);
				if (current.isEmpty() || !state.hasProperty(BlockStateProperties.DISTANCE))
					continue;
				best.put(side, next);
				if (current.getAsInt() > next)
					level.setBlock(side, state.setValue(BlockStateProperties.DISTANCE, next),
							Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				queue.add(side);
			}
		}
	}

	/** Walks out from every log inside the box, for trees placed from a template. */
	public static void updateBox(WorldGenLevel level, BlockPos min, BlockPos max) {
		java.util.List<BlockPos> logs = new java.util.ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max))
			if (level.getBlockState(pos).is(BlockTags.LOGS))
				logs.add(pos.immutable());
		update(level, logs);
	}
}
