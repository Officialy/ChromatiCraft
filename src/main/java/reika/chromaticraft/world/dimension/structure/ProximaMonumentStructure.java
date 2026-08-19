package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

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
		return Optional.of(new Structure.GenerationStub(monument, builder ->
				builder.addPiece(new MonumentPiece(monument.getX(), monument.getZ()))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.MONUMENT_TYPE.get();
	}
}
