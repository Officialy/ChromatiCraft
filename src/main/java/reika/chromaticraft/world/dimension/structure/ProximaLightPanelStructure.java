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

/** Vanilla structure start for the NBT-composed V33a Glowing Logic puzzle. */
public final class ProximaLightPanelStructure extends Structure {

	public static final MapCodec<ProximaLightPanelStructure> CODEC =
			simpleCodec(ProximaLightPanelStructure::new);

	public ProximaLightPanelStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return Optional.empty();
		StructureCalculator.StructurePlacement placement = layout.structures().getPlacementInChunk(
				DimensionStructureType.LIGHTPANEL, context.chunkPos().x(), context.chunkPos().z());
		if (placement == null || !(placement.getGenerator() instanceof LightPanelStructureGenerator plan))
			return Optional.empty();
		BlockPos entry = placement.placement();
		int surface = context.chunkGenerator().getFirstFreeHeight(entry.getX(), entry.getZ(),
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		BlockPos anchor = new BlockPos(entry.getX(), plan.getPosY(), entry.getZ());
		return Optional.of(new GenerationStub(anchor, builder -> builder.addPiece(
				new LightPanelPiece(placement, plan, surface))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.LIGHT_PANEL_TYPE.get();
	}
}
