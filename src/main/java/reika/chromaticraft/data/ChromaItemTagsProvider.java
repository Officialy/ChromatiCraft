package reika.chromaticraft.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaItemTags;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Builds the crystal shard tag family.
 *
 * <p>Each colour gets a pair of tags, and the two aggregate tags are composed from those rather than
 * from the items directly, so adding a colour only means adding its per-colour entries. Plain and
 * boosted per-colour tags stay disjoint, matching V33a's exact metadata ingredients; only the
 * aggregate shard tag contains both families for recipes that originally accepted all 32 variants.
 */
public final class ChromaItemTagsProvider extends ItemTagsProvider {

	public ChromaItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup, ChromatiCraft.MODID);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		var allShards = tag(ChromaItemTags.CRYSTAL_SHARDS);
		var allBoosted = tag(ChromaItemTags.BOOSTED_CRYSTAL_SHARDS);

		for (CrystalElement element : CrystalElement.elements) {
			var boostedColour = tag(ChromaItemTags.boostedCrystalShards(element));
			boostedColour.add(ChromaItems.BOOSTED_SHARDS.get(element).getKey());

			var colour = tag(ChromaItemTags.crystalShards(element));
			colour.add(ChromaItems.SHARDS.get(element).getKey());

			allShards.addTag(ChromaItemTags.crystalShards(element));
			allShards.addTag(ChromaItemTags.boostedCrystalShards(element));
			allBoosted.addTag(ChromaItemTags.boostedCrystalShards(element));
		}
	}
}
