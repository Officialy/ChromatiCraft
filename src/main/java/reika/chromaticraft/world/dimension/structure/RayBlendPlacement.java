package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Crystal Interference structure. */
public final class RayBlendPlacement implements StructurePlacement {

	public static final MapCodec<RayBlendPlacement> CODEC = MapCodec.unit(RayBlendPlacement::new);

	public RayBlendPlacement() {
	}

	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && ProximaStructureSegments.ownerAt(DimensionStructureType.RAYBLEND, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<RayBlendPlacement> codec() {
		return CODEC;
	}
}
