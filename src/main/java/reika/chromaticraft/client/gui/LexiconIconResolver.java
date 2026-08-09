package reika.chromaticraft.client.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/** Binds V33a guide source identities to the modern registries as each content family lands. */
final class LexiconIconResolver {

	private LexiconIconResolver() {}

	static ItemStack icon(LexiconCatalog.Entry entry) {
		ItemLike item = switch (entry.sourceType()) {
			case "machine" -> machine(entry.sourceId());
			case "block" -> block(entry.sourceId());
			case "tool" -> tool(entry.sourceId());
			case "structure" -> structure(entry.sourceId());
			default -> null;
		};
		return item != null ? new ItemStack(item) : ItemStack.EMPTY;
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
			case "elemental" -> ChromaItems.ELEMENTAL_STONES.get(CrystalElement.BLUE).get();
			case "datacrystal" -> ChromaItems.DATA_CRYSTAL.get();
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
