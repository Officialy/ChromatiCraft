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
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import reika.chromaticraft.block.BlockEncrustedCrystal;

import reika.chromaticraft.block.BlockChromaFluid;
import reika.chromaticraft.block.BlockChromaMud;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.block.worldgen26.BlockGlowDaisy;
import reika.chromaticraft.block.worldgen26.BlockGlowRoot;
import net.minecraft.world.item.Items;
import reika.chromaticraft.auxiliary.loot.ChromaBerryCount;
import reika.chromaticraft.auxiliary.loot.FortuneScaledChance;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/**
 * ChromatiCraft loot tables (port-in-progress). Emits a drops-self table for every registered block so
 * the vanilla loot-table validator is satisfied; specialised drops are added as those blocks port.
 *
 * <p>Crystalline stone ({@link BlockCrystallineStone}) is one block with 16 {@code TYPE} variants, each
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
				if (block instanceof reika.chromaticraft.block.crystal.BlockCaveCrystal crystal) {
					// V33a BlockCaveCrystal.getDrops: never itself, always N shards of its own colour.
					this.add(block, this.caveCrystalTable(block, crystal.getCrystalElement()));
				}
				else if (block instanceof reika.chromaticraft.block.dye26.BlockDyeLeaf leaf) {
					this.add(block, this.dyeLeafTable(block, leaf.getElement()));
				}
				else if (block instanceof BlockCrystallineStone stone) {
					this.add(block, this.crystallineStoneTable(stone));
				}
				else if (block instanceof BlockCrystalRune) {
					this.dropSelf(block);
				}
				else if (block instanceof BlockEncrustedCrystal) {
					this.add(block, noDrop()); // Synchronized face-growth state emits the original shard drops.
				}
				else if (block instanceof BlockChromaFluid || block.asItem() == net.minecraft.world.item.Items.AIR) {
					this.add(block, noDrop());
				}
				else if (block instanceof BlockChromaMud) {
					this.add(block, createSingleItemTable(net.minecraft.world.level.block.Blocks.DIRT));
				}
				else if (block instanceof BlockGlowDaisy) {
					this.add(block, this.glowstoneDustTable(block, 0.5F));
				}
				else if (block instanceof BlockGlowRoot) {
					this.add(block, this.glowstoneDustTable(block, 0.25F));
				}
				else {
					this.dropSelf(block);
				}
			}
		}

		/**
		 * V33a cave crystals are not silk-touchable into themselves and have no explosion drop; they
		 * always yield shards of their own colour, in the compound Fortune-scaled count from
		 * {@link reika.chromaticraft.auxiliary.loot.CrystalShardCount}. The
		 * {@code survives_explosion} condition reproduces V33a's {@code canDropFromExplosion()==false}.
		 */
		private LootTable.Builder caveCrystalTable(Block block, CrystalElement element) {
			return LootTable.lootTable().withPool(this.applyExplosionCondition(block,
					LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1))
							.add(LootItem.lootTableItem(ChromaItems.SHARDS.get(element).get())
									.apply(SetItemCountFunction.setCount(
											reika.chromaticraft.auxiliary.loot.CrystalShardCount.INSTANCE)))));
		}

		/**
		 * V33a {@code BlockDyeLeaf.getDrops}. Shearing (or silk touch) yields the leaf block and
		 * nothing else, matching {@code onSheared}; otherwise every entry below rolls independently
		 * with its own Fortune curve, and the leaf itself never drops.
		 *
		 * <p>The dye entry is the vanilla dye rather than ChromatiCraft's own: V33a rolls
		 * {@code getVanillaDyeChance}, whose default is 100, and {@code doWithChance(>=100)} always
		 * succeeds — so on default config the vanilla dye is the only outcome. ItemCrystalDye is not
		 * ported, so the non-default branch is a CHROMA-PORT for when it lands.
		 */
		private LootTable.Builder dyeLeafTable(Block block, CrystalElement element) {
			LootTable.Builder table = LootTable.lootTable();
			table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.when(this.hasShears().or(this.hasSilkTouch()))
					.add(LootItem.lootTableItem(block)));

			LootItemCondition.Builder notSheared = this.hasShears().or(this.hasSilkTouch()).invert();
			this.addDyeLeafDrop(table, notSheared, LootItem.lootTableItem(
					ChromaBlocks.dyeSapling(element).get()), FortuneScaledChance.linear(0.05, 1));
			this.addDyeLeafDrop(table, notSheared, LootItem.lootTableItem(Items.APPLE),
					FortuneScaledChance.linear(0.005, 5));
			this.addDyeLeafDrop(table, notSheared, LootItem.lootTableItem(vanillaDye(element)),
					FortuneScaledChance.linear(0.1, 1));
			this.addDyeLeafDrop(table, notSheared, LootItem.lootTableItem(
					ChromaBlocks.RAINBOW_SAPLING.get()), FortuneScaledChance.quadratic(0.0001));
			this.addDyeLeafDrop(table, notSheared,
					LootItem.lootTableItem(ChromaItems.BERRIES.get(element).get())
							.apply(SetItemCountFunction.setCount(ChromaBerryCount.INSTANCE)),
					FortuneScaledChance.exponential(ChromaBerryCount.BASE_CHANCE));
			return table;
		}

		private void addDyeLeafDrop(LootTable.Builder table, LootItemCondition.Builder notSheared,
				LootPoolSingletonContainer.Builder<?> entry, FortuneScaledChance chance) {
			table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.when(notSheared).when(() -> chance).add(entry));
		}

		/**
		 * The vanilla dye whose colour matches this crystal element. Mapped by name, not by ordinal:
		 * CrystalElement's order is the 1.7.10 dye *damage* order (black=0, red=1, green=2, ...),
		 * which is not modern DyeColor's id order, so DyeColor.byId(ordinal) would give wrong colours.
		 * 26.2 folded the sixteen dye items into one ColorCollection, hence Items.DYE.pick(...).
		 */
		private static Item vanillaDye(CrystalElement element) {
			net.minecraft.world.item.DyeColor colour = switch (element) {
				case BLACK -> net.minecraft.world.item.DyeColor.BLACK;
				case RED -> net.minecraft.world.item.DyeColor.RED;
				case GREEN -> net.minecraft.world.item.DyeColor.GREEN;
				case BROWN -> net.minecraft.world.item.DyeColor.BROWN;
				case BLUE -> net.minecraft.world.item.DyeColor.BLUE;
				case PURPLE -> net.minecraft.world.item.DyeColor.PURPLE;
				case CYAN -> net.minecraft.world.item.DyeColor.CYAN;
				case LIGHTGRAY -> net.minecraft.world.item.DyeColor.LIGHT_GRAY;
				case GRAY -> net.minecraft.world.item.DyeColor.GRAY;
				case PINK -> net.minecraft.world.item.DyeColor.PINK;
				case LIME -> net.minecraft.world.item.DyeColor.LIME;
				case YELLOW -> net.minecraft.world.item.DyeColor.YELLOW;
				case LIGHTBLUE -> net.minecraft.world.item.DyeColor.LIGHT_BLUE;
				case MAGENTA -> net.minecraft.world.item.DyeColor.MAGENTA;
				case ORANGE -> net.minecraft.world.item.DyeColor.ORANGE;
				case WHITE -> net.minecraft.world.item.DyeColor.WHITE;
			};
			return Items.DYE.pick(colour);
		}

		private LootTable.Builder glowstoneDustTable(Block block, float chance) {
			return createShearsDispatchTable(block,
					LootItem.lootTableItem(net.minecraft.world.item.Items.GLOWSTONE_DUST)
							.when(LootItemRandomChanceCondition.randomChance(chance)));
		}
		/**
		 * Each crystalline-stone variant is its own block now, so this is a plain self-drop except for
		 * the three glow variants, which V33a degrades to their base form unless silk-touched
		 * ({@code needsSilkTouch} + {@code damageDropped}). The old per-{@code TYPE} state dispatch
		 * existed only because all sixteen shared one block.
		 */
		private LootTable.Builder crystallineStoneTable(BlockCrystallineStone block) {
			StoneTypes type = block.getStoneType();
			LootPoolSingletonContainer.Builder<?> self = LootItem.lootTableItem(block);
			LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
			if (type.needsSilkTouch()) {
				Item base = ChromaBlocks.crystallineStone(type.getDropVariant()).get().asItem();
				pool.add(self.when(this.hasSilkTouch()).otherwise(LootItem.lootTableItem(base)));
			}
			else {
				pool.add(self);
			}
			return LootTable.lootTable().withPool(pool);
		}


		@Override
		protected Iterable<Block> getKnownBlocks() {
			return ChromaBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get()).toList();
		}
	}
}
