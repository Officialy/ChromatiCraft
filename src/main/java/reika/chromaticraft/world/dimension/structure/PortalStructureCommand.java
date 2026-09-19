package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Command-placeable form of the Portal Rift multiblock.
 *
 * <p>This deliberately has no structure-set entry: the portal is player-built and must never scatter
 * through natural terrain. Registering it as a real structure still gives it vanilla's standard
 * {@code /place structure chromaticraft:portal} workflow, including chunk-clipped template placement,
 * instead of misrepresenting the nineteen-block-wide assembly as a decoration feature.
 */
public final class PortalStructureCommand extends Structure {

	public static final MapCodec<PortalStructureCommand> CODEC = simpleCodec(PortalStructureCommand::new);

	public PortalStructureCommand(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int x = context.chunkPos().getMiddleBlockX();
		int z = context.chunkPos().getMiddleBlockZ();
		int y = context.chunkGenerator().getFirstOccupiedHeight(x, z,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		BlockPos centre = new BlockPos(x, y, z);
		return Optional.of(new GenerationStub(centre,
				builder -> builder.addPiece(new PortalStructurePiece(centre))));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.PORTAL_TYPE.get();
	}
}
