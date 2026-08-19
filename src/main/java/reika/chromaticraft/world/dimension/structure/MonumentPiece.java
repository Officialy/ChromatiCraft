package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import reika.chromaticraft.data.ChromaStructureTemplateProvider;

/**
 * V33a {@code MonumentGenerator}: the clearing the monument stands in, and the monument itself.
 *
 * <h2>Why a structure piece</h2>
 *
 * <p>Upstream carves an ellipsoid of radius thirty-two before it places anything — sixty-five blocks
 * across — and the monument is forty-three. Both are far wider than the forty-eight block window a
 * decoration feature may write to, and a feature that oversteps it has its writes dropped in silence.
 * A piece is laid out once and then painted chunk by chunk with a box clipped to each, which is the
 * only shape that can build this.
 *
 * <h2>Upstream's order, which matters</h2>
 *
 * <p>{@code startCalculate} does three things in sequence and the order is load-bearing: it hollows the
 * ellipsoid to air, it lays grass across the whole square at {@code posY - 1} — the whole square, not
 * only the ellipse, so the monument sits on a floor rather than in a bowl — and only then generates the
 * structure over the top. Doing the clearing after would erase the monument.
 *
 * <p>{@code posY} is 103, and is upstream's fixed altitude rather than anything read from terrain.
 * The monument is placed at {@code (x-21, posY, z-21)}, which is what centres a forty-three wide
 * template on the position the ring calculator chose.
 */
public class MonumentPiece extends StructurePiece {

	/** V33a MonumentGenerator.startCalculate: {@code posY = 103}. */
	public static final int MONUMENT_Y = 103;
	/** V33a's ellipsoid radii: {@code r} horizontally, {@code r2} vertically. */
	private static final int CLEAR_RADIUS = 32;
	private static final int CLEAR_HEIGHT = 24;
	/** The template's own size, and the half-width that centres it. */
	private static final int TEMPLATE_SIZE = 43;
	private static final int TEMPLATE_OFFSET = 21;

	private final int centreX;
	private final int centreZ;

	public MonumentPiece(int centreX, int centreZ) {
		super(ProximaStructurePieces.MONUMENT.get(), 0, boundsFor(centreX, centreZ));
		this.centreX = centreX;
		this.centreZ = centreZ;
	}

	public MonumentPiece(CompoundTag tag) {
		super(ProximaStructurePieces.MONUMENT.get(), tag);
		this.centreX = tag.getIntOr("CX", 0);
		this.centreZ = tag.getIntOr("CZ", 0);
	}

	/**
	 * The union of the clearing and the monument. The clearing is the wider of the two, and its grass
	 * floor reaches a block below where it starts, so the box has to include that or the floor is
	 * clipped away at the edges.
	 */
	private static BoundingBox boundsFor(int centreX, int centreZ) {
		return new BoundingBox(centreX - CLEAR_RADIUS, MONUMENT_Y - 1, centreZ - CLEAR_RADIUS,
				centreX + CLEAR_RADIUS, MONUMENT_Y + CLEAR_HEIGHT, centreZ + CLEAR_RADIUS);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", centreX);
		tag.putInt("CZ", centreZ);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unusedRandom, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		for (int i = -CLEAR_RADIUS; i <= CLEAR_RADIUS; i++) {
			for (int k = -CLEAR_RADIUS; k <= CLEAR_RADIUS; k++) {
				int x = centreX + i;
				int z = centreZ + k;
				// V33a lays the floor unconditionally across the square, inside the same loop.
				place(level, chunkBB, x, MONUMENT_Y - 1, z, grass);
				for (int j = 0; j <= CLEAR_HEIGHT; j++)
					if (insideEllipsoid(i, j, k))
						place(level, chunkBB, x, MONUMENT_Y + j, z, air);
			}
		}

		// The monument itself, over the cleared ground. Only the part of it inside this chunk's box is
		// written; the template is placed again, clipped differently, for every chunk it overlaps.
		BlockPos anchor = new BlockPos(centreX - TEMPLATE_OFFSET, MONUMENT_Y, centreZ - TEMPLATE_OFFSET);
		// Vanilla's own template placement rather than NBTStructureLoader: the loader is written for
		// the feature case, where the write window is the constraint and the caller does not get a box.
		// Here the box is exactly what is wanted, and placeInWorld honours it natively.
		level.getLevel().getStructureManager()
				.get(ChromaStructureTemplateProvider.PROXIMA_MONUMENT)
				.orElseThrow(() -> new IllegalStateException(
						"Missing monument template " + ChromaStructureTemplateProvider.PROXIMA_MONUMENT))
				.placeInWorld(level, anchor, anchor,
						new StructurePlaceSettings().setRotation(Rotation.NONE)
								.setIgnoreEntities(true).setBoundingBox(chunkBB),
						RandomSource.create(centreX * 31L + centreZ), 2);
	}

	/** V33a {@code ReikaMathLibrary.isPointInsideEllipse(i, j, k, r, r2, r)}. */
	private static boolean insideEllipsoid(int i, int j, int k) {
		double x = i / (double)CLEAR_RADIUS;
		double y = j / (double)CLEAR_HEIGHT;
		double z = k / (double)CLEAR_RADIUS;
		return x * x + y * y + z * z <= 1;
	}

	private static void place(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z,
			BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (chunkBB.isInside(pos))
			level.setBlock(pos, state, 2);
	}

	/** The template size, for anything that needs to know how wide the monument is. */
	public static int templateSize() {
		return TEMPLATE_SIZE;
	}
}
