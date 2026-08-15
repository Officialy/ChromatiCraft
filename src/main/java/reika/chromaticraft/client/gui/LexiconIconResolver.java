package reika.chromaticraft.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

/** Binds V33a guide source identities to the modern registries as each content family lands. */
public final class LexiconIconResolver {

	private LexiconIconResolver() {}

	public static ItemStack icon(LexiconCatalog.Entry entry) {
		List<ItemStack> icons = icons(entry);
		return icons.isEmpty() ? ItemStack.EMPTY : icons.getFirst();
	}

	/** V33a {@code ChromaResearch.getItemStacks}: every currently registered display variant. */
	public static List<ItemStack> icons(LexiconCatalog.Entry entry) {
		List<ItemStack> variants = variants(entry);
		if (!variants.isEmpty())
			return variants;
		ItemLike naturalStructureIcon = naturalStructureIcon(entry);
		if (naturalStructureIcon != null)
			return List.of(new ItemStack(naturalStructureIcon));
		ItemLike item = switch (entry.sourceType()) {
			case "machine" -> machine(entry.sourceId());
			case "block" -> block(entry.sourceId());
			case "tool" -> tool(entry.sourceId());
			case "resource" -> resource(entry.sourceId());
			case "structure" -> structure(entry.sourceId());
			default -> null;
		};
		return item != null ? List.of(new ItemStack(item)) : List.of();
	}

	/** Exact V33a structure-page shielding metadata, now expressed as independent registry blocks. */
	private static ItemLike naturalStructureIcon(LexiconCatalog.Entry entry) {
		ChromaShieldTypes type = switch (entry.id()) {
			case "CAVERN" -> ChromaShieldTypes.CLOAK;
			case "BURROW" -> ChromaShieldTypes.STONE;
			case "OCEAN" -> ChromaShieldTypes.MOSS;
			case "DESERT" -> ChromaShieldTypes.COBBLE;
			case "SNOW" -> ChromaShieldTypes.LIGHT;
			default -> null;
		};
		return type != null ? ChromaBlocks.shielding(type).get() : null;
	}

