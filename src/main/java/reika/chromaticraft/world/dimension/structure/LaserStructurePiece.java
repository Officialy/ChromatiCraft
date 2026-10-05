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
import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector26;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLaserController;
import reika.chromaticraft.tileentity.TileEntityLaserEffector;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/** Chunk-clipped composition of all authored rooms and their persistent puzzle devices. */
public final class LaserStructurePiece extends StructurePiece {

	private final BlockPos center;
	private final int floorY;
	private final int surfaceY;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public LaserStructurePiece(StructureCalculator.StructurePlacement placement,
			LaserStructureGenerator plan, int surfaceY) {
		this(placement.placement(), plan, surfaceY, placement.color, placement.generationIndex);
	}

	/** Also used by the in-world chunk-placement test, with the same data as natural placement. */
	public LaserStructurePiece(BlockPos placement, LaserStructureGenerator plan, int surfaceY,
			CrystalElement element, int index) {
		super(ProximaStructurePieces.LASER.get(), 0, boundsFor(placement, plan, surfaceY));
		center = placement.immutable();
		floorY = plan.getPosY();
		this.surfaceY = Math.max(surfaceY, floorY + 8);
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = element;
		generationIndex = index;
	}

	public LaserStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.LASER.get(), tag);
		center = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		floorY = tag.getIntOr("FloorY", 40);
		surfaceY = tag.getIntOr("SurfaceY", floorY + 20);
		difficulty = Math.clamp(tag.getIntOr("Difficulty", 3), 1, 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0),
				CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos center, LaserStructureGenerator plan, int surface) {
		return new BoundingBox(center.getX() - 7, plan.getPosY() - 1, center.getZ() - 24,
				center.getX() + plan.puzzle().lootStartX() + 14,
				Math.max(surface + 1, plan.getPosY() + 13), center.getZ() + 24);
	}

	@Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,
			CompoundTag tag) {
		tag.putInt("X", center.getX()); tag.putInt("Z", center.getZ());
		tag.putInt("FloorY", floorY); tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty); tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal()); tag.putInt("GenerationIndex", generationIndex);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		LaserPuzzleLayout layout = LaserPuzzleLayout.create(difficulty, puzzleSeed);
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LASER_ENTRANCE,
				center.offset(-5, floorY + 2, -5));
		placeShaftAndSurface(level, chunkBox);
		for (int index = 0; index < layout.rooms().size(); index++) {
			LaserPuzzleLayout.PlacedRoom placed = layout.rooms().get(index);
			LaserPuzzleLayout.RoomBlueprint room = placed.blueprint();
			int roomX = center.getX() + placed.x();
			int originZ = center.getZ() - (room.sizeZ() + 1) / 2;
			BlockPos effectorOrigin = new BlockPos(roomX, floorY, originZ);
			placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.laserRoom(room.name()),
					effectorOrigin.offset(-7, -1, -8));
			if (index < layout.rooms().size() - 1)
				placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LASER_TUNNEL,
						new BlockPos(roomX + room.sizeX() + 8, floorY + 2, center.getZ() - 3));
			bindRoom(level, chunkBox, room, effectorOrigin);
		}
		int lootX = center.getX() + layout.lootStartX();
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LASER_LOOT,
				new BlockPos(lootX, floorY + 2, center.getZ() - 3));
		BlockPos corePos = new BlockPos(lootX + 10, floorY + 4, center.getZ());
		if (chunkBox.isInside(corePos)) {
			level.setBlock(corePos, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(corePos) instanceof TileEntityDimensionCore core) {
				StructureGeneratorBase live = findLiveGenerator();
				if (live != null) core.setStructure(new StructurePair(live, color));
			}
		}
	}

	private void bindRoom(WorldGenLevel level, BoundingBox chunkBox,
			LaserPuzzleLayout.RoomBlueprint room, BlockPos origin) {
		BlockPos controllerPos = new BlockPos(origin.getX() + (room.sizeX() + 1) / 2,
				floorY + 4, origin.getZ() - 3);
		BlockPos panelPos = controllerPos.below();
		if (chunkBox.isInside(panelPos))
			level.setBlock(panelPos, ChromaBlocks.LIGHT_PANEL.get().defaultBlockState(), 2);
		if (chunkBox.isInside(controllerPos)) {
			level.setBlock(controllerPos, ChromaBlocks.LASER_CONTROLLER.get().defaultBlockState(), 2);
			if (level.getBlockEntity(controllerPos) instanceof TileEntityLaserController controller)
				controller.configure(room, origin);
		}
		BlockPos passwordPos = controllerPos.below(2);
		if (chunkBox.isInside(passwordPos)) {
			level.setBlock(passwordPos, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
			if (level.getBlockEntity(passwordPos) instanceof TileEntityStructurePassword password)
				password.setStructure(color, DimensionStructureType.LASER, generationIndex);
		}
		for (LaserPuzzleLayout.Effector effector : room.effectors()) {
			BlockPos at = origin.offset(effector.position());
			if (!chunkBox.isInside(at)) continue;
			BlockState state = ChromaBlocks.LASER_EFFECTOR.get().defaultBlockState()
					.setValue(BlockLaserEffector26.TYPE, effector.type().ordinal())
					.setValue(BlockLaserEffector26.DIRECTION, effector.direction().ordinal())
					.setValue(BlockLaserEffector26.FULL_BLOCK, effector.fullBlock())
					.setValue(BlockLaserEffector26.COLOR, (effector.red() ? 1 : 0)
							| (effector.green() ? 2 : 0) | (effector.blue() ? 4 : 0));
			level.setBlock(at, state, 2);
			if (level.getBlockEntity(at) instanceof TileEntityLaserEffector device)
				device.configure(effector, controllerPos);
		}
		BlockPos door = origin.offset(room.sizeX() + 7, 3, (room.sizeZ() + 1) / 2);
		for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
			BlockPos at = door.offset(0, dy, dz);
			if (!chunkBox.isInside(at)) continue;
			BlockState state = level.getBlockState(at);
			if (state.is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, BlockChromaDoor.withConnections(state, level, at), 2);
		}
	}

	private void placeShaftAndSurface(WorldGenLevel level, BoundingBox chunkBox) {
		for (int y = floorY + 7; y <= surfaceY; y++) for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos at = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
				if (!chunkBox.isInside(at)) continue;
				boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				BlockState state = wall ? shield(y % 12 == 0 ? ChromaShieldTypes.LIGHT
						: ChromaShieldTypes.STONE) : y == floorY + 7 || y == floorY + 12
						? ChromaBlocks.HOVER.get().defaultBlockState()
							.setValue(BlockHoverBlock.TYPE, BlockHoverBlock.HoverType.DAMPER)
							.setValue(BlockHoverBlock.DECAY, BlockHoverBlock.Decay.PERMANENT)
						: Blocks.AIR.defaultBlockState();
				level.setBlock(at, state, 2);
			}
		for (int radius = 3; radius <= 6; radius++) {
			BlockState state = shield(radius == 3 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE);
			int y = radius <= 4 ? surfaceY : surfaceY - 1;
			for (int d = -radius; d <= radius; d++) {
				setClipped(level, chunkBox, center.offset(d, y, -radius), state);
				setClipped(level, chunkBox, center.offset(d, y, radius), state);
				setClipped(level, chunkBox, center.offset(-radius, y, d), state);
				setClipped(level, chunkBox, center.offset(radius, y, d), state);
			}
		}
	}

	private static void setClipped(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
		if (box.isInside(pos)) level.setBlock(pos, state, 2);
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id,
			BlockPos anchor) {
		var template = level.getLevel().getStructureTemplateManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Chromatic Beams template " + id));
		var size = template.getSize();
		if (anchor.getX() > chunkBox.maxX() || anchor.getX() + size.getX() - 1 < chunkBox.minX()
				|| anchor.getZ() > chunkBox.maxZ() || anchor.getZ() + size.getZ() - 1 < chunkBox.minZ())
			return;
		template.placeInWorld(level, anchor, anchor, new StructurePlaceSettings()
						.setRotation(Rotation.NONE).setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(anchor.asLong() ^ puzzleSeed), 2);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.LASER
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base) return base;
		return null;
	}
}
