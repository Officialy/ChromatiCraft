package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;

/** Names exactly the chunks assigned Glowing Logic by Proxima's persistent ring layout. */
public final class LightPanelPlacement implements StructurePlacement {

	public static final MapCodec<LightPanelPlacement> CODEC = MapCodec.unit(LightPanelPlacement::new);

	public LightPanelPlacement() {
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		return layout != null && ProximaStructureSegments.ownerAt(DimensionStructureType.LIGHTPANEL, chunkX, chunkZ) != null;
	}

	@Override
	public MapCodec<LightPanelPlacement> codec() {
		return CODEC;
	}
}
