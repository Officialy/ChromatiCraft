package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Cellular Automata structure. */
public final class GOLPlacement extends StructurePlacement {

	public static final MapCodec<GOLPlacement> CODEC = MapCodec.unit(GOLPlacement::new);

	public GOLPlacement() {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1F, 0, Optional.empty());
	}

	@Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.GOL, chunkX, chunkZ) != null;
	}

	@Override public StructurePlacementType<?> type() { return ProximaStructures.GOL_PLACEMENT.get(); }
}
