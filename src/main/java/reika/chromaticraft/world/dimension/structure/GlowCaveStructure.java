package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The start half of V33a's {@code WorldGenGlowCave}: where a cave's mouth is.
 *
 * <p>Upstream requires the surface block there to be grass or sand, refuses if any cell of the grown
 * shape would touch water or existing shielding, and only then writes. The water and shielding checks
 * belong to the world rather than to the layout and are made by {@link GlowCavePiece} as it writes; what
 * is decided here is the mouth, which is the chunk's own surface column.
 *
 * <p>All the shape lives in {@link GlowCaveShape}. This exists because a cave wanders far past what a
 * feature may write — thirty-two blocks a segment, for as many segments as it takes to reach the
 * bottom — and a structure is the only thing in 26.2 that may write outside its own chunk.
 */
public class GlowCaveStructure extends Structure {

	public static final MapCodec<GlowCaveStructure> CODEC = simpleCodec(GlowCaveStructure::new);

	public GlowCaveStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
			int x = context.chunkPos().getMiddleBlockX();
			int z = context.chunkPos().getMiddleBlockZ();
			int y = context.chunkGenerator().getFirstOccupiedHeight(x, z,
					Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			addPieces(builder, context, new BlockPos(x, y, z));
		});
	}

	private static void addPieces(StructurePiecesBuilder builder, Structure.GenerationContext context,
			BlockPos anchor) {
		builder.addPiece(new GlowCavePiece(context.random(), anchor.getX(), anchor.getY(),
				anchor.getZ()));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.GLOW_CAVE_TYPE.get();
	}
}
