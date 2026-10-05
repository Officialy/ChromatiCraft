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

/** Vanilla structure start for the NBT-composed V33a Cellular Automata puzzle. */
public final class ProximaGOLStructure extends Structure {

	public static final MapCodec<ProximaGOLStructure> CODEC = simpleCodec(ProximaGOLStructure::new);

	public ProximaGOLStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return ProximaStructureSegments.generate(context, DimensionStructureType.GOL, placement -> {
			if (!(placement.getGenerator() instanceof GOLStructureGenerator plan))
				return null;
			BlockPos center = placement.placement();
			int shaftX = center.getX() - plan.radius() - 12;
			int surface = Integer.MIN_VALUE;
			for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
				surface = Math.max(surface, context.chunkGenerator().getFirstFreeHeight(shaftX + dx,
						center.getZ() + dz, Heightmap.Types.WORLD_SURFACE_WG,
						context.heightAccessor(), context.randomState()) - 1);
			BlockPos anchor = new BlockPos(center.getX(), plan.floorY(), center.getZ());
			int finalSurface = surface;
			return new ProximaStructureSegments.Candidate(new GOLStructurePiece(placement, plan, finalSurface), anchor);
		});
	}

	@Override public StructureType<?> type() { return ProximaStructures.GOL_TYPE.get(); }
}
