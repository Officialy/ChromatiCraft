package reika.chromaticraft.registry;

import java.util.function.Supplier;
import java.util.EnumMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.item.ItemChromaBerry;
import reika.chromaticraft.item.ItemChromaEther;
import reika.chromaticraft.item.ItemElementalStone;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.items.ItemStorageCrystal;

/**
 * ChromatiCraft item registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaItems} enum into
 * 26.2 {@link DeferredRegister} form (mirrors ReactorItems); grows as items are ported. Block items
 * are registered by {@link ChromaBlocks}.
 */
public final class ChromaItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromatiCraft.MODID);
	private static final ThreadLocal<ResourceKey<Item>> CURRENT_ITEM_KEY = new ThreadLocal<>();


	public static Item.Properties itemProperties() {
		Item.Properties properties = new Item.Properties();
		ResourceKey<Item> key = CURRENT_ITEM_KEY.get();
		return key != null ? properties.setId(key) : properties;
	}

	private static <I extends Item> DeferredItem<I> reg(String name, Supplier<I> factory) {
		return ITEMS.register(name, id -> {
			CURRENT_ITEM_KEY.set(ResourceKey.create(Registries.ITEM, id));
			try {
				return factory.get();
			}
			finally {
				CURRENT_ITEM_KEY.remove();
			}
		});
	}

	/** One registered 26.2 item for each former {@code CRAFTING} metadata variant. */
	public static final EnumMap<ChromaCraftingItems, DeferredItem<Item>> CRAFTING =
			new EnumMap<>(ChromaCraftingItems.class);
	static {
		for (ChromaCraftingItems item : ChromaCraftingItems.list)
			CRAFTING.put(item, reg(item.registryName(), () -> createCraftingItem(item)));
	}

	public static ItemStack craftingStack(ChromaCraftingItems item) {
		return new ItemStack(CRAFTING.get(item).get());
	}

	private static Item createCraftingItem(ChromaCraftingItems item) {
		return item == ChromaCraftingItems.ETHER_BERRIES ? new ItemChromaEther(itemProperties()) : new Item(itemProperties());
	}

	/** One registered 26.2 item for each former {@code CLUSTER} metadata variant. */
	public static final EnumMap<ChromaClusterItems, DeferredItem<Item>> CLUSTERS =
			new EnumMap<>(ChromaClusterItems.class);
	static {
		for (ChromaClusterItems item : ChromaClusterItems.list)
			CLUSTERS.put(item, reg(item.registryName(), () -> new Item(itemProperties())));
	}

	public static ItemStack clusterStack(ChromaClusterItems item) {
		return new ItemStack(CLUSTERS.get(item).get());
	}

	/** Required former {@code TIERED} variants, split from legacy metadata. */
	public static final EnumMap<ChromaTieredItems, DeferredItem<Item>> TIERED =
			new EnumMap<>(ChromaTieredItems.class);
	static {
		for (ChromaTieredItems item : ChromaTieredItems.list)
			TIERED.put(item, reg(item.registryName(), () -> new Item(itemProperties())));
	}

	public static ItemStack tieredStack(ChromaTieredItems item) {
		return new ItemStack(TIERED.get(item).get());
	}

	public static final DeferredItem<Item> CRYSTAL_POWDER = CRAFTING.get(ChromaCraftingItems.CRYSTAL_POWDER);

	/** V33a ChromaItems.TOOL: the Manipulator, the universal ChromatiCraft interaction tool. */
	public static final DeferredItem<reika.chromaticraft.item.ItemManipulator> MANIPULATOR =
			reg("manipulator", () -> new reika.chromaticraft.item.ItemManipulator(
					itemProperties().stacksTo(1)));
	/** V33a HELP: the complete Chromic Lexicon, with pages/notes/blanks in CUSTOM_DATA. */
	public static final DeferredItem<reika.chromaticraft.item.ItemChromaBook> LEXICON =
			reg("chromic_lexicon", () -> new reika.chromaticraft.item.ItemChromaBook(itemProperties()));
	/** V33a FRAGMENT: blank, decoded and chroma-soaked variants share one component-backed item. */
	public static final DeferredItem<reika.chromaticraft.item.ItemInfoFragment> INFO_FRAGMENT =
			reg("information_fragment", () -> new reika.chromaticraft.item.ItemInfoFragment(itemProperties()));
	/** V33a DATACRYSTAL, awarded by a completed data-node scan; owner is stored in CUSTOM_DATA. */
	public static final DeferredItem<reika.chromaticraft.item.ItemDataCrystal> DATA_CRYSTAL =
			reg("data_crystal", () -> new reika.chromaticraft.item.ItemDataCrystal(
					itemProperties().stacksTo(1).fireResistant()));
	/** V33a KEY: persistent UUID and auto-open binding mode use CUSTOM_DATA. */
	public static final DeferredItem<reika.chromaticraft.item.ItemDoorKey> DOOR_KEY =
			reg("door_key", () -> new reika.chromaticraft.item.ItemDoorKey(itemProperties()));
	/** V33a MISC metadata 1, promoted to its own registry identity for the Charger upgrade slot. */
	public static final DeferredItem<Item> SPEED_UPGRADE =
			reg("speed_upgrade", () -> new Item(itemProperties().stacksTo(1)));
	/** V33a DIMGEN metadata 9, consumed by the Player Aura Infuser's DIMENSION boost. */
	public static final DeferredItem<Item> GLOW_CAVE_DUST =
			reg("glow_cave_dust", () -> new Item(itemProperties()));
	/** V33a ARTEFACT metadata 1. It has none of the harmful full-artefact behavior. */
	public static final DeferredItem<Item> UNKNOWN_ARTEFACT_FRAGMENT =
			reg("unknown_artefact_fragment", () -> new Item(itemProperties().stacksTo(1)));
	/** V33a STORAGE metadata 0-6, promoted to seven stable item identities. */
	public static final EnumMap<StorageCrystalTier, DeferredItem<ItemStorageCrystal>> STORAGE_CRYSTALS =
			new EnumMap<>(StorageCrystalTier.class);
	static {
		for (StorageCrystalTier tier : StorageCrystalTier.list)
			STORAGE_CRYSTALS.put(tier, reg(tier.registryName(),
					() -> new ItemStorageCrystal(tier, itemProperties())));
	}

	public static ItemStack storageCrystalStack(StorageCrystalTier tier) {
		return new ItemStack(STORAGE_CRYSTALS.get(tier).get());
	}

	public static boolean isStorageCrystal(ItemStack stack) {
		return ItemStorageCrystal.isStorageCrystal(stack);
	}
	/** The former SHARD metadata item, split into one registered item per crystal element. */
	public static final EnumMap<CrystalElement, DeferredItem<ItemCrystalShard>> SHARDS = new EnumMap<>(CrystalElement.class);
	/** V33a {@code case SHARD: meta >= 16 ? "Boosted " : ""} — the port previously mis-called this "Charged". */
	public static final EnumMap<CrystalElement, DeferredItem<ItemCrystalShard>> BOOSTED_SHARDS = new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement element : CrystalElement.elements) {
			// Colour-suffixed id; getEnglishName() gives the underscored vanilla DyeColor spelling
			// (light_gray/light_blue) rather than the enum constant's LIGHTGRAY/LIGHTBLUE.
			String suffix = element.getEnglishName();
			SHARDS.put(element, reg("crystal_shard_" + suffix,
					() -> new ItemCrystalShard(element, false, itemProperties())));
			BOOSTED_SHARDS.put(element, reg("boosted_crystal_shard_" + suffix,
					() -> new ItemCrystalShard(element, true, itemProperties())));
		}
	}

	public static ItemStack shardStack(CrystalElement element) {
		return new ItemStack(SHARDS.get(element).get());
	}

	public static ItemStack boostedShardStack(CrystalElement element) {
		return boostedShardStack(element, 1);
	}

	public static ItemStack boostedShardStack(CrystalElement element, int count) {
		return new ItemStack(BOOSTED_SHARDS.get(element).get(), count);
	}

	private ChromaItems() {}
	/** Former BERRY metadata split into sixteen stable color identities. */
	public static final EnumMap<CrystalElement, DeferredItem<ItemChromaBerry>> BERRIES = new EnumMap<>(CrystalElement.class);
	/** Former ELEMENTAL metadata split into sixteen stable color identities. */
	public static final EnumMap<CrystalElement, DeferredItem<ItemElementalStone>> ELEMENTAL_STONES = new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement element : CrystalElement.elements) {
			String suffix = element.getEnglishName();
			BERRIES.put(element, reg("chroma_berries_" + suffix,
					() -> new ItemChromaBerry(element, itemProperties())));
			ELEMENTAL_STONES.put(element, reg("elemental_stone_" + suffix,
					() -> new ItemElementalStone(element, itemProperties())));
		}
	}

	public static ItemStack berryStack(CrystalElement element) {
		return new ItemStack(BERRIES.get(element).get());
	}

	public static ItemStack elementalStoneStack(CrystalElement element) {
		return new ItemStack(ELEMENTAL_STONES.get(element).get());
	}

	/** Modern world-fluid container for source placement and pickup. */
	public static final DeferredItem<Item> CHROMA_BUCKET = reg("chroma_bucket",
			() -> new BucketItem(ChromaFluids.CHROMA.get(), itemProperties().craftRemainder(Items.BUCKET).stacksTo(1)));
}
