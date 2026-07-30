package reika.chromaticraft.auxiliary.structure;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** Canonical NBT-backed five-block multichromic column supporting a compound repeater. */
public class CompoundRepeaterStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/compound_repeater");
	private static final BlockPos ANCHOR = new BlockPos(0, 5, 0);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(
				world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, UnaryOperator.identity());
	}
}
