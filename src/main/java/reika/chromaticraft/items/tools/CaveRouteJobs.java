package reika.chromaticraft.items.tools;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import reika.chromaticraft.block.BlockChromaTrail.TileChromaTrail;
import reika.chromaticraft.registry.ChromaBlocks;

/** Server-thread jobs share a single time/work budget across players and dimensions. */
@EventBusSubscriber(modid = "chromaticraft")
public final class CaveRouteJobs {
    private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();
    private static final int MAX_JOBS = 4, MAX_NODES = 262_144, WORK_PER_TICK = 1024;
    private static final long NANOS_PER_TICK = 4_000_000;
    private CaveRouteJobs() {}

    public static void start(ServerLevel level, ServerPlayer player, boolean deep) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Cave route requires server thread");
        JOBS.removeIf(job -> job.player.equals(player.getUUID()));
        if (JOBS.size() >= MAX_JOBS) { message(player, "chromaticraft.cave_route.busy"); return; }
        JOBS.addLast(new Job(level, player.getUUID(), player.blockPosition(), deep));
        message(player, "chromaticraft.cave_route.searching");
    }
    private static void message(ServerPlayer player, String key) { player.sendOverlayMessage(Component.translatable(key)); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) { runTick(event.getServer()); }
    public static void runTick(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Cave route requires server thread");
        long deadline = System.nanoTime() + NANOS_PER_TICK;
        int remaining = WORK_PER_TICK;
        while (!JOBS.isEmpty() && remaining > 0 && System.nanoTime() < deadline) {
            Job job = JOBS.removeFirst();
            ServerPlayer player = server.getPlayerList().getPlayer(job.player);
            if (player == null || player.level() != job.level || !player.isAlive()) continue;
            int used = job.advance(Math.min(32, remaining), deadline, player);
            remaining -= Math.max(1, used);
            if (!job.finished) JOBS.addLast(job);
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) JOBS.removeIf(job -> job.level == level);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { JOBS.clear(); }

    private static final class Job implements CaveRouteSearch.Terrain {
        final ServerLevel level;
        final UUID player;
        final CaveRouteSearch search;
        final boolean deep;
        List<BlockPos> route;
        int validationIndex, waypointIndex;
        boolean finished;
        Job(ServerLevel level, UUID player, BlockPos start, boolean deep) {
            this.level = level; this.player = player; this.deep = deep;
            search = new CaveRouteSearch(start, deep, level.getMinY(), level.getMaxY(), MAX_NODES);
        }
        @Override public boolean accessible(BlockPos pos) { return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos); }
        @Override public boolean passable(BlockPos pos) { return ItemCaveExitFinder.isPassable(level, pos); }
        @Override public boolean seesSky(BlockPos pos) { return level.canSeeSky(pos); }
        void fail(ServerPlayer player, String key) { finished = true; message(player, key); }
        int advance(int budget, long deadline, ServerPlayer player) {
            int used = 0;
            while (used < budget && !finished && System.nanoTime() < deadline) {
                if (route == null) {
                    used += search.advance(this, 1);
                    switch (search.status()) {
                        case FOUND -> route = search.route();
                        case NO_ROUTE -> fail(player, "chromaticraft.cave_route.no_route");
                        case TOO_LARGE -> fail(player, "chromaticraft.cave_route.too_large");
                        default -> {}
                    }
                } else if (validationIndex < route.size()) {
                    BlockPos pos = route.get(validationIndex++); used++;
                    if (!accessible(pos) || (!passable(pos) && !(validationIndex == route.size() && !deep && seesSky(pos))))
                        fail(player, "chromaticraft.cave_route.changed");
                } else {
                    // Original every-fourth-block spacing, plus the exit, including links across non-air points.
                    int last = route.size() - 1;
                    int index = Math.min(waypointIndex * 4, last);
                    BlockPos pos = route.get(index); used += 64; // At most sixteen block updates per server tick globally.
                    if (!accessible(pos)) { fail(player, "chromaticraft.cave_route.changed"); continue; }
                    BlockPos prev = waypointIndex > 0 ? route.get((waypointIndex - 1) * 4) : null;
                    BlockPos next = index < last ? route.get(Math.min(index + 4, last)) : null;
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, ChromaBlocks.CHROMA_TRAIL.get().defaultBlockState(), 3);
                        if (level.getBlockEntity(pos) instanceof TileChromaTrail trail) trail.setData(prev, next);
                    }
                    waypointIndex++;
                    if (index == last) { finished = true; message(player, "chromaticraft.cave_route.found"); }
                }
            }
            return used;
        }
    }
}
