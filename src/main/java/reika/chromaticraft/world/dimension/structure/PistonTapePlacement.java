package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Filter Cycle structure. */
public final class PistonTapePlacement implements StructurePlacement {

	public static final MapCodec<PistonTapePlacement> CODEC = MapCodec.unit(PistonTapePlacement::new);

	public PistonTapePlacement() {
	}

	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && ProximaStructureSegments.ownerAt(DimensionStructureType.PISTONTAPE, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<PistonTapePlacement> codec() {
		return CODEC;
	}
}
