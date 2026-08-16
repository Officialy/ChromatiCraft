package reika.chromaticraft.data;

import java.util.function.BiConsumer;

import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ChromaChests}, which is how Information Fragments, the Lexicon and elemental shards get
 * into ordinary worldgen chests. Without it there is no way to find a Fragment before building a Table,
 * so it is the front door to the whole mod's progression.
 *
 * <h2>What upstream did, and why this cannot be a literal port</h2>
 *
 * <p>1.7.10 chest generation was a single flat weighted pool per location, and {@code ChestGenHooks}
 * let a mod push entries straight into it. {@code ChromaChests} does exactly that: {@code addItem(tier,
 * Location.DUNGEON, FRAGMENT, 1, 1, 10)} means "weight 10 against whatever else is in the dungeon
 * pool". 26.2 has no such pool to push into — a vanilla chest table is several pools with their own
 * rolls and entries, generated from data — so the modern seam is a loot modifier that rolls a subtable
 * alongside the vanilla one. This class is those subtables; {@link ChromaLootModifierProvider} is the
 * wiring that attaches each to its vanilla table.
 *
 * <h2>What carries over exactly, and the one thing that does not</h2>
 *
 * <p>Every weight and every stack size below is V33a's, unchanged. Those are what encode the design:
 * a Fragment is weight 10 in a dungeon, 20 in a pyramid and <em>50</em> in a stronghold library, so a
 * library is the jackpot and a mineshaft (weight 2) is a consolation prize. The roll counts are each
 * location's own vanilla roll count in 26.2, so a chest that draws many items has many chances at a
 * Fragment, exactly as a 1.7.10 chest with a bigger count did.
 *
 * <p>The one value that cannot carry over is the denominator. V33a's weight 10 meant "10 out of the
 * 1.7.10 dungeon pool's total", and that pool no longer exists — 26.2's tables have different entries
 * and different totals, so there is nothing to match against. {@link #VANILLA_POOL_WEIGHT} stands in
 * for it: an empty entry at weight 100, chosen because the surviving vanilla pools these weights were
 * written against still total around a hundred (26.2's jungle temple main pool is 93). It sets the
 * absolute rate while the V33a weights keep every rate relative to every other. Without it the modifier
 * would guarantee ChromatiCraft loot in every single chest.
 */
public record ChromaChestLoot(HolderLookup.Provider registries) implements LootTableSubProvider {

	/**
	 * The stand-in for the 1.7.10 vanilla pool total these weights were written against. See the class
	 * documentation: this is the single value in the port that is chosen rather than carried over.
	 */
	private static final int VANILLA_POOL_WEIGHT = 100;

	/**
	 * One entry per {@code LootController.Location} that V33a injected into, paired with the 26.2 table
	 * that replaced it and that table's own roll count.
	 *
	 * <p>{@code Location.JUNGLE_DISPENSER} carries only the Glowing Sapling; upstream's shard line for
	 * it is commented out and stays that way.
	 */
	public enum Location {
		/** V33a {@code Location.BONUS} — {@code ChestGenHooks.BONUS_CHEST}. */
		BONUS(BuiltInLootTables.SPAWN_BONUS_CHEST, ConstantValue.exactly(1)),
		/** V33a {@code Location.VILLAGE} — {@code ChestGenHooks.VILLAGE_BLACKSMITH}. */
		VILLAGE(BuiltInLootTables.VILLAGE_WEAPONSMITH, UniformGenerator.between(3, 8)),
		/** V33a {@code Location.DUNGEON} — {@code ChestGenHooks.DUNGEON_CHEST}. */
		DUNGEON(BuiltInLootTables.SIMPLE_DUNGEON, UniformGenerator.between(1, 3)),
		/** V33a {@code Location.MINESHAFT} — {@code ChestGenHooks.MINESHAFT_CORRIDOR}. */
		MINESHAFT(BuiltInLootTables.ABANDONED_MINESHAFT, ConstantValue.exactly(1)),
		/** V33a {@code Location.STRONGHOLD_LIBRARY}. */
		STRONGHOLD_LIBRARY(BuiltInLootTables.STRONGHOLD_LIBRARY, UniformGenerator.between(2, 10)),
		/** V33a {@code Location.STRONGHOLD_CROSSING}. */
		STRONGHOLD_CROSSING(BuiltInLootTables.STRONGHOLD_CROSSING, UniformGenerator.between(1, 4)),
		/** V33a {@code Location.STRONGHOLD_HALLWAY} — {@code ChestGenHooks.STRONGHOLD_CORRIDOR}. */
		STRONGHOLD_HALLWAY(BuiltInLootTables.STRONGHOLD_CORRIDOR, UniformGenerator.between(2, 3)),
		/** V33a {@code Location.PYRAMID} — {@code ChestGenHooks.PYRAMID_DESERT_CHEST}. */
		PYRAMID(BuiltInLootTables.DESERT_PYRAMID, UniformGenerator.between(2, 4)),
		/** V33a {@code Location.JUNGLE_PUZZLE} — {@code ChestGenHooks.PYRAMID_JUNGLE_CHEST}. */
		JUNGLE_PUZZLE(BuiltInLootTables.JUNGLE_TEMPLE, UniformGenerator.between(2, 6)),
		/** V33a {@code Location.JUNGLE_DISPENSER} — {@code ChestGenHooks.PYRAMID_JUNGLE_DISPENSER}. */
		JUNGLE_DISPENSER(BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER, UniformGenerator.between(1, 2));

		/** The vanilla table this location's loot is added to. */
		public final ResourceKey<LootTable> target;
		/** The ChromatiCraft subtable the loot modifier rolls alongside it. */
		public final ResourceKey<LootTable> subtable;
		private final NumberProvider rolls;

		Location(ResourceKey<LootTable> target, NumberProvider rolls) {
			this.target = target;
			this.rolls = rolls;
			this.subtable = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(
					ChromatiCraft.MODID, "chests/injected/" + name().toLowerCase(java.util.Locale.ROOT)));
		}

		/** File name for this location's loot modifier, and so the id it is registered under. */
		public String modifierName() {
			return "chest_loot_" + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		for (Location location : Location.values()) {
			LootPool.Builder pool = LootPool.lootPool().setRolls(location.rolls);
			contents(location, pool);
			// The filler is what turns "guaranteed" into "at V33a's rate"; see VANILLA_POOL_WEIGHT.
			pool.add(EmptyLootItem.emptyItem().setWeight(VANILLA_POOL_WEIGHT));
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
			entry.apply(SetItemCountFunction.setCount(UniformGenerator.between(minimum, maximum)));
		pool.add(entry);
	}
}
