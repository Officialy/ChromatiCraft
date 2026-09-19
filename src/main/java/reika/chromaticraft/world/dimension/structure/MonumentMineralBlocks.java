package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code MonumentMineralBlocks}: the monument's mineral inlay, and the ritual's second gate.
 *
 * <h2>This is not decoration</h2>
 *
 * <p>The asymmetry in upstream's one helper is the whole mechanic:
 *
 * <pre>
 * private void setBlock(..., Block b) {
 *     if (ReikaRandomHelper.doWithChance(blockChance.get(b)))
 *         world.setBlock(x, y, z, b);
 *     parent.registerMineralBlock(x, y, z, b);   // unconditional
 * }
 * </pre>
 *
 * <p>The block is laid only on a per-material chance, but it is <em>registered</em> every time. So the
 * expected map holds all three hundred and five cells while the generated world receives a
 * random subset of them, and {@code MonumentCompletionRitual.doMineralChecks} compares the world against
 * the full map. The player has to finish the inlay by hand before the ritual will run — which is what
 * makes it a <em>completion</em> ritual. The later, active gold ring bypasses this helper entirely: all
 * seventy-six gold blocks are always placed and are not registered as player completion cells.
 *
 * <p>The centre chroma at (21, 3, 21) is registered and never placed at all, so it is always supplied by
 * the player.
 *
 * <p>Coordinates are relative to the monument template's own origin, the same frame the geometry uses.
 */
public final class MonumentMineralBlocks {

	/** V33a blockChance: the percentage chance each material is actually laid at generation. */
	private static final Map<Mineral, Integer> CHANCE = new EnumMap<>(Mineral.class);

	private static final List<Cell> CELLS = new ArrayList<>();
	private static final List<Vec3i> FIXED_GOLD = new ArrayList<>();

	/** The materials of the inlay. Kept as an enum so the table below reads as data, not as blocks. */
	public enum Mineral {
		QUARTZ, DIAMOND, REDSTONE, LAPIS, GLOWSTONE, EMERALD, GOLD, CHROMA;

		public Block block() {
			return switch (this) {
				case QUARTZ -> Blocks.QUARTZ_BLOCK;
				case DIAMOND -> Blocks.DIAMOND_BLOCK;
				case REDSTONE -> Blocks.REDSTONE_BLOCK;
				case LAPIS -> Blocks.LAPIS_BLOCK;
				case GLOWSTONE -> Blocks.GLOWSTONE;
				case EMERALD -> Blocks.EMERALD_BLOCK;
				case GOLD -> Blocks.GOLD_BLOCK;
				case CHROMA -> ChromaBlocks.CHROMA.get();
			};
		}
	}

	/** One expected cell of the inlay, offset from the monument template's origin. */
	public record Cell(Vec3i offset, Mineral mineral) {}

