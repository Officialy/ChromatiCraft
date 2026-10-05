package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.ProximaNoiseSettings;
import reika.chromaticraft.world.dimension.StructureCalculator;

/**
 * Splits an oversized Proxima structure into several vanilla structure starts.
 *
 * <p>26.3's {@code ChunkGenerator.createReferences} links a chunk only to starts within 8 chunks of it, so any part
 * of a start more than 8 chunks from its own chunk is never placed. V33a had no such limit (its generators wrote the
 * whole structure into a chunk-spliced cache), and Chromatic Beams, Glowing Logic and Crystal Music all reach well past
 * it. Each structure is therefore tiled into 15x15-chunk regions on a grid centred on its placement chunk; the centre
 * chunk of every region that its footprint touches is a start, and that start's piece is the full structure clipped
 * to the region ({@link ProximaSegmentPiece}). A start never reaches more than 7 chunks, and every chunk of the
 * structure belongs to exactly one region, so every block is written exactly once.
 *
 * <p>Placements cannot see the terrain, so the tiling uses a conservative footprint: the union of the piece's box over
 * every surface height Proxima can have. A start whose region the real structure turns out not to reach yields
 * nothing.
 */
public final class ProximaStructureSegments {

	/** Region side in chunks; odd, so the start sits at its centre. */
	public static final int TILE = 15;
	private static final int HALF = TILE / 2;

	private static final Map<StructureCalculator.StructurePlacement, BoundingBox> FOOTPRINTS = new WeakHashMap<>();

	private ProximaStructureSegments() {}

	/** The placement for which chunk ({@code chunkX}, {@code chunkZ}) is a region start, if any. */
	public static StructureCalculator.@Nullable StructurePlacement ownerAt(DimensionStructureType type, int chunkX, int chunkZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return null;
		for (StructureCalculator.StructurePlacement p : layout.structures().getPlacements()) {
			if (p.type != type || p.getGenerator() == null)
				continue;
			if (regionAt(p, chunkX, chunkZ) != null)
				return p;
		}
		return null;
	}

	/** The XZ region that chunk ({@code chunkX}, {@code chunkZ}) starts for this placement, or null. */
	public static @Nullable BoundingBox regionAt(StructureCalculator.StructurePlacement p, int chunkX, int chunkZ) {
		int pcx = p.placement().getX() >> 4;
		int pcz = p.placement().getZ() >> 4;
		if (Math.floorMod(chunkX - pcx, TILE) != 0 || Math.floorMod(chunkZ - pcz, TILE) != 0)
			return null;
		BoundingBox region = new BoundingBox((chunkX - HALF) << 4, Integer.MIN_VALUE / 2, (chunkZ - HALF) << 4,
				((chunkX + HALF) << 4) + 15, Integer.MAX_VALUE / 2, ((chunkZ + HALF) << 4) + 15);
		BoundingBox footprint = footprint(p);
		return footprint != null && region.intersects(footprint) ? region : null;
	}

	/** Every region start chunk of this placement, as {x, z} pairs. */
	public static List<int[]> starts(StructureCalculator.StructurePlacement p) {
		List<int[]> out = new ArrayList<>();
		BoundingBox fp = footprint(p);
		if (fp == null)
			return out;
		int pcx = p.placement().getX() >> 4;
		int pcz = p.placement().getZ() >> 4;
		int iMin = Math.floorDiv((fp.minX() >> 4) - pcx + HALF, TILE), iMax = Math.floorDiv((fp.maxX() >> 4) - pcx + HALF, TILE);
		int kMin = Math.floorDiv((fp.minZ() >> 4) - pcz + HALF, TILE), kMax = Math.floorDiv((fp.maxZ() >> 4) - pcz + HALF, TILE);
		for (int i = iMin; i <= iMax; i++)
			for (int k = kMin; k <= kMax; k++)
				if (regionAt(p, pcx + i * TILE, pcz + k * TILE) != null)
					out.add(new int[] {pcx + i * TILE, pcz + k * TILE});
		return out;
	}

	/** One placement's full structure and its anchor, as a type's structure start computes them. */
	public record Candidate(StructurePiece piece, BlockPos anchor) {}

