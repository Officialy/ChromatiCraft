/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.network;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.networking.TileEntityPylonLink;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/** Persistent owner/colour web connecting the pylon-link tiles in all loaded dimensions. */
public final class PylonLinkNetwork {

	public static final PylonLinkNetwork instance = new PylonLinkNetwork();

	private final Map<UUID, EnumMap<CrystalElement, Map<WorldLocation, PylonNode>>> links = new HashMap<>();
	private PylonLinkData activeData;

	private PylonLinkNetwork() {
	}

	public PylonNode addLocation(TileEntityPylonLink tile, TileEntityCrystalPylon pylon) {
		if (!(tile.getLevel() instanceof ServerLevel server) || tile.getUUID() == null)
			return null;
		this.ensureLoaded(server);
		Map<WorldLocation, PylonNode> subweb = this.getSubweb(tile.getUUID(), pylon.getColor());
		WorldLocation tileLocation = new WorldLocation(tile);
		PylonNode node = subweb.computeIfAbsent(tileLocation, ignored ->
				new PylonNode(tile.getUUID(), pylon.getColor(), tileLocation, new WorldLocation(pylon)));
		pylon.link(tile);
		this.markDirty();
		return node;
	}

	public void removeLocation(Level world, PylonNode connection) {
		if (!(world instanceof ServerLevel server) || connection == null)
			return;
		this.ensureLoaded(server);
		EnumMap<CrystalElement, Map<WorldLocation, PylonNode>> web = links.get(connection.owner());
		if (web != null) {
			Map<WorldLocation, PylonNode> subweb = web.get(connection.color());
			if (subweb != null)
				subweb.remove(connection.tile());
		}
		BlockEntity pylon = connection.pylon().getBlockEntity();
		if (pylon instanceof TileEntityCrystalPylon crystalPylon)
			crystalPylon.link(null);
		this.markDirty();
	}

	public Collection<WorldLocation> getLinkedPylons(Level world, UUID owner, CrystalElement color) {
		if (owner == null)
			return List.of();
		if (world instanceof ServerLevel server)
			this.ensureLoaded(server);
		EnumMap<CrystalElement, Map<WorldLocation, PylonNode>> web = links.get(owner);
		Map<WorldLocation, PylonNode> subweb = web != null ? web.get(color) : null;
		if (subweb == null)
			return List.of();
		ArrayList<WorldLocation> pylons = new ArrayList<>(subweb.size());
		for (PylonNode node : subweb.values())
			pylons.add(node.pylon());
		return List.copyOf(pylons);
	}

	private Map<WorldLocation, PylonNode> getSubweb(UUID owner, CrystalElement color) {
		return links.computeIfAbsent(owner, ignored -> new EnumMap<>(CrystalElement.class))
				.computeIfAbsent(color, ignored -> new HashMap<>());
	}

	private void ensureLoaded(ServerLevel level) {
		PylonLinkData data = level.getServer().overworld().getDataStorage().computeIfAbsent(PylonLinkData.TYPE);
		activeData = data;
	}

	private void markDirty() {
		if (activeData != null)
			activeData.setDirty();
	}

	private void load(CompoundTag root) {
		links.clear();
		ListTag entries = root.getListOrEmpty("entries");
		for (int i = 0; i < entries.size(); i++) {
			PylonNode node = PylonNode.fromTag(entries.getCompoundOrEmpty(i));
			if (node != null)
				this.getSubweb(node.owner(), node.color()).put(node.tile(), node);
		}
	}

	private CompoundTag save() {
		CompoundTag root = new CompoundTag();
		ListTag entries = new ListTag();
		for (EnumMap<CrystalElement, Map<WorldLocation, PylonNode>> web : links.values())
			for (Map<WorldLocation, PylonNode> subweb : web.values())
				for (PylonNode node : subweb.values())
					entries.add(node.toTag());
		root.put("entries", entries);
		return root;
	}

	private static final class PylonLinkData extends SavedData {
		private static final Codec<PylonLinkData> CODEC = CompoundTag.CODEC.xmap(PylonLinkData::new, PylonLinkData::saveData);
		private static final SavedDataType<PylonLinkData> TYPE = new SavedDataType<>(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_link_network"),
				PylonLinkData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

		private PylonLinkData() {
		}

		private PylonLinkData(CompoundTag tag) {
			instance.load(tag);
		}

		private CompoundTag saveData() {
			return instance.save();
		}
	}

	public record PylonNode(UUID owner, CrystalElement color, WorldLocation tile, WorldLocation pylon) {
		public CompoundTag toTag() {
			CompoundTag tag = new CompoundTag();
			tag.putString("owner", owner.toString());
			tag.putInt("color", color.ordinal());
			tag.put("tile", tile.writeToTag());
			tag.put("pylon", pylon.writeToTag());
			return tag;
		}

		public static PylonNode fromTag(CompoundTag tag) {
			String ownerText = tag.getStringOr("owner", "");
			if (ownerText.isEmpty() || !tag.contains("tile") || !tag.contains("pylon"))
				return null;
			try {
				UUID owner = UUID.fromString(ownerText);
				int index = Math.max(0, Math.min(CrystalElement.elements.length - 1, tag.getIntOr("color", 0)));
				return new PylonNode(owner, CrystalElement.elements[index],
						WorldLocation.readTag(tag.getCompoundOrEmpty("tile")),
						WorldLocation.readTag(tag.getCompoundOrEmpty("pylon")));
			}
			catch (IllegalArgumentException ex) {
				return null;
			}
		}
	}
}
