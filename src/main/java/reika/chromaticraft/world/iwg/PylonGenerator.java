/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.iwg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.event.level.ChunkEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.network.PylonLinkNetwork;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * V33a {@code PylonGenerator}'s pylon location cache: every crystal pylon's colour, place, power crystals, turbocharge,
 * player link and structure state, kept on the server (saved through {@code CrystalNetworker.PylonLocationData}) and
 * mirrored to each client for the Pylon Compass and the full-screen pylon overlay.
 *
 * <p>V33a's class was also the pylon worldgen itself (a {@code RetroactiveGenerator} with its own shuffled chunk grid).
 * That half lives in the port's placed feature, {@code PylonFeature} with {@code PylonGridPlacement}; this class keeps
 * {@link #canGenerateIn}, the dimension rule the overlays gate on.
 *
 * <p>V33a shared one map between the logical client and server (they ran in one JVM in single player) and stamped each
 * synced entry with the client's current dimension. Here the two sides keep separate maps and every entry carries its
 * own dimension. V33a pruned stale entries (pylon gone or recoloured) by loading each pylon's chunk when the cache was
 * read at world load; a 26.2 saved-data load cannot touch chunks, so each entry is checked when its chunk next loads.
 */
public final class PylonGenerator implements CrystalNetworker.PylonLocationDataHandler {

	public static final PylonGenerator instance = new PylonGenerator();

	public static final String NBT_TAG = "pylonloc";

	private final EnumMap<CrystalElement, Collection<PylonEntry>> colorCache = new EnumMap(CrystalElement.class);
	private final EnumMap<CrystalElement, Collection<PylonEntry>> clientCache = new EnumMap(CrystalElement.class);

	private PylonGenerator() {

	}

	/** Hooks the cache into the network's saved data; call once during setup. */
	public static void register() {
		CrystalNetworker.registerPylonLocationDataHandler(instance);
	}

	private EnumMap<CrystalElement, Collection<PylonEntry>> cache(boolean client) {
		return client ? clientCache : colorCache;
	}

	@Override
	public void savePylonLocations(CompoundTag NBT) {
		ListTag li = new ListTag();
		for (CrystalElement e : colorCache.keySet()) {
			Collection<PylonEntry> c = colorCache.get(e);
			for (PylonEntry loc : c) {
				CompoundTag tag = new CompoundTag();
				loc.location.saveAdditional("pos", tag);
				tag.putInt("color", e.ordinal());
				tag.putBoolean("turbo", loc.isTurboCharged);
				tag.putBoolean("struct", loc.hasStructure);
				if (loc.playerLink != null) {
					tag.putString("link", loc.playerLink.toString());
				}
				ListTag li2 = new ListTag();
				for (BlockPos c2 : loc.powerCrystals) {
					CompoundTag ct = new CompoundTag();
					ct.putInt("x", c2.getX());
					ct.putInt("y", c2.getY());
					ct.putInt("z", c2.getZ());
					li2.add(ct);
				}
				tag.put("crystals", li2);
				li.add(tag);
			}
		}
		NBT.put(NBT_TAG, li);
	}

	@Override
	public void loadPylonLocations(CompoundTag NBT) {
		ListTag li = NBT.getListOrEmpty(NBT_TAG);
		for (Tag t : li) {
			if (!(t instanceof CompoundTag tag))
				continue;
			WorldLocation loc = WorldLocation.load("pos", tag);
			if (loc == null)
				continue;
			CrystalElement e = CrystalElement.elements[tag.getIntOr("color", 0)];
			boolean turbo = tag.getBooleanOr("turbo", false);
			String uids = tag.getStringOr("link", "");
			UUID uid = uids.isEmpty() ? null : UUID.fromString(uids);
			boolean struct = tag.getBooleanOr("struct", false);
			ArrayList<BlockPos> li2 = new ArrayList();
			for (Tag t2 : tag.getListOrEmpty("crystals")) {
				if (t2 instanceof CompoundTag c)
					li2.add(new BlockPos(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0)));
			}
			this.addLocation(colorCache, new PylonEntry(e, loc, li2, turbo, uid, struct));
		}
	}

	/** V33a {@code validateCachedLocation}, run as a pylon's chunk loads: a pylon of the cached colour must still be there. */
	public void validateLoadedChunk(ChunkEvent.Load evt) {
		if (!(evt.getLevel() instanceof ServerLevel world) || !(evt.getChunk() instanceof LevelChunk chunk))
			return;
		boolean changed = false;
		for (CrystalElement e : colorCache.keySet()) {
			Iterator<PylonEntry> it = colorCache.get(e).iterator();
			while (it.hasNext()) {
				PylonEntry loc = it.next();
				if (!loc.location.getDimension().equals(world.dimension()))
					continue;
				BlockPos pos = loc.location.pos;
				if ((pos.getX() >> 4) != chunk.getPos().x() || (pos.getZ() >> 4) != chunk.getPos().z())
					continue;
				BlockEntity te = chunk.getBlockEntity(pos);
				if (!(te instanceof TileEntityCrystalPylon pylon) || pylon.getColor() != e) {
					it.remove();
					changed = true;
				}
			}
		}
		if (changed)
			CrystalNetworker.markPylonLocationsDirty(world);
	}

	/** V33a {@code PylonCacheLoader}: on login and on changing dimension, the player gets that dimension's pylons. */
	public void sendDimensionCacheToPlayer(ServerPlayer ep, ResourceKey<Level> dim) {
		List<PylonEntry> li = new ArrayList();
		for (CrystalElement e : colorCache.keySet()) {
			for (PylonEntry loc : colorCache.get(e)) {
				if (loc.location.getDimension().equals(dim))
					li.add(loc);
			}
		}
		if (!li.isEmpty())
			ChromaNetwork.sendPylonCache(ep, li);
	}

	/** V33a cleared the client cache on every kind of logout; the server's map goes with the server. */
	public void clearClientCache() {
		clientCache.clear();
	}

	public void clearServerCache() {
		colorCache.clear();
	}

	/** The nearest known pylon of that colour in the level's dimension, from whichever side's cache {@code world} belongs to. */
	public PylonEntry getNearestPylonSpawn(Level world, double x, double y, double z, CrystalElement e) {
		Collection<PylonEntry> c = this.cache(world.isClientSide()).get(e);
		if (c == null)
			return null;
		double dist = Double.POSITIVE_INFINITY;
		PylonEntry close = null;
		for (PylonEntry loc : c) {
			if (loc.location.getDimension().equals(world.dimension())) {
				double d = loc.location.getDistanceTo(x, y, z);
				if (d < dist) {
					dist = d;
					close = loc;
				}
			}
		}
		return close;
	}

	/**
	 * V33a {@code canGenerateIn}: the overworld always; never the Nether, the End, or any skyless or ceilinged
	 * dimension; a superflat world only with the flat-worldgen option; any other dimension only with the
	 * non-overworld option.
	 *
	 * <p>CHROMA-PORT: V33a also excluded the Extra Utilities Deep Dark, the Twilight Forest, the Miner's World and
	 * Witchery's dimensions, and in a Mystcraft age required its pylon page. None of those mods has a 26.2 port.
	 */
	public boolean canGenerateIn(Level world) {
		if (world.dimension() == Level.OVERWORLD)
			return true;
		if (world.dimension() == Level.NETHER || world.dimension() == Level.END)
			return false;
		DimensionType type = world.dimensionType();
		if (type.hasCeiling() || !type.hasSkyLight())
			return false;
		if (isFlat(world) && !ChromaOptions.FLATGEN.getState())
			return false;
		return ChromaOptions.NONWORLDGEN.getState();
	}

	private static boolean isFlat(Level world) {
		if (world instanceof ServerLevel sl)
			return sl.isFlat();
		// The client's level data keeps the superflat flag privately; its horizon is the world floor only when flat.
		return world.getLevelData() instanceof ClientLevel.ClientLevelData data && data.getHorizonHeight(world) != 63.0;
	}

	public void removeCachedPylon(TileEntityCrystalPylon te) {
		PylonEntry loc = new PylonEntry(te);
		Collection<PylonEntry> c = colorCache.get(te.getColor());
		if (c != null) {
			c.remove(loc);
		}
		if (te.getLevel() instanceof ServerLevel sl)
			CrystalNetworker.markPylonLocationsDirty(sl);
	}

	/** Records (or refreshes) a pylon and tells every player; server side only, as in V33a the client learns by packet. */
	public void cachePylon(TileEntityCrystalPylon te) {
		if (!(te.getLevel() instanceof ServerLevel sl))
			return;
		PylonEntry e = new PylonEntry(te);
		this.addLocation(colorCache, e);
		CrystalNetworker.markPylonLocationsDirty(sl);
		ChromaNetwork.sendPylonCacheToAll(sl.getServer(), List.of(e));
	}

	/** The client side of V33a's PYLONCACHE packet. */
	public void cachePylonLocations(List<PylonEntry> entries) {
		for (PylonEntry e : entries)
			this.addLocation(clientCache, e);
	}

	private void addLocation(EnumMap<CrystalElement, Collection<PylonEntry>> map, PylonEntry pe) {
		Collection<PylonEntry> c = map.get(pe.color);
		if (c == null) {
			c = new HashSet();
			map.put(pe.color, c);
		}
		c.remove(pe);
		c.add(pe);
	}

	public String printPylonCache() {
		return "Cache Debug: server "+colorCache+" / client "+clientCache;
	}

	public static class PylonEntry {

		public static final StreamCodec<ByteBuf, PylonEntry> STREAM_CODEC = StreamCodec.of(PylonEntry::write, PylonEntry::read);

		public final CrystalElement color;
		public final WorldLocation location;
		private final ArrayList<BlockPos> powerCrystals = new ArrayList();
		public final boolean isTurboCharged;
		public final UUID playerLink;
		public final boolean hasStructure;

		private PylonEntry(TileEntityCrystalPylon te) {
			this(te.getColor(), new WorldLocation(te), crystals(te), te.isEnhanced(), te.getLinkTileUUID(), te.hasStructure());
		}

		public PylonEntry(CrystalElement e, WorldLocation loc, List<BlockPos> li, boolean turbo, UUID link, boolean struct) {
			color = e;
			location = loc;
			powerCrystals.addAll(li);
			isTurboCharged = turbo;
			playerLink = link;
			hasStructure = struct;
		}

		private static List<BlockPos> crystals(TileEntityCrystalPylon te) {
			List<BlockPos> li = new ArrayList();
			for (TileEntityChromaCrystal c : te.getBoosterCrystals(false))
				li.add(c.getBlockPos());
			return li;
		}

		private static void write(ByteBuf buf, PylonEntry e) {
			ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, e.location.getDimension());
			BlockPos.STREAM_CODEC.encode(buf, e.location.pos);
			ByteBufCodecs.VAR_INT.encode(buf, e.color.ordinal());
			ByteBufCodecs.BOOL.encode(buf, e.isTurboCharged);
			ByteBufCodecs.BOOL.encode(buf, e.hasStructure);
			ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).encode(buf, Optional.ofNullable(e.playerLink));
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, e.powerCrystals);
		}

		private static PylonEntry read(ByteBuf buf) {
			ResourceKey<Level> dim = ResourceKey.streamCodec(Registries.DIMENSION).decode(buf);
			BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
			CrystalElement color = CrystalElement.elements[ByteBufCodecs.VAR_INT.decode(buf)];
			boolean turbo = ByteBufCodecs.BOOL.decode(buf);
			boolean struct = ByteBufCodecs.BOOL.decode(buf);
			UUID link = ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).decode(buf).orElse(null);
			List<BlockPos> crystals = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
			return new PylonEntry(color, new WorldLocation(dim, pos.getX(), pos.getY(), pos.getZ()), crystals, turbo, link, struct);
		}

		@Override
		public int hashCode() {
			return location.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof PylonEntry && ((PylonEntry)o).location.equals(location);
		}

		@Override
		public String toString() {
			return color.name()+" @ "+location.toString()+" [CRY = "+powerCrystals.size()+"x/TRB = "+isTurboCharged+"/LNK = "+playerLink+"/STR = "+hasStructure+"]";
		}

		public Collection<BlockPos> getCrystals() {
			return Collections.unmodifiableCollection(powerCrystals);
		}

		/** Returns the number of pylons *other* than this one that are linked. */
		public int getLinkedPylons(Level world) {
			if (playerLink == null)
				return 0;
			return PylonLinkNetwork.instance.getLinkedPylons(world, playerLink, color).size()-1;
		}

	}

}
