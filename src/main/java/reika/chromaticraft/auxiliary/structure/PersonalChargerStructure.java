package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ColoredStructureBase;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** NBT-backed V33a Personal Charger multiblock, recoloured from its four authored rune sockets. */
public final class PersonalChargerStructure extends ColoredStructureBase {

	private static final Identifier TEMPLATE = NBTStructureLoader.chromaTemplate("multiblock/personal_charger");
	/** The charger block is six cells above the old structure origin. */
	private static final BlockPos ANCHOR = new BlockPos(2, 6, 2);

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		return NBTStructureLoader.load(world, TEMPLATE, new BlockPos(x, y, z), ANCHOR, state ->
				ChromaBlocks.isRune(state)
						? ChromaBlocks.rune(this.getCurrentColor()).get().defaultBlockState()
						: state);
	}
}
