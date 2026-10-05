package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Authored V33a layout: every position retained, variants and wire facing use native 26.3 states. */
public class VoidMonsterNetherStructure extends ChromaStructureBase {

	private static final Collection<Coordinate> tntLocations = new ArrayList();

	private boolean useTNT = true;

	static {
		tntLocations.add(new Coordinate(1, 2, 1));
		tntLocations.add(new Coordinate(1, 2, 3));
		tntLocations.add(new Coordinate(3, 2, 1));
		tntLocations.add(new Coordinate(3, 2, 3));
	}

	@Override
	public void resetToDefaults() {
		useTNT = true;
	}

	public void setTNT(boolean tnt) {
		useTNT = tnt;
	}

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);

		this.setTile(array, x, y, z, ChromaTiles.VOIDTRAP);

		x -= 2;
		y -= 6;
		z -= 2;

		for (int i = -2; i <= 6; i++) {
			for (int k = -2; k <= 6; k++) {
				for (int j = 0; j <= 4; j++) {
					if (i < 0 || k < 0 || i > 4 || k > 4)
						array.setEmpty(x + i, y + j, z + k, false, false);
					//array.setBlock(x + i, y + j, z + k, Blocks.ice);
				}
			}
		}

		array.setBlock(x, y, z, ChromaBlocks.crystallineStone(StoneTypes.values()[8]).get());
		array.setBlock(x, y, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[7]).get());
		array.setBlock(x, y, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[8]).get());
		array.setBlock(x, y + 1, z, ChromaBlocks.crystallineStone(StoneTypes.values()[3]).get());
		array.setBlock(x, y + 1, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[3]).get());
		array.setBlock(x, y + 2, z, ChromaBlocks.crystallineStone(StoneTypes.values()[6]).get());
		array.setBlock(x, y + 2, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x, y + 2, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x, y + 2, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x, y + 2, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[6]).get());
		array.setBlock(x + 1, y, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[0]).get());
		array.setBlock(x + 1, y, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[0]).get());
		array.setBlock(x + 1, y + 1, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 1, y + 1, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 1, y + 2, z, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 1, y + 2, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[15]).get());
		array.setBlock(x + 1, y + 2, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 1, y + 3, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 1, y + 3, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 1, y + 4, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[9]).get());
		array.setBlock(x + 1, y + 4, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[9]).get());
		array.setBlock(x + 2, y, z, ChromaBlocks.crystallineStone(StoneTypes.values()[7]).get());
		array.setBlock(x + 2, y, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[7]).get());
		array.setBlock(x + 2, y + 2, z, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 2, y + 2, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[15]).get());
		array.setBlock(x + 2, y + 2, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[14]).get());
		array.setBlock(x + 2, y + 2, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[15]).get());
		array.setBlock(x + 2, y + 2, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 3, y, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[0]).get());
		array.setBlock(x + 3, y, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[0]).get());
		array.setBlock(x + 3, y + 1, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 3, y + 1, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 3, y + 2, z, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 3, y + 2, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[15]).get());
		array.setBlock(x + 3, y + 2, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 3, y + 3, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 3, y + 3, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[2]).get());
		array.setBlock(x + 3, y + 4, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[9]).get());
		array.setBlock(x + 3, y + 4, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[9]).get());
		array.setBlock(x + 4, y, z, ChromaBlocks.crystallineStone(StoneTypes.values()[8]).get());
		array.setBlock(x + 4, y, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[7]).get());
		array.setBlock(x + 4, y, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[8]).get());
		array.setBlock(x + 4, y + 1, z, ChromaBlocks.crystallineStone(StoneTypes.values()[3]).get());
		array.setBlock(x + 4, y + 1, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[3]).get());
		array.setBlock(x + 4, y + 2, z, ChromaBlocks.crystallineStone(StoneTypes.values()[6]).get());
		array.setBlock(x + 4, y + 2, z + 1, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 4, y + 2, z + 2, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 4, y + 2, z + 3, ChromaBlocks.crystallineStone(StoneTypes.values()[1]).get());
		array.setBlock(x + 4, y + 2, z + 4, ChromaBlocks.crystallineStone(StoneTypes.values()[6]).get());

		array.setBlock(x, y + 3, z, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x, y + 3, z + 1, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x, y + 3, z + 2, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x, y + 3, z + 3, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x, y + 3, z + 4, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 1, y + 3, z, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 1, y + 3, z + 4, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 2, y + 3, z, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 4, y + 3, z, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 4, y + 3, z + 1, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 4, y + 3, z + 2, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 4, y + 3, z + 3, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 4, y + 3, z + 4, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 2, y + 3, z + 4, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 3, y + 3, z, VoidRedstoneCheck.INSTANCE);
		array.setBlock(x + 3, y + 3, z + 4, VoidRedstoneCheck.INSTANCE);

		array.setBlock(x + 2, y + 1, z, Blocks.REDSTONE_TORCH.defaultBlockState());
		array.setBlock(x + 2, y + 1, z + 4, Blocks.REDSTONE_TORCH.defaultBlockState());
		array.setBlock(x, y + 1, z + 2, Blocks.REDSTONE_TORCH.defaultBlockState());
		array.setBlock(x + 4, y + 1, z + 2, Blocks.REDSTONE_TORCH.defaultBlockState());

		array.addBlock(x + 2, y + 1, z, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT,false));
		array.addBlock(x + 2, y + 1, z + 4, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT,false));
		array.addBlock(x, y + 1, z + 2, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT,false));
		array.addBlock(x + 4, y + 1, z + 2, Blocks.REDSTONE_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT,false));

		array.setBlock(x, y + 4, z, Blocks.AIR);
		array.setBlock(x, y + 4, z + 1, Blocks.AIR);
		array.setBlock(x, y + 4, z + 2, Blocks.AIR);
		array.setBlock(x, y + 4, z + 3, Blocks.AIR);
		array.setBlock(x, y + 4, z + 4, Blocks.AIR);
		array.setBlock(x, y + 5, z, Blocks.AIR);
		array.setBlock(x, y + 5, z + 1, Blocks.AIR);
		array.setBlock(x, y + 5, z + 2, Blocks.AIR);
		array.setBlock(x, y + 5, z + 3, Blocks.AIR);
		array.setBlock(x, y + 5, z + 4, Blocks.AIR);
		array.setBlock(x, y + 6, z, Blocks.AIR);
		array.setBlock(x, y + 6, z + 1, Blocks.AIR);
		array.setBlock(x, y + 6, z + 2, Blocks.AIR);
		array.setBlock(x, y + 6, z + 3, Blocks.AIR);
		array.setBlock(x, y + 6, z + 4, Blocks.AIR);
		array.setBlock(x + 1, y + 3, z + 2, Blocks.AIR);
		array.setBlock(x + 1, y + 4, z, Blocks.AIR);
		array.setBlock(x + 1, y + 4, z + 2, Blocks.AIR);
		array.setBlock(x + 1, y + 4, z + 4, Blocks.AIR);
		array.setBlock(x + 1, y + 5, z, Blocks.AIR);
		array.setBlock(x + 1, y + 5, z + 1, Blocks.AIR);
		array.setBlock(x + 1, y + 5, z + 2, Blocks.AIR);
		array.setBlock(x + 1, y + 5, z + 3, Blocks.AIR);
		array.setBlock(x + 1, y + 5, z + 4, Blocks.AIR);
		array.setBlock(x + 1, y + 6, z, Blocks.AIR);
		array.setBlock(x + 1, y + 6, z + 1, Blocks.AIR);
		array.setBlock(x + 1, y + 6, z + 2, Blocks.AIR);
		array.setBlock(x + 1, y + 6, z + 3, Blocks.AIR);
		array.setBlock(x + 1, y + 6, z + 4, Blocks.AIR);
		array.setBlock(x + 2, y + 3, z + 1, Blocks.AIR);
		array.setBlock(x + 2, y + 3, z + 2, Blocks.AIR);
		array.setBlock(x + 2, y + 3, z + 3, Blocks.AIR);
		array.setBlock(x + 2, y + 4, z, Blocks.AIR);
		array.setBlock(x + 2, y + 4, z + 1, Blocks.AIR);
		array.setBlock(x + 2, y + 4, z + 2, Blocks.AIR);
		array.setBlock(x + 2, y + 4, z + 3, Blocks.AIR);
		array.setBlock(x + 2, y + 4, z + 4, Blocks.AIR);
		array.setBlock(x + 2, y + 5, z, Blocks.AIR);
		array.setBlock(x + 2, y + 5, z + 1, Blocks.AIR);
		array.setBlock(x + 2, y + 5, z + 2, Blocks.AIR);
		array.setBlock(x + 2, y + 5, z + 3, Blocks.AIR);
		array.setBlock(x + 2, y + 5, z + 4, Blocks.AIR);
		array.setBlock(x + 2, y + 6, z, Blocks.AIR);
		array.setBlock(x + 2, y + 6, z + 1, Blocks.AIR);
		array.setBlock(x + 2, y + 6, z + 3, Blocks.AIR);
		array.setBlock(x + 2, y + 6, z + 4, Blocks.AIR);
		array.setBlock(x + 3, y + 3, z + 2, Blocks.AIR);
		array.setBlock(x + 3, y + 4, z, Blocks.AIR);
		array.setBlock(x + 3, y + 4, z + 2, Blocks.AIR);
		array.setBlock(x + 3, y + 4, z + 4, Blocks.AIR);
		array.setBlock(x + 3, y + 5, z, Blocks.AIR);
		array.setBlock(x + 3, y + 5, z + 1, Blocks.AIR);
		array.setBlock(x + 3, y + 5, z + 2, Blocks.AIR);
		array.setBlock(x + 3, y + 5, z + 3, Blocks.AIR);
		array.setBlock(x + 3, y + 5, z + 4, Blocks.AIR);
		array.setBlock(x + 3, y + 6, z, Blocks.AIR);
		array.setBlock(x + 3, y + 6, z + 1, Blocks.AIR);
		array.setBlock(x + 3, y + 6, z + 2, Blocks.AIR);
		array.setBlock(x + 3, y + 6, z + 3, Blocks.AIR);
		array.setBlock(x + 3, y + 6, z + 4, Blocks.AIR);
		array.setBlock(x + 4, y + 4, z, Blocks.AIR);
		array.setBlock(x + 4, y + 4, z + 1, Blocks.AIR);
		array.setBlock(x + 4, y + 4, z + 2, Blocks.AIR);
		array.setBlock(x + 4, y + 4, z + 3, Blocks.AIR);
		array.setBlock(x + 4, y + 4, z + 4, Blocks.AIR);
		array.setBlock(x + 4, y + 5, z, Blocks.AIR);
		array.setBlock(x + 4, y + 5, z + 1, Blocks.AIR);
		array.setBlock(x + 4, y + 5, z + 2, Blocks.AIR);
		array.setBlock(x + 4, y + 5, z + 3, Blocks.AIR);
		array.setBlock(x + 4, y + 5, z + 4, Blocks.AIR);
		array.setBlock(x + 4, y + 6, z, Blocks.AIR);
		array.setBlock(x + 4, y + 6, z + 1, Blocks.AIR);
		array.setBlock(x + 4, y + 6, z + 2, Blocks.AIR);
		array.setBlock(x + 4, y + 6, z + 3, Blocks.AIR);
		array.setBlock(x + 4, y + 6, z + 4, Blocks.AIR);

		if (useTNT) {
			for (Coordinate c : tntLocations) {
				array.setBlock(x+ c.xCoord(), y+ c.yCoord(), z+ c.zCoord(), Blocks.TNT);
			}
		}

		return array;
	}

	public static Collection<Coordinate> getTNTLocations() {
		return Collections.unmodifiableCollection(tntLocations);
	}

}
