package reika.chromaticraft.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.world.OverworldStructureFeature.Type;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.math.noise.VoronoiNoiseGenerator;

/**
 * The planning half of V33a {@code DungeonGenerator}: where the six natural overworld structures
 * (Crystal Shrine, Crystal Burrow, Ocean Temple, Sandy Burrow, Frozen Outpost, Biome Fragment) may
 * generate, and what became of each attempt.
 *
 * <p>Each structure type has a 2D Voronoi field whose cell roots are candidate zones, cells roughly
 * {@link #noiseScale} blocks apart with their roots jittered 55% of the way across. A chunk is
 * {@link Status#PLANNED} when, among the zones within that type's exclusion radius, the one chosen to
 * stand for the cluster lies in it. Zones whose biome cannot host the structure drop out
 * ({@link Status#BIOMEFAIL}), and the rest of a cluster is {@link Status#CLUSTERED}. The planned chunk
 * then tries to place the structure (see {@link OverworldStructureFeature}), and records
 * {@link Status#SUCCESS} or {@link Status#FAILURE}. Statuses persist with the level, as V33a's
 * {@code StructureStatus.dat} did, and feed the Artifact Distribution Map.
 *
 * <p>V33a seeded each field from the world seed, DragonAPI's world creation time and the save folder's
 * absolute path hash. The first two are kept; the path term is dropped, because it made the layout depend
 * on where the save sat on disk (moving or copying a world moved every unbuilt structure).
 */
public final class DungeonZones {

	private static final Map<ServerLevel, EnumMap<Type, VoronoiNoiseGenerator>> NOISE = new ConcurrentHashMap<>();

	private DungeonZones() {}

	/** V33a {@code StructureGenStatus}, colours included (the map and debug views use them). */
	public enum Status {
		INERT(0x000000),
		INERT_GEN(0x303030),
		BIOMEFAIL(0xff9020),
		PLANNED(0xffffff),
		GENERATING(0x22aaff),
		SUCCESS(0x00ff00),
		FAILURE(0xff0000),
		CLUSTERED(0xff00ff),
		REMOVED(0xffff00);

		public static final Status[] list = values();

		public final int renderColor;

		Status(int c) {
			renderColor = c;
		}

		public boolean hasStructure() {
			return this == SUCCESS || this == PLANNED || this == GENERATING;
		}

		public boolean isGenerated() {
			return this == SUCCESS || this == FAILURE || this == REMOVED || this == INERT_GEN;
		}

		public boolean isFinalized() {
			return this.isGenerated() || this == CLUSTERED || this == BIOMEFAIL;
		}

		public boolean canOverwrite(Status g) {
			if (this == INERT_GEN && g == PLANNED)
				return false;
			return this.isFinalized() && !g.isFinalized();
		}
	}

	/** V33a {@code getNoiseScale}. */
	public static int noiseScale(Type s) {
		return switch (s) {
			case DESERT -> 440;
			case OCEAN -> 640;
			case CAVERN -> 144;
			case BURROW -> 240;
			case SNOW -> 480;
			case BIOME_FRAGMENT -> 640;
		};
	}

	/** V33a {@code getExclusionZone}. */
	public static int exclusionZone(Type s) {
		return switch (s) {
			case DESERT -> 256;
			case OCEAN -> 512;
			case CAVERN, BURROW -> 64;
			case SNOW -> 256;
			case BIOME_FRAGMENT -> 440;
		};
	}

	/** V33a {@code getChunkKeepTryingRange}; 26.2 can only honour the first ring (see the feature). */
	public static int keepTryingRange(Type s) {
		return switch (s) {
			case OCEAN, SNOW -> 96;
			case DESERT, BIOME_FRAGMENT -> 64;
			default -> 48;
		};
	}

	private static VoronoiNoiseGenerator noise(ServerLevel level, Type s) {
		return NOISE.computeIfAbsent(level, l -> new EnumMap<>(Type.class)).computeIfAbsent(s, t -> {
			long sd = level.getSeed() ^ (t.ordinal() * 41381L)
					^ ~reika.dragonapi.libraries.level.ReikaWorldHelper.getCurrentWorldID(level).worldCreationTime;
			VoronoiNoiseGenerator v = (VoronoiNoiseGenerator)new VoronoiNoiseGenerator(sd ^ t.ordinal())
					.setFrequency(0.75D / noiseScale(t));
			v.randomFactor = 0.55;
			return v;
		});
	}

	public static void forget(ServerLevel level) {
		NOISE.remove(level);
	}

