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

/** Vanilla structure start for the NBT-composed V33a Three-Dimensional Maze. */
public final class ProximaThreeDMazeStructure extends Structure {

	public static final MapCodec<ProximaThreeDMazeStructure> CODEC =
			simpleCodec(ProximaThreeDMazeStructure::new);

	public ProximaThreeDMazeStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.TDMAZE, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof ThreeDMazeStructureGenerator plan))
			return Optional.empty();
		BlockPos entry = new BlockPos(plan.getEntryPosX(), 0, plan.getEntryPosZ());
		int surface = context.chunkGenerator().getFirstFreeHeight(entry.getX(), entry.getZ(),
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		BlockPos anchor = new BlockPos(plan.getPosX(), plan.getPosY(), plan.getPosZ());
		return Optional.of(new GenerationStub(anchor,
				builder -> builder.addPiece(new ThreeDMazePiece(placement, plan, surface))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.THREE_D_MAZE_TYPE.get();
	}
}
