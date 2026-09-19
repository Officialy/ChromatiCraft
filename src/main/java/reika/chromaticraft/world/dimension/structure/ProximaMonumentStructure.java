package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.Heightmap;

import reika.chromaticraft.world.dimension.ProximaGenerators;

/**
 * The start half of Proxima's monument. All of the shape is in {@link MonumentPiece}; this only says
 * where it stands.
 *
 * <p>Which is the whole difficulty. The monument does not scatter — V33a puts exactly one in the
 * dimension, at {@code StructureCalculator.getMonumentPosition()}, the centre of the ring the puzzle
 * structures are laid around. {@link MonumentPlacement} is what expresses "this one chunk and no
 * other"; by the time the start is computed the answer is already known, so this reads the same
 * position rather than deriving anything from the chunk it was asked about.
 */
public class ProximaMonumentStructure extends Structure {

	public static final MapCodec<ProximaMonumentStructure> CODEC =
			simpleCodec(ProximaMonumentStructure::new);

	public ProximaMonumentStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		BlockPos monument = ProximaGenerators.monumentPosition();
		if (monument == null)
			return Optional.empty();
		// V33a's fixed y=103 assumed its old terrain generator never rose through the monument. Modern
		// Proxima can exceed that, so retain 103 as the floor but lift the clearing above the highest
		// terrain cell in its authored 65x65 footprint.
		int y = MonumentPiece.MONUMENT_Y;
		for (int dx = -32; dx <= 32; dx++) {
			for (int dz = -32; dz <= 32; dz++) {
				y = Math.max(y, context.chunkGenerator().getFirstFreeHeight(
						monument.getX() + dx, monument.getZ() + dz,
						Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
			}
		}
		BlockPos raised = new BlockPos(monument.getX(), y, monument.getZ());
		return Optional.of(new Structure.GenerationStub(raised, builder ->
				builder.addPiece(new MonumentPiece(raised.getX(), raised.getY(), raised.getZ()))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.MONUMENT_TYPE.get();
	}
}
