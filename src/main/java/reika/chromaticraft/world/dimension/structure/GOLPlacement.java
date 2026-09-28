package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Cellular Automata structure. */
public final class GOLPlacement implements StructurePlacement {

	public static final MapCodec<GOLPlacement> CODEC = MapCodec.unit(GOLPlacement::new);

	public GOLPlacement() {
	}

	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.GOL, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<GOLPlacement> codec() {
		return CODEC;
	}
}
