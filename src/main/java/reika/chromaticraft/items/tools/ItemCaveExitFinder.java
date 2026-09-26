/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import java.util.ArrayList;
import java.util.LinkedList;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.AbstractSearch.PropagationCondition;
import reika.dragonapi.instantiable.data.blockstruct.AbstractSearch.TerminationCondition;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.blockstruct.BreadthFirstSearch;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code ItemCaveExitFinder}, the Subterranean Route Finder. Used underground it finds the shortest open route to
 * a block that sees the sky and marks it with trail waypoints every fourth block (ending at the exit); sneaking, it
 * instead finds the way down to the deepest reachable point within 400 blocks, never climbing above where it started.
 * Waypoints only go into air, never over liquids or leaves.
 *
 * <p>Open space is air, liquids, soft blocks, leaves, iron bars, doors (V33a's one wooden door is every wooden door
 * here), and existing trail and Hover blocks. The search runs to completion in the use tick, as it did in V33a.
 */
public class ItemCaveExitFinder extends Item {

	private static final TerminationCondition skyFinder = (world, pos) -> world.canSeeSky(pos);

	private static final PropagationCondition pathFinder = (world, pos, from) -> {
		BlockState b = world.getBlockState(pos);
		return b.isAir() || b.is(ChromaBlocks.HOVER.get()) || b.is(BlockTags.WOODEN_DOORS) || b.is(Blocks.IRON_DOOR)
				|| b.is(Blocks.IRON_BARS) || b.is(ChromaBlocks.CHROMA_TRAIL.get()) || ReikaBlockHelper.isLiquid(b)
				|| ReikaWorldHelper.softBlocks(world, pos) || ReikaBlockHelper.isLeaf(world, pos);
	};

	private static final class DownwardsFinder implements PropagationCondition {

		private final int startY;

		private DownwardsFinder(int y) {
			startY = y;
		}

		@Override
		public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
			return pathFinder.isValidLocation(world, pos, from) && pos.getY() <= startY;
		}

	}

	private static final class LowYFinder implements TerminationCondition {

		private final int endY;

		private LowYFinder(int y) {
			endY = y;
		}

		@Override
		public boolean isValidTerminus(Level world, BlockPos pos) {
			return pos.getY() == endY;
		}

	}

	public ItemCaveExitFinder(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		if (world.isClientSide())
			return InteractionResult.SUCCESS;
		BlockPos start = ep.blockPosition();
		int x = start.getX();
		int y = start.getY();
		int z = start.getZ();
		boolean deep = ep.isShiftKeyDown();
		if (!deep && skyFinder.isValidTerminus(world, start))
			return InteractionResult.SUCCESS;
		PropagationCondition f = deep ? new DownwardsFinder(y) : pathFinder;
		TerminationCondition t = skyFinder;
		BlockBox bounds = null;
		if (deep) {
			bounds = new BlockBox(x, y, z, x, y, z).expand(400, 256, 400);
			BlockArray b = new BlockArray();
			b.recursiveAddCallbackWithBounds(world, x, y, z, bounds.minX, world.getMinY(), bounds.minZ, bounds.maxX,
					world.getMaxY(), bounds.maxZ, f);
			t = new LowYFinder(b.getMinY());
		}
		BreadthFirstSearch s = new BreadthFirstSearch(start);
		if (bounds != null)
			s.limit = bounds;
		s.complete(world, f, t);
		LinkedList<BlockPos> li = new LinkedList(s.getResult());
		if (!li.isEmpty()) {
			int n = 0;
			ArrayList<BlockPos> li2 = new ArrayList();
			for (BlockPos c : li) {
				if (n%4 == 0 && !c.equals(li.getLast())) {
					li2.add(c);
				}
				n++;
			}
			li2.add(li.getLast());
			for (int i = 0; i < li2.size(); i++) {
				BlockPos c = li2.get(i);
				BlockPos next = i < li2.size()-1 ? li2.get(i+1) : null;
				BlockPos prev = i > 0 ? li2.get(i-1) : null;
				if (world.getBlockState(c).isAir()) { //do not overwrite liquids, leaves, etc
					world.setBlock(c, ChromaBlocks.CHROMA_TRAIL.get().defaultBlockState(), 3);
					if (world.getBlockEntity(c) instanceof TileChromaTrail te)
						te.setData(prev, next);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

}
