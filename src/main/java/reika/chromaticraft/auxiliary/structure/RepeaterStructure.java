package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** NBT-backed crystal-repeater multiblock with its rune colour supplied by the active repeater. */
public class RepeaterStructure extends ColoredStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/repeater");
	private static final BlockPos ANCHOR = new BlockPos(0, 3, 0);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(this.getCurrentColor()).get().defaultBlockState()
						: state);
	}
}
