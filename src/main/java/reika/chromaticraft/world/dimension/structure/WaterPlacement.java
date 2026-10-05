package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Channeled Flow structure. */
public final class WaterPlacement implements StructurePlacement {

	public static final MapCodec<WaterPlacement> CODEC = MapCodec.unit(WaterPlacement::new);

	public WaterPlacement() {
	}

	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.WATER, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<WaterPlacement> codec() {
		return CODEC;
	}
}
