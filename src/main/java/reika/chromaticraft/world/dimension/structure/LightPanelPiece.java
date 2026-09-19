package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLightSwitch;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/** Chunk-clipped NBT composition of V33a's full Glowing Logic structure. */
public final class LightPanelPiece extends StructurePiece {

	private static final int[] TIERS = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 6};

	private final BlockPos entry;
	private final int baseY;
	private final int surfaceY;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public LightPanelPiece(StructureCalculator.StructurePlacement placement,
			LightPanelStructureGenerator plan, int surfaceY) {
		super(ProximaStructurePieces.LIGHT_PANEL.get(), 0,
				boundsFor(placement.placement(), plan, surfaceY));
		entry = placement.placement().immutable();
		baseY = plan.getPosY();
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = placement.color;
		generationIndex = placement.generationIndex;
	}

	public LightPanelPiece(CompoundTag tag) {
		super(ProximaStructurePieces.LIGHT_PANEL.get(), tag);
		entry = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		baseY = tag.getIntOr("BaseY", 40);
		surfaceY = tag.getIntOr("SurfaceY", baseY + 19);
		difficulty = tag.getIntOr("Difficulty", 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos entry, LightPanelStructureGenerator plan, int surfaceY) {
		BlockPos loot = plan.lootCenter();
		int stairAnchorY = stairTopY(plan.getPosY(), surfaceY);
		int over = stairAnchorY + 28 - surfaceY - 2;
		int mountainRadius = Math.max(0, over * 8);
		return new BoundingBox(Math.min(entry.getX() - mountainRadius, entry.getX() - 9),
				plan.getPosY() - 1, Math.min(entry.getZ() - mountainRadius, entry.getZ() - 10),
				Math.max(loot.getX() + 10, entry.getX() + mountainRadius),
				Math.max(plan.getPosY() + 21, surfaceY + over + 12),
				Math.max(entry.getZ() + 10, entry.getZ() + mountainRadius));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", entry.getX()); tag.putInt("Z", entry.getZ());
		tag.putInt("BaseY", baseY); tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty); tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal()); tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		placeEntrance(level, chunkBox);
		List<BlockPos> rooms = roomOrigins();
		for (int room = 0; room < rooms.size(); room++) {
			int tier = TIERS[room];
			int radius = reika.chromaticraft.world.dimension.structure.lightpanel
					.LightPanelPatternLibrary.switchCount(tier) + 2;
			placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.lightPanelRoom(tier),
					rooms.get(room).offset(0, 0, -radius));
			bindRoomChunk(level, chunkBox, rooms.get(room), room,
					reika.chromaticraft.world.dimension.structure.lightpanel
							.LightPanelPatternLibrary.switchCount(tier));
		}
		BlockPos loot = lootCenter();
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LIGHT_PANEL_LOOT,
				loot.offset(-10, 0, -10));
		BlockPos controllerPos = loot.offset(0, 21, 0);
		if (chunkBox.isInside(controllerPos)
				&& level.getBlockEntity(controllerPos) instanceof TileEntityStructureController controller
				&& controller.getGlowingLogicRooms().isEmpty())
			controller.initializeGlowingLogic(rooms, difficulty, new Random(puzzleSeed));

		BlockPos corePos = loot.offset(0, 6, 0);
		if (chunkBox.isInside(corePos)) {
			level.setBlock(corePos, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(corePos) instanceof TileEntityDimensionCore core) {
				StructureGeneratorBase live = findLiveGenerator();
				if (live != null) core.setStructure(new StructurePair(live, color));
			}
		}
		BlockPos passwordPos = rooms.get(0).offset(1, 1, 0);
		if (chunkBox.isInside(passwordPos)
				&& level.getBlockEntity(passwordPos) instanceof TileEntityStructurePassword password)
			password.setStructure(color, DimensionStructureType.LIGHTPANEL, generationIndex);
		resolveDoors(level, chunkBox, rooms);
	}

	private void placeEntrance(WorldGenLevel level, BoundingBox chunkBox) {
		int y = baseY - 1;
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LIGHT_PANEL_STAIR_BOTTOM,
				new BlockPos(entry.getX() - 9, y, entry.getZ() - 8));
		y += 19;
		while (y + 16 < surfaceY) {
			placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LIGHT_PANEL_STAIR_SECTION,
					new BlockPos(entry.getX() - 8, y, entry.getZ() - 8));
			y += 16;
		}
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_LIGHT_PANEL_STAIR_TOP,
				new BlockPos(entry.getX() - 8, y, entry.getZ() - 8));
		y += 28;
		int over = y - surfaceY - 2;
		if (over > 2) generateMountain(level, chunkBox, over);
	}

	/** V33a LightPanelEntrance.generateMountain, clipped to this structure-piece chunk pass. */
	private void generateMountain(WorldGenLevel level, BoundingBox chunkBox, int over) {
		int radius = over * 8;
		int summit = surfaceY + over;
		int minDx = Math.max(-radius, chunkBox.minX() - entry.getX());
		int maxDx = Math.min(radius, chunkBox.maxX() - entry.getX());
		int minDz = Math.max(-radius, chunkBox.minZ() - entry.getZ());
		int maxDz = Math.min(radius, chunkBox.maxZ() - entry.getZ());
		for (int dx = minDx; dx <= maxDx; dx++) for (int dz = minDz; dz <= maxDz; dz++) {
			if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) continue;
			int x = entry.getX() + dx;
			int z = entry.getZ() + dz;
			int di = Math.max(0, Math.abs(dx) - 3);
			int dk = Math.max(0, Math.abs(dz) - 3);
			int target = summit - Math.max(di, dk) / 2;
			int localTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			if (target <= localTop) continue;
			for (int y = localTop; y <= target + 12; y++) {
				BlockPos at = new BlockPos(x, y, z);
				if (!chunkBox.isInside(at) || level.getBlockState(at).getBlock()
						instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield)
					continue;
				level.setBlock(at, y < target ? Blocks.STONE.defaultBlockState()
						: y == target ? Blocks.GRASS_BLOCK.defaultBlockState()
						: Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	private static int stairTopY(int baseY, int surfaceY) {
		int y = baseY - 1 + 19;
		while (y + 16 < surfaceY) y += 16;
		return y;
	}

	private void bindRoomChunk(WorldGenLevel level, BoundingBox chunkBox, BlockPos origin,
			int room, int switches) {
		BlockPos delegate = lootCenter().offset(0, 21, 0);
		for (int channel = 0; channel < switches; channel++) {
			BlockPos at = origin.offset(6, 2, -switches + channel * 2 + 1);
			if (chunkBox.isInside(at) && level.getBlockEntity(at) instanceof TileEntityLightSwitch panel) {
				panel.setData(room, channel);
				panel.setDelegate(delegate);
			}
		}
	}

	private static void resolveDoors(WorldGenLevel level, BoundingBox chunkBox, List<BlockPos> rooms) {
		for (BlockPos origin : rooms) for (int y = 1; y <= 3; y++) for (int z = -2; z <= 2; z++) {
			BlockPos at = origin.offset(12, y, z);
			if (chunkBox.isInside(at) && level.getBlockState(at).is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, BlockChromaDoor.withConnections(level.getBlockState(at), level, at), 2);
		}
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id, BlockPos anchor) {
		level.getLevel().getStructureManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Glowing Logic template " + id))
				.placeInWorld(level, anchor, anchor,
						new StructurePlaceSettings().setRotation(Rotation.NONE)
								.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(puzzleSeed ^ anchor.asLong()), 2);
	}

	private List<BlockPos> roomOrigins() {
		ArrayList<BlockPos> rooms = new ArrayList<>();
		int x = entry.getX() + LightPanelStructureGenerator.FIRST_ROOM_OFFSET;
		for (int room = 0; room < LightPanelStructureGenerator.roomCount(difficulty); room++) {
			rooms.add(new BlockPos(x, baseY, entry.getZ()));
			x += LightPanelStructureGenerator.ROOM_STRIDE;
		}
		return rooms;
	}

	private BlockPos lootCenter() {
		return new BlockPos(entry.getX() + LightPanelStructureGenerator.FIRST_ROOM_OFFSET
				+ LightPanelStructureGenerator.roomCount(difficulty) * LightPanelStructureGenerator.ROOM_STRIDE
				+ LightPanelStructureGenerator.LOOT_RADIUS, baseY, entry.getZ());
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.LIGHTPANEL
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base)
				return base;
		return null;
	}
}
