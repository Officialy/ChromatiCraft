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

/** Vanilla structure start for V33a's procedurally laid Channeled Flow floors. */
public final class ProximaWaterStructure extends Structure {

	public static final MapCodec<ProximaWaterStructure> CODEC = simpleCodec(ProximaWaterStructure::new);

	public ProximaWaterStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.WATER, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof WaterStructureGenerator plan))
			return Optional.empty();
		BlockPos center = placement.placement();
		// V33a WaterStructureEntrance: getTopSolidOrLiquidBlock at the centre column.
		int surface = context.chunkGenerator().getFirstFreeHeight(center.getX(), center.getZ(),
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		return Optional.of(new GenerationStub(new BlockPos(center.getX(), plan.getPosY(), center.getZ()),
				builder -> builder.addPiece(new WaterStructurePiece(placement, plan, surface))));
	}

	@Override public StructureType<?> type() { return ProximaStructures.WATER_TYPE.get(); }
}
