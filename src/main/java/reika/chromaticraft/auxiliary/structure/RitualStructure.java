package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

public class RitualStructure extends ChromaStructureBase {

	private static final BlockPos TABLE_ANCHOR = new BlockPos(5, 2, 5);

	public final boolean isEnhanced;
	private boolean allowEnhance;
	private boolean requireEnhance;

	public RitualStructure() {
		this(false);
	}

	protected RitualStructure(boolean enhanced) {
		isEnhanced = enhanced;
		initializeEnhance(enhanced, enhanced);
	}

	public void initializeEnhance(boolean allow, boolean require) {
		allowEnhance = allow;
		requireEnhance = require;
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		initializeEnhance(isEnhanced, isEnhanced);
	}

	@Override
	public FilledBlockArray getArray(Level world, int x, int y, int z) {
		Identifier template = requireEnhance ? ChromaStructureTemplateProvider.RITUAL_ENHANCED
				: ChromaStructureTemplateProvider.RITUAL_BASE;
		FilledBlockArray array = NBTStructureLoader.load(world, template,
				new BlockPos(x, y + 2, z), TABLE_ANCHOR, state -> state, false,
				(relative, state) -> cellRule(relative, state));
		array.setBlock(x, y + 2, z, ChromaBlocks.RITUAL_TABLE.get());
		return array;
	}

	private BlockCheck cellRule(BlockPos relative, BlockState state) {
		int x = relative.getX();
		int y = relative.getY();
		int z = relative.getZ();
		if (y == 0 && ((x == 0 || x == 10) && z >= 1 && z <= 9
				|| (z == 0 || z == 10) && x >= 1 && x <= 9)) {
			FilledBlockArray.MultiKey alternatives = new FilledBlockArray.MultiKey();
			alternatives.add(new BlockKey(state));
			for (var rune : ChromaBlocks.RUNES)
				alternatives.add(new BlockKey(rune.get().defaultBlockState()));
			return alternatives;
		}
		if (!requireEnhance && allowEnhance && state.getBlock() instanceof BlockCrystallineStone stone) {
			StoneTypes glow = stone.getStoneType().getGlowingVariant();
			if (glow != null && (y == 1 && stone.getStoneType() == StoneTypes.BEAM
					|| y == 2 && stone.getStoneType() == StoneTypes.COLUMN)) {
				FilledBlockArray.MultiKey alternatives = new FilledBlockArray.MultiKey();
				alternatives.add(new BlockKey(state));
				alternatives.add(new BlockKey(ChromaBlocks.crystallineStone(glow).get().defaultBlockState()));
				return alternatives;
			}
		}
		return null;
	}

	public static final class Enhanced extends RitualStructure {
		public Enhanced() {
			super(true);
		}
	}
}
