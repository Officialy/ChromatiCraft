package reika.chromaticraft.auxiliary.structure;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** NBT-backed exact V33a BoostedRelayStructure, anchored on the Relay Source itself. */
public final class RelaySourceStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/relay_source");
	private static final BlockPos ANCHOR = new BlockPos(2, 3, 2);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR,
				UnaryOperator.identity());
	}
}
