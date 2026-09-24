package reika.chromaticraft.magic.lore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.magic.progression.ProgressAccess;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * Server-authoritative persistence and replay boundary for V33a's lore-key assembly.
 *
 * <p>It is also a {@link ProgressAccess}, as upstream's is: finishing the lore board is one of the
 * gates on the player's elemental buffer capacity. That check reads a persisted flag rather than
 * replaying the puzzle, so unlike the rest of this class it is safe on either side.
 */
public final class LoreManager implements ProgressAccess {

	public static final LoreManager instance = new LoreManager();
	private static final String TAG = "loretowers";
	private static final String MOVES = "puzzle_moves";

	private LoreManager() {}

	public KeyAssemblyPuzzle puzzle(Player player) {
		if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level))
			throw new IllegalStateException("Lore puzzle authority is server-side");
		long seed = KeyAssemblyPuzzle.calcSeed(level.getSeed(), player.getUUID());
		KeyAssemblyPuzzle puzzle = KeyAssemblyPuzzle.generate(seed);
		for (int packed : moves(player))
			puzzle.move(unpackQ(packed), unpackR(packed), unpackS(packed));
		return puzzle;
	}

	public PuzzleState state(Player player) {
		KeyAssemblyPuzzle puzzle = puzzle(player);
		return new PuzzleState(puzzle.seed(), KeyAssemblyPuzzle.scannedMask(player),
				hasPlayerCompletedBoard(player), moves(player));
	}

	/** Validates against a replay of the authoritative seed before persisting the move. */
	public PuzzleState move(ServerPlayer player, int q, int r, int s) {
		if (hasPlayerCompletedBoard(player)) return state(player);
		KeyAssemblyPuzzle puzzle = puzzle(player);
		if (puzzle.move(q, r, s)) {
			List<Integer> moves = new ArrayList<>(moves(player));
			moves.add(pack(q, r, s));
			int[] encoded = moves.stream().mapToInt(Integer::intValue).toArray();
			tag(player).putIntArray(MOVES, encoded);
			if (puzzle.isComplete()) setBoardCompletion(player, true);
			try {
				ReikaPlayerAPI.syncCustomData(player);
			}
			catch (Exception ignored) {
				// GameTest mock players do not negotiate the custom-data channel.
			}
		}
		return state(player);
	}

	@Override
	public boolean playerHas(Player ep) {
		return this.hasPlayerCompletedBoard(ep);
	}

	public boolean hasPlayerCompletedBoard(Player player) {
		return tag(player).getBooleanOr("complete", false);
	}

	public void setBoardCompletion(Player player, boolean complete) {
		tag(player).putBoolean("complete", complete);
	}

	/** V33a {@code initTowers}: lay out the tower positions the first time a level asks. */
	public void initTowers(net.minecraft.server.level.ServerLevel world) {
		if (!Towers.initialized(world))
			Towers.loadPositions(world, 64 * 16 * 2);
	}

	/**
	 * V33a {@code getNearestActiveTower}: the closest tower, by its root or generated position, whose data
	 * node this player has not yet scanned. Block coordinates.
	 */
	public Towers getNearestActiveTower(net.minecraft.server.level.ServerLevel world, double x, double z, Player ep) {
		this.initTowers(world);
		Towers ret = null;
		double mind = Double.POSITIVE_INFINITY;
		for (Towers t : Towers.towerList) {
			net.minecraft.world.level.ChunkPos p = t.getRootPosition();
			net.minecraft.core.BlockPos p2 = t.getGeneratedLocation();
			if (p2 != null && ep != null && world.getBlockEntity(p2)
					instanceof reika.chromaticraft.tileentity.TileEntityDataNode te && te.hasBeenScanned(ep))
				continue;
			double d1 = Math.hypot(x - p.x(), z - p.z());
			double d2 = p2 != null ? Math.hypot(x - p2.getX(), z - p2.getZ()) : Double.POSITIVE_INFINITY;
			double d = Math.min(d1, d2);
			if (ret == null || d < mind) {
				ret = t;
				mind = d;
			}
		}
		return ret;
	}

	public void resetBoard(Player player) {
		tag(player).remove(MOVES);
		tag(player).putBoolean("complete", false);
	}

	private static List<Integer> moves(Player player) {
		int[] array = tag(player).getIntArray(MOVES).orElseGet(() -> new int[0]);
		List<Integer> result = new ArrayList<>(array.length);
		for (int value : array) result.add(value);
		return List.copyOf(result);
	}

	private static CompoundTag tag(Player player) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag lore = NBTCompat.getCompound(root, TAG);
		root.put(TAG, lore);
		return lore;
	}

	private static int pack(int q, int r, int s) {
		return (q + 8 & 15) << 8 | (r + 8 & 15) << 4 | s + 8 & 15;
	}

	private static int unpackQ(int value) { return (value >> 8 & 15) - 8; }
	private static int unpackR(int value) { return (value >> 4 & 15) - 8; }
	private static int unpackS(int value) { return (value & 15) - 8; }

	public record PuzzleState(long seed, int scannedMask, boolean complete, List<Integer> moves) {}
}
