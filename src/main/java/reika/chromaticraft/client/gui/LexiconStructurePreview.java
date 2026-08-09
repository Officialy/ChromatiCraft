package reika.chromaticraft.client.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.registry.ChromaBlocks;

/** Client-side, read-only view of one canonical generated structure-template NBT. */
final class LexiconStructurePreview {

	private static final Map<String, LexiconStructurePreview> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, String> TEMPLATES = Map.ofEntries(
			Map.entry("pylon", "multiblock/pylon"),
			Map.entry("casting1", "multiblock/casting_l1"),
			Map.entry("casting2", "multiblock/casting_l2"),
			Map.entry("casting3", "multiblock/casting_l3"),
			Map.entry("repeater", "multiblock/repeater"),
			Map.entry("compound", "multiblock/compound_repeater"),
			Map.entry("pylonbroadcast", "multiblock/pylon_broadcast"),
			Map.entry("datanode", "worldgen/data_node"));

	private final String sourceId;
	private final Identifier templateId;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final List<PreviewBlock> blocks;
	private final String error;

	private LexiconStructurePreview(String sourceId, Identifier templateId, int sizeX, int sizeY,
			int sizeZ, List<PreviewBlock> blocks, String error) {
		this.sourceId = sourceId;
		this.templateId = templateId;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.blocks = List.copyOf(blocks);
		this.error = error;
	}

	static LexiconStructurePreview get(LexiconCatalog.Entry entry, Minecraft minecraft) {
		String path = TEMPLATES.get(entry.sourceId());
		if (path == null)
			return unavailable(entry.sourceId(), "NBT template not ported yet");
		return CACHE.computeIfAbsent(entry.sourceId(), ignored -> load(entry.sourceId(), path, minecraft));
	}

	static void clearCache() {
		CACHE.clear();
	}

	private static LexiconStructurePreview load(String sourceId, String path, Minecraft minecraft) {
		Identifier templateId = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
		// Structure templates belong to the server-data pack, which the client ResourceManager does
		// not index. They are nevertheless bundled in the same mod jar, so read that canonical copy
		// directly rather than maintaining a second, drift-prone asset-side template set.
		String resourcePath = "/data/" + templateId.getNamespace() + "/structure/"
				+ templateId.getPath() + ".nbt";
		try (InputStream stream = LexiconStructurePreview.class.getResourceAsStream(resourcePath)) {
			if (stream == null)
				throw new FileNotFoundException(resourcePath);
			CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.create(16L * 1024 * 1024));
			int[] size = ints(root.getListOrEmpty("size"), 3);
			ListTag paletteTags = root.getListOrEmpty("palette");
			var blockLookup = minecraft.player.registryAccess().lookupOrThrow(Registries.BLOCK);
			ArrayList<BlockState> palette = new ArrayList<>(paletteTags.size());
			for (var tag : paletteTags) {
				CompoundTag stateTag = tag.asCompound().orElseThrow(() ->
						new IOException("Non-compound palette entry in " + templateId));
				palette.add(NbtUtils.readBlockState(blockLookup, stateTag));
			}

			ArrayList<PreviewBlock> blocks = new ArrayList<>();
			for (var tag : root.getListOrEmpty("blocks")) {
				CompoundTag blockTag = tag.asCompound().orElseThrow(() ->
						new IOException("Non-compound block entry in " + templateId));
				int[] pos = ints(blockTag.getListOrEmpty("pos"), 3);
				int stateIndex = blockTag.getIntOr("state", -1);
				if (stateIndex < 0 || stateIndex >= palette.size())
					throw new IOException("Palette index " + stateIndex + " outside " + palette.size());
				BlockState state = palette.get(stateIndex);
				if (!isDisplayBlock(state))
					continue;
				// V33a rendered the data-node relay column as the data node itself. DUMMY_AUX is
				// intentionally block-only in 26.2, so it otherwise disappears from an item-icon view.
				boolean dataNodeRelay = sourceId.equals("datanode") && state.is(ChromaBlocks.DUMMY_AUX.get());
				ItemStack icon = new ItemStack(dataNodeRelay ? ChromaBlocks.DATA_NODE.get() : state.getBlock());
				if (!icon.isEmpty() && !icon.is(Items.AIR))
					blocks.add(new PreviewBlock(new BlockPos(pos[0], pos[1], pos[2]), state, icon, dataNodeRelay));
			}
			addDisplayControllers(sourceId, size, blocks);
			blocks.sort(Comparator.comparingInt(block -> block.pos().getY()));
			return new LexiconStructurePreview(sourceId, templateId, size[0], size[1], size[2], blocks, null);
		}
		catch (Exception ex) {
			return unavailable(sourceId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
		}
	}

	private static boolean isDisplayBlock(BlockState state) {
		return !state.isAir() && !state.is(Blocks.CAVE_AIR) && !state.is(Blocks.VOID_AIR)
				&& !state.is(Blocks.STRUCTURE_VOID);
	}

	/** Restores the controller/item-stand substitutions V33a GuiStructure added for display. */
	private static void addDisplayControllers(String sourceId, int[] size, List<PreviewBlock> blocks) {
		int cx = size[0] / 2;
		int cz = size[2] / 2;
		if (sourceId.startsWith("casting")) {
			add(blocks, new BlockPos(cx, 1, cz), ChromaBlocks.CASTING_TABLE.get().defaultBlockState());
			if (!sourceId.equals("casting1")) {
				for (int x = -4; x <= 4; x += 2) {
					for (int z = -4; z <= 4; z += 2) {
						if (x == 0 && z == 0) continue;
						int y = Math.abs(x) == 4 || Math.abs(z) == 4 ? 2 : 1;
						add(blocks, new BlockPos(cx + x, y, cz + z),
								ChromaBlocks.ITEM_STAND.get().defaultBlockState());
					}
				}
			}
		}
		else if (sourceId.equals("pylon") || sourceId.equals("pylonbroadcast")) {
			add(blocks, new BlockPos(cx, size[1] - 1, cz), ChromaBlocks.PYLON.get().defaultBlockState());
		}
	}

	private static void add(List<PreviewBlock> blocks, BlockPos pos, BlockState state) {
		blocks.removeIf(block -> block.pos().equals(pos));
		ItemStack icon = new ItemStack(state.getBlock());
		if (!icon.isEmpty())
			blocks.add(new PreviewBlock(pos, state, icon, true));
	}

	private static int[] ints(ListTag list, int expected) throws IOException {
		if (list.size() != expected)
			throw new IOException("Expected " + expected + " integers, got " + list.size());
		int[] values = new int[expected];
		for (int i = 0; i < expected; i++) {
			int index = i;
			values[i] = list.get(i).asInt().orElseThrow(() ->
					new IOException("Non-integer at list index " + index));
		}
		return values;
	}

	private static LexiconStructurePreview unavailable(String sourceId, String error) {
		return new LexiconStructurePreview(sourceId, null, 0, 0, 0, List.of(), error);
	}

	boolean available() { return error == null; }
	String error() { return error; }
	String sourceId() { return sourceId; }
	Identifier templateId() { return templateId; }
	int sizeX() { return sizeX; }
	int sizeY() { return sizeY; }
	int sizeZ() { return sizeZ; }
	List<PreviewBlock> blocks() { return blocks; }

	record PreviewBlock(BlockPos pos, BlockState state, ItemStack icon, boolean displayOverride) {}
}
