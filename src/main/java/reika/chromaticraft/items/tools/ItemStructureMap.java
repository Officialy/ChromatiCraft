/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import reika.chromaticraft.magic.lore.LoreManager;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.world.DungeonZones;
import reika.chromaticraft.world.OverworldStructureFeature;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * V33a {@code ItemStructureMap}, the Artifact Distribution Map. Right-click cycles what it seeks (strongholds,
 * the glowing towers, or one of the ChromatiCraft structures); sneak-right-click begins a scan from where the
 * player stands, which runs while the map is held and, when done, fills the map with the sites found: yellow
 * where a structure is planned, green where it has been generated. Held in first person, it is read like a
 * vanilla map ({@code StructureMapRenderer}).
 *
 * <p>The structure scans cover 257x257 chunks around the origin, 24 chunks a tick, reading each chunk's zone
 * status ({@link DungeonZones#getGenStatus}); strongholds and towers are known in full at once.
 *
 * <p>V33a kept the seek mode in the item damage. It is a setting, not a variant, so it lives in the map's
 * custom data ("mode"). The scan's progress is also written there (every ten ticks) so the tooltip can show it
 * on a dedicated server; V33a read the server's search table from the client, which only worked in
 * singleplayer.
 */
public class ItemStructureMap extends Item {

	private static final String TAG_NAME = "mapdata";

	private static final Map<UUID, StructureSearch> activeSearches = new HashMap<>();

	public ItemStructureMap(Item.Properties properties) {
		super(properties.stacksTo(1));
	}

	public static StructureSearchType getMode(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		int mode = tag == null ? 0 : tag.getIntOr("mode", 0);
		return StructureSearchType.list[Mth.clamp(mode, 0, StructureSearchType.list.length - 1)];
	}

	private static CompoundTag tag(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		return tag != null ? tag : new CompoundTag();
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		if (world instanceof ServerLevel server) {
			CompoundTag tag = tag(is);
			if (ep.isShiftKeyDown()) {
				if (activeSearches.containsKey(ep.getUUID())) {
					ep.sendSystemMessage(Component.literal("Another scan is in progress."));
				}
				else if (tag.contains(TAG_NAME) || tag.getBooleanOr("scanning", false) || tag.contains("searcher")) {
					ep.sendSystemMessage(Component.literal("This map is already scanning the world."));
				}
				else {
					StructureSearchType s = getMode(is);
					int x0 = Mth.floor(ep.getX());
					int z0 = Mth.floor(ep.getZ());
					tag.putBoolean("scanning", true);
					tag.putString("searcher", ep.getUUID().toString());
					tag.putFloat("progress", 0);
					ReikaItemHelper.setStackTag(is, tag);
					activeSearches.put(ep.getUUID(), s.beginSearch(server, x0, z0));
				}
			}
			else {
				int mode = getMode(is).ordinal();
				boolean flag = true;
				while (flag) {
					mode = (mode + 1) % StructureSearchType.list.length;
					flag = StructureSearchType.list[mode].skip();
				}
				tag.putInt("mode", mode);
				ReikaItemHelper.setStackTag(is, tag);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, EquipmentSlot slot) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		if (slot != EquipmentSlot.MAINHAND || tag == null || !tag.getBooleanOr("scanning", false) || !tag.contains("searcher"))
			return;
		UUID uid = UUID.fromString(tag.getStringOr("searcher", ""));
		StructureSearch s = activeSearches.get(uid);
		if (s == null) {
			activeSearches.remove(uid);
			return;
		}
		StructureMapData data = s.tick(world);
		if (data != null) {
			tag.put(TAG_NAME, data.writeToNBT());
			tag.remove("scanning");
			tag.remove("progress");
			ReikaItemHelper.setStackTag(is, tag);
			activeSearches.remove(uid);
			ChromaSounds.CRAFTDONE.playSound(e);
		}
		else if (world.getGameTime() % 10 == 0) {
			tag.putFloat("progress", s.getProgress());
			ReikaItemHelper.setStackTag(is, tag);
		}
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		CompoundTag tag = tag(is);
		li.accept(Component.literal("Seeking ").append(getMode(is).displayName()));
		if (tag.getBooleanOr("scanning", false)) {
			li.accept(Component.literal(String.format(Locale.ROOT, "Map is scanning: %.1f%% complete.",
					tag.getFloatOr("progress", 0))));
		}
		else if (tag.contains(TAG_NAME)) {
			// V33a showed the count while Ctrl was held; the advanced-tooltip flag stands in for it.
			if (flag.isAdvanced()) {
				StructureMapData map = StructureMapData.readFromNBT(tag.getCompoundOrEmpty(TAG_NAME));
				map.addTooltip(li);
			}
			else {
				li.accept(Component.literal("Map is populated."));
			}
		}
		else {
			li.accept(Component.literal("Map is empty."));
		}
	}

	/** The stored scan, for the renderer; null if the map is empty. */
	public static StructureMapData getMapData(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		if (tag == null || !tag.contains(TAG_NAME))
			return null;
		return StructureMapData.readFromNBT(tag.getCompoundOrEmpty(TAG_NAME));
	}

	public enum StructureSearchType {
		STRONGHOLD(),
		TOWERS(),
		CAVERN(OverworldStructureFeature.Type.CAVERN, "Crystal Shrine"),
		BURROW(OverworldStructureFeature.Type.BURROW, "Crystal Burrow"),
		OCEAN(OverworldStructureFeature.Type.OCEAN, "Ocean Temple"),
		DESERT(OverworldStructureFeature.Type.DESERT, "Sandy Burrow"),
		SNOWSTRUCT(OverworldStructureFeature.Type.SNOW, "Frozen Outpost"),
		MINESHAFT(),
		BIOME(OverworldStructureFeature.Type.BIOME_FRAGMENT, "Magic Outpost"),
		;

		public static final StructureSearchType[] list = values();

		private final OverworldStructureFeature.Type structure;
		/** V33a {@code chromastruct.*} display name. */
		private final String structureName;

		StructureSearchType() {
			this(null, null);
		}

		StructureSearchType(OverworldStructureFeature.Type s, String name) {
			structure = s;
			structureName = name;
		}

		private StructureSearch beginSearch(ServerLevel world, int x0, int z0) {
			return new StructureSearch(this, x0, z0);
		}

		private boolean skip() {
			return this == MINESHAFT;
		}

		public int getSearchRange() {
			if (structure != null) {
				return 128 * 16;
			}
			return switch (this) {
				case STRONGHOLD, TOWERS -> -1;
				default -> 1;
			};
		}

		public Component displayName() {
			if (this == TOWERS)
				return Component.literal("GlowingTowers").withStyle(ChatFormatting.OBFUSCATED);
			if (structure != null)
				return Component.literal(structureName);
			return Component.literal(this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT));
		}
	}

	private static class StructureSearch {

		private static final int RANGE = 128;

		private final StructureSearchType type;
		private final int originX;
		private final int originZ;

		private int scanI = -RANGE;
		private int scanK = -RANGE;
		private int scannedChunks = 0;

		private final LinkedHashMap<BlockPos, StructurePresence> foundStructures = new LinkedHashMap<>();
		private BlockBox bounds = BlockBox.nothing();
		private int confirmed;

		private StructureSearch(StructureSearchType t, int x, int z) {
			type = t;
			originX = x;
			originZ = z;
		}

		private boolean updateScanChunk() {
			boolean flag1 = false;
			scanI++;
			if (scanI > RANGE) {
				scanI = -RANGE;
				flag1 = true;
			}
			if (flag1) {
				scanK++;
				if (scanK > RANGE) {
					scanK = -RANGE;
					return true;
				}
			}
			return false;
		}

		public float getProgress() {
			int r = RANGE * 2 + 1;
			return scannedChunks * 100F / (r * r);
		}

		private void add(BlockPos c, StructurePresence s) {
			bounds = bounds.addCoordinate(c.getX(), c.getY(), c.getZ());
			if (s == StructurePresence.CONFIRMED)
				confirmed++;
			foundStructures.put(c, s);
		}

		/**
		 * V33a {@code tick}; returns the finished map, or null while scanning. Return coords are absolute.
		 *
		 * <p>V33a counted and bounded only the current tick's finds for the structure scans (its counters were
		 * locals); the finished map re-counts confirmed sites when read, so only its stored bounds differed. They
		 * accumulate over the whole scan here.
		 */
		private StructureMapData tick(ServerLevel world) {
			if (type.structure != null) {
				for (int i = 0; i < 24; i++) {
					int rx = originX + scanI * 16;
					int rz = originZ + scanK * 16;
					DungeonZones.Status at = DungeonZones.getGenStatus(world, type.structure, rx, rz);
					if (at.hasStructure())
						this.add(new BlockPos(rx, 0, rz), at.isFinalized() ? StructurePresence.CONFIRMED : StructurePresence.EXPECTED);
					scannedChunks++;
					if (this.updateScanChunk())
						return new StructureMapData(foundStructures, originX, originZ, confirmed, bounds);
				}
				return null;
			}
			switch (type) {
				case STRONGHOLD -> this.findStrongholds(world);
				case TOWERS -> {
					LoreManager.instance.initTowers(world);
					for (Towers t : Towers.towerList) {
						BlockPos c = t.getGeneratedLocation();
						// V33a fell back to the root chunk's coordinates as if they were block coordinates, which
						// plotted ungenerated towers near the origin; the chunk's centre is used instead.
						if (c == null)
							c = new BlockPos(t.getRootPosition().getMiddleBlockX(), 0, t.getRootPosition().getMiddleBlockZ());
						c = new BlockPos(c.getX(), 0, c.getZ());
						boolean cf = ReikaWorldHelper.isChunkGenerated(world, c.getX(), c.getZ());
						this.add(c, cf ? StructurePresence.CONFIRMED : StructurePresence.EXPECTED);
					}
				}
				default -> {}
			}
			return new StructureMapData(foundStructures, originX, originZ, confirmed, bounds);
		}

		/**
		 * V33a read MapGenStronghold's planned ring positions (expected) and its generated starts (confirmed, at
		 * the start's centre). 26.2 strongholds are concentric-ring placements: every ring position is expected,
		 * and where that chunk has been generated its stronghold start is read and marked confirmed.
		 */
		private void findStrongholds(ServerLevel world) {
			var registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
			Holder<Structure> stronghold = registry.get(BuiltinStructures.STRONGHOLD).orElse(null);
			if (stronghold == null)
				return;
			var state = world.getChunkSource().getGeneratorState();
			for (StructurePlacement placement : state.getPlacementsForStructure(stronghold)) {
				if (!(placement instanceof ConcentricRingsStructurePlacement rings))
					continue;
				List<ChunkPos> positions = state.getRingPositionsFor(rings);
				if (positions == null)
					continue;
				for (ChunkPos pos : positions) {
					this.add(new BlockPos(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()), StructurePresence.EXPECTED);
					if (ReikaWorldHelper.isChunkGeneratedChunkCoords(world, pos.x(), pos.z())) {
						ChunkAccess chunk = world.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS, true);
						StructureStart start = chunk != null ? chunk.getStartForStructure(stronghold.value()) : null;
						if (start != null && start.isValid()) {
							var bb = start.getBoundingBox();
							this.add(new BlockPos(bb.getCenter().getX(), 0, bb.getCenter().getZ()), StructurePresence.CONFIRMED);
						}
					}
				}
			}
		}
	}

	public static final class StructureMapData {

		public final Map<BlockPos, StructurePresence> map;
		public final int originX;
		public final int originZ;
		public final BlockBox bounds;
		private final int confirmedCount;

		private StructureMapData(Map<BlockPos, StructurePresence> data, int x, int z, int cc, BlockBox box) {
			map = data;
			originX = x;
			originZ = z;
			confirmedCount = cc;
			bounds = box;
		}

		public void addTooltip(Consumer<Component> li) {
			li.accept(Component.literal("Confirmed " + confirmedCount + "/" + map.size() + " locations."));
		}

		public static StructureMapData readFromNBT(CompoundTag data) {
			LinkedHashMap<BlockPos, StructurePresence> values = new LinkedHashMap<>();
			int cc = 0;
			for (Tag t : data.getListOrEmpty("mapdata")) {
				if (!(t instanceof CompoundTag tag))
					continue;
				CompoundTag loc = tag.getCompoundOrEmpty("loc");
				BlockPos pos = new BlockPos(loc.getIntOr("x", 0), loc.getIntOr("y", 0), loc.getIntOr("z", 0));
				StructurePresence s;
				try {
					s = StructurePresence.valueOf(tag.getStringOr("state", "EXPECTED"));
				}
				catch (IllegalArgumentException e) {
					continue;
				}
				if (s == StructurePresence.CONFIRMED)
					cc++;
				values.put(pos, s);
			}
			CompoundTag meta = data.getCompoundOrEmpty("metadata");
			return new StructureMapData(values, meta.getIntOr("rootX", 0), meta.getIntOr("rootZ", 0), cc,
					BlockBox.load(meta));
		}

		private CompoundTag writeToNBT() {
			CompoundTag ret = new CompoundTag();
			ListTag tag = new ListTag();
			for (Entry<BlockPos, StructurePresence> e : map.entrySet()) {
				CompoundTag val = new CompoundTag();
				CompoundTag loc = new CompoundTag();
				loc.putInt("x", e.getKey().getX());
				loc.putInt("y", e.getKey().getY());
				loc.putInt("z", e.getKey().getZ());
				val.put("loc", loc);
				val.putString("state", e.getValue().name());
				tag.add(val);
			}
			CompoundTag meta = new CompoundTag();
			bounds.saveAdditional(meta);
			meta.putInt("rootX", originX);
			meta.putInt("rootZ", originZ);
			ret.put("mapdata", tag);
			ret.put("metadata", meta);
			return ret;
		}
	}

	public enum StructurePresence {
		EXPECTED(0xffff00),
		CONFIRMED(0x00ff00),
		FAILED(0xff0000);

		public final int renderColor;

		StructurePresence(int c) {
			renderColor = c;
		}
	}

}
