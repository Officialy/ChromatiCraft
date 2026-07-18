package reika.chromaticraft.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import reika.chromaticraft.registry.ChromaBlocks;

/**
 * ChromatiCraft loot tables (port-in-progress). Emits a drops-self table for every registered block so
 * the vanilla loot-table validator is satisfied; specialised drops are added as those blocks port.
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
			for (var holder : ChromaBlocks.BLOCKS.getEntries())
				this.dropSelf(holder.get());
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return ChromaBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get()).toList();
		}
	}
}
