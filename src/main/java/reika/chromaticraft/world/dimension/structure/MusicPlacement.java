package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Crystal Music structure. */
public final class MusicPlacement implements StructurePlacement {

	public static final MapCodec<MusicPlacement> CODEC = MapCodec.unit(MusicPlacement::new);

	public MusicPlacement() {
	}

	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.MUSIC, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<MusicPlacement> codec() {
		return CODEC;
	}
}
