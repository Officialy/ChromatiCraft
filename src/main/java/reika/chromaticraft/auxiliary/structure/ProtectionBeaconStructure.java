package reika.chromaticraft.auxiliary.structure;

import net.minecraft.world.World;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.block.blockpylonstructure.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;


public class ProtectionBeaconStructure extends ChromaStructureBase {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);

		for (int i = -2; i <= 2; i++) {
			for (int k = -2; k <= 2; k++) {
				array.setBlock(x+i, y-1, z+k, crystalstone, StoneTypes.SMOOTH.ordinal());
			}
		}

		for (int i = -3; i <= 3; i++) {
			for (int k = -3; k <= 3; k++) {
				if (i != 0 || k != 0) {
					array.setEmpty(x+i, y, z+k, false, false);
					array.setEmpty(x+i, y+1, z+k, false, false);
				}
			}
		}

		for (int i = -2; i <= 2; i++) {
			array.setBlock(x+3, y-1, z+i, crystalstone, StoneTypes.GROOVE2.ordinal());
			array.setBlock(x-3, y-1, z+i, crystalstone, StoneTypes.GROOVE2.ordinal());
			array.setBlock(x+i, y-1, z+3, crystalstone, StoneTypes.GROOVE1.ordinal());
			array.setBlock(x+i, y-1, z-3, crystalstone, StoneTypes.GROOVE1.ordinal());
		}

		array.setBlock(x-3, y-1, z-3, crystalstone, StoneTypes.CORNER.ordinal());
		array.setBlock(x+3, y-1, z-3, crystalstone, StoneTypes.CORNER.ordinal());
		array.setBlock(x-3, y-1, z+3, crystalstone, StoneTypes.CORNER.ordinal());
		array.setBlock(x+3, y-1, z+3, crystalstone, StoneTypes.CORNER.ordinal());

		array.setBlock(x-2, y, z-2, crystalstone, StoneTypes.COLUMN.ordinal());
		array.setBlock(x+2, y, z-2, crystalstone, StoneTypes.COLUMN.ordinal());
		array.setBlock(x-2, y, z+2, crystalstone, StoneTypes.COLUMN.ordinal());
		array.setBlock(x+2, y, z+2, crystalstone, StoneTypes.COLUMN.ordinal());

		array.setBlock(x-2, y+1, z-2, crystalstone, StoneTypes.FOCUS.ordinal());
		array.setBlock(x+2, y+1, z-2, crystalstone, StoneTypes.FOCUS.ordinal());
		array.setBlock(x-2, y+1, z+2, crystalstone, StoneTypes.FOCUS.ordinal());
		array.setBlock(x+2, y+1, z+2, crystalstone, StoneTypes.FOCUS.ordinal());

		this.setTile(array, x, y, z, ChromaTiles.BEACON);

		array.addBlock(x+1, y, z, ChromaBlocks.ADJACENCY.getBlockInstance());
		array.addBlock(x-1, y, z, ChromaBlocks.ADJACENCY.getBlockInstance());
		array.addBlock(x, y, z+1, ChromaBlocks.ADJACENCY.getBlockInstance());
		array.addBlock(x, y, z-1, ChromaBlocks.ADJACENCY.getBlockInstance());

		return array;
	}

}
