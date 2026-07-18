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

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.block.blockchromatrail.TileChromaTrail;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.abstractsearch.PropagationCondition;
import reika.dragonapi.instantiable.data.blockstruct.abstractsearch.TerminationCondition;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.blockstruct.BreadthFirstSearch;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.libraries.world.ReikaBlockHelper;
import reika.dragonapi.libraries.world.ReikaWorldHelper;


public class ItemCaveExitFinder extends ItemChromaTool {

	private static final TerminationCondition skyFinder = new TerminationCondition() {

		@Override
		public boolean isValidTerminus(World world, int x, int y, int z) {
			return world.canBlockSeeTheSky(x, y, z);
		}

	};

	private static final PropagationCondition pathFinder = new PropagationCondition() {

		@Override
		public boolean isValidLocation(World world, int x, int y, int z, Coordinate from) {
			Block b = world.getBlock(x, y, z);
			return b.isAir(world, x, y, z) || b == ChromaBlocks.HOVER.getBlockInstance() || b == Blocks.wooden_door || b == Blocks.iron_door || b == Blocks.iron_bars || b == ChromaBlocks.TRAIL.getBlockInstance() || ReikaBlockHelper.isLiquid(b) || ReikaWorldHelper.softBlocks(world, x, y, z) || ReikaBlockHelper.isLeaf(world, x, y, z);
		}

	};

	private static final class DownwardsFinder implements PropagationCondition {

		private final int startY;

		private DownwardsFinder(int y) {
			startY = y;
		}

		@Override
		public boolean isValidLocation(World world, int x, int y, int z, Coordinate from) {
			return pathFinder.isValidLocation(world, x, y, z, from) && y <= startY;
		}

	};

	private static final class LowYFinder implements TerminationCondition {

		private final int endY;

		private LowYFinder(int y) {
			endY = y;
		}

		@Override
		public boolean isValidTerminus(World world, int x, int y, int z) {
			return y == endY;
		}

	};

	public ItemCaveExitFinder(int index) {
		super(index);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep) {
		if (world.isRemote)
			return is;
		int x = MathHelper.floor_double(ep.posX);
		int y = MathHelper.floor_double(ep.posY);
		int z = MathHelper.floor_double(ep.posZ);
		boolean deep = ep.isSneaking();
		if (!deep && skyFinder.isValidTerminus(world, x, y, z))
			return is;
		PropagationCondition f = deep ? new DownwardsFinder(y) : pathFinder;
		TerminationCondition t = skyFinder;
		BlockBox bounds = null;
		if (deep) {
			bounds = new BlockBox(x, y, z, x, y, z).expand(400, 256, 400);
			BlockArray b = new BlockArray();
			b.recursiveAddCallbackWithBounds(world, x, y, z, bounds.minX, 0, bounds.minZ, bounds.maxX, 256, bounds.maxZ, f);
			t = new LowYFinder(b.getMinY());
		}
		BreadthFirstSearch s = new BreadthFirstSearch(x, y, z, f, t);
		if (bounds != null)
			s.limit = bounds;
		while (!s.tick(world)) {

		}
		LinkedList<Coordinate> li = s.getResult().getPath();
		if (!li.isEmpty()) {
			int n = 0;
			ArrayList<Coordinate> li2 = new ArrayList();
			for (Coordinate c : li) {
				if (n%4 == 0 && !c.equals(li.getLast())) {
					li2.add(c);
				}
				n++;
			}
			li2.add(li.getLast());
			for (int i = 0; i < li2.size(); i++) {
				Coordinate c = li2.get(i);
				Coordinate next = i < li2.size()-1 ? li2.get(i+1) : null;
				Coordinate prev = i > 0 ? li2.get(i-1) : null;
				if (c.getBlock(world).isAir(world, c.xCoord, c.yCoord, c.zCoord)) { //do not overwrite liquids, leaves, etc
					c.setBlock(world, ChromaBlocks.TRAIL.getBlockInstance());
					TileChromaTrail te = (TileChromaTrail)c.getTileEntity(world);
					if (te == null) {
						te = new TileChromaTrail();
						te.worldObj = world;
						te.xCoord = c.xCoord;
						te.yCoord = c.yCoord;
						te.zCoord = c.zCoord;
						world.setTileEntity(c.xCoord, c.yCoord, c.zCoord, te);
					}
					te.setData(prev, next);
					//ReikaJavaLibrary.pConsole(c);
				}
			}
		}
		return is;
	}

}
