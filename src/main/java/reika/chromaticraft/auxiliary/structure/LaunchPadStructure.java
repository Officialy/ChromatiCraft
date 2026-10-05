package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

import java.util.function.UnaryOperator;

/**
 * V33a {@code LaunchPadStructure}: the Vertical Propulsion Panel set flush in a 3x3 ring of crystalline stone, corners
 * at the corners and grooves along the edges. Built from the {@code multiblock/launch_pad} template, which
 * {@code ChromaStructureTemplateProvider} writes from V33a's own layout table.
 */
public class LaunchPadStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/launch_pad");
	/** The pad is the centre of the ring. */
	private static final BlockPos ANCHOR = new BlockPos(1, 0, 1);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, UnaryOperator.identity());
	}

}
