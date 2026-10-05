package reika.chromaticraft.world.dimension.structure;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names the Proxima chunk assigned the Chromatic Beams structure. */
public final class LaserPlacement implements StructurePlacement {
	public static final MapCodec<LaserPlacement> CODEC = MapCodec.unit(LaserPlacement::new);
	@Override public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && ProximaStructureSegments.ownerAt(DimensionStructureType.LASER, chunkX, chunkZ) != null;
	}
	@Override public MapCodec<LaserPlacement> codec() { return CODEC; }
}
