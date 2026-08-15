package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** Canonical NBT-backed port of V33a's two stone rings and Liquid Chroma infusion ring. */
public final class InfusionStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/infusion");
	private static final BlockPos ANCHOR = new BlockPos(3, 2, 3);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR,
				state -> state, false);
	}
}
