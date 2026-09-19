package reika.chromaticraft.auxiliary.structure;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** NBT-backed guide representation of V33a's one-log Wooden Repeater support. */
public final class WeakRepeaterStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/weak_repeater");
	private static final BlockPos ANCHOR = new BlockPos(0, 1, 0);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR,
				UnaryOperator.identity());
	}
}
