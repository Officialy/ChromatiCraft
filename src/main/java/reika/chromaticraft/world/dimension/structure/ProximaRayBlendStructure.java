package reika.chromaticraft.world.dimension.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import reika.chromaticraft.world.dimension.DimensionStructureType;

/** Vanilla structure start for V33a's Crystal Interference. */
public final class ProximaRayBlendStructure extends Structure {

	public static final MapCodec<ProximaRayBlendStructure> CODEC = simpleCodec(ProximaRayBlendStructure::new);

	public ProximaRayBlendStructure(StructureSettings settings) { super(settings); }

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return ProximaStructureSegments.generate(context, DimensionStructureType.RAYBLEND, placement -> {
			if (!(placement.getGenerator() instanceof RayBlendStructureGenerator plan))
				return null;
			RayBlendEntrancePlan.Terrain terrain = terrain(context);
			BlockPos entrance = RayBlendEntrancePlan.center(plan.layout());
			int surface = terrain.topSolid(entrance.getX(), entrance.getZ());
			List<RayBlendEntrancePlan.Deposit> deposits = RayBlendEntrancePlan.deposits(plan.layout(), terrain);
			RayBlendLayout layout = plan.layout();
			return new ProximaStructureSegments.Candidate(new RayBlendStructurePiece(placement, plan, surface, deposits),
					new BlockPos(layout.originX(), layout.posY(), layout.originZ()));
		});
	}

	/**
	 * The generator's prediction of V33a's terrain reads. {@code getTopSolidOrLiquidBlock} skips fluid (it tests
	 * {@code blocksMovement}), so it is the ocean-floor height; Proxima's surface rule tops a column with grass
	 * exactly when no fluid stands on it, so a dry column's top block is grass.
	 */
	private static RayBlendEntrancePlan.Terrain terrain(GenerationContext context) {
		Map<Long, int[]> columns = new HashMap<>();
		return new RayBlendEntrancePlan.Terrain() {
			private int[] column(int x, int z) {
				return columns.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> new int[] {
						context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
								context.heightAccessor(), context.randomState()),
						context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
								context.heightAccessor(), context.randomState())});
			}

			@Override
			public int topSolid(int x, int z) {
				return column(x, z)[0];
			}

			@Override
			public boolean isGrass(int x, int y, int z) {
				int[] c = column(x, z);
				return c[0] - 1 == y && c[0] == c[1];
			}
		};
	}

	@Override public StructureType<?> type() { return ProximaStructures.RAYBLEND_TYPE.get(); }
}
