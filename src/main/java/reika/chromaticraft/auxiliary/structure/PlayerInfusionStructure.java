package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** Canonical NBT-backed port of V33a's 9x4x9 Player Infusion fountain. */
public final class PlayerInfusionStructure extends ChromaStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/player_infusion");
	private static final BlockPos ANCHOR = new BlockPos(4, 3, 4);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR,
				state -> state, false);
	}
}
