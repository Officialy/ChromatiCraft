package reika.chromaticraft.magic.lore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.magic.ElementMixer;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.Hex;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.MapShape;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.Point;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * Deterministic V33a lore-key board. It retains the 169-cell flower, thirteen voids, 500 legal
 * shuffle moves, colour-relation validity rule and three four-cell revelations per lore tower.
 * Rendering and networking consume immutable cell views instead of being embedded in the puzzle.
 */
public final class KeyAssemblyPuzzle {

	public static final int SIZE = 15;
	public static final int CELL_SIZE = 15;
	public static final int TOWER_COUNT = 13;
	public static final int GROUPS_PER_TOWER = 3;
	public static final int GROUP_SIZE = 4;
	public static final int REQUIRED_CELLS = TOWER_COUNT * GROUPS_PER_TOWER * GROUP_SIZE;
	public static final int VOID_COUNT = 169 - REQUIRED_CELLS;

	private final HexGrid grid = new HexGrid(SIZE, CELL_SIZE, true, MapShape.HEXAGON).flower();
	private final Map<Hex, Cell> cells = new HashMap<>();
	private final Set<Hex> empty = new HashSet<>();
	private final long seed;

	private KeyAssemblyPuzzle(long seed) {
		this.seed = seed;
		for (Hex hex : grid.getAllHexes()) cells.put(hex, new Cell(hex));
		generate(new Random(seed));
	}

	public static KeyAssemblyPuzzle generate(long seed) {
		return new KeyAssemblyPuzzle(seed);
	}

	public static long calcSeed(long worldSeed, UUID player) {
		return ReikaMathLibrary.cantorCombine(worldSeed, player.getMostSignificantBits(),
				player.getLeastSignificantBits());
	}

	public long seed() {
		return seed;
	}

	private void generate(Random random) {
		List<Hex> order = new ArrayList<>(grid.getAllHexes());
		order.sort(KeyAssemblyPuzzle::compareHex);
		for (Hex hex : order) cells.get(hex).color = generateColor(hex, random);
		Collections.shuffle(order, random);
		for (int i = 0; i < VOID_COUNT; i++) {
			Cell cell = cells.get(order.get(i));
			cell.color = null;
			empty.add(cell.hex);
		}
		for (int i = 0; i < 500; i++) {
			List<Hex> holes = new ArrayList<>(empty);
			Hex hole = holes.get(random.nextInt(holes.size()));
			List<Hex> sources = occupiedNeighbours(hole);
			if (!sources.isEmpty()) moveInternal(sources.get(random.nextInt(sources.size())), hole);
		}
		assignTowerGroups(random);
	}

	private CrystalElement generateColor(Hex hex, Random random) {
		if (random.nextInt(32) == 0) return CrystalElement.BROWN;
		Set<CrystalElement> related = new HashSet<>();
		for (Hex neighbour : hex.getNeighbors()) {
			Cell cell = cells.get(neighbour);
			if (cell != null && cell.color != null)
				related.addAll(ElementMixer.instance.getRelatedColors(cell.color));
		}
		if (related.isEmpty()) return CrystalElement.elements[random.nextInt(CrystalElement.elements.length)];
		List<CrystalElement> sorted = new ArrayList<>(related);
		sorted.sort(java.util.Comparator.comparingInt(Enum::ordinal));
		return sorted.get(random.nextInt(sorted.size()));
	}

	private void assignTowerGroups(Random random) {
		List<Hex> occupied = new ArrayList<>();
		for (Cell cell : cells.values()) if (cell.color != null) occupied.add(cell.hex);
		occupied.sort(KeyAssemblyPuzzle::compareHex);
		Collections.shuffle(occupied, random);
		for (Towers tower : Towers.towerList) {
			for (int group = 0; group < GROUPS_PER_TOWER; group++) {
				for (int member = 0; member < GROUP_SIZE; member++) {
					int index = tower.ordinal() * GROUPS_PER_TOWER * GROUP_SIZE + group * GROUP_SIZE + member;
					cells.get(occupied.get(index)).tower = tower;
				}
			}
		}
	}

