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
 * from the items directly, so adding a colour only means adding its per-colour entries. A boosted
 * shard lands in both its own colour tag and the plain one, which is what lets a recipe asking for
 * "a black shard" also accept a boosted black shard.
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

			// The plain per-colour tag carries the boosted variant too, via the tag rather than the
			// item, so the "is a boosted black shard a black shard" relationship is stated once.
			var colour = tag(ChromaItemTags.crystalShards(element));
			colour.add(ChromaItems.SHARDS.get(element).getKey());
			colour.addTag(ChromaItemTags.boostedCrystalShards(element));

			allShards.addTag(ChromaItemTags.crystalShards(element));
			allBoosted.addTag(ChromaItemTags.boostedCrystalShards(element));
		}
	}
}
