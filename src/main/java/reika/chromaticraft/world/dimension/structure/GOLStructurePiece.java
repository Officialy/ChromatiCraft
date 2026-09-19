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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.BlockHoverBlock.Decay;
import reika.chromaticraft.block.BlockHoverBlock.HoverType;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityGOLController;
import reika.chromaticraft.tileentity.TileEntityGOLTile;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/** Chunk-clipped NBT composition and mutable binding for V33a's Cellular Automata structure. */
public final class GOLStructurePiece extends StructurePiece {

	private final BlockPos center;
	private final int floorY;
	private final int surfaceY;
	private final int difficulty;
	private final CrystalElement color;
	private final int generationIndex;

	public GOLStructurePiece(StructureCalculator.StructurePlacement placement,
			GOLStructureGenerator plan, int surfaceY) {
		super(ProximaStructurePieces.GOL.get(), 0, boundsFor(placement.placement(), plan, surfaceY));
		center = placement.placement().immutable();
		floorY = plan.floorY();
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		color = placement.color;
		generationIndex = placement.generationIndex;
	}

	public GOLStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.GOL.get(), tag);
		center = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		floorY = tag.getIntOr("FloorY", 41);
		surfaceY = tag.getIntOr("SurfaceY", floorY + 20);
		difficulty = Math.clamp(tag.getIntOr("Difficulty", 3), 1, 3);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos center, GOLStructureGenerator plan, int surfaceY) {
		int radius = plan.radius();
		return new BoundingBox(center.getX() - radius - 18, Math.min(plan.floorY() - 1, surfaceY - 1),
				center.getZ() - radius - 1, center.getX() + radius + 11,
				Math.max(plan.floorY() + 13, surfaceY + 5), center.getZ() + radius + 1);
	}

	@Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", center.getX()); tag.putInt("Z", center.getZ());
		tag.putInt("FloorY", floorY); tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty); tag.putInt("Color", color.ordinal());
		tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		GOLPuzzleLayout layout = GOLPuzzleLayout.forDifficulty(difficulty);
		int radius = layout.radius();
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.golChamber(difficulty),
				center.offset(-radius - 1, floorY - 1, -radius - 1));
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_GOL_ENTRANCE_PREFAB,
				center.offset(-radius - 9, floorY, -8));
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_GOL_ENTRANCE_DOOR,
				center.offset(-radius - 1, floorY + 1, -8));
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_GOL_EXIT_DOOR,
				center.offset(radius + 1, floorY + 1, -4));
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_GOL_LOOT,
				center.offset(radius + 2, floorY - 1, -3));

		placeShaftAndSurface(level, chunkBox, radius);
		bindRuntime(level, chunkBox, layout);
	}

	private void placeShaftAndSurface(WorldGenLevel level, BoundingBox chunkBox, int radius) {
		BlockPos shaft = center.offset(-radius - 12, 0, 0);
		for (int y = floorY; y < surfaceY; y++) for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos at = new BlockPos(shaft.getX() + dx, y, shaft.getZ() + dz);
				if (!chunkBox.isInside(at)) continue;
				int height = y - floorY;
				boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && height > 0;
				BlockState state;
				if (inner && height >= 5 && y < surfaceY - 5 && height % 8 == 5)
					state = ChromaBlocks.HOVER.get().defaultBlockState()
							.setValue(BlockHoverBlock.TYPE, HoverType.DAMPER)
							.setValue(BlockHoverBlock.DECAY, Decay.PERMANENT);
				else if (inner) state = Blocks.AIR.defaultBlockState();
				else state = shield(height % 8 == 2 && (dx == 0 || dz == 0)
						? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE);
				level.setBlock(at, state, 2);
			}
		for (int y = floorY + 1; y <= floorY + 3; y++) for (int dz = -1; dz <= 1; dz++) {
			BlockPos doorway = new BlockPos(shaft.getX() + 2, y, shaft.getZ() + dz);
			if (chunkBox.isInside(doorway)) level.setBlock(doorway, Blocks.AIR.defaultBlockState(), 2);
		}
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_GOL_SURFACE,
				new BlockPos(shaft.getX() - 6, surfaceY - 1, shaft.getZ() - 6));
	}

	private void bindRuntime(WorldGenLevel level, BoundingBox chunkBox, GOLPuzzleLayout layout) {
		int radius = layout.radius();
		BlockPos controllerPos = center.offset(-radius - 3, floorY + 1, 0);
		BlockPos doorCenter = center.offset(radius + 1, floorY + 1, 0);
		if (chunkBox.isInside(controllerPos)) {
			level.setBlock(controllerPos, ChromaBlocks.GOL_CONTROLLER.get().defaultBlockState(), 2);
			if (level.getBlockEntity(controllerPos) instanceof TileEntityGOLController controller)
				controller.configure(center.getX() - radius, center.getX() + radius,
						center.getZ() - radius, center.getZ() + radius, floorY,
						layout.maxSelected(), layout.requiredTrail(), doorCenter);
		}
		for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			BlockPos cellPos = center.offset(x, floorY, z);
			if (chunkBox.isInside(cellPos) && level.getBlockEntity(cellPos) instanceof TileEntityGOLTile cell)
				cell.bind(controllerPos);
		}
		bindPassword(level, chunkBox, controllerPos.above());
		bindPassword(level, chunkBox, center.offset(-radius - 12, floorY - 1, 0));
		resolveDoor(level, chunkBox, doorCenter);

		BlockPos corePos = center.offset(radius + 8, floorY + 2, 0);
		if (chunkBox.isInside(corePos)) {
			level.setBlock(corePos, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(corePos) instanceof TileEntityDimensionCore core) {
				StructureGeneratorBase live = findLiveGenerator();
				if (live != null) core.setStructure(new StructurePair(live, color));
			}
		}
	}

	private void bindPassword(WorldGenLevel level, BoundingBox chunkBox, BlockPos pos) {
		if (!chunkBox.isInside(pos)) return;
		level.setBlock(pos, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
		if (level.getBlockEntity(pos) instanceof TileEntityStructurePassword password)
			password.setStructure(color, DimensionStructureType.GOL, generationIndex);
	}

	private static void resolveDoor(WorldGenLevel level, BoundingBox chunkBox, BlockPos center) {
		for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
			BlockPos at = center.offset(0, dy, dz);
			if (chunkBox.isInside(at) && level.getBlockState(at).is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, BlockChromaDoor.withConnections(level.getBlockState(at), level, at), 2);
		}
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id, BlockPos anchor) {
		level.getLevel().getStructureManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Cellular Automata template " + id))
				.placeInWorld(level, anchor, anchor, new StructurePlaceSettings().setRotation(Rotation.NONE)
						.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(anchor.asLong() ^ difficulty), 2);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.GOL
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base) return base;
		return null;
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
	}
}
