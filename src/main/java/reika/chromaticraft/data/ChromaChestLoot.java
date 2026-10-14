package reika.chromaticraft.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

import java.util.function.BiConsumer;

/**
 * V33a {@code ChromaChests}, which is how Information Fragments, the Lexicon and elemental shards get
 * into ordinary worldgen chests. Without it there is no way to find a Fragment before building a Table,
 * so it is the front door to the whole mod's progression.
 *
 * <h2>What upstream did</h2>
 *
 * <p>1.7.10 chest generation was a single flat weighted pool per location, and DragonAPI's
 * {@code LootController} pushed every mod entry straight into that {@code ChestGenHooks} pool.
 * {@code addItem(tier, Location.DUNGEON, FRAGMENT, 1, 1, 10)} therefore meant "weight 10 in the one
 * pool holding the vanilla dungeon entries", and the chest drew {@code getCount()} items from that
 * pool with replacement. 26.x has no such pool to push into — a vanilla chest table is several pools
 * with their own rolls and entries, generated from data — so the modern seam is a loot modifier that
 * rolls a subtable alongside the vanilla one. This class is those subtables;
 * {@link ChromaLootModifierProvider} is the wiring that attaches each to its vanilla table.
 *
 * <h2>How each subtable reproduces the 1.7.10 odds</h2>
 *
 * <p>Each subtable is the 1.7.10 pool with its vanilla entries folded into one empty entry, rolled the
 * 1.7.10 number of times. Every ChromatiCraft item then keeps its original per-roll chance of
 * {@code weight / (vanilla + ChromatiCraft)} and its original expected count per chest. None of the
 * three columns is chosen:
 *
 * <ul>
 * <li><b>Weights and stack sizes</b> are V33a's {@code ChromaChests}, unchanged. They encode the design:
 * a Fragment is weight 10 in a dungeon, 20 in a pyramid and <em>50</em> in a stronghold library, so a
 * library is the jackpot and a mineshaft (weight 2) is a consolation prize.</li>
 * <li><b>Rolls</b> are Forge 10.13.4.1614 {@code ChestGenHooks.init}'s {@code addInfo} min/max as
 * {@code getCount} uses them: {@code min + nextInt(max - min)}, so the max is exclusive, and a location
 * whose min equals its max draws exactly that many.</li>
 * <li><b>The empty weight</b> is the total of the vanilla content array {@code addInfo} registers (read
 * from the Forge-patched structure classes) plus the enchanted book {@code init} adds afterwards:
 * weight 1, weight 2 in the library, and none in the bonus chest, village blacksmith or jungle
 * dispenser.</li>
 * </ul>
 *
 * <p>Other mods' entries are left out of the denominator: each mod's subtable reproduces its own share
 * against vanilla alone, so the rates do not depend on which other mods are installed. RotaryCraft's
 * {@code RoCChestLoot} uses the same values.
 *
 * <p>Do not swap these for the 26.x vanilla roll counts and a flat filler. That is what this class did
 * before, with an empty weight of 100: a mineshaft rolled once instead of three to six times, a dungeon
 * one to three times instead of eight, and a dungeon chest carried a Fragment about a third as often as
 * in V33a. The faithful values also lower one rate, which is correct and not a regression: the 26.x
 * stronghold library rolls 2-10 but the 1.7.10 one drew 1-4, so a library chest now carries a Fragment
 * about 76% of the time rather than 84%.
 */
public record ChromaChestLoot(LootTableSubProvider.Context context) implements LootTableSubProvider {

	/**
	 * One entry per {@code LootController.Location} that V33a injected into, paired with the 26.x table
	 * that replaced it and the 1.7.10 pool it reproduces: that pool's {@code ChestGenHooks} category and
	 * vanilla content array (whose total is in brackets), its {@code addInfo} range and its book.
	 *
	 * <p>{@code Location.JUNGLE_DISPENSER} carries only the Glowing Sapling; upstream's shard line for
	 * it is commented out and stays that way.
	 */
	public enum Location {
		/** {@code BONUS_CHEST}: {@code WorldServer.bonusChestContent} (64), addInfo 10..10, no book. */
		BONUS(BuiltInLootTables.SPAWN_BONUS_CHEST, 10, 10, 64),
		/** {@code VILLAGE_BLACKSMITH}: {@code House2.villageBlacksmithChestContents} (94), addInfo 3..9, no book. */
		VILLAGE(BuiltInLootTables.VILLAGE_WEAPONSMITH, 3, 8, 94),
		/** {@code DUNGEON_CHEST}: {@code WorldGenDungeons.field_111189_a} (119) + book, addInfo 8..8. */
		DUNGEON(BuiltInLootTables.SIMPLE_DUNGEON, 8, 8, 120),
		/** {@code MINESHAFT_CORRIDOR}: {@code mineshaftChestContents} (79) + book, addInfo 3..7. */
		MINESHAFT(BuiltInLootTables.ABANDONED_MINESHAFT, 3, 6, 80),
		/** {@code STRONGHOLD_LIBRARY}: {@code Library} contents (42) + book of weight 2, addInfo 1..5. */
		STRONGHOLD_LIBRARY(BuiltInLootTables.STRONGHOLD_LIBRARY, 1, 4, 44),
		/** {@code STRONGHOLD_CROSSING}: {@code RoomCrossing} contents (61) + book, addInfo 1..5. */
		STRONGHOLD_CROSSING(BuiltInLootTables.STRONGHOLD_CROSSING, 1, 4, 62),
		/** {@code STRONGHOLD_CORRIDOR}: {@code ChestCorridor.strongholdChestContents} (98) + book, addInfo 2..4. */
		STRONGHOLD_HALLWAY(BuiltInLootTables.STRONGHOLD_CORRIDOR, 2, 3, 99),
		/** {@code PYRAMID_DESERT_CHEST}: {@code DesertPyramid.itemsToGenerateInTemple} (72) + book, addInfo 2..7. */
		PYRAMID(BuiltInLootTables.DESERT_PYRAMID, 2, 6, 73),
		/** {@code PYRAMID_JUNGLE_CHEST}: {@code JunglePyramid.junglePyramidsChestContents} (72) + book, addInfo 2..7. */
		JUNGLE_PUZZLE(BuiltInLootTables.JUNGLE_TEMPLE, 2, 6, 73),
		/** {@code PYRAMID_JUNGLE_DISPENSER}: {@code junglePyramidsDispenserContents} (30), addInfo 2..2, no book. */
		JUNGLE_DISPENSER(BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER, 2, 2, 30);

