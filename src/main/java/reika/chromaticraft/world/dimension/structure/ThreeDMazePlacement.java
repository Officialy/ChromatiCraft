package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the chunks assigned a Three-Dimensional Maze by Proxima's persistent layout. */
public final class ThreeDMazePlacement implements StructurePlacement {

	public static final MapCodec<ThreeDMazePlacement> CODEC = MapCodec.unit(ThreeDMazePlacement::new);

	public ThreeDMazePlacement() {
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && ProximaStructureSegments.ownerAt(DimensionStructureType.TDMAZE, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<ThreeDMazePlacement> codec() {
		return CODEC;
	}
}
