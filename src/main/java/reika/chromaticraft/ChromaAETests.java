package reika.chromaticraft;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.storage.DriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import reika.chromaticraft.block.BlockMEDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/** GameTests for ChromatiCraft's Applied Energistics 2 content. Only loaded and registered when AE2 is present. */
final class ChromaAETests {
	private ChromaAETests() {}

	static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
		ChromaGameTests.register(event, env, "ae2_crystal_cell_one_colour_family", 20, ChromaAETests::crystalCellFamily);
		ChromaGameTests.register(event, env, "ae2_void_cell_one_type", 20, ChromaAETests::voidCellOneType);
		ChromaGameTests.register(event, env, "ae2_me_distributor_stocks_inventory", 400, ChromaAETests::meDistributorStocksInventory);
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

	/**
	 * V33a ME Distributor: mounted on a chest (facing up, the drive above it), each rule moves its transfer item out
	 * of the network into the chest while the network holds fewer of the rule's query item than the threshold.
	 */
	static void meDistributorStocksInventory(GameTestHelper helper) {
		BlockPos chest = new BlockPos(2, 1, 2);
		BlockPos dist = chest.above();
		BlockPos drivePos = dist.above();
		helper.setBlock(chest, Blocks.CHEST);
		helper.setBlock(dist, ChromaBlocks.ME_DISTRIBUTOR.get().defaultBlockState().setValue(BlockMEDistributor.FACING, Direction.UP));
		helper.setBlock(drivePos, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("ae2", "drive")));
		helper.setBlock(drivePos.east(), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("ae2", "creative_energy_cell")));
		helper.getBlockEntity(drivePos, DriveBlockEntity.class).getInternalInventory().setItemDirect(0,
				new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ae2", "item_storage_cell_1k"))));

		TileEntityMEDistributor med = helper.getBlockEntity(dist, TileEntityMEDistributor.class);
		//rule 0: while the network has fewer than 5 gold, send iron (there are 2 gold, so all the iron goes)
		med.setMapping(0, new ItemStack(Items.GOLD_INGOT)); //slot i: counted against the threshold
		med.setMapping(TileEntityMEDistributor.NSLOTS, new ItemStack(Items.IRON_INGOT)); //slot i+NSLOTS: moved
		med.setThreshold(0, 5);
		//rule 1: while the network has fewer than 1 cobblestone, send gold (there are 5, so no gold moves)
		med.setMapping(1, new ItemStack(Items.COBBLESTONE));
		med.setMapping(1 + TileEntityMEDistributor.NSLOTS, new ItemStack(Items.GOLD_INGOT));
		med.setThreshold(1, 1);

		helper.runAfterDelay(10, () -> {
			MEStorage c = cell(helper, drivePos);
			c.insert(AEItemKey.of(Items.IRON_INGOT), 10, Actionable.MODULATE, IActionSource.empty());
			c.insert(AEItemKey.of(Items.GOLD_INGOT), 2, Actionable.MODULATE, IActionSource.empty());
			c.insert(AEItemKey.of(Items.COBBLESTONE), 5, Actionable.MODULATE, IActionSource.empty());
		});
		helper.runAfterDelay(300, () -> { //grid boot + the 40-tick cache pass + a few 10..40-tick checks
			ChestBlockEntity inv = helper.getBlockEntity(chest, ChestBlockEntity.class);
			int iron = 0, gold = 0;
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack is = inv.getItem(i);
				if (is.is(Items.IRON_INGOT))
					iron += is.getCount();
				if (is.is(Items.GOLD_INGOT))
					gold += is.getCount();
			}
			MEStorage c = cell(helper, drivePos);
			helper.assertTrue(iron == 10, "all ten iron must be moved into the chest, has " + iron);
			helper.assertTrue(c.getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT)) == 0, "the moved iron must leave the network");
			helper.assertTrue(gold == 0 && c.getAvailableStacks().get(AEItemKey.of(Items.GOLD_INGOT)) == 2,
					"gold must stay in the network while its rule's query is above threshold, chest has " + gold);
			helper.succeed();
		});
	}

	private static MEStorage cell(GameTestHelper helper, BlockPos drivePos) {
		MEStorage c = helper.getBlockEntity(drivePos, DriveBlockEntity.class).getCellInventory(0);
		helper.assertTrue(c != null, "the drive must expose its cell");
		return c;
	}
}
