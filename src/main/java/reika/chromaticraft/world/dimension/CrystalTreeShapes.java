package reika.chromaticraft.world.dimension;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ProximaDecoTypes;
import reika.dragonapi.instantiable.RevolvedPattern;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.libraries.ReikaDirectionHelper;

/**
 * V33a {@code WorldGenCrystalTree.CrystalTree}: the twelve crown layouts Proxima's crystal forest is
 * built from, each stamped into a {@link FilledBlockArray} a block at a time.
 *
 * <p>Every layout is upstream's geometry transcribed unchanged — the diamond and square tests, the
 * radius tables, the hand-placed cells. The only substitutions are the two block identities: the trunk
 * is Stone Shielding ({@code WorldGenCrystalTree.CRYSTAL_TRUNK}) and the foliage is Crystal Leaves.
 *
 * <p>{@link #XMAS} is present but never registered, exactly as upstream leaves it: its radius table is
 * empty, so it would generate a bare nineteen-block trunk and nothing else, and V33a's static block
 * skips it with {@code if (tree != CrystalTree.XMAS)}. Keeping the constant preserves the layout's
 * place in the size tables and makes clear it is unfinished upstream rather than dropped here.
 *
 * <p>{@link #treeSize} is which of the four size classes a layout belongs to, and {@link #trunkWidth}
 * how many blocks across its trunk is — the latter also being what a {@link RevolvedPattern} needs to
 * mirror its quadrants either side of the trunk instead of over it.
 */
public enum CrystalTreeShapes {

	VANILLA(0, 1),
	DYE(1, 1),
	CLIPPED(0, 1),
	ARCH(0, 1),
	UMBRELLA(1, 1),
	GROVE(0, 1),
	GROVE2(0, 1),
	RINGS(1, 1),
	BOWL(1, 1),
	WAVE(2, 2),
	FIR(2, 1),
	/** Unfinished upstream: an empty radius table, and never registered. See the class documentation. */
	XMAS(3, 2),
	CANOPY(3, 2);

	public static final CrystalTreeShapes[] list = values();

	/** V33a treeHeights, indexed by {@link #treeSize}: the trunk height range each class is given. */
	public static final int[][] TREE_HEIGHTS = {{2, 5}, {3, 7}, {4, 10}, {12, 30}};

	public final int treeSize;
	public final int trunkWidth;

	CrystalTreeShapes(int treeSize, int trunkWidth) {
		this.treeSize = treeSize;
		this.trunkWidth = trunkWidth;
	}

	/** V33a's static block registers every layout but XMAS. */
	public boolean isRegistered() {
		return this != XMAS;
	}

	private static BlockKey trunk() {
		return new BlockKey(ChromaBlocks.shielding(ChromaShieldTypes.STONE).get().defaultBlockState());
	}

	private static BlockKey leaf() {
		return new BlockKey(ChromaBlocks.deco(ProximaDecoTypes.CRYSTALLEAF).get().defaultBlockState());
	}

	/** V33a {@code populate}: stamps this layout's crown into the array, centred on the origin. */
	public void populate(FilledBlockArray f, Level world) {
		BlockKey t = trunk();
		BlockKey l = leaf();
		switch (this) {
			case VANILLA, CLIPPED, ARCH -> {
				for (int i = 0; i <= 2; i++) {
					int r = i == 2 ? 1 : 2;
					for (int a = -r; a <= r; a++)
						for (int b = -r; b <= r; b++)
							if (this == VANILLA || (this == ARCH && i == 0)
									|| Math.abs(a) != 2 || Math.abs(b) != 2)
								if (this != ARCH || i == 2 || (a == 0 && b == 0)
										|| Math.abs(a) > 1 || Math.abs(b) > 1)
									f.setBlock(a, i, b, a != 0 || b != 0 ? l : t);
				}
				spire(f, l, 3);
			}
			case DYE -> {
				int[] r = {1, 2, 1, 2, 1, 2, 1};
				for (int i = 0; i < r.length; i++)
					for (int a = -r[i]; a <= r[i]; a++)
						for (int b = -r[i]; b <= r[i]; b++)
							f.setBlock(a, i, b, a != 0 || b != 0 ? l : t);
				spire(f, l, r.length);
			}
			case UMBRELLA -> umbrella(f, t, l);
			case GROVE, GROVE2 -> {
				int[] r = this == GROVE2 ? new int[] {1, 2, 2, 2, 1} : new int[] {1, 2, 2, 2, 2, 1};
				for (int h = 0; h < r.length; h++)
					for (int a = -2; a <= 2; a++)
						for (int b = -2; b <= 2; b++)
							if (Math.abs(a) + Math.abs(b) <= r[h])
								// GROVE2 pinches its second layer in at the corners.
								if (this == GROVE || h != 1 || Math.abs(a) <= 1 || Math.abs(b) <= 1)
									f.setBlock(a, h, b, a != 0 || b != 0 ? l : t);
				f.setBlock(0, r.length, 0, l);
			}
			case RINGS -> rings(f, t, l);
			case BOWL -> bowl(f, t, l);
			case WAVE -> revolved(f, world, 13, 5, 12, 11, new int[][] {
					{4, 4, 4, 3, 1},
					{5, 5, 5, 4, 3, 1},
					{4, 4, 4, 3, 1},
					{2, 3, 2},
					{2, 2, 1},
					{4, 4, 4, 3, 1},
					{5, 5, 5, 4, 3},
					{4, 4, 4, 3, 1},
					{3, 3, 3, 1},
					{2, 2, 1},
					{2, 1},
					{1}});
			case FIR -> fir(f, t, l);
			// V33a's radius table for XMAS is empty and the layout is never registered.
			case XMAS -> revolved(f, world, 19, 5, 19, 19, new int[][] {});
			case CANOPY -> revolved(f, world, 7, 7, 7, 6, new int[][] {
					{5, 5, 5, 4, 3, 1},
					{5, 6, 6, 5, 4, 3},
					{5, 5, 5, 4, 3, 1},
					{4, 5, 4, 3, 2},
					{3, 4, 3, 2},
					{2, 3, 2},
					{1, 1}});
		}
	}

