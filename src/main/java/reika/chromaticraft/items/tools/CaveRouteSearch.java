package reika.chromaticraft.items.tools;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Incremental BFS: one predecessor per discovered position, rather than a copied path per branch. */
public final class CaveRouteSearch {
    public enum Status { SEARCHING, RECONSTRUCTING, FOUND, NO_ROUTE, TOO_LARGE }
    public interface Terrain {
        boolean accessible(BlockPos pos);
        boolean passable(BlockPos pos);
        boolean seesSky(BlockPos pos);
    }
    private static final Direction[] NEIGHBORS = Direction.values();
    private final BlockPos start;
    private final boolean deep;
    private final int minY, maxY, maxNodes;
    private final Long2LongOpenHashMap predecessors = new Long2LongOpenHashMap();
    private final LongArrayFIFOQueue frontier = new LongArrayFIFOQueue();
    private final ArrayList<BlockPos> reversePath = new ArrayList<>();
    private BlockPos deepest;
    private long trace;
    private Status status = Status.SEARCHING;

    public CaveRouteSearch(BlockPos start, boolean deep, int minY, int maxY, int maxNodes) {
        if (maxNodes < 1 || minY > maxY) throw new IllegalArgumentException("Invalid cave search bounds");
        this.start = start.immutable(); this.deep = deep; this.minY = minY; this.maxY = maxY; this.maxNodes = maxNodes;
        deepest = this.start;
        predecessors.put(start.asLong(), start.asLong()); frontier.enqueue(start.asLong());
        if (deep && start.getY() == minY) reconstruct(this.start);
    }
    public Status status() { return status; }
    public int visitedCount() { return predecessors.size(); }
    public List<BlockPos> route() { return status == Status.FOUND ? java.util.Collections.unmodifiableList(reversePath.reversed()) : List.of(); }
    private boolean inside(BlockPos pos) {
        return pos.getY() >= minY && pos.getY() <= maxY && (!deep || (pos.getY() <= start.getY()
                && Math.abs((long)pos.getX() - start.getX()) <= 400 && Math.abs((long)pos.getZ() - start.getZ()) <= 400));
    }
    private void reconstruct(BlockPos target) { trace = target.asLong(); status = Status.RECONSTRUCTING; }

    /** Budget includes path reconstruction. World queries happen only after the non-loading accessibility check. */
    public int advance(Terrain terrain, int budget) {
        int used = 0;
        while (used < budget && (status == Status.SEARCHING || status == Status.RECONSTRUCTING)) {
            used++;
            if (status == Status.RECONSTRUCTING) {
                reversePath.add(BlockPos.of(trace));
                if (trace == start.asLong()) status = Status.FOUND;
                else trace = predecessors.get(trace);
                continue;
            }
            if (frontier.isEmpty()) { if (deep) reconstruct(deepest); else status = Status.NO_ROUTE; continue; }
            BlockPos current = BlockPos.of(frontier.dequeueLong());
            if (!terrain.accessible(current)) continue; // A chunk may unload between slices.
            for (Direction direction : NEIGHBORS) {
                BlockPos next = current.relative(direction);
                long key = next.asLong();
                if (!inside(next) || predecessors.containsKey(key) || !terrain.accessible(next)) continue;
                boolean exit = !deep && terrain.seesSky(next);
                if (!exit && !terrain.passable(next)) continue;
                if (predecessors.size() >= maxNodes) { status = Status.TOO_LARGE; return used; }
                predecessors.put(key, current.asLong());
                if (exit) { reconstruct(next); break; }
                if (next.getY() < deepest.getY()) deepest = next;
                if (deep && next.getY() == minY) { reconstruct(next); break; } // Absolute minimum; BFS already gives its shortest route.
                frontier.enqueue(key);
            }
        }
        return used;
    }
}
