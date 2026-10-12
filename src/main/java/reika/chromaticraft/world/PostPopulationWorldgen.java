package reika.chromaticraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.world.OverworldStructureFeature.Type;
import reika.chromaticraft.world.luminous.LuminousIslandPiece;
import reika.dragonapi.auxiliary.trackers.BoundedPopulation;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saved post-population work. Workers only enqueue; the server loads its bounded footprint with
 * asynchronous loading tickets and writes only after every required chunk is FULL. One job runs per
 * level, and one dungeon candidate chunk per tick. Exploration, an explicit forced chunk or a finite
 * pregeneration scope admits job origins; loading support chunks never extends a scope.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class PostPopulationWorldgen extends SavedData {
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(
            BuiltInRegistries.TICKET_TYPE, ChromatiCraft.MODID);
    private static final DeferredHolder<TicketType, TicketType> TICKET = TICKETS.register(
            "post_population", () -> new TicketType(200, TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    public static final Codec<PostPopulationWorldgen> CODEC = CompoundTag.CODEC.xmap(
            PostPopulationWorldgen::new, PostPopulationWorldgen::save);
    private static final SavedDataType<PostPopulationWorldgen> DATA_TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "post_population"),
            PostPopulationWorldgen::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    private static final Map<ServerLevel, PostPopulationWorldgen> CACHE = new ConcurrentHashMap<>();
    private final Map<String, Job> jobs = new TreeMap<>();
    private final Map<String, Job> pending = new TreeMap<>();
    private Job active;
    private final reika.dragonapi.instantiable.worldgen.PopulationChunkLoader loading = new reika.dragonapi.instantiable.worldgen.PopulationChunkLoader();

    private static final class Job {
        String id;
        ChunkPos origin;
        int radius, cursor, tries;
        Type dungeon;
        reika.chromaticraft.magic.lore.Towers tower;
        SavedWorldgenRandom random;
        CompoundTag island;
        boolean finished;
        private java.util.List<ChunkPos> retryChunks;

        java.util.List<ChunkPos> retryChunks() {
            if (retryChunks == null) retryChunks = DungeonRetryPlan.chunks(dungeon, origin);
            return retryChunks;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id);
            tag.putInt("X", origin.x()); tag.putInt("Z", origin.z());
            tag.putInt("Radius", radius); tag.putInt("Cursor", cursor); tag.putInt("Tries", tries);
            tag.putBoolean("Finished", finished);
            if (dungeon != null) {
                tag.putString("Dungeon", dungeon.name());
                tag.put("Random", random.save());
            } else if (tower != null) {
                tag.putString("Tower", tower.name());
                tag.put("Random", random.save());
            } else tag.put("Island", island.copy());
            return tag;
        }

        static Job load(CompoundTag tag) {
            Job job = new Job();
            job.id = tag.getStringOr("Id", "");
            job.origin = new ChunkPos(tag.getIntOr("X", 0), tag.getIntOr("Z", 0));
            job.radius = tag.getIntOr("Radius", 0); job.cursor = tag.getIntOr("Cursor", 0);
            job.tries = tag.getIntOr("Tries", 20); job.finished = tag.getBooleanOr("Finished", false);
            if (tag.contains("Dungeon")) {
                job.dungeon = Type.valueOf(tag.getStringOr("Dungeon", ""));
                job.random = SavedWorldgenRandom.load(tag.getCompoundOrEmpty("Random"));
            } else if (tag.contains("Tower")) {
                job.tower = reika.chromaticraft.magic.lore.Towers.valueOf(tag.getStringOr("Tower", ""));
                job.random = SavedWorldgenRandom.load(tag.getCompoundOrEmpty("Random"));
            } else job.island = tag.getCompoundOrEmpty("Island").copy();
            return job;
        }
    }

    private PostPopulationWorldgen() {}

    private PostPopulationWorldgen(CompoundTag tag) {
        for (var entry : tag.getListOrEmpty("Jobs")) {
            if (entry instanceof CompoundTag record) {
                Job job = Job.load(record);
                jobs.put(job.id, job);
                if (!job.finished) pending.put(job.id, job);
            }
        }
    }

    public static PostPopulationWorldgen get(ServerLevel level) {
        var data = CACHE.computeIfAbsent(level, l -> l.getDataStorage().computeIfAbsent(DATA_TYPE));
        BoundedPopulation.get(level).registerQueue("chromaticraft", data::pendingIn);
        return data;
    }

    private synchronized long pendingIn(java.util.function.Predicate<ChunkPos> origins) {
        return pending.values().stream().filter(job -> origins.test(job.origin)).count();
    }

    private synchronized CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Job job : jobs.values()) list.add(job.save());
        tag.put("Jobs", list);
        return tag;
    }

    public synchronized void enqueueDungeon(Type type, ChunkPos origin, long seed) {
        Job job = new Job();
        job.id = "dungeon/" + type + "/" + origin.x() + "/" + origin.z();
        job.origin = origin; job.dungeon = type;
        job.radius = 3; // The current candidate's structure, annexes and neighbouring site checks.
        job.random = new SavedWorldgenRandom(seed);
        job.tries = ChromaOptions.getStructureTriesPerChunk();
        if (jobs.putIfAbsent(job.id, job) == null) {
            pending.put(job.id, job);
            setDirty();
        }
    }

    public synchronized void enqueueIsland(ChunkPos origin, long key, int radius, CompoundTag payload) {
        Job job = new Job();
        job.id = "island/" + origin.x() + "/" + origin.z() + "/" + key;
        job.origin = origin; job.radius = radius; job.island = payload.copy();
        if (jobs.putIfAbsent(job.id, job) == null) {
            pending.put(job.id, job);
            setDirty();
        }
    }

    public synchronized void enqueueTower(ChunkPos origin, reika.chromaticraft.magic.lore.Towers tower, long seed) {
        Job job = new Job();
        job.id = "tower/" + tower + "/" + origin.x() + "/" + origin.z();
        job.origin = origin; job.tower = tower; job.radius = 2;
        job.random = new SavedWorldgenRandom(seed);
        if (jobs.putIfAbsent(job.id, job) == null) {
            pending.put(job.id, job);
            setDirty();
        }
    }

    public synchronized boolean isTowerFinished(ChunkPos origin, reika.chromaticraft.magic.lore.Towers tower) {
        Job job = jobs.get("tower/" + tower + "/" + origin.x() + "/" + origin.z());
        return job != null && job.finished;
    }

    public synchronized boolean isDungeonFinished(Type type, ChunkPos origin) {
        Job job = jobs.get("dungeon/" + type + "/" + origin.x() + "/" + origin.z());
        return job != null && job.finished;
    }

    public synchronized boolean isIslandFinished(ChunkPos origin, long key) {
        Job job = jobs.get("island/" + origin.x() + "/" + origin.z() + "/" + key);
        return job != null && job.finished;
    }

    public synchronized boolean hasPendingWork(ChunkPos origin) {
        return pending.values().stream().anyMatch(job -> job.origin.equals(origin));
    }

    private boolean explored(ServerLevel level, Job job) {
        if (BoundedPopulation.get(level).admits(job.origin)) return true;
        if (level.getChunkSource().getForceLoadedChunks().contains(job.origin.pack())) return true;
        int range = level.getServer().getPlayerList().getViewDistance();
        for (var player : level.players()) {
            ChunkPos playerChunk = ChunkPos.containing(player.blockPosition());
            if (Math.abs(playerChunk.x() - job.origin.x()) <= range
                    && Math.abs(playerChunk.z() - job.origin.z()) <= range) return true;
        }
        return false;
    }

    private synchronized void tick(ServerLevel level) {
        if (active != null && !explored(level, active)) release(level);
        if (active == null) {
            for (Job job : pending.values()) {
                if (explored(level, job)
                        && (BoundedPopulation.get(level).admits(job.origin)
                            || level.getChunkSource().getChunkNow(job.origin.x(), job.origin.z()) != null)) {
                    active = job;
                    break;
                }
            }
        }
        if (active == null) return;
        var chunks = active.dungeon == null ? java.util.List.<ChunkPos>of()
                : active.retryChunks();
        ChunkPos window = active.dungeon == null ? active.origin : chunks.get(active.cursor);
        if (!loading.ready(level, TICKET.get(), window, active.radius)) return;
        if (active.dungeon != null) {
            ChunkPos at = chunks.get(active.cursor);
            var feature = new OverworldStructureFeature(active.dungeon, true);
            ChunkPos placed = feature.tryGenerateInChunk(level, level, at.x(), at.z(), active.random, active.tries);
            if (placed != null) {
                DungeonZones.markChunkStatus(level, active.origin.x(), active.origin.z(), active.dungeon, DungeonZones.Status.INERT_GEN);
                DungeonZones.markChunkStatus(level, placed.x(), placed.z(), active.dungeon, DungeonZones.Status.SUCCESS);
                active.finished = true;
            } else if (++active.cursor == chunks.size()) {
                DungeonZones.markChunkStatus(level, active.origin.x(), active.origin.z(), active.dungeon, DungeonZones.Status.FAILURE);
                active.finished = true;
            }
        } else if (active.tower != null) {
            DataTowerFeature.placeAt(level, new net.minecraft.core.BlockPos(
                    active.origin.getMinBlockX() + 8, 0, active.origin.getMinBlockZ() + 8), active.tower, active.random);
            active.finished = true;
        } else {
            if (active.island.getBooleanOr("Candidate", false))
                reika.chromaticraft.world.luminous.LuminousIslandStructure.placeAfterPopulation(level, active.island);
            else new LuminousIslandPiece(active.island).placeAfterPopulation(level);
            active.finished = true;
        }
        setDirty();
        if (active.finished) {
            pending.remove(active.id);
            release(level);
        }
    }

    private void release(ServerLevel level) {
        removeTicket(level);
        active = null;
    }

    private void removeTicket(ServerLevel level) {
        loading.release(level, TICKET.get());
    }

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) get(level);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) get(level).tick(level);
    }

    @SubscribeEvent
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        // LevelEvent.Unload fires after vanilla drains chunks. A still-admitted job must release
        // its loading ticket before that drain, while its cursor/RNG remain saved for next boot.
        for (var level : event.getServer().getAllLevels()) {
            PostPopulationWorldgen data = CACHE.get(level);
            if (data != null) data.release(level);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PostPopulationWorldgen data = CACHE.remove(level);
            if (data != null) data.release(level);
        }
    }
}