	/** The five-block cross several layouts finish with: a centre and its four neighbours. */
	private static void spire(FilledBlockArray f, BlockKey l, int y) {
		f.setBlock(0, y, 0, l);
		f.setBlock(1, y, 0, l);
		f.setBlock(-1, y, 0, l);
		f.setBlock(0, y, 1, l);
		f.setBlock(0, y, -1, l);
	}

	/**
	 * A {@link RevolvedPattern} layout: a trunk column, then a quadrant described row by row, mirrored.
	 *
	 * @param trunkTop the last trunk layer; above it the column is foliage
	 */
	private void revolved(FilledBlockArray f, Level world, int height, int size, int trunkHeight,
			int trunkTop, int[][] rows) {
		RevolvedPattern pattern = new RevolvedPattern(world, trunkWidth, height, size);
		for (int i = 0; i < trunkHeight; i++)
			pattern.addBlock(i < trunkTop ? trunk() : leaf(), i, 0, 0);
		for (int h = 0; h < rows.length; h++) {
			int[] row = rows[h];
			for (int d = 0; d < row.length; d++) {
				// The first column starts one out so the trunk is not overwritten by its own crown.
				int start = d == 0 ? 1 : 0;
				for (int a = start; a < start + row[d]; a++)
					pattern.addBlock(leaf(), h, a, d);
			}
		}
		pattern.calculate();
		pattern.populate(f);
	}

	private static void umbrella(FilledBlockArray f, BlockKey t, BlockKey l) {
		for (int h = 0; h <= 1; h++) {
			for (int b = -1; b <= 1; b++) {
				f.setBlock(-4, h, b, l);
				f.setBlock(4, h, b, l);
			}
			for (int b = 2; b <= 3; b++) {
				f.setBlock(-3, h, -b, l);
				f.setBlock(-3, h, b, l);
				f.setBlock(3, h, -b, l);
				f.setBlock(3, h, b, l);
			}
			if (h == 1)
				for (int b = -1; b <= 1; b++) {
					f.setBlock(-3, h, b, l);
					f.setBlock(3, h, b, l);
				}
			for (int i = 2; i <= 6; i++) {
				// The middle of each side runs one block wider than its ends.
				int inset = i > 2 && i < 6 ? 0 : 1;
				f.setBlock(i - 4, h, -4 + inset, l);
				f.setBlock(i - 4, h, 4 - inset, l);
				if (h == 1) {
					f.setBlock(i - 4, h, -3 + inset, l);
					f.setBlock(i - 4, h, 3 - inset, l);
				}
			}
		}
		for (int a = -3; a <= 3; a++)
			for (int b = -3; b <= 3; b++)
				if (Math.abs(a) + Math.abs(b) <= 4)
					f.setBlock(a, 2, b, l);
		for (int a = -1; a <= 1; a++)
			for (int b = -1; b <= 1; b++)
				f.setBlock(a, 3, b, l);
		for (int h = 0; h < 3; h++)
			f.setBlock(0, h, 0, t);
	}

