package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
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
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.auxiliary.structure.PortalStructure;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;

/** Writes the NBT-backed portal and the eight End Crystals which are entities outside its template. */
public final class PortalStructurePiece extends StructurePiece {

	private static final int TEMPLATE_RADIUS = 7;
	private static final int ENTITY_RADIUS = 9;
	private static final int TEMPLATE_HEIGHT = 10;

	private final BlockPos centre;

	public PortalStructurePiece(BlockPos centre) {
		super(ProximaStructurePieces.PORTAL.get(), 0, boundsFor(centre));
		this.centre = centre.immutable();
	}

	public PortalStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.PORTAL.get(), tag);
		this.centre = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 0),
				tag.getIntOr("CZ", 0));
	}

	private static BoundingBox boundsFor(BlockPos centre) {
		return new BoundingBox(centre.getX() - ENTITY_RADIUS, centre.getY(),
				centre.getZ() - ENTITY_RADIUS, centre.getX() + ENTITY_RADIUS,
				centre.getY() + TEMPLATE_HEIGHT - 1, centre.getZ() + ENTITY_RADIUS);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", centre.getX());
		tag.putInt("CY", centre.getY());
		tag.putInt("CZ", centre.getZ());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		BlockPos origin = centre.offset(-TEMPLATE_RADIUS, 0, -TEMPLATE_RADIUS);
		level.getLevel().getStructureManager().get(ChromaStructureTemplateProvider.PORTAL)
				.orElseThrow(() -> new IllegalStateException(
						"Missing portal template " + ChromaStructureTemplateProvider.PORTAL))
				.placeInWorld(level, origin, origin,
						new StructurePlaceSettings().setRotation(Rotation.NONE)
								.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(centre.asLong()), 2);

		for (BlockPos relative : PortalStructure.ENDER_CRYSTALS) {
			BlockPos crystalPos = centre.offset(relative);
			if (!chunkBox.isInside(crystalPos))
				continue;
			// ItemEnderCrystal lays the same bedrock support before restoring a captured crystal. The
			// bedrock is not part of the matching template because V33a displayed it as the mover item,
			// but a command-built portal must still be a complete, stable test fixture.
			level.setBlock(crystalPos.below(), Blocks.BEDROCK.defaultBlockState(), 2);
			AABB cell = new AABB(crystalPos);
			if (!level.getEntitiesOfClass(EndCrystal.class, cell).isEmpty())
				continue;
			EndCrystal crystal = new EndCrystal(level.getLevel(), crystalPos.getX() + 0.5,
					crystalPos.getY(), crystalPos.getZ() + 0.5);
			level.addFreshEntity(crystal);
		}

		// The template is clipped per chunk. Only the pass containing the controller can validate it,
		// and by then all immediately adjacent template cells for that chunk have been written.
		if (chunkBox.isInside(centre)
				&& level.getBlockEntity(centre) instanceof TileEntityCrystalPortal portal)
			portal.validateStructure();
	}
}