	static {
		CHANCE.put(Mineral.QUARTZ, 80);
		CHANCE.put(Mineral.DIAMOND, 75);
		CHANCE.put(Mineral.REDSTONE, 40);
		CHANCE.put(Mineral.LAPIS, 50);
		CHANCE.put(Mineral.GLOWSTONE, 35);
		CHANCE.put(Mineral.EMERALD, 67);
		CHANCE.put(Mineral.GOLD, 25);
		CHANCE.put(Mineral.CHROMA, 50);

			c(23,0,16,Mineral.DIAMOND); c(23,0,26,Mineral.DIAMOND); c(22,0,16,Mineral.DIAMOND); c(22,0,26,Mineral.DIAMOND);
			c(21,0,16,Mineral.DIAMOND); c(21,0,26,Mineral.DIAMOND); c(20,0,26,Mineral.DIAMOND); c(20,0,16,Mineral.DIAMOND);
			c(19,0,16,Mineral.DIAMOND); c(19,0,26,Mineral.DIAMOND); c(18,0,16,Mineral.DIAMOND); c(18,0,26,Mineral.DIAMOND);
			c(17,0,17,Mineral.DIAMOND); c(17,0,25,Mineral.DIAMOND); c(16,0,18,Mineral.DIAMOND); c(16,0,19,Mineral.DIAMOND);
			c(16,0,20,Mineral.DIAMOND); c(16,0,21,Mineral.DIAMOND); c(16,0,22,Mineral.DIAMOND); c(16,0,23,Mineral.DIAMOND);
			c(16,0,24,Mineral.DIAMOND); c(26,0,18,Mineral.DIAMOND); c(26,0,19,Mineral.DIAMOND); c(26,0,20,Mineral.DIAMOND);
			c(26,0,21,Mineral.DIAMOND); c(26,0,22,Mineral.DIAMOND); c(26,0,23,Mineral.DIAMOND); c(26,0,24,Mineral.DIAMOND);
			c(25,0,17,Mineral.DIAMOND); c(25,0,25,Mineral.DIAMOND); c(24,0,16,Mineral.DIAMOND); c(24,0,26,Mineral.DIAMOND);
		// V33a leaves the obsolete gold-ring table above the live one inside a block comment. Gold is
		// therefore not part of the probabilistic completion map at all; the active ring below is laid
		// directly and unconditionally after the monument geometry.
		g(8,16); g(8,17); g(8,18); g(8,19); g(8,20); g(8,21); g(8,22); g(8,23); g(8,24); g(8,25); g(8,26);
		g(9,14); g(9,15); g(9,27); g(9,28); g(10,13); g(10,29); g(16,8); g(11,12); g(11,30);
		g(12,11); g(12,31); g(13,10); g(13,32); g(14,9); g(14,33); g(15,9); g(15,33); g(16,34);
		g(17,8); g(17,34); g(18,8); g(18,34); g(19,8); g(19,34); g(20,8); g(20,34); g(21,8); g(21,34);
		g(22,8); g(22,34); g(23,8); g(23,34); g(24,8); g(24,34); g(25,8); g(25,34); g(26,8); g(26,34);
		g(27,9); g(27,33); g(28,9); g(28,33); g(29,10); g(29,32); g(30,11); g(30,31); g(31,12); g(31,30);
		g(32,13); g(32,29); g(33,14); g(33,15); g(33,27); g(33,28);
		g(34,16); g(34,17); g(34,18); g(34,19); g(34,20); g(34,21); g(34,22); g(34,23); g(34,24); g(34,25); g(34,26);
			c(11,12,5,Mineral.QUARTZ); c(6,12,12,Mineral.QUARTZ); c(5,12,11,Mineral.QUARTZ); c(5,12,31,Mineral.QUARTZ);
			c(3,12,18,Mineral.QUARTZ); c(39,12,18,Mineral.QUARTZ); c(39,12,24,Mineral.QUARTZ); c(37,12,11,Mineral.QUARTZ);
			c(36,12,12,Mineral.QUARTZ); c(36,12,30,Mineral.QUARTZ); c(31,12,5,Mineral.QUARTZ); c(31,12,37,Mineral.QUARTZ);
			c(30,12,6,Mineral.QUARTZ); c(30,12,36,Mineral.QUARTZ); c(24,12,3,Mineral.QUARTZ); c(18,12,39,Mineral.QUARTZ);
			c(18,12,3,Mineral.QUARTZ); c(12,12,36,Mineral.QUARTZ); c(12,12,6,Mineral.QUARTZ); c(24,12,39,Mineral.QUARTZ);
			c(37,12,31,Mineral.QUARTZ); c(3,12,24,Mineral.QUARTZ); c(6,12,30,Mineral.QUARTZ); c(11,12,37,Mineral.QUARTZ);
			c(30,0,17,Mineral.EMERALD); c(30,0,18,Mineral.EMERALD); c(30,0,19,Mineral.EMERALD); c(30,0,20,Mineral.EMERALD);
			c(30,0,21,Mineral.EMERALD); c(30,0,22,Mineral.EMERALD); c(30,0,23,Mineral.EMERALD); c(30,0,24,Mineral.EMERALD);
			c(30,0,25,Mineral.EMERALD); c(29,0,16,Mineral.EMERALD); c(29,0,26,Mineral.EMERALD); c(28,0,15,Mineral.EMERALD);
			c(28,0,27,Mineral.EMERALD); c(27,0,28,Mineral.EMERALD); c(27,0,14,Mineral.EMERALD); c(26,0,29,Mineral.EMERALD);
			c(26,0,13,Mineral.EMERALD); c(25,0,30,Mineral.EMERALD); c(25,0,12,Mineral.EMERALD); c(24,0,30,Mineral.EMERALD);
			c(24,0,12,Mineral.EMERALD); c(23,0,30,Mineral.EMERALD); c(23,0,12,Mineral.EMERALD); c(22,0,30,Mineral.EMERALD);
			c(22,0,12,Mineral.EMERALD); c(21,0,30,Mineral.EMERALD); c(21,0,12,Mineral.EMERALD); c(20,0,30,Mineral.EMERALD);
			c(20,0,12,Mineral.EMERALD); c(19,0,30,Mineral.EMERALD); c(19,0,12,Mineral.EMERALD); c(18,0,30,Mineral.EMERALD);
			c(18,0,12,Mineral.EMERALD); c(17,0,30,Mineral.EMERALD); c(17,0,12,Mineral.EMERALD); c(16,0,13,Mineral.EMERALD);
			c(15,0,14,Mineral.EMERALD); c(14,0,15,Mineral.EMERALD); c(13,0,16,Mineral.EMERALD); c(12,0,17,Mineral.EMERALD);
			c(12,0,18,Mineral.EMERALD); c(12,0,19,Mineral.EMERALD); c(12,0,20,Mineral.EMERALD); c(12,0,21,Mineral.EMERALD);
			c(12,0,22,Mineral.EMERALD); c(12,0,23,Mineral.EMERALD); c(12,0,24,Mineral.EMERALD); c(12,0,25,Mineral.EMERALD);
			c(13,0,26,Mineral.EMERALD); c(14,0,27,Mineral.EMERALD); c(15,0,28,Mineral.EMERALD); c(16,0,29,Mineral.EMERALD);
			c(40,6,34,Mineral.LAPIS); c(40,7,8,Mineral.LAPIS); c(40,5,8,Mineral.LAPIS); c(2,5,8,Mineral.LAPIS);
			c(2,5,34,Mineral.LAPIS); c(2,6,8,Mineral.LAPIS); c(2,6,34,Mineral.LAPIS); c(2,7,8,Mineral.LAPIS);
			c(2,7,34,Mineral.LAPIS); c(8,5,2,Mineral.LAPIS); c(8,5,40,Mineral.LAPIS); c(8,6,2,Mineral.LAPIS);
			c(8,6,40,Mineral.LAPIS); c(8,7,2,Mineral.LAPIS); c(8,7,40,Mineral.LAPIS); c(34,5,2,Mineral.LAPIS);
			c(34,5,40,Mineral.LAPIS); c(34,6,2,Mineral.LAPIS); c(34,6,40,Mineral.LAPIS); c(34,7,2,Mineral.LAPIS);
			c(34,7,40,Mineral.LAPIS); c(40,5,34,Mineral.LAPIS); c(40,6,8,Mineral.LAPIS); c(40,7,34,Mineral.LAPIS);
			c(39,6,9,Mineral.GLOWSTONE); c(39,6,33,Mineral.GLOWSTONE); c(38,6,8,Mineral.GLOWSTONE); c(38,6,34,Mineral.GLOWSTONE);
			c(34,6,4,Mineral.GLOWSTONE); c(34,6,38,Mineral.GLOWSTONE); c(33,6,3,Mineral.GLOWSTONE); c(33,6,39,Mineral.GLOWSTONE);
			c(32,6,2,Mineral.GLOWSTONE); c(32,6,40,Mineral.GLOWSTONE); c(25,5,0,Mineral.GLOWSTONE); c(25,5,42,Mineral.GLOWSTONE);
			c(25,6,0,Mineral.GLOWSTONE); c(25,6,42,Mineral.GLOWSTONE); c(25,7,0,Mineral.GLOWSTONE); c(25,7,42,Mineral.GLOWSTONE);
			c(24,5,0,Mineral.GLOWSTONE); c(24,5,42,Mineral.GLOWSTONE); c(24,7,0,Mineral.GLOWSTONE); c(24,7,42,Mineral.GLOWSTONE);
			c(23,5,0,Mineral.GLOWSTONE); c(23,5,42,Mineral.GLOWSTONE); c(23,6,0,Mineral.GLOWSTONE); c(23,6,42,Mineral.GLOWSTONE);
			c(23,7,0,Mineral.GLOWSTONE); c(23,7,42,Mineral.GLOWSTONE); c(19,5,0,Mineral.GLOWSTONE); c(19,5,42,Mineral.GLOWSTONE);
			c(19,6,0,Mineral.GLOWSTONE); c(19,6,42,Mineral.GLOWSTONE); c(19,7,0,Mineral.GLOWSTONE); c(19,7,42,Mineral.GLOWSTONE);
			c(18,7,0,Mineral.GLOWSTONE); c(18,7,42,Mineral.GLOWSTONE); c(18,5,0,Mineral.GLOWSTONE); c(18,5,42,Mineral.GLOWSTONE);
			c(17,5,0,Mineral.GLOWSTONE); c(17,5,42,Mineral.GLOWSTONE); c(17,6,0,Mineral.GLOWSTONE); c(17,6,42,Mineral.GLOWSTONE);
			c(17,7,0,Mineral.GLOWSTONE); c(17,7,42,Mineral.GLOWSTONE); c(10,6,2,Mineral.GLOWSTONE); c(10,6,40,Mineral.GLOWSTONE);
			c(9,6,3,Mineral.GLOWSTONE); c(9,6,39,Mineral.GLOWSTONE); c(8,6,4,Mineral.GLOWSTONE); c(4,6,8,Mineral.GLOWSTONE);
			c(4,6,34,Mineral.GLOWSTONE); c(3,6,9,Mineral.GLOWSTONE); c(3,6,33,Mineral.GLOWSTONE); c(2,6,10,Mineral.GLOWSTONE);
			c(0,5,17,Mineral.GLOWSTONE); c(0,5,18,Mineral.GLOWSTONE); c(0,5,19,Mineral.GLOWSTONE); c(0,5,23,Mineral.GLOWSTONE);
			c(0,5,24,Mineral.GLOWSTONE); c(0,5,25,Mineral.GLOWSTONE); c(0,6,17,Mineral.GLOWSTONE); c(0,6,19,Mineral.GLOWSTONE);
			c(0,6,23,Mineral.GLOWSTONE); c(0,6,25,Mineral.GLOWSTONE); c(0,7,17,Mineral.GLOWSTONE); c(0,7,18,Mineral.GLOWSTONE);
			c(0,7,19,Mineral.GLOWSTONE); c(0,7,23,Mineral.GLOWSTONE); c(0,7,24,Mineral.GLOWSTONE); c(0,7,25,Mineral.GLOWSTONE);
			c(2,6,32,Mineral.GLOWSTONE); c(8,6,38,Mineral.GLOWSTONE); c(42,5,17,Mineral.GLOWSTONE); c(42,5,18,Mineral.GLOWSTONE);
			c(42,5,19,Mineral.GLOWSTONE); c(42,5,23,Mineral.GLOWSTONE); c(42,5,24,Mineral.GLOWSTONE); c(42,5,25,Mineral.GLOWSTONE);
			c(42,6,17,Mineral.GLOWSTONE); c(42,6,19,Mineral.GLOWSTONE); c(42,6,23,Mineral.GLOWSTONE); c(42,6,25,Mineral.GLOWSTONE);
			c(42,7,17,Mineral.GLOWSTONE); c(42,7,18,Mineral.GLOWSTONE); c(42,7,19,Mineral.GLOWSTONE); c(42,7,23,Mineral.GLOWSTONE);
			c(42,7,24,Mineral.GLOWSTONE); c(42,7,25,Mineral.GLOWSTONE); c(40,6,10,Mineral.GLOWSTONE); c(40,6,32,Mineral.GLOWSTONE);
			c(4,0,17,Mineral.REDSTONE); c(4,0,18,Mineral.REDSTONE); c(4,0,19,Mineral.REDSTONE); c(4,0,23,Mineral.REDSTONE);
			c(4,0,24,Mineral.REDSTONE); c(4,0,25,Mineral.REDSTONE); c(38,0,17,Mineral.REDSTONE); c(38,0,18,Mineral.REDSTONE);
			c(38,0,19,Mineral.REDSTONE); c(38,0,23,Mineral.REDSTONE); c(38,0,24,Mineral.REDSTONE); c(38,0,25,Mineral.REDSTONE);
			c(36,0,17,Mineral.REDSTONE); c(36,0,18,Mineral.REDSTONE); c(36,0,19,Mineral.REDSTONE); c(36,0,20,Mineral.REDSTONE);
			c(36,0,21,Mineral.REDSTONE); c(36,0,22,Mineral.REDSTONE); c(36,0,23,Mineral.REDSTONE); c(36,0,24,Mineral.REDSTONE);
			c(36,0,25,Mineral.REDSTONE); c(34,0,13,Mineral.REDSTONE); c(34,0,29,Mineral.REDSTONE); c(29,0,34,Mineral.REDSTONE);
			c(29,0,8,Mineral.REDSTONE); c(28,0,35,Mineral.REDSTONE); c(28,0,7,Mineral.REDSTONE); c(25,0,36,Mineral.REDSTONE);
			c(25,0,38,Mineral.REDSTONE); c(25,0,4,Mineral.REDSTONE); c(25,0,6,Mineral.REDSTONE); c(24,0,36,Mineral.REDSTONE);
			c(24,0,38,Mineral.REDSTONE); c(24,0,4,Mineral.REDSTONE); c(24,0,6,Mineral.REDSTONE); c(23,0,4,Mineral.REDSTONE);
			c(22,0,36,Mineral.REDSTONE); c(22,0,6,Mineral.REDSTONE); c(21,0,36,Mineral.REDSTONE); c(21,0,6,Mineral.REDSTONE);
			c(20,0,36,Mineral.REDSTONE); c(20,0,6,Mineral.REDSTONE); c(19,0,36,Mineral.REDSTONE); c(19,0,4,Mineral.REDSTONE);
			c(18,0,36,Mineral.REDSTONE); c(18,0,4,Mineral.REDSTONE); c(17,0,36,Mineral.REDSTONE); c(17,0,4,Mineral.REDSTONE);
			c(14,0,35,Mineral.REDSTONE); c(14,0,7,Mineral.REDSTONE); c(13,0,34,Mineral.REDSTONE); c(13,0,8,Mineral.REDSTONE);
			c(8,0,29,Mineral.REDSTONE); c(6,0,17,Mineral.REDSTONE); c(6,0,18,Mineral.REDSTONE); c(6,0,19,Mineral.REDSTONE);
			c(6,0,20,Mineral.REDSTONE); c(6,0,21,Mineral.REDSTONE); c(6,0,22,Mineral.REDSTONE); c(6,0,23,Mineral.REDSTONE);
			c(6,0,24,Mineral.REDSTONE); c(6,0,25,Mineral.REDSTONE); c(7,0,14,Mineral.REDSTONE); c(7,0,28,Mineral.REDSTONE);
			c(8,0,13,Mineral.REDSTONE); c(17,0,6,Mineral.REDSTONE); c(17,0,38,Mineral.REDSTONE); c(18,0,6,Mineral.REDSTONE);
			c(18,0,38,Mineral.REDSTONE); c(19,0,6,Mineral.REDSTONE); c(19,0,38,Mineral.REDSTONE); c(23,0,6,Mineral.REDSTONE);
			c(23,0,36,Mineral.REDSTONE); c(23,0,38,Mineral.REDSTONE); c(35,0,14,Mineral.REDSTONE); c(35,0,28,Mineral.REDSTONE);
			c(20,1,19,Mineral.CHROMA); c(22,1,19,Mineral.CHROMA); c(20,1,23,Mineral.CHROMA); c(22,1,23,Mineral.CHROMA);
			c(19,1,20,Mineral.CHROMA); c(19,1,22,Mineral.CHROMA); c(23,1,20,Mineral.CHROMA); c(23,1,22,Mineral.CHROMA);
		// V33a registers this one without ever placing it: the heart of the inlay is always the
		// player's to supply.
		CELLS.add(new Cell(new Vec3i(21, 3, 21), Mineral.CHROMA));
	}

