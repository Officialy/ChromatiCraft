package reika.chromaticraft.auxiliary.structure;

import net.minecraft.world.World;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.networking.TileEntityWeakRepeater;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.libraries.registry.ReikaTreeHelper;
import reika.dragonapi.modregistry.ModWoodList;


public class WeakRepeaterStructure extends ChromaStructureBase {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);

		array.setBlock(x, y, z, ChromaTiles.WEAKREPEATER.getBlock(), ChromaTiles.WEAKREPEATER.getBlockMetadata());
		for (int i = 0; i < ReikaTreeHelper.treeList.length; i++) {
			ReikaTreeHelper tree = ReikaTreeHelper.treeList[i];
			array.addBlock(x, y-1, z, tree.getLogID(), tree.getLogMetadatas().get(0));
		}
		for (int i = 0; i < ModWoodList.woodList.length; i++) {
			ModWoodList tree = ModWoodList.woodList[i];
			if (tree.exists() && TileEntityWeakRepeater.isValidWood(tree)) {
				array.addBlock(x, y-1, z, tree.getLogID(), tree.getLogMetadatas().get(0));
			}
		}

		return array;
	}

}
