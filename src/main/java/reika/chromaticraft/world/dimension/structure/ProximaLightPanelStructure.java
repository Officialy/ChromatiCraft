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

/** Vanilla structure start for the NBT-composed V33a Glowing Logic puzzle. */
public final class ProximaLightPanelStructure extends Structure {

	public static final MapCodec<ProximaLightPanelStructure> CODEC =
			simpleCodec(ProximaLightPanelStructure::new);

	public ProximaLightPanelStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return ProximaStructureSegments.generate(context, DimensionStructureType.LIGHTPANEL, placement -> {
			if (!(placement.getGenerator() instanceof LightPanelStructureGenerator plan))
				return null;
			BlockPos entry = placement.placement();
			int surface = context.chunkGenerator().getFirstFreeHeight(entry.getX(), entry.getZ(),
					Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			BlockPos anchor = new BlockPos(entry.getX(), plan.getPosY(), entry.getZ());
			return new ProximaStructureSegments.Candidate(new LightPanelPiece(placement, plan, surface), anchor);
		});
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.LIGHT_PANEL_TYPE.get();
	}
}
