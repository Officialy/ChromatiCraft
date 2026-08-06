package reika.chromaticraft.magic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.AngleMap;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * V33a {@code WarpNetwork}: which warp node you arrive at is decided by the direction you left in.
 *
 * <p>Every node stores its peers in an {@link AngleMap} keyed by the bearing from this node to that
 * one, so a jump is resolved by taking the player's facing and finding the nearest stored bearing
 * within a tolerance. Links are symmetric — adding a node writes the forward bearing into every
 * existing node's map and the reciprocal (bearing + 180) into its own — which is what makes the
 * network navigable in both directions without storing edges twice.
 */
public class WarpNetwork {

	public static final WarpNetwork instance = new WarpNetwork();

	private final Map<WorldLocation, AngleMap<WorldLocation>> data = new HashMap<>();

	private WarpNetwork() {}

	public Collection<WorldLocation> getAllNodes() {
		return Collections.unmodifiableCollection(data.keySet());
	}

	public void addLocation(WorldLocation loc) {
		data.put(loc, this.calculateLinks(loc));
		WarpNetworkData.initNetworkData(loc.getWorld()).setDirty();
	}

	/**
	 * V33a getLink: the peer whose stored bearing is nearest {@code angle}, or null if the nearest is
	 * outside {@code tolerance}. Both neighbours of the angle are considered because the map wraps.
	 */
	public WorldLocation getLink(WorldLocation loc, double angle, double tolerance) {
		WarpNetworkData.initNetworkData(loc.getWorld());
		AngleMap<WorldLocation> map = data.get(loc);
		if (map == null || map.isEmpty())
			return null;
		if (map.size() == 1) {
			Entry<Double, WorldLocation> e = map.firstEntry();
			return within(e.getKey(), angle, tolerance) ? e.getValue() : null;
		}
		Entry<Double, WorldLocation> below = map.floorEntry(angle);
		Entry<Double, WorldLocation> above = map.ceilingEntry(angle);
		if (below == null)
			return within(above.getKey(), angle, tolerance) ? above.getValue() : null;
		if (above == null)
			return within(below.getKey(), angle, tolerance) ? below.getValue() : null;
		double d1 = Math.abs(ReikaVectorHelper.getAngleDifference(below.getKey(), angle));
		double d2 = Math.abs(ReikaVectorHelper.getAngleDifference(above.getKey(), angle));
		boolean left = d1 < d2;
		return (left ? d1 : d2) <= tolerance ? (left ? below.getValue() : above.getValue()) : null;
	}

	private static boolean within(double stored, double angle, double tolerance) {
		return Math.abs(ReikaVectorHelper.getAngleDifference(stored, angle)) <= tolerance;
	}

	/** V33a calculateLinks: write the bearing into each peer, and its reciprocal into this node. */
	private AngleMap<WorldLocation> calculateLinks(WorldLocation loc) {
		AngleMap<WorldLocation> ret = new AngleMap<>();
		for (WorldLocation key : data.keySet()) {
			if (key.getDimension().equals(loc.getDimension()) && !loc.equals(key)) {
				AngleMap<WorldLocation> map = data.get(key);
				double phi = ReikaPhysicsHelper.cartesianToPolar(
						loc.pos.getX() - key.pos.getX(), 0, loc.pos.getZ() - key.pos.getZ())[2];
				map.put(phi, loc);
				ret.put((phi + 180) % 360, key);
			}
		}
		return ret;
	}

	public void clear() {
		data.clear();
	}

	public void load(CompoundTag nbt) {
		data.clear();
		ListTag list = nbt.getListOrEmpty("data");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompoundOrEmpty(i);
			WorldLocation key = WorldLocation.load("location", entry);
			if (key == null)
				continue;
			AngleMap<WorldLocation> map = new AngleMap<>();
			ListTag peers = entry.getListOrEmpty("map");
			for (int k = 0; k < peers.size(); k++) {
				CompoundTag tag = peers.getCompoundOrEmpty(k);
				WorldLocation loc = WorldLocation.readTag(tag);
				if (loc != null)
					map.put(tag.getDoubleOr("angle", 0), loc);
			}
			data.put(key, map);
		}
	}

	public void save(CompoundTag nbt) {
		ListTag list = new ListTag();
		for (Entry<WorldLocation, AngleMap<WorldLocation>> e : data.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.put("location", e.getKey().writeToTag());
			ListTag peers = new ListTag();
			AngleMap<WorldLocation> map = e.getValue();
			for (double angle : map.keySet()) {
				CompoundTag tag = map.get(angle).writeToTag();
				tag.putDouble("angle", angle);
				peers.add(tag);
			}
			entry.put("map", peers);
			list.add(entry);
		}
		nbt.put("data", list);
	}

	/** Modern SavedData scoped per level, matching the pylon-location data's shape. */
	public static class WarpNetworkData extends SavedData {

		private static final String IDENTIFIER = "warpnet";
		private static final Codec<WarpNetworkData> CODEC = CompoundTag.CODEC.xmap(
				WarpNetworkData::new, WarpNetworkData::saveTag);
		private static final SavedDataType<WarpNetworkData> TYPE = new SavedDataType<>(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, IDENTIFIER),
				WarpNetworkData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

		private WarpNetworkData() {
			this(new CompoundTag());
		}

		private WarpNetworkData(CompoundTag tag) {
			instance.load(tag);
		}

		private CompoundTag saveTag() {
			CompoundTag tag = new CompoundTag();
			instance.save(tag);
			return tag;
		}

		public static WarpNetworkData initNetworkData(Level level) {
			if (!(level instanceof ServerLevel server))
				throw new IllegalStateException("Warp network data is server-side only");
			return server.getDataStorage().computeIfAbsent(TYPE);
		}
	}
}
