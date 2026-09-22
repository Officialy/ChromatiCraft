package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

import java.util.List;

/** Canonical NBT-backed V33a turbocharged-pylon structure, anchored on the pylon tile. */
public final class PylonTurboStructure extends ColoredStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/pylon_turbo");
	private static final BlockPos ANCHOR = new BlockPos(7, 9, 7);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		FilledBlockArray array = NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(this.getCurrentColor()).get().defaultBlockState()
						: state);
		for (BlockPos pos : List.copyOf(array.keySet())) {
			BlockKey key = array.getBlockKeyAt(pos.getX(), pos.getY(), pos.getZ());
			if (key != null && (key.blockID.getBlock() == ChromaBlocks.crystallineStone(StoneTypes.STABILIZER).get()
					|| key.blockID.getBlock() == ChromaBlocks.crystallineStone(StoneTypes.RESORING).get())) {
				array.addBlock(pos.getX(), pos.getY(), pos.getZ(),
						ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState());
			}
		}
		return array;
	}
}
