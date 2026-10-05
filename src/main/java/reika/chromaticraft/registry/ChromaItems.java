package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.item.ItemChromaBerry;
import reika.chromaticraft.item.ItemChromaEther;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.item.ItemElementalStone;
import reika.chromaticraft.items.ItemStorageCrystal;

import java.util.EnumMap;
import java.util.function.Supplier;

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

	/** V33a MAGICBRANCH metadata 0-5, preserving the six animated material variants. */
	public static final EnumMap<reika.chromaticraft.items.ItemMagicBranch.BranchTypes, DeferredItem<reika.chromaticraft.items.ItemMagicBranch>> BRANCHES =
			new EnumMap<>(reika.chromaticraft.items.ItemMagicBranch.BranchTypes.class);
	static {
		for (var type : reika.chromaticraft.items.ItemMagicBranch.BranchTypes.list)
			BRANCHES.put(type, reg(type.registryName(), () -> new reika.chromaticraft.items.ItemMagicBranch(itemProperties(),type)));
	}

	/** One registered 26.3 item for each former {@code CLUSTER} metadata variant. */
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
			TIERED.put(item, reg(item.registryName(), () -> new reika.chromaticraft.items.ItemTieredResource(itemProperties(), item)));
	}

	public static ItemStack tieredStack(ChromaTieredItems item) {
		return new ItemStack(TIERED.get(item).get());
	}

	/** V33a DIMGEN: the resource drops are separate from their decoration blocks. */
	public static final EnumMap<ChromaDimensionItems, DeferredItem<reika.chromaticraft.items.ItemDimGen>> DIMENSION_RESOURCES =
			new EnumMap<>(ChromaDimensionItems.class);
	static {
		for (ChromaDimensionItems type : ChromaDimensionItems.values())
			DIMENSION_RESOURCES.put(type, reg(type.registryName(),
					() -> new reika.chromaticraft.items.ItemDimGen(itemProperties(), type)));
	}

	/** V33a LENS metadata 0-15, promoted to one stable item identity per element. */
	public static final EnumMap<CrystalElement, DeferredItem<Item>> TINTED_LENSES =
			new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement element : CrystalElement.elements)
			TINTED_LENSES.put(element, reg("tinted_crystal_lens_" + element.getEnglishName(),
					() -> new Item(itemProperties())));
	}

	public static ItemStack tintedLensStack(CrystalElement element) {
		return new ItemStack(TINTED_LENSES.get(element).get());
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
	public static final DeferredItem<reika.chromaticraft.items.ItemDimGen> GLOW_CAVE_DUST =
			DIMENSION_RESOURCES.get(ChromaDimensionItems.GLOWCAVE);
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemEfficiencyCrystal> EFFICIENCY_GEM =
			reg("efficiency_gem", () -> new reika.chromaticraft.items.tools.ItemEfficiencyCrystal(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemWarpCapsule> RECALL_TABLET =
			reg("recall_tablet", () -> new reika.chromaticraft.items.tools.ItemWarpCapsule(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemCaptureWand> CAPTURE_WAND =
			reg("capture_star", () -> new reika.chromaticraft.items.tools.wands.ItemCaptureWand(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemRecipeCacheCapsule> RECIPE_TABLET =
			reg("recipe_tablet", () -> new reika.chromaticraft.items.tools.ItemRecipeCacheCapsule(itemProperties()));
	/** V33a ENDERCRYSTAL: captures and replaces vanilla End Crystals; count is component-backed. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemEnderCrystal> ENDER_CRYSTAL_MOVER =
			reg("ender_crystal_mover", () -> new reika.chromaticraft.items.tools.ItemEnderCrystal(
					itemProperties()));
	/** V33a FERTILITYSEED metadata 0-6: one registered item per potency, all "Fertility Seeds". */
	public static final java.util.List<DeferredItem<reika.chromaticraft.items.ItemFertilitySeed>> FERTILITY_SEEDS =
			new java.util.ArrayList<>();
	static {
		for (int i = 0; i < reika.chromaticraft.items.ItemFertilitySeed.POTENCIES; i++) {
			final int potency = i;
			FERTILITY_SEEDS.add(reg("fertility_seeds_" + i,
					() -> new reika.chromaticraft.items.ItemFertilitySeed(potency, itemProperties())));
		}
	}
	/** V33a ENDEREYE, the Enhanced Ender Eye; its owner is stored in CUSTOM_DATA. */
	public static final DeferredItem<reika.chromaticraft.items.ItemT2EnderEye> ENDER_EYE =
			reg("enhanced_ender_eye", () -> new reika.chromaticraft.items.ItemT2EnderEye(itemProperties()));
	/** V33a CAVEPATHER, the Subterranean Route Finder. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemCaveExitFinder> CAVE_PATHER =
			reg("cave_pather", () -> new reika.chromaticraft.items.tools.ItemCaveExitFinder(itemProperties().stacksTo(1)));
	/** V33a SHARE, the Ownership Expansion Key. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemOwnerKey> OWNERSHIP_KEY =
			reg("ownership_key", () -> new reika.chromaticraft.items.tools.ItemOwnerKey(itemProperties().stacksTo(1)));
	/** V33a LIGHTGUN, the Illumination Gun. */
	/** V33a TELEPORT, the Mobility Star (tool sprite 83). */
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemMobilityWand> MOBILITY_WAND =
			reg("mobility_star", () -> new reika.chromaticraft.items.tools.wands.ItemMobilityWand(itemProperties()));
	/** V33a SHOO, the Animal Repulsion Star (tool sprite 86): inert in V33a, with no recipe. */
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemShooWand> SHOO_WAND =
			reg("animal_repulsion_star", () -> new reika.chromaticraft.items.tools.wands.ItemShooWand(itemProperties()));
	/** V33a GROWTH, the Growth Star (tool sprite 87). */
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemGrowthWand> GROWTH_WAND =
			reg("growth_star", () -> new reika.chromaticraft.items.tools.wands.ItemGrowthWand(itemProperties()));
	/** V33a LIGHTWAND (tool sprite 91); V33a shipped no name for it. */
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemLightWand> LIGHT_WAND =
			reg("light_wand", () -> new reika.chromaticraft.items.tools.wands.ItemLightWand(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemExcavationWand> EXCAVATION_WAND =
			reg("excavation_star", () -> new reika.chromaticraft.items.tools.wands.ItemExcavationWand(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemBuilderWand> CONSTRUCTION_WAND =
			reg("construction_star", () -> new reika.chromaticraft.items.tools.wands.ItemBuilderWand(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemFlightWand> HOVER_WAND =
			reg("hover_star", () -> new reika.chromaticraft.items.tools.wands.ItemFlightWand(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemMultiTool> MULTITOOL =
			reg("multitool", () -> new reika.chromaticraft.items.tools.ItemMultiTool(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemOreSilker> ORE_HARVESTER =
			reg("ore_harvester", () -> new reika.chromaticraft.items.tools.ItemOreSilker(itemProperties()));
	/** V33a SPAWNERBYPASS, tool sprite 43. */
	public static final DeferredItem<reika.chromaticraft.items.tools.powered.ItemSpawnerBypass> SPAWNER_BYPASS =
			reg("spawner_bypass", () -> new reika.chromaticraft.items.tools.powered.ItemSpawnerBypass(itemProperties()));
	/** V33a BULKMOVER, tool sprite 12. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemBulkMover> BULK_MOVER =
			reg("bulk_mover", () -> new reika.chromaticraft.items.tools.ItemBulkMover(itemProperties()));
	/** V33a BOTTLENECK, tool sprite 48. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemBottleneckFinder> BOTTLENECK_FINDER =
			reg("bottleneck_finder", () -> new reika.chromaticraft.items.tools.ItemBottleneckFinder(itemProperties()));
	/** V33a MOBSONAR, tool sprite 92. */
	public static final DeferredItem<reika.chromaticraft.items.tools.wands.ItemMobSonar> MOB_SONAR =
			reg("creature_sounding_star", () -> new reika.chromaticraft.items.tools.wands.ItemMobSonar(itemProperties()));
	/** V33a RIFT, the World Rift: an ItemLinkedTilePlacer placing ChromaBlocks.RIFT in linked pairs. */
	public static final DeferredItem<reika.chromaticraft.items.itemblock.ItemLinkedTilePlacer> WORLD_RIFT =
			reg("world_rift", () -> new reika.chromaticraft.items.itemblock.ItemLinkedTilePlacer(itemProperties()));
	/** V33a ETHERPENDANT, the Luma Capsule (tool sprite 72): carried, it counts as every Void Gaze level. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemEtherealPendant> LUMA_CAPSULE =
			reg("luma_capsule", () -> new reika.chromaticraft.items.tools.ItemEtherealPendant(itemProperties()));
	/** V33a AURAPOUCH, the Aura Pouch (tool sprite 29); leather's enchantability, as V33a returned. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemAuraPouch> AURA_POUCH =
			reg("aura_pouch", () -> new reika.chromaticraft.items.tools.ItemAuraPouch(itemProperties().enchantable(15)));
	/** V33a PURIFY, the Aura Purification Gem (sprites 33-36 by charge state). */
	public static final DeferredItem<reika.chromaticraft.items.tools.powered.ItemPurifyCrystal> PURIFY =
			reg("purification_gem", () -> new reika.chromaticraft.items.tools.powered.ItemPurifyCrystal(itemProperties()));
	/** V33a PROBE, Examination Crystals (sprites 53-56 by charge state). */
	public static final DeferredItem<reika.chromaticraft.items.tools.powered.ItemCrystalProbe> PROBE =
			reg("probe", () -> new reika.chromaticraft.items.tools.powered.ItemCrystalProbe(itemProperties()));
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemLightGun> LIGHT_GUN =
			reg("light_gun", () -> new reika.chromaticraft.items.tools.ItemLightGun(itemProperties().stacksTo(1)));
	/** V33a FINDER, the Pylon Compass (a one-item ItemChromaTool). */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemPylonFinder> PYLON_FINDER =
			reg("pylon_finder", () -> new reika.chromaticraft.items.tools.ItemPylonFinder(itemProperties().stacksTo(1)));
	/** V33a STRUCTMAP, the Artifact Distribution Map; its seek mode and scan live in CUSTOM_DATA. */
	public static final DeferredItem<reika.chromaticraft.items.tools.ItemStructureMap> STRUCTURE_MAP =
			reg("structure_map", () -> new reika.chromaticraft.items.tools.ItemStructureMap(itemProperties()));
	/**
	 * V33a POTION, the Crystal Potion: one item per colour V33a could make (CrystalPotionController
	 * requiresCustomPotion), and its splash form, which V33a kept in the same damage value.
	 */
	public static final EnumMap<CrystalElement, DeferredItem<reika.chromaticraft.items.tools.ItemCrystalPotion>> CRYSTAL_POTIONS =
			new EnumMap<>(CrystalElement.class);
	public static final EnumMap<CrystalElement, DeferredItem<reika.chromaticraft.items.tools.ItemSplashCrystalPotion>> SPLASH_CRYSTAL_POTIONS =
			new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement e : CrystalElement.elements) {
			if (!reika.chromaticraft.items.tools.ItemCrystalPotion.isCrystalPotionColor(e))
				continue;
			CRYSTAL_POTIONS.put(e, reg("crystal_potion_" + e.getEnglishName(),
					() -> new reika.chromaticraft.items.tools.ItemCrystalPotion(e, itemProperties().stacksTo(1)
							.component(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
									net.minecraft.world.item.alchemy.PotionContents.EMPTY)
							.component(net.minecraft.core.component.DataComponents.CONSUMABLE,
									net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
							.usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE))));
			SPLASH_CRYSTAL_POTIONS.put(e, reg("splash_crystal_potion_" + e.getEnglishName(),
					() -> new reika.chromaticraft.items.tools.ItemSplashCrystalPotion(e, itemProperties().stacksTo(1)
							.component(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
									net.minecraft.world.item.alchemy.PotionContents.EMPTY))));
		}
	}
	/**
	 * V33a VOIDCELL / CRYSTALCELL / SHIELDEDCELL: Applied Energistics 2 storage cells, registered only with AE2 (null
	 * otherwise), as V33a's {@code ModList.APPENG} prerequisite dummied them out. See {@code modinterface.ae}.
	 */
	public static final DeferredItem<Item> VOID_CELL = reika.dragonapi.ModList.APPENG.isLoaded()
			? reg("void_cell", reika.chromaticraft.modinterface.ae.ChromaAEItems::voidCell) : null;
	public static final DeferredItem<Item> CRYSTAL_CELL = reika.dragonapi.ModList.APPENG.isLoaded()
			? reg("crystal_cell", reika.chromaticraft.modinterface.ae.ChromaAEItems::crystalCell) : null;
	public static final DeferredItem<Item> SHIELDED_CELL = reika.dragonapi.ModList.APPENG.isLoaded()
			? reg("shielded_cell", reika.chromaticraft.modinterface.ae.ChromaAEItems::shieldedCell) : null;

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

	// CHROMA-PORT: ItemMagicRegistry/ItemElementCalculator retain the original LIGHTGRAY 2, GRAY 1, PINK 2, GREEN 1 composition; bind this identity when that subsystem ports.
    public static final DeferredItem<Item> VOID_MONSTER_ESSENCE = reg("void_monster_essence", () -> new Item(itemProperties()));
    private ChromaItems() {}
	/** Former BERRY metadata split into sixteen stable color identities. */
	public static final EnumMap<CrystalElement, DeferredItem<ItemChromaBerry>> BERRIES = new EnumMap<>(CrystalElement.class);
	/** Former ELEMENTAL metadata split into sixteen stable color identities. */
	public static final EnumMap<CrystalElement, DeferredItem<ItemElementalStone>> ELEMENTAL_STONES = new EnumMap<>(CrystalElement.class);
	static {
		// Separate loops: creative tabs list items in registration order, and V33a showed each family's
		// sixteen metadata values together rather than interleaved.
		for (CrystalElement element : CrystalElement.elements)
			BERRIES.put(element, reg("chroma_berries_" + element.getEnglishName(),
					() -> new ItemChromaBerry(element, itemProperties())));
		for (CrystalElement element : CrystalElement.elements)
			ELEMENTAL_STONES.put(element, reg("elemental_stone_" + element.getEnglishName(),
					() -> new ItemElementalStone(element, itemProperties())));
	}

	/**
	 * V33a SEED ("Crystal Seeds"), whose colour was {@code meta % 16}, split into sixteen identities; the modifier
	 * bits above the colour and the sterile flag are item data (see {@link reika.chromaticraft.items.ItemCrystalSeeds}).
	 */
	public static final EnumMap<CrystalElement, DeferredItem<reika.chromaticraft.items.ItemCrystalSeeds>> CRYSTAL_SEEDS =
			new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement element : CrystalElement.elements)
			CRYSTAL_SEEDS.put(element, reg("crystal_seeds_" + element.getEnglishName(),
					() -> new reika.chromaticraft.items.ItemCrystalSeeds(element, itemProperties())));
	}

	/** V33a PENDANT ("Pendant") and PENDANT3 ("Enhanced Pendant"), metadata 0-15 split per element. */
	public static final EnumMap<CrystalElement, DeferredItem<reika.chromaticraft.items.tools.ItemPendant>> PENDANTS =
			new EnumMap<>(CrystalElement.class);
	public static final EnumMap<CrystalElement, DeferredItem<reika.chromaticraft.items.tools.ItemBoostedPendant>> BOOSTED_PENDANTS =
			new EnumMap<>(CrystalElement.class);
	static {
		for (CrystalElement element : CrystalElement.elements)
			PENDANTS.put(element, reg("pendant_" + element.getEnglishName(),
					() -> new reika.chromaticraft.items.tools.ItemPendant(element, itemProperties())));
		for (CrystalElement element : CrystalElement.elements)
			BOOSTED_PENDANTS.put(element, reg("enhanced_pendant_" + element.getEnglishName(),
					() -> new reika.chromaticraft.items.tools.ItemBoostedPendant(element, itemProperties())));
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
	/** V33a's Ethereal Luma container, promoted from the legacy fluid-container integration. */
	public static final DeferredItem<Item> LUMA_BUCKET = reg("ethereal_luma_bucket",
			() -> new BucketItem(ChromaFluids.LUMA.get(), itemProperties().craftRemainder(Items.BUCKET).stacksTo(1)));
	/** V33a Liquid Ender container, now a normal source-fluid bucket. */
	public static final DeferredItem<Item> ENDER_BUCKET = reg("liquid_ender_bucket",
			() -> new BucketItem(ChromaFluids.ENDER.get(), itemProperties().craftRemainder(Items.BUCKET).stacksTo(1)));
}