		/** The vanilla table this location's loot is added to. */
		public final ResourceKey<LootTable> target;
		/** The ChromatiCraft subtable the loot modifier rolls alongside it. */
		public final ResourceKey<LootTable> subtable;
		private final int minRolls;
		private final int maxRolls;
		private final int vanillaWeight;

		Location(ResourceKey<LootTable> target, int minRolls, int maxRolls, int vanillaWeight) {
			this.target = target;
			this.minRolls = minRolls;
			this.maxRolls = maxRolls;
			this.vanillaWeight = vanillaWeight;
			this.subtable = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(
					ChromatiCraft.MODID, "chests/injected/" + name().toLowerCase(java.util.Locale.ROOT)));
		}

		/** File name for this location's loot modifier, and so the id it is registered under. */
		public String modifierName() {
			return "chest_loot_" + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	@Override
	public void run() {
        BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output = context::accept;
		for (Location location : Location.values()) {
			LootPool.Builder pool = LootPool.lootPool()
					.setRolls(ContextIntProviders.between(location.minRolls, location.maxRolls));
			contents(location, pool);
			// Stands in for the 1.7.10 vanilla entries these weights were drawn against; see the class doc.
			pool.add(EmptyLootItem.emptyItem().setWeight(location.vanillaWeight));
			output.accept(location.subtable, LootTable.lootTable().withPool(pool));
		}
	}

	/**
	 * The V33a {@code ChromaChests.addToChests} body, one location at a time. Tiers are not carried:
	 * {@code registerToWorldGen} filters by {@code ChromaOptions.CHESTGEN}, whose default is 4, and 4 is
	 * the highest tier used — so upstream's default registers every entry here.
	 */
	private static void contents(Location location, LootPool.Builder pool) {
		switch (location) {
			case BONUS -> {
				shards(pool, 1, 5, 1);
				add(pool, ChromaItems.LEXICON.get(), 1, 1, 3);
			}
			case VILLAGE -> {
				shards(pool, 1, 3, 2);
				add(pool, ChromaItems.LEXICON.get(), 1, 1, 1);
				add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 1, 5);
			}
			case DUNGEON -> {
				shards(pool, 2, 8, 2);
				add(pool, ChromaItems.LEXICON.get(), 1, 1, 2);
				add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 1, 10);
				add(pool, reika.chromaticraft.registry.ChromaBlocks.GLOW_SAPLING.get(), 1, 1, 1);
			}
			case MINESHAFT -> add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 1, 2);
			case STRONGHOLD_LIBRARY -> {
				add(pool, ChromaItems.LEXICON.get(), 1, 1, 8);
				add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 3, 50);
			}
			case STRONGHOLD_CROSSING -> add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 3, 10);
			case STRONGHOLD_HALLWAY -> add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 3, 10);
			case PYRAMID -> {
				shards(pool, 4, 16, 5);
				add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 1, 20);
			}
			case JUNGLE_PUZZLE -> add(pool, ChromaItems.INFO_FRAGMENT.get(), 1, 2, 20);
			// V33a's only live JUNGLE_DISPENSER entry; its shard line is commented out upstream.
			case JUNGLE_DISPENSER ->
					add(pool, reika.chromaticraft.registry.ChromaBlocks.GLOW_SAPLING.get(), 1, 1, 2);
		}
	}

	/**
	 * V33a loops all sixteen elements and adds each shard at the same weight, so the colour a player
	 * finds is uniform while the chance of finding <em>a</em> shard is sixteen times the listed weight.
	 * Keeping the loop rather than collapsing it to one entry preserves that.
	 */
	private static void shards(LootPool.Builder pool, int minimum, int maximum, int weight) {
		for (CrystalElement element : CrystalElement.elements)
			add(pool, ChromaItems.SHARDS.get(element).get(), minimum, maximum, weight);
	}

	private static void add(LootPool.Builder pool, ItemLike item, int minimum, int maximum, int weight) {
		LootItem.Builder<?> entry = LootItem.lootTableItem(item).setWeight(weight);
		if (minimum != 1 || maximum != 1)
			entry.apply(SetItemCountFunction.setCount(ContextIntProviders.between(minimum, maximum)));
		pool.add(entry);
	}
}