	/** V33a {@code canGenerateIn}: the overworld, and superflat worlds only when allowed and deep enough. */
	public static boolean canGenerateIn(ServerLevel level) {
		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD)
			return false;
		if (level.getChunkSource().getGenerator() instanceof FlatLevelSource flat)
			return ChromaOptions.FLATGEN.getState() && flat.settings().getLayers().size() > 15;
		return true;
	}

	/** V33a {@code getNearbyZones}: zone roots within {@code r}, skipping finalized empty ones. */
	private static Collection<WorldLocation> getNearbyZones(Type s, ServerLevel level, double x, double z, double r) {
		Collection<DecimalPosition> li = noise(level, s).getCellsWithin2D(x, z, r);
		Collection<WorldLocation> ret = new ArrayList<>();
		Data data = Data.get(level);
		for (DecimalPosition d : li) {
			int cx = Mth.floor(d.xCoord) >> 4;
			int cz = Mth.floor(d.zCoord) >> 4;
			Status get = data.putIfAbsent(s, cx, cz, Status.PLANNED);
			if (get != null && get.isFinalized() && !get.hasStructure())
				continue;
			ret.add(new WorldLocation(level, d));
		}
		return ret;
	}

	/** V33a {@code isGennableChunk}, block coordinates. */
	public static Status isGennableChunk(ServerLevel level, int x, int z, Type s) {
		ArrayList<WorldLocation> c = new ArrayList<>(getNearbyZones(s, level, x, z, exclusionZone(s)));
		if (c.isEmpty())
			return Status.INERT;
		Status failret = Status.INERT;
		Iterator<WorldLocation> it = c.iterator();
		while (it.hasNext()) {
			WorldLocation loc = it.next();
			// V33a isValidBiomeNear stepped an 8-block grid but tested the centre every time.
			if (!isValidBiomeAt(level, loc.pos.getX(), loc.pos.getZ(), s)) {
				it.remove();
				if ((x >> 4 == loc.pos.getX() >> 4) && (z >> 4 == loc.pos.getZ() >> 4))
					failret = Status.BIOMEFAIL;
			}
		}
		if (c.isEmpty())
			return failret;
		WorldLocation usable;
		if (c.size() == 1) {
			usable = c.getFirst();
		}
		else {
			Collections.sort(c);
			usable = c.getFirst();
			for (WorldLocation loc : c) {
				if (!usable.equals(loc))
					markChunkStatus(level, loc.pos.getX() >> 4, loc.pos.getZ() >> 4, s, Status.CLUSTERED);
			}
		}
		return (x >> 4 == usable.pos.getX() >> 4) && (z >> 4 == usable.pos.getZ() >> 4) ? Status.PLANNED : Status.INERT;
	}

	/** V33a {@code getGenStatus}, block coordinates; what the Artifact Distribution Map shows. */
	public static Status getGenStatus(ServerLevel level, Type s, int x, int z) {
		Status def = isGennableChunk(level, x, z, s);
		Data data = Data.get(level);
		for (WorldLocation loc : getNearbyZones(s, level, x, z, 32)) {
			int cx = loc.pos.getX() >> 4;
			int cz = loc.pos.getZ() >> 4;
			Status get = data.get(s, cx, cz);
			if (get == null || def.canOverwrite(get))
				data.put(s, cx, cz, def);
		}
		Status stat = data.get(s, x >> 4, z >> 4);
		return stat != null ? stat : def;
	}

	/** V33a {@code markChunkStatus}, chunk coordinates. */
	public static void markChunkStatus(ServerLevel level, int cx, int cz, Type s, Status stat) {
		Data.get(level).put(s, cx, cz, stat);
	}

	/** V33a {@code isValidBiomeAt}: the planned biome, without generating the chunk. */
	public static boolean isValidBiomeAt(ServerLevel level, int x, int z, Type s) {
		var source = level.getChunkSource();
		Holder<Biome> biome = source.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
				QuartPos.fromBlock(level.getSeaLevel()), QuartPos.fromBlock(z), source.randomState().sampler());
		return isValidBiome(s, biome);
	}

	/**
	 * V33a {@code isValidBiome}. 1.7.10 read each biome's top block and decorator; 26.2 biomes carry
	 * neither, so the same questions are asked through biome tags (see {@link #grassTopped}).
	 */
	public static boolean isValidBiome(Type s, Holder<Biome> b) {
		boolean ocean = b.is(BiomeTags.IS_OCEAN);
		return switch (s) {
			case OCEAN -> ocean;
			case CAVERN -> true;
			case BURROW -> !ocean && grassTopped(b) && !isValidBiome(Type.SNOW, b) && !isChromaBiome(b);
			case DESERT -> !ocean && b.is(Tags.Biomes.IS_SANDY) && !b.is(BiomeTags.IS_BADLANDS) && !b.is(BiomeTags.IS_BEACH);
			// Not Ice Plains Spikes; grass-topped, snowing, treeless, and not a "forest".
			case SNOW -> !ocean && !b.is(Biomes.ICE_SPIKES) && grassTopped(b) && b.is(Tags.Biomes.IS_SNOWY)
					&& !b.is(BiomeTags.IS_FOREST) && !b.is(Tags.Biomes.IS_FOREST) && !b.is(BiomeTags.IS_TAIGA);
			case BIOME_FRAGMENT -> !b.is(ChromaBiomes.RAINBOW_STREAM) && isChromaBiome(b);
		};
	}

	/** V33a {@code ChromatiCraft.isCCBiome}: the Rainbow Forest (and its stream), Ender Forest and Cliffs. */
	public static boolean isChromaBiome(Holder<Biome> b) {
		return b.is(ChromaBiomes.RAINBOW_FOREST) || b.is(ChromaBiomes.RAINBOW_STREAM) || b.is(ChromaBiomes.ENDER_FOREST)
				|| b.is(ChromaBiomes.LUMINOUS_CLIFFS) || b.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
	}

	/**
	 * 1.7.10's {@code biome.topBlock == Blocks.grass}: every land biome except those surfaced with sand,
	 * terracotta, mycelium, stone, snow or ice.
	 */
	public static boolean grassTopped(Holder<Biome> b) {
		return !b.is(BiomeTags.IS_OCEAN) && !b.is(Tags.Biomes.IS_SANDY) && !b.is(BiomeTags.IS_BADLANDS)
				&& !b.is(BiomeTags.IS_BEACH) && !b.is(Tags.Biomes.IS_MUSHROOM) && !b.is(Biomes.STONY_SHORE)
				&& !b.is(Biomes.ICE_SPIKES) && !b.is(Tags.Biomes.IS_MOUNTAIN_PEAK) && !b.is(Tags.Biomes.IS_MOUNTAIN_SLOPE)
				&& !b.is(Tags.Biomes.IS_UNDERGROUND);
	}

	/** Per-level persistent status map, V33a {@code StructureStatus.dat}. */
	public static final class Data extends SavedData {

		private static final Codec<Data> CODEC = CompoundTag.CODEC.xmap(Data::new, Data::save);
		private static final SavedDataType<Data> TYPE = new SavedDataType<>(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "structure_status"),
				Data::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
		private static final Map<ServerLevel, Data> CACHE = new ConcurrentHashMap<>();

		private final EnumMap<Type, Long2ObjectMap<Status>> map = new EnumMap<>(Type.class);

		private Data() {
			for (Type t : Type.values())
				map.put(t, Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()));
		}

		private Data(CompoundTag tag) {
			this();
			for (Type t : Type.values()) {
				CompoundTag sub = tag.getCompoundOrEmpty(t.name());
				for (String key : sub.keySet()) {
					try {
						map.get(t).put(Long.parseLong(key), Status.list[sub.getIntOr(key, 0)]);
					}
					catch (RuntimeException ignored) {
						// skip malformed entries
					}
				}
			}
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			for (Map.Entry<Type, Long2ObjectMap<Status>> e : map.entrySet()) {
				CompoundTag sub = new CompoundTag();
				synchronized (e.getValue()) {
					for (Long2ObjectMap.Entry<Status> s : e.getValue().long2ObjectEntrySet())
						sub.putInt(Long.toString(s.getLongKey()), s.getValue().ordinal());
				}
				tag.put(e.getKey().name(), sub);
			}
			return tag;
		}

		/** Worldgen threads reach this, so the storage lookup is done once and cached per level. */
		public static Data get(ServerLevel level) {
			return CACHE.computeIfAbsent(level, l -> {
				synchronized (Data.class) {
					return l.getDataStorage().computeIfAbsent(TYPE);
				}
			});
		}

		public static void forget(ServerLevel level) {
			CACHE.remove(level);
		}

		Status get(Type s, int cx, int cz) {
			return map.get(s).get(ChunkPos.pack(cx, cz));
		}

		Status putIfAbsent(Type s, int cx, int cz, Status stat) {
			Status old = map.get(s).putIfAbsent(ChunkPos.pack(cx, cz), stat);
			if (old == null)
				this.setDirty();
			return old;
		}

		void put(Type s, int cx, int cz, Status stat) {
			map.get(s).put(ChunkPos.pack(cx, cz), stat);
			this.setDirty();
		}
	}

	/** Release the per-level caches when a level unloads. */
	@net.neoforged.fml.common.EventBusSubscriber(modid = ChromatiCraft.MODID)
	public static final class Lifecycle {
		private Lifecycle() {}

		/**
		 * Build the fields and load the status data on the server thread. The world-ID lookup behind the
		 * seed is file-backed and not thread-safe, and worldgen workers would otherwise race to it.
		 */
		@net.neoforged.bus.api.SubscribeEvent
		public static void onLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
			if (event.getLevel() instanceof ServerLevel level && canGenerateIn(level)) {
				for (Type t : Type.values())
					noise(level, t);
				Data.get(level);
			}
		}

		@net.neoforged.bus.api.SubscribeEvent
		public static void onUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
			if (event.getLevel() instanceof ServerLevel level) {
				forget(level);
				Data.forget(level);
			}
		}
	}
}
