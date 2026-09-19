package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;

/** Chunk-clipped writer for a complete casting temple and its central Casting Table. */
public final class CastingTemplePiece extends StructurePiece {

	private final BlockPos support;
	private final int tier;

	public CastingTemplePiece(BlockPos support, int tier) {
		super(ProximaStructurePieces.CASTING_TEMPLE.get(), 0, boundsFor(support, tier));
		this.support = support.immutable();
		this.tier = tier;
	}

	public CastingTemplePiece(CompoundTag tag) {
		super(ProximaStructurePieces.CASTING_TEMPLE.get(), tag);
		this.support = new BlockPos(tag.getIntOr("SX", 0), tag.getIntOr("SY", 0),
				tag.getIntOr("SZ", 0));
		this.tier = tag.getIntOr("Tier", 1);
	}

	private static BoundingBox boundsFor(BlockPos support, int tier) {
		int radius = tier == 3 ? 8 : 6;
		return new BoundingBox(support.getX() - radius, support.getY(), support.getZ() - radius,
				support.getX() + radius, support.getY() + 9, support.getZ() + radius);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("SX", support.getX());
		tag.putInt("SY", support.getY());
		tag.putInt("SZ", support.getZ());
		tag.putInt("Tier", tier);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		int radius = tier == 3 ? 8 : 6;
		Identifier template = NBTStructureLoader.chromaTemplate("multiblock/casting_l" + tier);
		BlockPos origin = support.offset(-radius, 0, -radius);
		// The canonical matcher NBT intentionally omits unconstrained interior cells. During command
		// placement those cells are authored air, so clear the above-foundation volume explicitly and
		// clip it to the chunk currently being processed just like StructureTemplate does.
		for (int x = support.getX() - radius; x <= support.getX() + radius; x++) {
			for (int y = support.getY() + 1; y <= support.getY() + 6; y++) {
				for (int z = support.getZ() - radius; z <= support.getZ() + radius; z++) {
					BlockPos clear = new BlockPos(x, y, z);
					if (chunkBox.isInside(clear))
						level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		level.getLevel().getStructureManager().get(template)
				.orElseThrow(() -> new IllegalStateException("Missing casting template " + template))
				.placeInWorld(level, origin, origin,
						new StructurePlaceSettings().setRotation(Rotation.NONE)
								.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(support.asLong() ^ tier), 2);

		if (chunkBox.isInside(support)) {
			if (level.getBlockState(support).isAir())
				level.setBlock(support, Blocks.STONE.defaultBlockState(), 2);
			level.setBlock(support.above(), ChromaBlocks.CASTING_TABLE.get().defaultBlockState(), 2);
			if (level.getBlockEntity(support.above()) instanceof TileEntityCastingTable table)
				table.initializeCommandPlacedTier(tier);
		}
		// Temple and above recipes use the canonical twenty-four stand sockets. The matcher NBT
		// omits them because they are apparatus rather than structural constraints; a command-spawned
		// demonstration structure nevertheless needs the usable apparatus, not just the shell.
		if (tier >= 2) {
			BlockPos table = support.above();
			for (int dx = -4; dx <= 4; dx += 2) {
				for (int dz = -4; dz <= 4; dz += 2) {
					if (dx == 0 && dz == 0)
						continue;
					int dy = Math.abs(dx) == 4 || Math.abs(dz) == 4 ? 1 : 0;
					BlockPos stand = table.offset(dx, dy, dz);
					if (chunkBox.isInside(stand))
						level.setBlock(stand, ChromaBlocks.ITEM_STAND.get().defaultBlockState(), 2);
				}
			}
		}
	}
}
