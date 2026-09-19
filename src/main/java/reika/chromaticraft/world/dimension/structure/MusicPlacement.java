package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the Proxima chunks assigned a Crystal Music structure. */
public final class MusicPlacement extends StructurePlacement {

	public static final MapCodec<MusicPlacement> CODEC = MapCodec.unit(MusicPlacement::new);

	public MusicPlacement() {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1F, 0, Optional.empty());
	}

	@Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && layout.structures().getPlacementInChunk(
				DimensionStructureType.MUSIC, chunkX, chunkZ) != null;
	}

	@Override public StructurePlacementType<?> type() { return ProximaStructures.MUSIC_PLACEMENT.get(); }
}
