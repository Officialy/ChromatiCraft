package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the chunks assigned a Three-Dimensional Maze by Proxima's persistent layout. */
public final class ThreeDMazePlacement extends StructurePlacement {

	public static final MapCodec<ThreeDMazePlacement> CODEC = MapCodec.unit(ThreeDMazePlacement::new);

	public ThreeDMazePlacement() {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1F, 0, Optional.empty());
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.TDMAZE, chunkX, chunkZ) != null;
	}

	@Override
	public StructurePlacementType<?> type() {
		return ProximaStructures.THREE_D_MAZE_PLACEMENT.get();
	}
}