	/**
	 * The start for this chunk: every placement of the type that claims the chunk as a region start is offered to
	 * {@code factory} in turn, and the first whose real structure reaches the region wins. (With the conservative
	 * footprint, two far-apart placements can both claim a chunk that only one of them actually reaches.)
	 */
	public static Optional<Structure.GenerationStub> generate(Structure.GenerationContext context, DimensionStructureType type,
			java.util.function.Function<StructureCalculator.StructurePlacement, @Nullable Candidate> factory) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return Optional.empty();
		for (StructureCalculator.StructurePlacement p : layout.structures().getPlacements()) {
			if (p.type != type || p.getGenerator() == null
					|| regionAt(p, context.chunkPos().x(), context.chunkPos().z()) == null)
				continue;
			Candidate c = factory.apply(p);
			if (c == null)
				continue;
			Optional<Structure.GenerationStub> stub = stub(context, p, c.piece(), c.anchor());
			if (stub.isPresent())
				return stub;
		}
		return Optional.empty();
	}

	/**
	 * The stub for one region start: the full piece itself when the structure is small enough to fit its centre
	 * region, otherwise the piece clipped to this chunk's region. The anchor stays the structure's own, so every
	 * start passes the same biome check the single start used to.
	 */
	public static Optional<Structure.GenerationStub> stub(Structure.GenerationContext context,
			StructureCalculator.StructurePlacement p, StructurePiece full, BlockPos anchor) {
		BoundingBox region = regionAt(p, context.chunkPos().x(), context.chunkPos().z());
		if (region == null)
			return Optional.empty();
		BoundingBox box = full.getBoundingBox();
		if (box.minX() >= region.minX() && box.maxX() <= region.maxX() && box.minZ() >= region.minZ()
				&& box.maxZ() <= region.maxZ())
			return Optional.of(new Structure.GenerationStub(anchor, builder -> builder.addPiece(full)));
		if (!region.intersects(box))
			return Optional.empty();
		BoundingBox clipped = new BoundingBox(Math.max(box.minX(), region.minX()), box.minY(),
				Math.max(box.minZ(), region.minZ()), Math.min(box.maxX(), region.maxX()), box.maxY(),
				Math.min(box.maxZ(), region.maxZ()));
		return Optional.of(new Structure.GenerationStub(anchor,
				builder -> builder.addPiece(new ProximaSegmentPiece(full, clipped))));
	}

	/** The union of the placement's piece box over every Proxima surface height. */
	static synchronized @Nullable BoundingBox footprint(StructureCalculator.StructurePlacement p) {
		BoundingBox cached = FOOTPRINTS.get(p);
		if (cached != null)
			return cached;
		BoundingBox union = null;
		for (int surface = ProximaNoiseSettings.MIN_Y; surface <= ProximaNoiseSettings.MIN_Y + ProximaNoiseSettings.HEIGHT;
				surface += 8) {
			StructurePiece piece = piece(p, surface);
			if (piece == null)
				return null;
			BoundingBox b = piece.getBoundingBox();
			union = union == null ? b : BoundingBox.encapsulatingBoxes(List.of(union, b)).orElse(union);
		}
		FOOTPRINTS.put(p, union);
		return union;
	}

	/** The full piece of a placement at a given surface height. */
	public static @Nullable StructurePiece piece(StructureCalculator.StructurePlacement p, int surface) {
		var g = p.getGenerator();
		return switch (p.type) {
			case LIGHTPANEL -> g instanceof LightPanelStructureGenerator plan ? new LightPanelPiece(p, plan, surface) : null;
			case TDMAZE -> g instanceof ThreeDMazeStructureGenerator plan ? new ThreeDMazePiece(p, plan, surface) : null;
			case MUSIC -> g instanceof MusicStructureGenerator plan ? new MusicStructurePiece(p, plan, surface) : null;
			case GOL -> g instanceof GOLStructureGenerator plan ? new GOLStructurePiece(p, plan, surface) : null;
			case LASER -> g instanceof LaserStructureGenerator plan ? new LaserStructurePiece(p, plan, surface) : null;
			case WATER -> g instanceof WaterStructureGenerator plan ? new WaterStructurePiece(p, plan, surface) : null;
			case PISTONTAPE -> g instanceof PistonTapeStructureGenerator plan ? new PistonTapeStructurePiece(p, plan, surface) : null;
			default -> null;
		};
	}
}
