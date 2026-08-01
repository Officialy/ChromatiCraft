package reika.chromaticraft.auxiliary.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import reika.chromaticraft.base.ChromaStructureBase;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

/**
 * NBT-backed V33a casting temple tiers. Templates own all geometry; Java only restores the
 * original alternative-block matches that vanilla structure palettes cannot encode.
 */
public abstract class CastingStructure extends ChromaStructureBase {

	private static final int[][] CARDINALS = {{1,0},{-1,0},{0,1},{0,-1}};
	private static final int[][] OUTER_REPEATER_POSTS = {
			{-6,-8},{-2,-8},{2,-8},{6,-8},{-6,8},{-2,8},{2,8},{6,8},
			{-8,-6},{-8,-2},{-8,2},{-8,6},{8,-6},{8,-2},{8,2},{8,6}
	};
	private final int tier;
	private final Identifier template;
	private final BlockPos anchor;

	protected CastingStructure(int tier) {
		this.tier = tier;
		template = NBTStructureLoader.chromaTemplate("multiblock/casting_l" + tier);
		anchor = new BlockPos(tier == 3 ? 8 : 6, 0, tier == 3 ? 8 : 6);
	}

	@Override
	public FilledBlockArray getArray(Level level, int x, int y, int z) {
		FilledBlockArray array = NBTStructureLoader.load(level, template, new BlockPos(x, y, z), anchor, state -> state);
		if (tier == 1) {
			for (int[] direction : CARDINALS) {
				for (int distance = 3; distance <= 5; distance++)
					this.addRuneAlternative(array, x + direction[0] * distance, y, z + direction[1] * distance);
				this.addRuneAlternative(array, x + direction[0], y, z + direction[1]);
			}
			for (int i = -3; i <= 3; i++) for (int dy = 0; dy <= 1; dy++) {
				if (dy == 0 || Math.abs(i) % 2 == 1) {
					this.addRuneAlternative(array, x - 3, y + dy, z + i);
					this.addRuneAlternative(array, x + 3, y + dy, z + i);
					this.addRuneAlternative(array, x + i, y + dy, z - 3);
			for (int[] direction : CARDINALS)
				array.addBlock(x + direction[0], y + 1, z + direction[1], Blocks.FIRE);
					this.addRuneAlternative(array, x + i, y + dy, z + 3);
				}
			}
		}
		else {
			for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++)
				this.addRuneAlternative(array, x + dx, y, z + dz);
		}
		if (tier == 3) {
			for (int[] post : OUTER_REPEATER_POSTS)
				array.addBlock(x + post[0], y + 3, z + post[1], RuneBlockCheck.INSTANCE);
			BlockCrystallineStone.StoneTypes alternate = BlockCrystallineStone.StoneTypes.BRICKS;
			for (int i = -7; i <= 7; i++) {
				array.addBlock(x - 7, y, z + i, ChromaBlocks.crystallineStone(alternate).get().defaultBlockState());
				array.addBlock(x + 7, y, z + i, ChromaBlocks.crystallineStone(alternate).get().defaultBlockState());
				array.addBlock(x + i, y, z - 7, ChromaBlocks.crystallineStone(alternate).get().defaultBlockState());
				array.addBlock(x + i, y, z + 7, ChromaBlocks.crystallineStone(alternate).get().defaultBlockState());
			}
			BlockCrystallineStone.StoneTypes resourceRing = BlockCrystallineStone.StoneTypes.RESORING;
			int[] offsets = {-6, -2, 2, 6};
			for (int offset : offsets) {
				array.addBlock(x + offset, y + 1, z - 8, ChromaBlocks.crystallineStone(resourceRing).get().defaultBlockState());
				array.addBlock(x + offset, y + 1, z + 8, ChromaBlocks.crystallineStone(resourceRing).get().defaultBlockState());
				array.addBlock(x - 8, y + 1, z + offset, ChromaBlocks.crystallineStone(resourceRing).get().defaultBlockState());
				array.addBlock(x + 8, y + 1, z + offset, ChromaBlocks.crystallineStone(resourceRing).get().defaultBlockState());
			}
		}
		return array;
	}

	private void addRuneAlternative(FilledBlockArray array, int x, int y, int z) {
		if (BlockCrystallineStone.isCrystallineStone(array.getBlockAt(x, y, z)))
			array.addBlock(x, y, z, RuneBlockCheck.INSTANCE);
	}

	public static final class Tier1 extends CastingStructure { public Tier1() { super(1); } }
	public static final class Tier2 extends CastingStructure { public Tier2() { super(2); } }
	public static final class Tier3 extends CastingStructure { public Tier3() { super(3); } }
}
