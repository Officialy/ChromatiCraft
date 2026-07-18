package reika.chromaticraft.auxiliary.structure.worldgen;

import java.util.Random;

import net.minecraft.world.World;

import reika.chromaticraft.base.GeneratedStructureBase;
import reika.chromaticraft.block.blockdummyaux.TileEntityDummyAux;
import reika.chromaticraft.block.blockdummyaux.tileentitydummyaux.Flags;
import reika.chromaticraft.block.worldgen.blockstructureshield.BlockType;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.DragonAPICore;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.Coordinate;


public class DataTowerStructure extends GeneratedStructureBase {

	@Override
	public FilledBlockArray getArray(World world, int x, int y, int z) {
		FilledBlockArray array = new FilledBlockArray(world);
		Random r = DragonAPICore.rand;

		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				array.setBlock(x+i, y, z+k, shield, r.nextInt(3) == 0 ? BlockType.MOSS.metadata : BlockType.STONE.metadata);
			}
		}

		array.setBlock(x+1, y+1, z, shield, r.nextInt(3) == 0 ? BlockType.MOSS.metadata : BlockType.STONE.metadata);
		array.setBlock(x-1, y+1, z, shield, r.nextInt(3) == 0 ? BlockType.MOSS.metadata : BlockType.STONE.metadata);
		array.setBlock(x, y+1, z+1, shield, r.nextInt(3) == 0 ? BlockType.MOSS.metadata : BlockType.STONE.metadata);
		array.setBlock(x, y+1, z-1, shield, r.nextInt(3) == 0 ? BlockType.MOSS.metadata : BlockType.STONE.metadata);

		array.setBlock(x, y+1, z, ChromaTiles.DATANODE.getBlock(), ChromaTiles.DATANODE.getBlockMetadata());

		for (int i = 0; i < 4; i++) {
			TileEntityDummyAux te = new TileEntityDummyAux();
			te.setFlag(Flags.HITBOX, true);
			te.setFlag(Flags.RENDER, false);
			te.setFlag(Flags.MOUSEOVER, false);
			te.link(new Coordinate(x, y+1, z));
			array.setTile(x, y+2+i, z, ChromaBlocks.DUMMYAUX.getBlockInstance(), 0, te, "loc", "flags");
		}

		return array;
	}

	@Override
	public int getStructureVersion() {
		return 0;
	}

}
