package reika.chromaticraft.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
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
import reika.chromaticraft.block.worldgen26.BlockTieredOre;
import reika.chromaticraft.block.worldgen26.BlockTieredPlant;
import reika.chromaticraft.block.worldgen26.BlockCaveIndicator;
import reika.chromaticraft.block.worldgen26.BlockDecoFlower;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.world.OverworldStructureFeature;

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

	public static final ResourceKey<LootTable> VILLAGE_CASTING_LOOT = ResourceKey.create(
			net.minecraft.core.registries.Registries.LOOT_TABLE,
			net.minecraft.resources.Identifier.fromNamespaceAndPath("chromaticraft", "chests/village_casting"));
	public static final ResourceKey<LootTable> VILLAGE_CASTING_JUNK = ResourceKey.create(
			net.minecraft.core.registries.Registries.LOOT_TABLE,
			net.minecraft.resources.Identifier.fromNamespaceAndPath("chromaticraft", "chests/village_casting_junk"));

	public ChromaLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Set.of(), List.of(
				new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK),
				new SubProviderEntry(BurrowCache::new, LootContextParamSets.CHEST),
				new SubProviderEntry(VillageCasting::new, LootContextParamSets.CHEST),
				// V33a ChromaChests' injections into the vanilla chest tables; attached by
				// ChromaLootModifierProvider.
				new SubProviderEntry(ChromaChestLoot::new, LootContextParamSets.CHEST)
		), registries);
	}

	/** V33a village-blacksmith loot plus ChromaChests' VILLAGE additions and the bonus roll. */
	private record VillageCasting(HolderLookup.Provider registries) implements LootTableSubProvider {
		@Override
		public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
			// Copy the active 26.2 weaponsmith chest pools into this table instead of nesting the
			// vanilla table. LootTableProvider validates only the tables generated by this provider,
			// so a cross-pack reference is rejected even though it resolves at runtime. Keeping these
			// entries here also makes the village casting chest a self-contained data-driven table.
			LootPool.Builder weaponsmith = LootPool.lootPool().setRolls(UniformGenerator.between(3, 8));
			add(weaponsmith, Items.DIAMOND, 1, 3, 3);
			add(weaponsmith, Items.IRON_INGOT, 1, 5, 10);
			add(weaponsmith, Items.GOLD_INGOT, 1, 3, 5);
			add(weaponsmith, Items.BREAD, 1, 3, 15);
			add(weaponsmith, Items.APPLE, 1, 3, 15);
			add(weaponsmith, Items.IRON_PICKAXE, 1, 1, 5);
			add(weaponsmith, Items.IRON_SWORD, 1, 1, 5);
			add(weaponsmith, Items.IRON_SPEAR, 1, 1, 5);
			add(weaponsmith, Items.COPPER_SPEAR, 1, 1, 7);
			add(weaponsmith, Items.IRON_CHESTPLATE, 1, 1, 5);
			add(weaponsmith, Items.IRON_HELMET, 1, 1, 5);
			add(weaponsmith, Items.IRON_LEGGINGS, 1, 1, 5);
			add(weaponsmith, Items.IRON_BOOTS, 1, 1, 5);
			add(weaponsmith, Items.OBSIDIAN, 3, 7, 5);
			add(weaponsmith, Items.OAK_SAPLING, 3, 7, 5);
			add(weaponsmith, Items.SADDLE, 1, 1, 3);
			add(weaponsmith, Items.COPPER_HORSE_ARMOR, 1, 1, 1);
			add(weaponsmith, Items.IRON_HORSE_ARMOR, 1, 1, 1);
			add(weaponsmith, Items.GOLDEN_HORSE_ARMOR, 1, 1, 1);
			add(weaponsmith, Items.DIAMOND_HORSE_ARMOR, 1, 1, 1);

			LootPool.Builder bundle = LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.add(LootItem.lootTableItem(Items.BUNDLE))
					.add(EmptyLootItem.emptyItem().setWeight(2));
			LootPool.Builder chroma = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
			for (CrystalElement element : CrystalElement.elements)
				add(chroma, ChromaItems.SHARDS.get(element).get(), 1, 3, 2);
			add(chroma, ChromaItems.LEXICON.get(), 1, 1, 1);
			add(chroma, ChromaItems.INFO_FRAGMENT.get(), 1, 1, 5);
			output.accept(VILLAGE_CASTING_LOOT, LootTable.lootTable()
					// V33a starts from the active village blacksmith table.
					.withPool(weaponsmith)
					.withPool(bundle)
					// generateMultipliedLoot(1): one additional weighted draw.
					.withPool(chroma)
					// Unowned structure chests independently have the V33a 50% fragment insertion.
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
							.when(LootItemRandomChanceCondition.randomChance(0.5F))
							.add(LootItem.lootTableItem(ChromaItems.INFO_FRAGMENT.get()))));

			LootPool.Builder junk = LootPool.lootPool().setRolls(ConstantValue.exactly(10));
			add(junk, Items.COAL, 1, 1, 125);
			add(junk, Items.IRON_INGOT, 1, 1, 100);
			add(junk, Items.GOLD_INGOT, 1, 1, 50);
			add(junk, Items.REDSTONE, 1, 1, 75);
			add(junk, Items.DYE.pick(net.minecraft.world.item.DyeColor.BLUE), 1, 1, 40);
			add(junk, Items.DIAMOND, 1, 1, 10);
			add(junk, Items.EMERALD, 1, 1, 2);
			output.accept(VILLAGE_CASTING_JUNK, LootTable.lootTable().withPool(junk));
		}

		private static void add(LootPool.Builder pool, net.minecraft.world.level.ItemLike item,
				int minimum, int maximum, int weight) {
			pool.add(LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(
					UniformGenerator.between(minimum, maximum))));
		}
	}

	/** Data-driven form of V33a BurrowStructure.buildLootCache's weighted 13-20 draws. */
	private record BurrowCache(HolderLookup.Provider registries) implements LootTableSubProvider {
		@Override
		public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
			LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(13, 20));
			add(pool, Items.IRON_ORE, 12, 32, 35);
			add(pool, Items.GOLD_ORE, 8, 24, 15);
			addTag(pool, "ores/copper", 16, 40, 40);
			addTag(pool, "ores/tin", 16, 40, 40);
			addTag(pool, "ores/silver", 12, 24, 20);
			addTag(pool, "ores/nickel", 12, 24, 15);
			addTag(pool, "ores/lead", 12, 24, 20);
			add(pool, Items.DIAMOND, 1, 4, 5);
			add(pool, Items.DIAMOND, 3, 12, 2);
			add(pool, dye(net.minecraft.world.item.DyeColor.BLUE), 2, 6, 20);
			add(pool, dye(net.minecraft.world.item.DyeColor.BLUE), 12, 30, 5);
			add(pool, Items.REDSTONE, 4, 64, 40);
			add(pool, Items.COAL, 16, 64, 40);
			add(pool, Items.COAL, 4, 24, 60);
			add(pool, Items.GOLD_INGOT, 4, 16, 25);
			add(pool, Items.GOLD_INGOT, 16, 40, 10);
			add(pool, Items.IRON_INGOT, 16, 40, 60);
			add(pool, Items.IRON_INGOT, 32, 64, 20);
			addTag(pool, "ingots/nickel", 16, 48, 40);
			addTag(pool, "ingots/lead", 16, 48, 40);
			addTag(pool, "ingots/silver", 16, 48, 50);
			add(pool, Items.FLINT, 12, 32, 40);
			add(pool, Items.CLAY_BALL, 30, 60, 40);
			add(pool, Items.SLIME_BALL, 10, 20, 15);
			add(pool, Items.BONE, 5, 15, 40);
			add(pool, Items.ROTTEN_FLESH, 5, 15, 50);
			add(pool, Items.STRING, 10, 30, 50);
			add(pool, Items.GUNPOWDER, 5, 20, 30);
			add(pool, Items.LEATHER, 10, 25, 30);
			add(pool, Items.FEATHER, 5, 15, 25);
			add(pool, dye(net.minecraft.world.item.DyeColor.BLACK), 5, 15, 30);
			add(pool, Items.ENDER_PEARL, 4, 12, 10);
			add(pool, Items.WHEAT, 18, 30, 35);
			add(pool, Items.CARROT, 18, 30, 35);
			add(pool, Items.POTATO, 18, 30, 35);
			add(pool, Items.APPLE, 18, 30, 35);
			add(pool, Items.PORKCHOP, 8, 16, 20);
			add(pool, Items.BEEF, 8, 16, 20);
			add(pool, Items.COD, 8, 16, 20);
			add(pool, Items.CHICKEN, 8, 16, 20);
			add(pool, Items.SUGAR_CANE, 1, 6, 20);
			for (CrystalElement element : CrystalElement.elements) {
				add(pool, ChromaItems.SHARDS.get(element).get(), 2, 8, 5);
				add(pool, ChromaBlocks.caveCrystal(element).get(), 2, 8, 1);
			}
			add(pool, ChromaItems.TIERED.get(ChromaTieredItems.AURA_DUST).get(), 6, 30, 5);
			add(pool, Items.LAVA_BUCKET, 1, 1, 10);
			add(pool, Items.QUARTZ, 12, 32, 15);
			add(pool, Items.GLOWSTONE_DUST, 4, 12, 25);
			add(pool, Items.GLOWSTONE_DUST, 16, 32, 5);
			add(pool, Items.BLAZE_POWDER, 4, 8, 15);
			// V33a's second blaze-powder entry was Thaumcraft-only; modern Thaumcraft is absent.
			add(pool, Items.TORCH, 4, 20, 50);
			add(pool, Items.OAK_PLANKS, 24, 64, 50);
			add(pool, Items.SAND, 24, 64, 50);
			add(pool, Items.OBSIDIAN, 4, 8, 10);
			add(pool, Items.OBSIDIAN, 8, 16, 5);
			add(pool, Items.MOSSY_COBBLESTONE, 16, 32, 15);
			add(pool, Items.COBBLESTONE, 32, 64, 100);
			add(pool, Items.DIRT, 32, 64, 100);
			add(pool, Items.GRAVEL, 32, 64, 40);
			add(pool, dye(net.minecraft.world.item.DyeColor.RED), 4, 16, 20);
			add(pool, dye(net.minecraft.world.item.DyeColor.YELLOW), 4, 16, 20);
			add(pool, dye(net.minecraft.world.item.DyeColor.GREEN), 4, 16, 20);
			add(pool, Items.PAPER, 2, 8, 15);
			LootTable.Builder table = LootTable.lootTable().withPool(pool);
			// V33a independently adds ice in cold biomes (25%) and the dominant sapling (1/3).
			table.withPool(biomeBonus(Items.ICE, 12, 32, 0.25F,
					Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES,
					Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS));
			table.withPool(biomeBonus(Items.SPRUCE_SAPLING, 1, 6, 1F / 3F,
					Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
					Biomes.OLD_GROWTH_SPRUCE_TAIGA));
			table.withPool(biomeBonus(Items.BIRCH_SAPLING, 1, 6, 1F / 3F,
					Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST));
			table.withPool(biomeBonus(Items.JUNGLE_SAPLING, 1, 6, 1F / 3F,
					Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE));
			table.withPool(biomeBonus(Items.ACACIA_SAPLING, 1, 6, 1F / 3F,
					Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA));
			table.withPool(biomeBonus(Items.DARK_OAK_SAPLING, 1, 6, 1F / 3F,
					Biomes.DARK_FOREST));
			table.withPool(biomeBonus(Items.MANGROVE_PROPAGULE, 1, 6, 1F / 3F,
					Biomes.MANGROVE_SWAMP));
			table.withPool(biomeBonus(Items.CHERRY_SAPLING, 1, 6, 1F / 3F,
					Biomes.CHERRY_GROVE));
			table.withPool(biomeBonus(Items.OAK_SAPLING, 1, 6, 1F / 3F,
					Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST,
					Biomes.SWAMP, Biomes.WINDSWEPT_FOREST, Biomes.WOODED_BADLANDS));
			output.accept(OverworldStructureFeature.BURROW_CACHE_LOOT, table);
		}

		@SafeVarargs
		private LootPool.Builder biomeBonus(net.minecraft.world.level.ItemLike item, int minimum,
				int maximum, float chance, ResourceKey<Biome>... keys) {
			var biomes = registries.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
			HolderSet<Biome> set = HolderSet.direct(java.util.Arrays.stream(keys)
					.map(biomes::getOrThrow).toList());
			return LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.when(LootItemRandomChanceCondition.randomChance(chance))
					.when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(set)))
					.add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(
							UniformGenerator.between(minimum, maximum))));
		}

		private static void add(LootPool.Builder pool, net.minecraft.world.level.ItemLike item,
				int minimum, int maximum, int weight) {
			pool.add(LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(
					UniformGenerator.between(minimum, maximum))));
		}

		private static void addTag(LootPool.Builder pool, String path, int minimum, int maximum,
				int weight) {
			TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
					net.minecraft.resources.Identifier.fromNamespaceAndPath("c", path));
			pool.add(TagEntry.expandTag(tag).setWeight(weight).apply(SetItemCountFunction.setCount(
					UniformGenerator.between(minimum, maximum))));
		}

		private static Item dye(net.minecraft.world.item.DyeColor color) {
			return Items.DYE.pick(color);
		}
	}

	private static final class Blocks extends BlockLootSubProvider {

		Blocks(HolderLookup.Provider registries) {
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		protected void generate() {
			for (var holder : ChromaBlocks.BLOCKS.getEntries()) {
				Block block = holder.get();
				// V33a unbreakable blocks never reach the loot system at all. The Portal Rift is
				// declared noLootTable() and is removed only by the Manipulator, which drops its own
				// items directly, so asking for a table here would fail outright.
				if (block.getLootTable().isEmpty())
					continue;
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
				else if (block instanceof BlockLootChest) {
					// V33a drops the inventory on break; the contents are the loot, not the block.
					this.add(block, noDrop());
				}
				else if (block instanceof BlockStructureShield) {
					// V33a damageDropped = meta % 8: a reinforced shield yields the plain form of the
					// same material, and the plain form drops itself.
					this.dropSelf(block);
				}
				else if (block == ChromaBlocks.GLOWING_LEAVES.get()) {
					// V33a getDrops is an if/else-if, not two independent rolls: a Glowing Sapling on a
					// one-in-fifty roll that Fortune shortens, and only otherwise glowstone dust. An
					// alternatives entry is exactly that -- the first branch whose conditions pass wins.
					// The dust branch cannot fire at all without Fortune, which is upstream's own
					// arithmetic rather than an omission here.
					this.add(block, LootTable.lootTable().withPool(LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1))
							.add(net.minecraft.world.level.storage.loot.entries.AlternativesEntry.alternatives(
									LootItem.lootTableItem(ChromaBlocks.GLOW_SAPLING.get())
											.when(() -> FortuneScaledChance.reciprocal(50, 5)),
									LootItem.lootTableItem(net.minecraft.world.item.Items.GLOWSTONE_DUST)
											.when(() -> FortuneScaledChance.reciprocalComplement(1, 0.5))
											.apply(net.minecraft.world.level.storage.loot.functions.ApplyBonusCount
													.addUniformBonusCount(registries.lookupOrThrow(
															net.minecraft.core.registries.Registries.ENCHANTMENT)
															.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 1))))));
				}
				else if (block instanceof reika.chromaticraft.block.dimension.BlockDimensionDeco deco) {
					// V33a addDrops: one item, except Glow Cave which yields one to six, then scaled by
					// how tuned the breaking player is up to the variant's own ceiling. The scaling has
					// to be a provider rather than a fixed count because it depends on who broke it.
					var type = deco.getDecoType();
					var count = type == reika.chromaticraft.registry.ProximaDecoTypes.GLOWCAVE
							? reika.chromaticraft.auxiliary.loot.TuningScaledCount.between(1, 6, type.maxDrops())
							: reika.chromaticraft.auxiliary.loot.TuningScaledCount.single(type.maxDrops());
					this.add(block, LootTable.lootTable().withPool(LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1))
							.add(LootItem.lootTableItem(block)
									.apply(SetItemCountFunction.setCount(count)))));
				}
				else if (block instanceof reika.chromaticraft.block.BlockHoverBlock) {
					this.add(block, noDrop());
				}
				else if (block instanceof reika.chromaticraft.block.dimension.structure.locks.BlockLockKey) {
					// Its channel/delegate/structure identity are emitted manually from the block entity.
					this.add(block, noDrop());
				}
				else if (block instanceof BlockDecoFlower flower) {
					// V33a routes these through PlantDropManager rather than the block's own drop:
					// each flower yields its registered resource, never itself.
					this.add(block, createSingleItemTable(
							ChromaItems.CRAFTING.get(flower.getFlower().drop()).get()));
				}
				else if (block instanceof BlockCaveIndicator) {
					// V33a getItemDropped defers to Blocks.stone, so mining one yields cobblestone
					// and the crystal itself is never obtainable from the world.
					this.add(block, createSingleItemTable(net.minecraft.world.level.block.Blocks.COBBLESTONE));
				}
				else if (block instanceof BlockTieredPlant) {
					// Same contract as the tiered ores: V33a decides every drop in code from the
					// breaker's progression, and an insufficient player cannot target the plant at
					// all, so there is nothing for a loot table to describe.
					this.add(block, noDrop());
				}
				else if (block instanceof BlockTieredOre) {
					// V33a BlockTieredResource hard-overrides the whole vanilla drop path to nothing;
					// which resources (if any) a miner gets is decided by their progression in code.
					this.add(block, noDrop());
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
