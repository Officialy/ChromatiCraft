package reika.chromaticraft.client.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.registry.*;

import java.util.ArrayList;
import java.util.List;

/** Binds V33a guide source identities to the modern registries as each content family lands. */
public final class LexiconIconResolver {

	private LexiconIconResolver() {}

	public static ItemStack icon(LexiconCatalog.Entry entry) {
		// V33a ChromaResearch.ACCEL's icon is the light blue Tile Accelerator, though its stacks list runs from black.
		if ("adjacency".equals(entry.sourceId()))
			return reika.chromaticraft.registry.AdjacencyUpgrades.LIGHTBLUE.getStackOfTier(0);
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
			case "coloraltar" -> addElements(items,color -> ChromaBlocks.COLORED_ALTARS.get(color).get());
            case "elemental" -> addElements(items, element -> ChromaItems.ELEMENTAL_STONES.get(element).get());
			case "crystal" -> addElements(items, element -> ChromaBlocks.caveCrystal(element).get());
			case "rune" -> addElements(items, element -> ChromaBlocks.rune(element).get());
			case "dyeleaf" -> addElements(items, element -> ChromaBlocks.dyeLeaves(element).get());
			case "lamp" -> addElements(items, element -> ChromaBlocks.crystalLamp(element).get());
			case "glass" -> addElements(items, element -> ChromaBlocks.crystalGlass(element).get());
			case "glow" -> {
				for (var holder : ChromaBlocks.CRYSTAL_GLOWS)
					items.add(new ItemStack(holder.get()));
			}
			case "super" -> addElements(items, element -> ChromaBlocks.superCrystal(element).get());
			case "berry" -> addElements(items, element -> ChromaItems.BERRIES.get(element).get());
			// V33a ChromaResearch.PENDANT: each colour's Pendant followed by its Enhanced Pendant.
			case "pendant" -> {
				for (CrystalElement element : CrystalElement.elements) {
					items.add(new ItemStack(ChromaItems.PENDANTS.get(element).get()));
					items.add(new ItemStack(ChromaItems.BOOSTED_PENDANTS.get(element).get()));
				}
			}
			// V33a ChromaResearch.getItemStacks for ACCEL: every tier of each implemented colour.
			case "adjacency" -> {
				for (reika.chromaticraft.registry.AdjacencyUpgrades a : reika.chromaticraft.registry.AdjacencyUpgrades.upgrades) {
					if (a.isImplemented()) {
						for (int k = 0; k < reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade.MAX_TIER; k++)
							items.add(a.getStackOfTier(k));
					}
				}
			}
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
			case "relay" -> items.add(new ItemStack(ChromaBlocks.RELAY_SOURCE.get()));
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
			case "weakrepeater" -> ChromaBlocks.WEAK_REPEATER.get();
			case "skypeater" -> ChromaBlocks.SKYPEATER.get();
			case "compound" -> ChromaBlocks.COMPOUND.get();
			case "pylonlink" -> ChromaBlocks.PYLON_LINK.get();
			case "stand" -> ChromaBlocks.ITEM_STAND.get();
			case "table" -> ChromaBlocks.CASTING_TABLE.get();
			case "ritual" -> ChromaBlocks.RITUAL_TABLE.get();
			case "collector" -> ChromaBlocks.COLLECTOR.get();
			case "brewer" -> ChromaBlocks.CRYSTAL_BREWER.get();
			case "launchpad" -> ChromaBlocks.LAUNCH_PAD.get();
			case "particles" -> ChromaBlocks.PARTICLE_SPAWNER.get();
			case "turret" -> ChromaBlocks.LUMEN_TURRET.get();
			case "voidtrap" -> ChromaBlocks.VOID_MONSTER_TRAP.get();
            case "deathfog" -> ChromaBlocks.DEATH_FOG_EMITTER.get();
            case "lumenwire" -> ChromaBlocks.LUMEN_WIRE.get();
			case "fluidrelay" -> ChromaBlocks.FLUID_RELAY.get();
			case "medistributor" -> ChromaBlocks.ME_DISTRIBUTOR.get();
			case "telepump" -> ChromaBlocks.TELEPUMP.get();
			case "rift" -> ChromaItems.WORLD_RIFT.get();
			case "enchanter" -> ChromaBlocks.AUTO_ENCHANTER.get();
			case "toolstorage" -> ChromaBlocks.TOOL_STORAGE.get();
			case "chromaflower" -> ChromaBlocks.CHROMA_FLOWER.get();
			case "focuscrystal" -> ChromaBlocks.FOCUS_CRYSTAL.get();
			case "personal" -> ChromaBlocks.PERSONAL_CHARGER.get();
			case "relaysource" -> ChromaBlocks.RELAY_SOURCE.get();
			case "funcrelay", "functionrelay" -> ChromaBlocks.FUNCTION_RELAY.get();
			default -> null;
		};
	}

	private static ItemLike block(String id) {
		return switch (id) {
			case "crystal" -> ChromaBlocks.caveCrystal(CrystalElement.PURPLE).get();
			case "rune" -> ChromaBlocks.rune(CrystalElement.LIGHTBLUE).get();
			case "glass" -> ChromaBlocks.crystalGlass(CrystalElement.BLUE).get(); // V33a GLASS research icon meta
			case "selectiveglass" -> ChromaBlocks.SELECTIVE_GLASS.get();
			case "glow" -> ChromaBlocks.crystalGlow(CrystalElement.RED,
					reika.chromaticraft.block.crystal.BlockCrystalGlow.Bases.STONE).get(); // V33a GLOW icon meta RED
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
			case "coloraltar" -> ChromaBlocks.COLORED_ALTARS.get(CrystalElement.BLUE).get();
            case "elemental" -> ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLUE).get();
			case "datacrystal" -> ChromaItems.DATA_CRYSTAL.get();
			case "fertilityseed" -> ChromaItems.FERTILITY_SEEDS.getFirst().get();
			// V33a ChromaResearch.SEED: ChromaItems.SEED.getStackOf(CrystalElement.MAGENTA).
			case "seed" -> ChromaItems.CRYSTAL_SEEDS.get(CrystalElement.MAGENTA).get();
			case "purify" -> ChromaItems.PURIFY.get();
			case "efficiency" -> ChromaItems.EFFICIENCY_GEM.get();
			case "warpcapsule" -> ChromaItems.RECALL_TABLET.get();
			case "capture" -> ChromaItems.CAPTURE_WAND.get();
			case "excavator" -> ChromaItems.EXCAVATION_WAND.get();
			case "builder" -> ChromaItems.CONSTRUCTION_WAND.get();
			case "hoverwand" -> ChromaItems.HOVER_WAND.get();
			case "mobsonar" -> ChromaItems.MOB_SONAR.get();
			case "multitool" -> ChromaItems.MULTITOOL.get();
			case "bulkmover" -> ChromaItems.BULK_MOVER.get();
			case "bottleneck" -> ChromaItems.BOTTLENECK_FINDER.get();
			case "spawnerbypass" -> ChromaItems.SPAWNER_BYPASS.get();
			case "branches" -> ChromaItems.BRANCHES.get(reika.chromaticraft.items.ItemMagicBranch.BranchTypes.CRYSTAL).get();
			case "oresilk" -> ChromaItems.ORE_HARVESTER.get();
			case "recipecache" -> ChromaItems.RECIPE_TABLET.get();
			case "aurapouch" -> ChromaItems.AURA_POUCH.get();
			case "etherpendant" -> ChromaItems.LUMA_CAPSULE.get();
			case "teleport" -> ChromaItems.MOBILITY_WAND.get();
			case "growth" -> ChromaItems.GROWTH_WAND.get();
			case "endereye" -> ChromaItems.ENDER_EYE.get();
			case "artefact" -> ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get();
			case "structmap" -> ChromaItems.STRUCTURE_MAP.get();
			case "finder" -> ChromaItems.PYLON_FINDER.get();
			case "lightgun" -> ChromaItems.LIGHT_GUN.get();
			case "probe" -> ChromaItems.PROBE.get();
			case "share" -> ChromaItems.OWNERSHIP_KEY.get();
			case "cavepather" -> ChromaItems.CAVE_PATHER.get();
			// Applied Energistics 2 cells; unregistered (null) without AE2, like their V33a pages.
			case "voidcell" -> ChromaItems.VOID_CELL != null ? ChromaItems.VOID_CELL.get() : null;
			case "crystalcell" -> ChromaItems.CRYSTAL_CELL != null ? ChromaItems.CRYSTAL_CELL.get() : null;
			case "shieldedcell" -> ChromaItems.SHIELDED_CELL != null ? ChromaItems.SHIELDED_CELL.get() : null;
			default -> null;
		};
	}

	private static ItemLike resource(String id) {
		return switch (id) {
			case "voidessence" -> ChromaItems.VOID_MONSTER_ESSENCE.get();
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
			case "nethertrap", "voidritual" -> ChromaBlocks.VOID_MONSTER_TRAP.get();
            case "pylon", "pylonbroadcast" -> ChromaBlocks.PYLON.get();
			case "casting1", "casting2", "casting3" -> ChromaBlocks.CASTING_TABLE.get();
			case "repeater" -> ChromaBlocks.REPEATER.get();
			case "weakrepeater" -> ChromaBlocks.WEAK_REPEATER.get();
			case "compound" -> ChromaBlocks.COMPOUND.get();
			case "personal" -> ChromaBlocks.PERSONAL_CHARGER.get();
			case "relay" -> ChromaBlocks.RELAY_SOURCE.get();
			case "ritual", "ritual2" -> ChromaBlocks.RITUAL_TABLE.get();
			default -> ChromaBlocks.crystallineStone(
					reika.chromaticraft.block.BlockCrystallineStone.StoneTypes.SMOOTH).get();
		};
	}
}
