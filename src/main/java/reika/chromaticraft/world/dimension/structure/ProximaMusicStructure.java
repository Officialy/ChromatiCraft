package reika.chromaticraft.world.dimension.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;

import java.util.Optional;

/** Vanilla structure start for the NBT-composed V33a Crystal Music puzzle. */
public final class ProximaMusicStructure extends Structure {

	public static final MapCodec<ProximaMusicStructure> CODEC = simpleCodec(ProximaMusicStructure::new);

	public ProximaMusicStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return ProximaStructureSegments.generate(context, DimensionStructureType.MUSIC, placement -> {
			if (!(placement.getGenerator() instanceof MusicStructureGenerator plan))
				return null;
			BlockPos entry = placement.placement();
			int surface = context.chunkGenerator().getFirstFreeHeight(entry.getX(), entry.getZ(),
					Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			BlockPos anchor = new BlockPos(plan.getPosX(), plan.getPosY(), plan.getPosZ());
			return new ProximaStructureSegments.Candidate(new MusicStructurePiece(placement, plan, surface), anchor);
		});
	}

	@Override public StructureType<?> type() { return ProximaStructures.MUSIC_TYPE.get(); }
}
