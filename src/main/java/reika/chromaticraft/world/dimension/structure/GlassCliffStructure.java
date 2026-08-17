package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The start half of V33a's {@code WorldGenGlassCliffs}. All of the shape lives in
 * {@link GlassCliffPiece}; this only decides where a chain begins and hands the piece its seed.
 *
 * <p>The chain is anchored on the chunk's surface, which is what upstream's decorator did by passing
 * {@code getTopSolidOrLiquidBlock} at the chosen column. Reading it here is safe in a way it is not
 * inside a feature: a structure start resolves heights through the chunk generator rather than through
 * a bounded level view, so nothing is asked of a neighbouring chunk.
 */
public class GlassCliffStructure extends Structure {

	public static final MapCodec<GlassCliffStructure> CODEC = simpleCodec(GlassCliffStructure::new);

	public GlassCliffStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
			BlockPos anchor = new BlockPos(context.chunkPos().getMiddleBlockX(),
					context.chunkGenerator().getFirstOccupiedHeight(context.chunkPos().getMiddleBlockX(),
							context.chunkPos().getMiddleBlockZ(), Heightmap.Types.WORLD_SURFACE_WG,
							context.heightAccessor(), context.randomState()),
					context.chunkPos().getMiddleBlockZ());
			addPieces(builder, context, anchor);
		});
	}

	private static void addPieces(StructurePiecesBuilder builder, Structure.GenerationContext context,
			BlockPos anchor) {
		builder.addPiece(new GlassCliffPiece(context.random(), anchor.getX(), anchor.getY(), anchor.getZ()));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.GLASS_CLIFF_TYPE.get();
	}
}