	/** Moves a tile into an adjacent void, matching V33a's click action. */
	public boolean move(int q, int r, int s) {
		Hex source = grid.getHex(q, r, s);
		if (source == null || cells.get(source).color == null) return false;
		for (Hex neighbour : source.getNeighbors()) {
			if (empty.contains(neighbour)) {
				moveInternal(source, neighbour);
				return true;
			}
		}
		return false;
	}

	public void applyPackedMoves(int[] moves) {
		for (int value : moves)
			this.move((value >> 8 & 15) - 8, (value >> 4 & 15) - 8, (value & 15) - 8);
	}

	private void moveInternal(Hex source, Hex target) {
		Cell from = cells.get(source);
		Cell to = cells.get(target);
		to.color = from.color;
		to.tower = from.tower;
		from.color = null;
		from.tower = null;
		empty.remove(target);
		empty.add(source);
	}

	public boolean isComplete() {
		for (Cell cell : cells.values())
			if (cell.color != null && !isValid(cell)) return false;
		return true;
	}

	private boolean isValid(Cell cell) {
		if (!ElementMixer.instance.hasMixes(cell.color)) return true;
		for (Hex neighbour : cell.hex.getNeighbors()) {
			Cell adjacent = cells.get(neighbour);
			if (adjacent != null && adjacent.color != null
					&& ElementMixer.instance.related(cell.color, adjacent.color)) return true;
		}
		return false;
	}

	public List<CellView> cells(int scannedMask) {
		List<CellView> result = new ArrayList<>(cells.size());
		for (Cell cell : cells.values()) {
			Point point = grid.getHexLocation(cell.hex);
			boolean known = cell.color == null || cell.tower == null
					|| (scannedMask & 1 << cell.tower.ordinal()) != 0;
			result.add(new CellView(cell.hex.q, cell.hex.r, cell.hex.s, point.x, point.y,
					cell.color, cell.color != null && isValid(cell), known, cell.tower));
		}
		result.sort((a, b) -> {
			int y = Double.compare(a.y, b.y);
			return y != 0 ? y : Double.compare(a.x, b.x);
		});
		return List.copyOf(result);
	}

	public CellView cellAt(double x, double y, int scannedMask) {
		Hex hex = grid.getHexAtLocation((int)Math.round(x), (int)Math.round(y));
		if (hex == null) return null;
		for (CellView view : cells(scannedMask))
			if (view.q == hex.q && view.r == hex.r && view.s == hex.s) return view;
		return null;
	}

	public static int scannedMask(Player player) {
		int mask = 0;
		for (Towers tower : Towers.towerList)
			if (LoreTowerProgress.hasScanned(player, tower)) mask |= 1 << tower.ordinal();
		return mask;
	}

	private List<Hex> occupiedNeighbours(Hex hole) {
		List<Hex> list = new ArrayList<>();
		for (Hex neighbour : hole.getNeighbors()) {
			Cell cell = cells.get(neighbour);
			if (cell != null && cell.color != null) list.add(neighbour);
		}
		list.sort(KeyAssemblyPuzzle::compareHex);
		return list;
	}

	private static int compareHex(Hex a, Hex b) {
		int q = Integer.compare(a.q, b.q);
		int r = q == 0 ? Integer.compare(a.r, b.r) : q;
		return r == 0 ? Integer.compare(a.s, b.s) : r;
	}

	private static final class Cell {
		private final Hex hex;
		private CrystalElement color;
		private Towers tower;
		private Cell(Hex hex) { this.hex = hex; }
	}

	public record CellView(int q, int r, int s, double x, double y, CrystalElement color,
			boolean active, boolean known, Towers tower) {}
}
