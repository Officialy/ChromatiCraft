package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * One region of an oversized Proxima structure (see {@link ProximaStructureSegments}): the structure's own full piece,
 * whose writes are kept to this piece's box. Every full piece already clips its writes to the box it is handed, so
 * passing the chunk box narrowed to the region reproduces exactly the writes the whole piece would make there.
 */
public final class ProximaSegmentPiece extends StructurePiece {

	private final StructurePiece inner;

	public ProximaSegmentPiece(StructurePiece inner, BoundingBox region) {
		super(ProximaStructurePieces.SEGMENT.get(), 0, region);
		this.inner = inner;
	}

	public ProximaSegmentPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(ProximaStructurePieces.SEGMENT.get(), tag);
		CompoundTag innerTag = tag.getCompoundOrEmpty("Inner");
		Identifier id = Identifier.tryParse(innerTag.getStringOr("id", ""));
		StructurePieceType type = id == null ? null : BuiltInRegistries.STRUCTURE_PIECE.getValue(id);
		if (type == null)
			throw new IllegalStateException("Unknown Proxima segment piece " + innerTag.getStringOr("id", ""));
		inner = type.load(context, innerTag);
	}

	public StructurePiece inner() {
		return inner;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.put("Inner", inner.createTag(context));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		BoundingBox region = getBoundingBox();
		if (!region.intersects(chunkBox))
			return;
		BoundingBox clipped = new BoundingBox(Math.max(chunkBox.minX(), region.minX()), Math.max(chunkBox.minY(), region.minY()),
				Math.max(chunkBox.minZ(), region.minZ()), Math.min(chunkBox.maxX(), region.maxX()),
				Math.min(chunkBox.maxY(), region.maxY()), Math.min(chunkBox.maxZ(), region.maxZ()));
		inner.postProcess(level, structureManager, generator, random, clipped, chunkPos, referencePos);
	}
}
