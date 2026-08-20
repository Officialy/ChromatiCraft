package reika.chromaticraft.world.dimension.structure;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import reika.chromaticraft.block.decoration.BlockEtherealLight;
import reika.chromaticraft.block.dimension.BlockBedrockCrack;
import reika.chromaticraft.block.dimension.BlockVoidCave;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenGlowCave}'s writing half: what each cell of {@link GlowCaveShape} becomes.
 *
 * <p>A cell is air unless it is on the surface of the cave, and the surface is what everything
 * interesting happens on. Below a noise-driven height the walls are <b>bedrock</b> — solid to y 8,
 * thinning out to nothing by y 32 — and cracked bedrock appears in it, more often the deeper it is, up
 * to a 6.6% chance at the bottom. Above that line the walls are Stone Shielding, one in five of it
 * mossy, with one cell in twenty replaced by Glowing Cave Rock and its own neighbours backfilled so the
 * glow is set into the wall rather than floating in it.
 *
 * <p>In the open air: Ethereal Lights, more of them the deeper the cave runs (one cell in 160 above y
 * 16, and as often as one in ten near the floor), and above y 12 they carry the particle flag. Cave
 * plants on the floor, one in sixty, wherever there is headroom. And at y 2 the walls become Void
 * Caves, each flagged toward whichever sides have open air beside and below them — the lip the light
 * falls over.
 *
 * <p>A structure rather than a feature, because a cave wanders up to thirty-two blocks per segment with
 * no bound on how many segments it takes; that is far outside a feature's write window. The whole shape
 * is grown once when the start is made and each {@code postProcess} writes only the part inside the box
 * it is handed.
 */
public class GlowCavePiece extends StructurePiece {

	/** V33a MAX_CRACK_CHANCE / MAX_CRACK_Y / FULL_BEDROCK_Y / MAX_BEDROCK_Y. */
	private static final double MAX_CRACK_CHANCE = 0.066;
	private static final int MAX_CRACK_Y = 20;
	private static final int FULL_BEDROCK_Y = 8;
	private static final int MAX_BEDROCK_Y = 32;

	private final long seed;
	private final int originX;
	private final int originY;
	private final int originZ;

	private Set<BlockPos> cells;

	public GlowCavePiece(RandomSource rand, int x, int y, int z) {
		// A placeholder box for the super constructor: the real one cannot be known until the shape has
		// been grown, and the shape needs the seed this stores.
		super(ProximaStructurePieces.GLOW_CAVE.get(), 0, new BoundingBox(x, y, z, x, y, z));
		this.seed = rand.nextLong();
		this.originX = x;
		this.originY = y;
		this.originZ = z;
		this.boundingBox = boundsOf(shape());
	}

	public GlowCavePiece(CompoundTag tag) {
		super(ProximaStructurePieces.GLOW_CAVE.get(), tag);
		this.seed = tag.getLongOr("Seed", 0L);
		this.originX = tag.getIntOr("OX", 0);
		this.originY = tag.getIntOr("OY", 0);
		this.originZ = tag.getIntOr("OZ", 0);
	}

	/** The cells, grown once and kept: every chunk this overlaps asks for the same set. */
	private Set<BlockPos> shape() {
		if (cells == null)
			cells = GlowCaveShape.grow(RandomSource.create(seed), originX + 0.5, originY + 1.5,
					originZ + 0.5);
		return cells;
	}

