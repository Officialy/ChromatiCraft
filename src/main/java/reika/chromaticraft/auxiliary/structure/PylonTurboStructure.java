package reika.chromaticraft.auxiliary.structure;

import net.minecraft.world.World;

import reika.chromaticraft.auxiliary.structure.worldgen.PylonStructure;
import reika.chromaticraft.block.blockpylonstructure.StoneTypes;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.tileentitypylonturbocharger.Location;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

public class PylonTurboStructure extends PylonStructure {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = super.getArray(world, x, y, z);
		y -= 9;

		array.setBlock(x, y+9, z,  ChromaTiles.PYLON.getBlock(), ChromaTiles.PYLON.getBlockMetadata());

		BlockKey[] col = new BlockKey[]{
				new BlockKey(crystalstone, StoneTypes.COLUMN.ordinal()),
				new BlockKey(crystalstone, StoneTypes.FOCUS.ordinal()),
				new BlockKey(ChromaTiles.PYLONTURBO.getBlock(), ChromaTiles.PYLONTURBO.getBlockMetadata()),
		};

		for (int l = 0; l < Location.list.length; l++) {
			Location loc = Location.list[l];
			Coordinate c = loc.position;
			for (int i = 0; i < col.length; i++) {
				array.setBlock(x+c.xCoord, y+1+i, z+c.zCoord, col[i].blockID, col[i].metadata);
			}
		}

		for (Coordinate c : TileEntityCrystalPylon.getPowerCrystalLocations()) {
			this.setTile(array, x+c.xCoord, y+9+c.yCoord, z+c.zCoord, ChromaTiles.CRYSTAL);
		}

		this.setTile(array, x, y+1, z, ChromaTiles.PYLONTURBO);

		return array;
	}

}
