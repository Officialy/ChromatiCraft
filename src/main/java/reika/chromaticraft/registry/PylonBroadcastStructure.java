package reika.chromaticraft.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.auxiliary.structure.RegistryBlockCheck;
import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.interfaces.BlockCheck;

/** The V33a pylon broadcast-upgrade monument, sourced from canonical structure NBT. */
public final class PylonBroadcastStructure extends ColoredStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/pylon_broadcast");
	private static final Identifier CHROMA = Identifier.fromNamespaceAndPath("chromaticraft", "chroma");
	private static final BlockPos ANCHOR = new BlockPos(5, 10, 5);
	private static final BlockCheck CHROMA_CHECK = new RegistryBlockCheck(CHROMA);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		FilledBlockArray array = NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(this.getCurrentColor()).get().defaultBlockState()
						: state);
		int baseY = y - 9;
		for (int offset = -2; offset <= 2; offset++) {
			this.requireChroma(array, x + offset, baseY, z + 4);
			this.requireChroma(array, x + offset, baseY, z - 4);
			this.requireChroma(array, x + 4, baseY, z + offset);
			this.requireChroma(array, x - 4, baseY, z + offset);
		}
		for (int offset = 2; offset <= 3; offset++) {
			for (int xSign : new int[] {-1, 1}) for (int zSign : new int[] {-1, 1}) {
				this.requireChroma(array, x + xSign * offset, baseY, z + zSign * 2);
				this.requireChroma(array, x + xSign * 2, baseY, z + zSign * offset);
			}
		}
		return array;
	}

	private void requireChroma(FilledBlockArray array, int x, int y, int z) {
		array.setBlock(x, y, z, CHROMA_CHECK);
	}
}
