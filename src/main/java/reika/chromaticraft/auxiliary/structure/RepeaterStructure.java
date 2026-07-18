package reika.chromaticraft.auxiliary.structure;

import net.minecraft.world.World;

import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;


public class RepeaterStructure extends ColoredStructureBase {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);
		this.setTile(array, x, y, z, ChromaTiles.REPEATER);
		array.setBlock(x, y-1, z, ChromaBlocks.RUNE.getBlockInstance(), this.getCurrentColor().ordinal());
		array.setBlock(x, y-2, z, crystalstone, 0);
		array.setBlock(x, y-3, z, crystalstone, 0);
		return array;
	}

}
