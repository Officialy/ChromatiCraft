package reika.chromaticraft.auxiliary.structure;

import net.minecraft.world.World;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.block.blockpylonstructure.StoneTypes;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;


public class TreeSendFocusStructure extends ChromaStructureBase {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);

		LumenTreeStructure.getTrunkBlocks(array, x, y, z);

		for (int i = -2; i <= 3; i += 5) {
			for (int k = -2; k <= 3; k += 5) {
				int dx = x+i;
				int dz = z+k-1;
				int dy = y-12;
				array.setBlock(dx, dy, dz, crystalstone, StoneTypes.COLUMN.ordinal());
				array.setBlock(dx, dy+1, dz, crystalstone, StoneTypes.FOCUS.ordinal());
			}
		}

		return array;
	}

}
