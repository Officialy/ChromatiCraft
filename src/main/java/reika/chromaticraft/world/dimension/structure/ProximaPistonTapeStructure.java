package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;

/** Vanilla structure start for V33a's Filter Cycle stages. */
public final class ProximaPistonTapeStructure extends Structure {

	public static final MapCodec<ProximaPistonTapeStructure> CODEC = simpleCodec(ProximaPistonTapeStructure::new);

	public ProximaPistonTapeStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.PISTONTAPE, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof PistonTapeStructureGenerator plan))
			return Optional.empty();
		BlockPos start = plan.plan().entranceStart();
		// V33a PistonTapeEntrance: getTopSolidOrLiquidBlock at the shaft's column.
		int surface = context.chunkGenerator().getFirstFreeHeight(start.getX(), start.getZ(),
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		return Optional.of(new GenerationStub(start,
				builder -> builder.addPiece(new PistonTapeStructurePiece(placement, plan, surface))));
	}

	@Override public StructureType<?> type() { return ProximaStructures.PISTONTAPE_TYPE.get(); }
}