	private static BoundingBox boundsOf(Set<BlockPos> cells) {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos pos : cells) {
			minX = Math.min(minX, pos.getX() - 1);
			minY = Math.min(minY, pos.getY() - 1);
			minZ = Math.min(minZ, pos.getZ() - 1);
			maxX = Math.max(maxX, pos.getX() + 1);
			maxY = Math.max(maxY, pos.getY() + 1);
			maxZ = Math.max(maxZ, pos.getZ() + 1);
		}
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,
			CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putInt("OX", originX);
		tag.putInt("OY", originY);
		tag.putInt("OZ", originZ);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource unusedRandom, BoundingBox chunkBB, ChunkPos chunkPos,
			BlockPos referencePos) {
		Set<BlockPos> set = shape();
		// Seeded off the piece, not off the chunk, so every chunk that writes part of this cave makes
		// the same decisions about it.
		RandomSource rand = RandomSource.create(seed ^ 0x9E3779B97F4A7C15L);
		net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise wallNoise =
				new net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise(
						new net.minecraft.world.level.levelgen.WorldgenRandom(
								new net.minecraft.world.level.levelgen.LegacyRandomSource(seed)),
						java.util.List.of(-2, -1, 0));

		Set<BlockPos> air = new HashSet<>();
		Set<BlockPos> ledges = new HashSet<>();
		for (BlockPos cell : set) {
			if (cell.getY() < level.getMinY())
				continue;
			boolean top = cell.getY() == originY;
			boolean edge = cell.getY() <= originY && !set.containsAll(neighbours(cell));
			BlockState state = Blocks.AIR.defaultBlockState();

			if (!top && edge)
				state = wall(level, rand, wallNoise, cell, set, chunkBB);
			if (cell.getY() == 0)
				state = Blocks.AIR.defaultBlockState();

			if (state.isAir())
				state = openAir(level, rand, cell, set, originY);
			if (state.is(ChromaBlocks.BEDROCK_CRACK.get()) || state.is(Blocks.BEDROCK)) {
				if (cell.getY() == 2)
					ledges.add(cell);
			}
			if (chunkBB.isInside(cell))
				level.setBlock(cell, state, 2);
			if (state.isAir())
				air.add(cell);
		}

		// V33a's falls pass: a bedrock cell at y 2 becomes a Void Cave flagged toward every side that
		// has open air beside it and below that, which is exactly the lip a fall can pour over. A cell
		// with air directly above it is skipped -- that is a hole, not a ledge.
		for (BlockPos cell : ledges) {
			if (air.contains(cell.above()))
				continue;
			BlockState state = ChromaBlocks.VOID_CAVE.get().defaultBlockState();
			boolean any = false;
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos side = cell.relative(dir);
				if (!air.contains(side) || !air.contains(side.below()))
					continue;
				state = state.setValue(BlockVoidCave.property(dir), true);
				any = true;
			}
			if (any && chunkBB.isInside(cell))
				level.setBlock(cell, state, 2);
		}
	}

	/** The six cells around one, which is what upstream's {@code getAdjacentCoordinates} returns. */
	private static java.util.List<BlockPos> neighbours(BlockPos pos) {
		java.util.List<BlockPos> list = new java.util.ArrayList<>(6);
		for (Direction dir : Direction.values())
			list.add(pos.relative(dir));
		return list;
	}

	/** V33a's edge branch: bedrock and its cracks below the noise line, shielding and glow above it. */
	private BlockState wall(WorldGenLevel level, RandomSource rand,
			net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise noise, BlockPos cell,
			Set<BlockPos> set, BoundingBox chunkBB) {
		if (isBedrockWall(noise, cell)) {
			if (cell.getY() >= MAX_CRACK_Y || rand.nextDouble() >= crackChance(cell))
				return Blocks.BEDROCK.defaultBlockState();
			// A crack is set into solid bedrock: its own exposed neighbours are filled so it does not
			// simply open into the cave.
			for (BlockPos side : neighbours(cell))
				if (!set.contains(side) && chunkBB.isInside(side))
					level.setBlock(side, Blocks.BEDROCK.defaultBlockState(), 2);
			return ChromaBlocks.BEDROCK_CRACK.get().defaultBlockState()
					.setValue(BlockBedrockCrack.DEPTH, crackDepth(rand, cell));
		}
		if (rand.nextInt(20) != 0) {
			return ChromaBlocks.shielding(rand.nextInt(5) == 0 ? ChromaShieldTypes.MOSS
					: ChromaShieldTypes.STONE).get().defaultBlockState();
		}
		// Glowing Cave Rock, backed by shielding so the glow sits in the wall rather than in a hole.
		for (BlockPos side : neighbours(cell))
			if (!set.contains(side) && chunkBB.isInside(side))
				level.setBlock(side, ChromaBlocks.shielding(ChromaShieldTypes.STONE).get()
						.defaultBlockState(), 2);
		return ChromaBlocks.deco(ProximaDecoTypes.GLOWCAVE).get().defaultBlockState();
	}

	/**
	 * V33a's two open-air rolls: the lights, whose chance climbs steeply toward the floor, and the cave
	 * plants, which need two blocks of headroom.
	 */
	private static BlockState openAir(WorldGenLevel level, RandomSource rand, BlockPos cell,
			Set<BlockPos> set, int upper) {
		int y = cell.getY();
		if (rand.nextInt(y >= 16 ? 160 : 10 + 10 * y) == 0)
			return ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState()
					.setValue(BlockEtherealLight.PARTICLES, y > 12);
		if (rand.nextInt(60) == 0 && y < upper - 10 && !set.contains(cell.above(2)))
			return ChromaBlocks.tieredPlant(ChromaTieredPlants.ROCK_FLOWER).get().defaultBlockState();
		return Blocks.AIR.defaultBlockState();
	}

	/**
	 * V33a isBedrockWall: solid bedrock to y 8, none at all past y 32, and in between a noise-chosen
	 * line so the transition is ragged rather than flat.
	 */
	private static boolean isBedrockWall(
			net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise noise, BlockPos cell) {
		if (cell.getY() >= MAX_BEDROCK_Y)
			return false;
		if (cell.getY() <= FULL_BEDROCK_Y)
			return true;
		double n = noise.getValue(cell.getX() / 3D, cell.getZ() / 3D, false);
		double line = FULL_BEDROCK_Y + (n + 1) / 2D * (MAX_BEDROCK_Y - FULL_BEDROCK_Y);
		return cell.getY() <= line;
	}

	/** V33a getBedrockCrackChance: full at y 8 and below, tapering to nothing at y 20. */
	private static double crackChance(BlockPos cell) {
		if (cell.getY() <= FULL_BEDROCK_Y)
			return MAX_CRACK_CHANCE;
		double f = (cell.getY() - FULL_BEDROCK_Y) / (double)(MAX_CRACK_Y - FULL_BEDROCK_Y);
		return (1 - f) * MAX_CRACK_CHANCE;
	}

	/** V33a getRandomCrackMeta: the full nine depths deep down, shallower ones higher up. */
	private static int crackDepth(RandomSource rand, BlockPos cell) {
		double max = 9;
		if (cell.getY() > FULL_BEDROCK_Y)
			max *= 1D - (cell.getY() - FULL_BEDROCK_Y) / (double)(MAX_CRACK_Y - FULL_BEDROCK_Y);
		return rand.nextInt(1 + (int)max);
	}
}
