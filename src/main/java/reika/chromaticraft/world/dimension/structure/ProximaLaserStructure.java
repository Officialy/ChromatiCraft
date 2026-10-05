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

/** Vanilla structure start for V33a's authored Chromatic Beams rooms. */
public final class ProximaLaserStructure extends Structure {
	public static final MapCodec<ProximaLaserStructure> CODEC = simpleCodec(ProximaLaserStructure::new);
	public ProximaLaserStructure(StructureSettings settings) { super(settings); }

	@Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.LASER, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof LaserStructureGenerator plan))
			return Optional.empty();
		BlockPos center = placement.placement();
		// V33a LaserEntrance: getTopSolidOrLiquidBlock at the centre column (the cell above the top
		// solid block), then upward to the first air cell, which is exactly the first free height.
		int finalSurface = context.chunkGenerator().getFirstFreeHeight(center.getX(), center.getZ(),
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		return Optional.of(new GenerationStub(new BlockPos(center.getX(), plan.getPosY(), center.getZ()),
				builder -> builder.addPiece(new LaserStructurePiece(placement, plan, finalSurface))));
	}

	@Override public StructureType<?> type() { return ProximaStructures.LASER_TYPE.get(); }
}
