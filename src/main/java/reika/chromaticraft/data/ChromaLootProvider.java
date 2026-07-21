package reika.chromaticraft.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;

import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/**
 * ChromatiCraft loot tables (port-in-progress). Emits a drops-self table for every registered block so
 * the vanilla loot-table validator is satisfied; specialised drops are added as those blocks port.
 *
 * <p>Crystalline stone ({@link BlockPylonStructure}) is one block with 16 {@code TYPE} variants, each
 * its own {@link net.minecraft.world.item.BlockItem}. A plain {@code dropSelf} would drop only the
 * variant-0 item for every state (the metadata-collapse trap), so it gets a per-state table: each
 * {@code TYPE} value drops its own item, and the three glow variants drop their base form unless
 * silk-touched (mirrors the 1.7.10 {@code damageDropped} + {@code needsSilkTouch}).
 */
public final class ChromaLootProvider extends LootTableProvider {

	public ChromaLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Set.of(), List.of(
				new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)
		), registries);
	}

	private static final class Blocks extends BlockLootSubProvider {

		Blocks(HolderLookup.Provider registries) {
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		protected void generate() {
			for (var holder : ChromaBlocks.BLOCKS.getEntries()) {
				Block block = holder.get();
				if (block instanceof BlockPylonStructure) {
					this.add(block, this.pylonStructureTable(block));
				}
				else if (block instanceof BlockCrystalRune) {
					this.add(block, this.runeTable(block));
				}
				else {
					this.dropSelf(block);
				}
			}
		}

		private LootTable.Builder pylonStructureTable(Block block) {
			LootTable.Builder table = LootTable.lootTable();
			for (StoneTypes t : StoneTypes.list) {
				LootItemCondition.Builder isType = LootItemBlockStatePropertyCondition
						.hasBlockStateProperties(block)
						.setProperties(StatePropertiesPredicate.Builder.properties()
								.hasProperty(BlockPylonStructure.TYPE, t.ordinal()));
				Item self = ChromaBlocks.PYLONSTRUCT_ITEMS.get(t.ordinal()).get();
				LootPoolSingletonContainer.Builder<?> selfEntry = LootItem.lootTableItem(self);
				LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(isType);
				if (t.needsSilkTouch()) {
					// With silk touch drops itself; otherwise the glow variant degrades to its base form.
					Item base = ChromaBlocks.PYLONSTRUCT_ITEMS.get(t.getDropVariant().ordinal()).get();
					pool.add(selfEntry.when(this.hasSilkTouch()).otherwise(LootItem.lootTableItem(base)));
				}
				else {
					pool.add(selfEntry);
				}
				table.withPool(pool);
			}
			return table;
		}

		/** Per-colour loot: each COLOR value drops its own rune item. */
		private LootTable.Builder runeTable(Block block) {
			LootTable.Builder table = LootTable.lootTable();
			for (CrystalElement e : CrystalElement.elements) {
				LootItemCondition.Builder isColor = LootItemBlockStatePropertyCondition
						.hasBlockStateProperties(block)
						.setProperties(StatePropertiesPredicate.Builder.properties()
								.hasProperty(BlockCrystalRune.COLOR, e.ordinal()));
				table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(isColor)
						.add(LootItem.lootTableItem(ChromaBlocks.RUNE_ITEMS.get(e.ordinal()).get())));
			}
			return table;
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return ChromaBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get()).toList();
		}
	}
}
