package reika.chromaticraft.base;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.abstractsearch.FoundPath;
import reika.dragonapi.instantiable.data.blockstruct.abstractsearch.PropagationCondition;
import reika.dragonapi.instantiable.data.blockstruct.abstractsearch.TerminationCondition;
import reika.dragonapi.instantiable.data.blockstruct.BreadthFirstSearch;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.Coordinate;


public abstract class BlockProtectedByStructure extends Block {

	protected BlockProtectedByStructure(Material mat) {
		super(mat);
	}

	@Override
	public final float getPlayerRelativeBlockHardness(EntityPlayer ep, World world, int x, int y, int z) {
		int dy = y;
		MultiBlockChromaTile te = this.getMultiblockTile(world, x, y, z);
		if (te == null)
			return super.getPlayerRelativeBlockHardness(ep, world, x, y, z);
		if (te instanceof CrystalSource)
			return -1;
		return te instanceof OwnedTile && !((OwnedTile)te).isOwnedByPlayer(ep) ? -1 : super.getPlayerRelativeBlockHardness(ep, world, x, y, z);
	}

	private MultiBlockChromaTile getMultiblockTile(World world, int x, int y, int z) {
		PropagationCondition prop = new PropagationCondition() {
			@Override
			public boolean isValidLocation(World world, int x, int y, int z, Coordinate from) {
				Block b = world.getBlock(x, y, z);
				return b instanceof BlockProtectedByStructure || b == ChromaBlocks.RUNE.getBlockInstance();
			}
		};
		TerminationCondition end = new TerminationCondition() {
			@Override
			public boolean isValidTerminus(World world, int x, int y, int z) {
				return world.getTileEntity(x, y, z) instanceof MultiBlockChromaTile;
			}
		};
		BreadthFirstSearch bfs = new BreadthFirstSearch(x, y, z, prop, end);
		bfs.limit = BlockBox.block(x, y, z).expand(15, 40, 15);
		bfs.complete(world);
		FoundPath li = bfs.getResult();
		//ReikaJavaLibrary.pConsole(li);
		if (li == null || li.isEmpty())
			return null;
		TileEntity te = li.getPath().getLast().getTileEntity(world);
		return te instanceof MultiBlockChromaTile ? (MultiBlockChromaTile)te : null;
	}

}
