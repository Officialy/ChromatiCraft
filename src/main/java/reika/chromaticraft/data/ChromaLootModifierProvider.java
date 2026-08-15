package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaChestLoot.Location;

/**
 * Attaches each {@link ChromaChestLoot} subtable to the vanilla chest table it belongs to.
 *
 * <p>This is the modern replacement for {@code LootController.registerToWorldGen}, which reached into
 * {@code ChestGenHooks} and mutated the vanilla pool in place. A global loot modifier is the equivalent
 * seam: {@link LootTableIdCondition} predicates on the table being rolled, and
 * {@link AddTableLootModifier} rolls the ChromatiCraft subtable and appends the result.
 *
 * <p>The targeting matters more than it looks. Every ChromatiCraft structure that places a chest —
 * {@code NetherRoofStructureFeature}, {@code OverworldStructureFeature} — hands it a <em>vanilla</em>
 * table id, because that is what V33a's structures did once {@code ChestGenHooks} had been seeded. So
 * modifying the vanilla tables is what puts Information Fragments back in the Nether and Overworld
 * structure chests, not just in vanilla dungeons. Both features use {@code setLootTable}, which fills
 * lazily through {@code LootTable.fill}, so the modifiers run for them exactly as they do for a vanilla
 * chest.
 */
public final class ChromaLootModifierProvider extends GlobalLootModifierProvider {

	public ChromaLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, ChromatiCraft.MODID);
	}

	@Override
	protected void start() {
		for (Location location : Location.values())
			add(location.modifierName(), new AddTableLootModifier(
					new LootItemCondition[] {LootTableIdCondition.builder(location.target.identifier()).build()},
					0, location.subtable));
	}
}