	private static List<ItemStack> variants(LexiconCatalog.Entry entry) {
		ArrayList<ItemStack> items = new ArrayList<>();
		switch (entry.sourceId()) {
			case "elemental" -> addElements(items, element -> ChromaItems.ELEMENTAL_STONES.get(element).get());
			case "crystal" -> addElements(items, element -> ChromaBlocks.caveCrystal(element).get());
			case "rune" -> addElements(items, element -> ChromaBlocks.rune(element).get());
			case "dyeleaf" -> addElements(items, element -> ChromaBlocks.dyeLeaves(element).get());
			case "lamp" -> addElements(items, element -> ChromaBlocks.crystalLamp(element).get());
			case "super" -> addElements(items, element -> ChromaBlocks.superCrystal(element).get());
			case "berry" -> addElements(items, element -> ChromaItems.BERRIES.get(element).get());
			case "shards" -> addElements(items, element -> ChromaItems.SHARDS.get(element).get());
			case "dusts" -> {
				// Exact V33a DUSTS display list. Do not broaden this to every tiered item.
				items.add(ChromaItems.tieredStack(ChromaTieredItems.AURA_DUST));
				items.add(ChromaItems.tieredStack(ChromaTieredItems.PURITY_DUST));
				items.add(ChromaItems.tieredStack(ChromaTieredItems.ELEMENT_DUST));
				items.add(ChromaItems.tieredStack(ChromaTieredItems.BEACON_DUST));
				items.add(ChromaItems.tieredStack(ChromaTieredItems.RESONANCE_DUST));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.TELEPORTATION_DUST));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.ICY_DUST));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.ETHER_BERRIES));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.ENERGY_POWDER));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.LIVING_ESSENCE));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.VOID_DUST));
			}
			case "groups" -> {
				for (ChromaClusterItems item : ChromaClusterItems.list)
					items.add(ChromaItems.clusterStack(item));
				items.add(ChromaItems.craftingStack(ChromaCraftingItems.ELEMENT_UNIT));
			}
			case "cores" -> addCrafting(items, ChromaCraftingItems.CRYSTAL_FOCUS,
					ChromaCraftingItems.ENERGY_CORE, ChromaCraftingItems.TRANSFORMATION_CORE,
					ChromaCraftingItems.VOID_CORE, ChromaCraftingItems.CRYSTAL_LENS);
			case "hicores" -> addCrafting(items, ChromaCraftingItems.HIGH_ENERGY_CORE,
					ChromaCraftingItems.HIGH_TRANSFORMATION_CORE, ChromaCraftingItems.HIGH_VOID_CORE,
					ChromaCraftingItems.GLOW_CHUNK, ChromaCraftingItems.LUMEN_CORE);
			case "irid" -> addCrafting(items, ChromaCraftingItems.RAW_CRYSTAL,
					ChromaCraftingItems.IRIDESCENT_CRYSTAL, ChromaCraftingItems.IRIDESCENT_CHUNK);
			case "alloys" -> addCrafting(items, ChromaCraftingItems.CHROMA_INGOT,
					ChromaCraftingItems.FIERY_INGOT, ChromaCraftingItems.ENDER_INGOT,
					ChromaCraftingItems.WATER_INGOT, ChromaCraftingItems.CONDUCTIVE_INGOT,
					ChromaCraftingItems.AURA_INGOT, ChromaCraftingItems.COMPLEX_INGOT,
					ChromaCraftingItems.SPACE_INGOT);
			case "pylonstruct" -> {
				for (StoneTypes type : StoneTypes.list)
					items.add(new ItemStack(ChromaBlocks.crystallineStone(type).get()));
			}
			default -> { }
		}
		return List.copyOf(items);
	}

	private static void addElements(List<ItemStack> items,
			java.util.function.Function<CrystalElement, ItemLike> item) {
		for (CrystalElement element : CrystalElement.elements)
			items.add(new ItemStack(item.apply(element)));
	}

	private static void addCrafting(List<ItemStack> items, ChromaCraftingItems... variants) {
		for (ChromaCraftingItems item : variants)
			items.add(ChromaItems.craftingStack(item));
	}

	private static ItemLike machine(String id) {
		return switch (id) {
			case "pylon" -> ChromaBlocks.PYLON.get();
			case "repeater" -> ChromaBlocks.REPEATER.get();
			case "skypeater" -> ChromaBlocks.SKYPEATER.get();
			case "compound" -> ChromaBlocks.COMPOUND.get();
			case "pylonlink" -> ChromaBlocks.PYLON_LINK.get();
			case "stand" -> ChromaBlocks.ITEM_STAND.get();
			case "table" -> ChromaBlocks.CASTING_TABLE.get();
			case "focuscrystal" -> ChromaBlocks.FOCUS_CRYSTAL.get();
			default -> null;
		};
	}

	private static ItemLike block(String id) {
		return switch (id) {
			case "crystal" -> ChromaBlocks.caveCrystal(CrystalElement.PURPLE).get();
			case "rune" -> ChromaBlocks.rune(CrystalElement.LIGHTBLUE).get();
			case "pylonstruct" -> ChromaBlocks.crystallineStone(
					reika.chromaticraft.block.BlockCrystallineStone.StoneTypes.SMOOTH).get();
			case "mud" -> ChromaBlocks.MUD.get();
			case "rainbowleaf" -> ChromaBlocks.RAINBOW_LEAVES.get();
			case "dyeleaf" -> ChromaBlocks.dyeLeaves(CrystalElement.BROWN).get();
			case "luma" -> ChromaBlocks.LUMA.get();
			case "warpnode" -> ChromaBlocks.WARP_NODE.get();
			case "structshield" -> ChromaBlocks.shielding(
					reika.chromaticraft.registry.ChromaShieldTypes.STONE).get();
			default -> null;
		};
	}

	private static ItemLike tool(String id) {
		return switch (id) {
			case "help" -> ChromaItems.LEXICON.get();
			case "fragment" -> ChromaItems.INFO_FRAGMENT.get();
			case "tool" -> ChromaItems.MANIPULATOR.get();
			case "storage" -> ChromaItems.STORAGE_CRYSTALS.get(
					reika.chromaticraft.registry.StorageCrystalTier.NULA).get();
			case "elemental" -> ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLUE).get();
			case "datacrystal" -> ChromaItems.DATA_CRYSTAL.get();
			default -> null;
		};
	}

	private static ItemLike resource(String id) {
		return switch (id) {
			case "shards" -> ChromaItems.SHARDS.get(CrystalElement.RED).get();
			case "dusts" -> ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get();
			case "groups" -> ChromaItems.CLUSTERS.get(ChromaClusterItems.CRYSTAL_CORE).get();
			case "cores" -> ChromaItems.CRAFTING.get(ChromaCraftingItems.ENERGY_CORE).get();
			case "hicores" -> ChromaItems.CRAFTING.get(ChromaCraftingItems.HIGH_ENERGY_CORE).get();
			case "irid" -> ChromaItems.CRAFTING.get(ChromaCraftingItems.IRIDESCENT_CRYSTAL).get();
			case "alloys" -> ChromaItems.CRAFTING.get(ChromaCraftingItems.CHROMA_INGOT).get();
			default -> null;
		};
	}

	private static ItemLike structure(String id) {
		return switch (id) {
			case "pylon", "pylonbroadcast" -> ChromaBlocks.PYLON.get();
			case "casting1", "casting2", "casting3" -> ChromaBlocks.CASTING_TABLE.get();
			case "repeater" -> ChromaBlocks.REPEATER.get();
			case "compound" -> ChromaBlocks.COMPOUND.get();
			default -> ChromaBlocks.crystallineStone(
					reika.chromaticraft.block.BlockCrystallineStone.StoneTypes.SMOOTH).get();
		};
	}
}
