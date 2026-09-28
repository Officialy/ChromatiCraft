package reika.chromaticraft.world.dimension.structure;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import reika.chromaticraft.world.dimension.ProximaGenerators;

/**
 * "The one chunk the monument stands in, and no other."
 *
 * <p>Neither vanilla placement fits. {@code RandomSpreadStructurePlacement} scatters on a grid, and
 * {@code ConcentricRingsStructurePlacement} lays a ring about the origin; the monument is a single
 * structure at a position the dimension's own layout chose —
 * {@code StructureCalculator.getMonumentPosition()}, the centre of the ring the puzzle structures are
 * laid around — and neither can be made to name it.
 *
 * <p>The layout is computed off-thread when the server starts and waited for when Proxima loads, both
 * of which happen before any chunk of it is generated, so by the time this is asked the answer exists.
 * Before it does, this answers no rather than guessing: a monument placed at a fallback position would
 * be written into a saved chunk and be wrong for the life of that world.
 */
public class MonumentPlacement implements StructurePlacement {

	/**
	 * There is nothing to configure: the position comes from the dimension's layout, not from data.
	 * A unit codec keeps the datapack entry to {@code {"type": "chromaticraft:monument"}}.
	 */
	public static final MapCodec<MonumentPlacement> CODEC =
			MapCodec.unit(MonumentPlacement::new);

	public MonumentPlacement() {
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		BlockPos monument = ProximaGenerators.monumentPosition();
		return monument != null && (monument.getX() >> 4) == chunkX && (monument.getZ() >> 4) == chunkZ;
	}

	@Override
	public MapCodec<MonumentPlacement> codec() {
		return CODEC;
	}
}