	private MonumentMineralBlocks() {}

	private static void c(int x, int y, int z, Mineral m) {
		CELLS.add(new Cell(new Vec3i(x, y, z), m));
	}

	private static void g(int x, int z) {
		FIXED_GOLD.add(new Vec3i(x, 0, z));
	}

	/** Every cell the ritual expects to find, whether or not generation laid it. */
	public static List<Cell> expected() {
		return Collections.unmodifiableList(CELLS);
	}

	/** The active V33a gold ring: always generated and never part of the completion check. */
	public static List<Vec3i> fixedGold() {
		return Collections.unmodifiableList(FIXED_GOLD);
	}

	public static int chance(Mineral m) {
		return CHANCE.get(m);
	}

	/**
	 * Which cells generation should actually lay, rolled per cell against its material's chance. The
	 * centre chroma is excluded here because upstream never lays it.
	 */
	public static Map<BlockPos, Mineral> roll(BlockPos anchor, RandomSource random) {
		Map<BlockPos, Mineral> placed = new java.util.LinkedHashMap<>();
		for (int i = 0; i < CELLS.size() - 1; i++) {
			Cell cell = CELLS.get(i);
			if (random.nextInt(100) < CHANCE.get(cell.mineral()))
				placed.put(anchor.offset(cell.offset()), cell.mineral());
		}
		return placed;
	}
}
