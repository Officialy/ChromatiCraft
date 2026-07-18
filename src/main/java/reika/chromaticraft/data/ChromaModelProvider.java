package reika.chromaticraft.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * ChromatiCraft block/item model provider (port-in-progress). Simple full-cube blocks use
 * {@link BlockModelGenerators#createTrivialCube}; grows into per-block handling (multi-variant,
 * BER item models) as those blocks port — see ReactorModelProvider for the richer patterns.
 */
public class ChromaModelProvider extends ModelProvider {

	public ChromaModelProvider(PackOutput output) {
		super(output, ChromatiCraft.MODID);
	}

	@Override
	protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
		blockModels.createTrivialCube(ChromaBlocks.STORAGE.get());
	}
}
