package reika.chromaticraft.world.dimension.structure;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import reika.chromaticraft.world.OverworldStructureFeature;

/**
 * Single-chunk command piece for the complete Burrow inspection form.
 *
 * <p>The combined base/furnace/cache bounds are only 11x12x11. The structure generation point
 * deliberately keeps those bounds inside its centre chunk, so delegating to the authoritative
 * feature callback path cannot perform the far-chunk writes that made large configured features
 * unsafe elsewhere in this port.
 */
public final class BurrowCommandPiece extends StructurePiece {

	private final BlockPos controller;

	public BurrowCommandPiece(BlockPos controller) {
		super(ProximaStructurePieces.BURROW.get(), 0, boundsFor(controller));
		this.controller = controller.immutable();
	}

	public BurrowCommandPiece(CompoundTag tag) {
		super(ProximaStructurePieces.BURROW.get(), tag);
		controller = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 0),
				tag.getIntOr("CZ", 0));
	}

	private static BoundingBox boundsFor(BlockPos controller) {
		// Union of base (-3,-3,-3)..(6,8,3), furnace (0,-3,-4)..(6,2,6), and
		// cache (0,-3,-4)..(7,1,2), all expressed relative to the controller.
		return new BoundingBox(controller.getX() - 3, controller.getY() - 3, controller.getZ() - 4,
				controller.getX() + 7, controller.getY() + 8, controller.getZ() + 6);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", controller.getX());
		tag.putInt("CY", controller.getY());
		tag.putInt("CZ", controller.getZ());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		// The bounds invariant above means this piece is processed exactly once and every authored
		// cell is in chunkBox. Reuse the same NBT placement/callback authority as the feature alias.
		new OverworldStructureFeature(OverworldStructureFeature.Type.BURROW, false).place(
				new FeaturePlaceContext<>(Optional.empty(), level, generator, random, controller,
						NoneFeatureConfiguration.INSTANCE));
	}
}
