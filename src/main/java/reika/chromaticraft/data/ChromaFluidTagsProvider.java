package reika.chromaticraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaFluids;

import java.util.concurrent.CompletableFuture;

public final class ChromaFluidTagsProvider extends FluidTagsProvider {
	public ChromaFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup, ChromatiCraft.MODID);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		tag(FluidTags.WATER).add(ChromaFluids.CHROMA.getKey(), ChromaFluids.FLOWING_CHROMA.getKey());
	}
}
