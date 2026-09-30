package reika.chromaticraft;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/** GameTests for ChromatiCraft's Applied Energistics 2 content. Only loaded and registered when AE2 is present. */
final class ChromaAETests {
	private ChromaAETests() {}

	static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
		ChromaGameTests.register(event, env, "ae2_crystal_cell_one_colour_family", 20, ChromaAETests::crystalCellFamily);
		ChromaGameTests.register(event, env, "ae2_void_cell_one_type", 20, ChromaAETests::voidCellOneType);
	}

	private static long insert(StorageCell cell, net.minecraft.world.item.Item item, int n) {
		return cell.insert(AEItemKey.of(item), n, Actionable.MODULATE, IActionSource.empty());
	}

	/** V33a: a crystal cell takes all sixteen colours of one crystal type, and nothing else once it holds one. */
	static void crystalCellFamily(GameTestHelper helper) {
		ItemStack cellStack = new ItemStack(ChromaItems.CRYSTAL_CELL.get());
		StorageCell cell = StorageCells.getCellInventory(cellStack, null);
		helper.assertTrue(cell != null, "the crystal cell must be an AE2 storage cell");
		helper.assertTrue(insert(cell, Items.DIAMOND, 1) == 0, "a non-crystal item must be refused");
		for (CrystalElement e : CrystalElement.elements)
			helper.assertTrue(insert(cell, ChromaItems.SHARDS.get(e).get(), 4) == 4, "every shard colour must fit, " + e + " did not");
		cell.persist();
		StorageCell reread = StorageCells.getCellInventory(cellStack, null);
		helper.assertTrue(insert(reread, ChromaItems.BOOSTED_SHARDS.get(CrystalElement.RED).get(), 1) == 0,
				"a boosted shard is a different crystal type and must be refused once plain shards are stored");
		helper.assertTrue(insert(reread, Items.DYE.red(), 1) == 0, "dye is a different type and must be refused");
		helper.assertTrue(reread.getAvailableStacks().get(AEItemKey.of(ChromaItems.SHARDS.get(CrystalElement.BLACK).get())) == 4,
				"stored shards must persist on the cell item");

		ItemStack dyeCell = new ItemStack(ChromaItems.CRYSTAL_CELL.get());
		StorageCell dyes = StorageCells.getCellInventory(dyeCell, null);
		helper.assertTrue(insert(dyes, Items.DYE.red(), 1) == 1 && insert(dyes, Items.DYE.blue(), 1) == 1, "vanilla dyes are one crystal type");
		helper.succeed();
	}

	/** V33a: the void cell holds exactly one item type. */
	static void voidCellOneType(GameTestHelper helper) {
		StorageCell cell = StorageCells.getCellInventory(new ItemStack(ChromaItems.VOID_CELL.get()), null);
		helper.assertTrue(cell != null, "the void cell must be an AE2 storage cell");
		helper.assertTrue(insert(cell, Items.COBBLESTONE, 100000) == 100000, "a void cell must hold a lot of one type");
		helper.assertTrue(insert(cell, Items.DIRT, 1) == 0, "a second type must be refused");
		helper.succeed();
	}
}
