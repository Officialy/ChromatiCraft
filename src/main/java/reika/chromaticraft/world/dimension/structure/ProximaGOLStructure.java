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

/** Vanilla structure start for the NBT-composed V33a Cellular Automata puzzle. */
public final class ProximaGOLStructure extends Structure {

	public static final MapCodec<ProximaGOLStructure> CODEC = simpleCodec(ProximaGOLStructure::new);

	public ProximaGOLStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.GOL, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof GOLStructureGenerator plan))
			return Optional.empty();
		BlockPos center = placement.placement();
		int shaftX = center.getX() - plan.radius() - 12;
		int surface = Integer.MIN_VALUE;
		for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
			surface = Math.max(surface, context.chunkGenerator().getFirstFreeHeight(shaftX + dx,
					center.getZ() + dz, Heightmap.Types.WORLD_SURFACE_WG,
					context.heightAccessor(), context.randomState()) - 1);
		BlockPos anchor = new BlockPos(center.getX(), plan.floorY(), center.getZ());
		int finalSurface = surface;
		return Optional.of(new GenerationStub(anchor,
				builder -> builder.addPiece(new GOLStructurePiece(placement, plan, finalSurface))));
	}

	@Override public StructureType<?> type() { return ProximaStructures.GOL_TYPE.get(); }
}