	private static void rings(FilledBlockArray f, BlockKey t, BlockKey l) {
		for (int h = 0; h <= 1; h++)
			for (int a = -2; a <= 2; a++)
				for (int b = -2; b <= 2; b++)
					if (Math.abs(a) + Math.abs(b) <= h + 1)
						f.setBlock(a, h, b, a != 0 || b != 0 ? l : t);
		for (int h = 2; h <= 6; h++) {
			if (h % 2 == 0) {
				// The wide rings: a seven-square with its corners cut, plus four spurs.
				for (int a = -3; a <= 3; a++)
					for (int b = -3; b <= 3; b++)
						if (Math.abs(a) < 3 || Math.abs(b) < 3)
							f.setBlock(a, h, b, a != 0 || b != 0 ? l : t);
				f.setBlock(-4, h, 0, l);
				f.setBlock(4, h, 0, l);
				f.setBlock(0, h, 4, l);
				f.setBlock(0, h, -4, l);
			}
			else {
				for (int a = -3; a <= 3; a++)
					for (int b = -3; b <= 3; b++)
						if (Math.abs(a) + Math.abs(b) <= 3)
							f.setBlock(a, h, b, a != 0 || b != 0 ? l : t);
			}
		}
		for (int a = -2; a <= 2; a++)
			for (int b = -2; b <= 2; b++)
				if (Math.abs(a) < 2 || Math.abs(b) < 2)
					f.setBlock(a, 7, b, a != 0 || b != 0 ? l : t);
		for (int a = -1; a <= 1; a++)
			for (int b = -1; b <= 1; b++)
				f.setBlock(a, 8, b, l);
	}

	private static void bowl(FilledBlockArray f, BlockKey t, BlockKey l) {
		// V33a authors the bowl's wall as a literal seven-by-seven slice, repeated three layers up.
		BlockKey[][] slice = {
				{null, null, l, l, l, null, null},
				{null, l, null, null, null, l, null},
				{l, null, null, null, null, null, l},
				{l, null, null, t, null, null, l},
				{l, null, null, null, null, null, l},
				{null, l, null, null, null, l, null},
				{null, null, l, l, l, null, null},
		};
		for (int h = 0; h <= 2; h++)
			for (int a = 0; a < slice.length; a++)
				for (int b = 0; b < slice.length; b++)
					if (slice[a][b] != null)
						f.setBlock(a - 3, h, b - 3, slice[a][b]);
		for (int a = -2; a <= 2; a++)
			for (int b = -2; b <= 2; b++)
				if (Math.abs(a) < 2 || Math.abs(b) < 2)
					f.setBlock(a, 3, b, a != 0 || b != 0 ? l : t);
		spire(f, l, 4);
		// The eight cells that close the bowl's rim into a ring.
		for (int sign = -1; sign <= 1; sign += 2)
			for (int other = -1; other <= 1; other += 2) {
				f.setBlock(sign, 2, other * 2, l);
				f.setBlock(sign * 2, 2, other, l);
			}
	}

	private static void fir(FilledBlockArray f, BlockKey t, BlockKey l) {
		int[] diamond = {3, 2, 2, 4, 5, 4, 2, 3, 2, 1};
		int[] square = {3, 2, 2, 3, 4, 3, 2, 3, 2, 1};
		for (int h = 0; h < diamond.length; h++) {
			int r = square[h];
			for (int a = -r; a <= r; a++)
				for (int b = -r; b <= r; b++)
					if (Math.abs(a) + Math.abs(b) <= diamond[h])
						f.setBlock(a, h, b, a != 0 || b != 0 ? l : t);
		}
		f.setBlock(0, diamond.length, 0, l);
		// The four buttressed roots and boughs, one per horizontal direction. V33a indexes
		// ForgeDirection 2..5, which is north, south, west, east in that order.
		for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
			Direction left = ReikaDirectionHelper.getLeftBy90(dir);
			for (int d = 0; d < 2; d++) {
				f.setBlock(d * dir.getStepX(), 0, d * dir.getStepZ(), t);
				f.setBlock(d * dir.getStepX(), 8, d * dir.getStepZ(), t);
			}
			for (int d = 0; d < 3; d++)
				f.setBlock(d * dir.getStepX(), 4, d * dir.getStepZ(), t);
			f.setBlock(3 * dir.getStepX() + left.getStepX(), 4, 3 * dir.getStepZ() + left.getStepZ(), t);
			f.setBlock(3 * dir.getStepX() - left.getStepX(), 4, 3 * dir.getStepZ() - left.getStepZ(), t);
		}
	}
}
