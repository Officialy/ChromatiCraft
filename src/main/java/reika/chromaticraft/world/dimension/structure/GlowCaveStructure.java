package reika.chromaticraft.world.dimension.structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The start half of V33a's {@code WorldGenGlowCave}: where a cave's mouth is.
 *
	 * <p>Upstream requires the surface block there to be grass or sand and refuses if any cell of the
	 * grown shape would touch water or existing shielding. Surface and water are evaluated here from the
	 * generator's noise columns before a piece exists, so rejection is atomic and never reads or loads a
	 * neighbouring chunk. Structure Shielding is not part of Proxima's base terrain; conflicts with other
	 * structures are prevented by the structure-set spacing rather than discovered after one chunk of a
	 * cave has already been painted.
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
		int x = context.chunkPos().getMiddleBlockX();
		int z = context.chunkPos().getMiddleBlockZ();
		int y = context.chunkGenerator().getFirstOccupiedHeight(x, z,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		NoiseColumn mouth = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(),
				context.randomState());
		if (!mouth.getBlock(y).is(Blocks.GRASS_BLOCK) && !mouth.getBlock(y).is(Blocks.SAND))
			return Optional.empty();

		long caveSeed = context.random().nextLong();
		Set<BlockPos> cells = GlowCaveShape.grow(RandomSource.create(caveSeed), x + 0.5, y + 1.5,
				z + 0.5);
		if (touchesWater(context, cells))
			return Optional.empty();
		BlockPos anchor = new BlockPos(x, y, z);
		return Optional.of(new Structure.GenerationStub(anchor,
				builder -> builder.addPiece(new GlowCavePiece(caveSeed, x, y, z, cells))));
	}

	/** V33a's preflight rejects water in a cave cell or any of its six neighbours. */
	private static boolean touchesWater(Structure.GenerationContext context, Set<BlockPos> cells) {
		// The old implementation performed seven hash lookups and constructed six BlockPos objects for
		// every cave cell. A large branched cave can contain hundreds of thousands of cells, which made
		// /locate appear to hang. Deduplicate the exact same candidate positions as primitive longs,
		// then sample each once; the V33a water rejection is unchanged.
		LongOpenHashSet candidates = new LongOpenHashSet(cells.size() * 2);
		for (BlockPos cell : cells) {
			int x = cell.getX();
			int y = cell.getY();
			int z = cell.getZ();
			candidates.add(cell.asLong());
			candidates.add(BlockPos.asLong(x + 1, y, z));
			candidates.add(BlockPos.asLong(x - 1, y, z));
			candidates.add(BlockPos.asLong(x, y + 1, z));
			candidates.add(BlockPos.asLong(x, y - 1, z));
			candidates.add(BlockPos.asLong(x, y, z + 1));
			candidates.add(BlockPos.asLong(x, y, z - 1));
		}
		Map<Long, NoiseColumn> columns = new HashMap<>();
		for (long packed : candidates)
			if (isWater(context, columns, BlockPos.of(packed)))
				return true;
		return false;
	}

	private static boolean isWater(Structure.GenerationContext context,
			Map<Long, NoiseColumn> columns, BlockPos pos) {
		long columnKey = BlockPos.asLong(pos.getX(), 0, pos.getZ());
		NoiseColumn column = columns.computeIfAbsent(columnKey,
				ignored -> context.chunkGenerator().getBaseColumn(pos.getX(), pos.getZ(),
						context.heightAccessor(), context.randomState()));
		return column.getBlock(pos.getY()).getFluidState().is(FluidTags.WATER);
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.GLOW_CAVE_TYPE.get();
	}
}
