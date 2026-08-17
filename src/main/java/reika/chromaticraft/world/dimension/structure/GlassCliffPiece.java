package reika.chromaticraft.world.dimension.structure;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenGlassCliffs}: a chain of glass cliffs sweeping across Proxima's plains.
 *
 * <h2>Why this is a structure piece rather than a feature</h2>
 *
 * <p>Upstream's generator does two things a decoration feature in 26.2 cannot. It spans roughly a
 * hundred blocks — the chain steps from {@code d = -32} to {@code 32} and each cliff is up to
 * forty-eight long — against the forty-eight block window a feature may write to. And its inner
 * {@code generate} calls {@code getTopSolidOrLiquidBlock} for <em>every</em> cell of its height map, so
 * it reads terrain far outside the chunk it was anchored in; during decoration that can synchronously
 * request a neighbouring chunk and deadlock the worldgen worker.
 *
 * <p>A structure piece solves both at once, which is why the whole class of oversized Proxima
 * generators belongs here. The chain is decided once when the start is computed, and
 * {@link #postProcess} is then called for each chunk the piece overlaps with a box clipped to that
 * chunk. Every column this touches is one the caller may both read and write, so the terrain reads
 * become legal without changing what they mean.
 *
 * <h2>What carries over exactly</h2>
 *
 * <p>All of it, and the arithmetic is upstream's line for line: a bearing, a chain of cliffs stepped
 * {@code 6 + rand*6} apart along the perpendicular, each scaled by {@code f = 1 - 0.8*|d|/32} with
 * length falling as {@code f^0.75} and height as {@code f^0.5}, and a wander {@code o} that accumulates
 * {@code (-len/2 + rand*len)/4} per step. Each cliff then sweeps its own length in quarter-block
 * increments, stamping a disc of radius {@code 0.0625 + 0.75*0.75^(|d|/length)} whose height follows
 * {@code maxHeight * (1-|d|/length)^0.5}, keeping the tallest value where discs overlap. The column is
 * Cliff Glass with three blocks of stone under its lip and one grass block on top.
 *
 * <p>Because the chain is drawn from the piece's own stored seed rather than from the per-chunk random,
 * every chunk that paints part of it agrees on the same shape.
 */
public class GlassCliffPiece extends StructurePiece {

	/** V33a steps the chain from -32 to 32 along the perpendicular. */
	private static final int CHAIN_REACH = 32;

	private final long shapeSeed;
	private final int originX;
	private final int originY;
	private final int originZ;

	public GlassCliffPiece(RandomSource random, int originX, int originY, int originZ) {
		super(ProximaStructurePieces.GLASS_CLIFF.get(), 0, boundsFor(originX, originY, originZ));
		this.shapeSeed = random.nextLong();
		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;
	}

	public GlassCliffPiece(CompoundTag tag) {
		super(ProximaStructurePieces.GLASS_CLIFF.get(), tag);
		this.shapeSeed = tag.getLongOr("ShapeSeed", 0);
		this.originX = tag.getIntOr("OX", 0);
		this.originY = tag.getIntOr("OY", 0);
		this.originZ = tag.getIntOr("OZ", 0);
	}

	/**
	 * The chain reaches {@link #CHAIN_REACH} along the perpendicular and up to forty-eight along each
	 * cliff, and the wander adds to that, so the box is generous rather than tight. An over-large box
	 * costs only the chunks that are asked and find nothing in range; too small a box would silently
	 * clip the cliff.
	 */
	private static BoundingBox boundsFor(int x, int y, int z) {
		int reach = CHAIN_REACH + 48 + 16;
		return new BoundingBox(x - reach, y - 8, z - reach, x + reach, y + 48, z + reach);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("ShapeSeed", shapeSeed);
		tag.putInt("OX", originX);
		tag.putInt("OY", originY);
		tag.putInt("OZ", originZ);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unusedRandom, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		// The chain must come out identical for every chunk that paints part of it, so it is drawn from
		// the piece's own stored seed and never from the per-chunk random handed in here.
		RandomSource random = RandomSource.create(shapeSeed);
		BlockState cliffGlass = ChromaBlocks.deco(ProximaDecoTypes.CLIFFGLASS).get().defaultBlockState();

		double bearing = random.nextDouble() * 360;
		double perpendicular = bearing + 90;
		double length = 16 + random.nextInt(33);
		double height = 16 + random.nextInt(17);
		double spacing = 6 + random.nextDouble() * 6;
		double wander = 0;
		for (double d = -CHAIN_REACH; d <= CHAIN_REACH; d += spacing) {
			double cx = originX + d * Math.cos(Math.toRadians(perpendicular))
					+ wander * Math.cos(Math.toRadians(bearing));
			double cz = originZ + d * Math.sin(Math.toRadians(perpendicular))
					+ wander * Math.sin(Math.toRadians(bearing));
			double falloff = 1 - 0.8 * Math.abs(d) / CHAIN_REACH;
			cliff(level, chunkBB, cliffGlass, cx, cz,
					bearing, length * Math.pow(falloff, 0.75), height * Math.pow(falloff, 0.5));
			// The wander is drawn every step whether or not the cliff touched this chunk, so the chain
			// stays in step across chunk boundaries.
			wander += (-length / 2 + random.nextDouble() * length) / 4D;
		}
	}

	/**
	 * One cliff of the chain: V33a's {@code Cliff.calculate} followed by {@code Cliff.generate}. The
	 * height map is accumulated first because overlapping discs keep the tallest value, then written.
	 */
	private void cliff(WorldGenLevel level, BoundingBox chunkBB, BlockState cliffGlass,
			double centreX, double centreZ, double bearing, double length, double maxHeight) {
		if (length <= 0)
			return;
		Map<Long, Integer> heights = new HashMap<>();
		for (double d = -length; d <= length; d += 0.25) {
			double x = centreX + 0.5 + d * Math.cos(Math.toRadians(bearing));
			double z = centreZ + 0.5 + d * Math.sin(Math.toRadians(bearing));
			double columnHeight = maxHeight * Math.sqrt(1 - Math.abs(d) / length);
			double radius = 0.0625 + 0.75 * Math.pow(0.75, Math.abs(d) / length);
			double step = Math.min(radius / 2, 0.5);
			for (double i = -radius; i <= radius; i += step) {
				for (double k = -radius; k <= radius; k += step) {
					if (i * i + k * k > radius * radius)
						continue;
					int bx = Mth.floor(x + i);
					int bz = Mth.floor(z + k);
					// Only the centre of a disc rounds its height; the rim truncates, which is what
					// gives a cliff its stepped lip rather than a smooth ramp.
					int h = i == 0 && k == 0 ? (int)Math.round(columnHeight) : (int)columnHeight;
					long key = net.minecraft.core.BlockPos.asLong(bx, 0, bz);
					heights.merge(key, h, Math::max);
				}
			}
		}
		for (Map.Entry<Long, Integer> entry : heights.entrySet()) {
			int bx = net.minecraft.core.BlockPos.getX(entry.getKey());
			int bz = net.minecraft.core.BlockPos.getZ(entry.getKey());
			// The clip is what makes this safe: outside the chunk's box we neither read nor write.
			if (!chunkBB.isInside(new BlockPos(bx, chunkBB.minY(), bz)))
				continue;
			int top = entry.getValue();
			int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz) - originY;
			for (int i = surface; i <= top; i++) {
				BlockPos pos = new BlockPos(bx, originY + i, bz);
				if (!chunkBB.isInside(pos) || level.isOutsideBuildHeight(pos.getY()))
					continue;
				BlockState state = cliffGlass;
				if (i == top)
					state = Blocks.GRASS_BLOCK.defaultBlockState();
				else if (top - i < 3)
					state = Blocks.STONE.defaultBlockState();
				level.setBlock(pos, state, 3);
			}
		}
	}
}
