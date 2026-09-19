package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.BlockHoverBlock.Decay;
import reika.chromaticraft.block.BlockHoverBlock.HoverType;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/** Chunk-clipped NBT composition of V33a's complete Three-Dimensional Maze. */
public final class ThreeDMazePiece extends StructurePiece {

	private final BlockPos origin;
	private final int baseY;
	private final int surfaceY;
	private final int difficulty;
	private final long mazeSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public ThreeDMazePiece(StructureCalculator.StructurePlacement placement,
			ThreeDMazeStructureGenerator plan, int surfaceY) {
		super(ProximaStructurePieces.THREE_D_MAZE.get(), 0,
				boundsFor(new BlockPos(plan.getPosX(), plan.getPosY(), plan.getPosZ()), plan, surfaceY));
		origin = new BlockPos(plan.getPosX(), 0, plan.getPosZ());
		baseY = plan.getPosY();
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		mazeSeed = plan.mazeSeed();
		color = placement.color;
		generationIndex = placement.generationIndex;
	}

	public ThreeDMazePiece(CompoundTag tag) {
		super(ProximaStructurePieces.THREE_D_MAZE.get(), tag);
		origin = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		baseY = tag.getIntOr("BaseY", 75);
		surfaceY = tag.getIntOr("SurfaceY", baseY);
		difficulty = tag.getIntOr("Difficulty", 3);
		mazeSeed = tag.getLongOr("MazeSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos origin, ThreeDMazeStructureGenerator plan, int surfaceY) {
		int span = plan.width() * ThreeDMazeLayout.CELL_SIZE;
		int entryX = origin.getX() + span / 2 + 2;
		int entryZ = origin.getZ() + span / 2 + 2;
		int bottom = plan.getPosY() - plan.height() * ThreeDMazeLayout.CELL_SIZE - 10;
		return new BoundingBox(Math.min(origin.getX(), entryX - 8), bottom,
				Math.min(origin.getZ(), entryZ - 8), Math.max(origin.getX() + span, entryX + 8),
				Math.max(plan.getPosY(), surfaceY + 5), Math.max(origin.getZ() + span, entryZ + 8));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", origin.getX()); tag.putInt("Z", origin.getZ());
		tag.putInt("BaseY", baseY); tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty); tag.putLong("MazeSeed", mazeSeed);
		tag.putInt("Color", color.ordinal()); tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos,
			BlockPos referencePos) {
		ThreeDMazeLayout layout = ThreeDMazeLayout.create(difficulty, mazeSeed);
		placeCells(level, chunkBox, layout);
		carveCells(level, chunkBox, layout);
		formRooms(level, chunkBox, layout);
		placeReward(level, chunkBox, layout);
		placeEntrance(level, chunkBox, layout);
	}

	private void placeCells(WorldGenLevel level, BoundingBox chunkBox, ThreeDMazeLayout layout) {
		int firstX = Mth.clamp(Math.floorDiv(chunkBox.minX() - origin.getX() - 4, 4), 0, layout.width() - 1);
		int lastX = Mth.clamp(Math.floorDiv(chunkBox.maxX() - origin.getX(), 4), 0, layout.width() - 1);
		int firstZ = Mth.clamp(Math.floorDiv(chunkBox.minZ() - origin.getZ() - 4, 4), 0, layout.width() - 1);
		int lastZ = Mth.clamp(Math.floorDiv(chunkBox.maxZ() - origin.getZ(), 4), 0, layout.width() - 1);
		for (int x = firstX; x <= lastX; x++) for (int y = 0; y < layout.height(); y++)
			for (int z = firstZ; z <= lastZ; z++)
				placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_TD_MAZE_CELL,
						cellOrigin(layout, x, y, z));
	}

	private void carveCells(WorldGenLevel level, BoundingBox chunkBox, ThreeDMazeLayout layout) {
		int firstX = Mth.clamp(Math.floorDiv(chunkBox.minX() - origin.getX() - 4, 4), 0, layout.width() - 1);
		int lastX = Mth.clamp(Math.floorDiv(chunkBox.maxX() - origin.getX(), 4), 0, layout.width() - 1);
		int firstZ = Mth.clamp(Math.floorDiv(chunkBox.minZ() - origin.getZ() - 4, 4), 0, layout.width() - 1);
		int lastZ = Mth.clamp(Math.floorDiv(chunkBox.maxZ() - origin.getZ(), 4), 0, layout.width() - 1);
		for (int x = firstX; x <= lastX; x++) for (int y = 0; y < layout.height(); y++)
			for (int z = firstZ; z <= lastZ; z++) {
				BlockPos cell = cellOrigin(layout, x, y, z);
				for (Direction direction : Direction.values())
					paintFace(level, chunkBox, cell, direction,
							(layout.connections(x, y, z) & bit(direction)) != 0,
							(layout.windows(x, y, z) & bit(direction)) != 0,
							layout.lighted(x, y, z));
			}
	}

	private static void paintFace(WorldGenLevel level, BoundingBox chunkBox, BlockPos cell,
			Direction direction, boolean open, boolean window, boolean lighted) {
		BlockState stone = shield(ChromaShieldTypes.STONE);
		BlockState glass = shield(ChromaShieldTypes.GLASS);
		BlockState light = shield(ChromaShieldTypes.LIGHT);
		for (int a = 1; a <= 3; a++) for (int b = 1; b <= 3; b++) {
			BlockPos at = switch (direction) {
				case DOWN -> cell.offset(a, 0, b);
				case UP -> cell.offset(a, 4, b);
				case NORTH -> cell.offset(a, b, 0);
				case SOUTH -> cell.offset(a, b, 4);
				case WEST -> cell.offset(0, b, a);
				case EAST -> cell.offset(4, b, a);
			};
			if (!chunkBox.isInside(at)) continue;
			BlockState state = open ? Blocks.AIR.defaultBlockState()
					: window ? glass : lighted && a == 2 && b == 2 ? light : stone;
			level.setBlock(at, state, 2);
		}
	}

	private void formRooms(WorldGenLevel level, BoundingBox chunkBox, ThreeDMazeLayout layout) {
		for (ThreeDMazeLayout.Room room : layout.rooms()) {
			BlockPos center = cellOrigin(layout, room.x(), room.y(), room.z());
			for (int cx = -room.radius(); cx <= room.radius(); cx++)
				for (int cz = -room.radius(); cz <= room.radius(); cz++) {
					BlockPos cell = center.offset(cx * 4, 0, cz * 4);
					for (int x = 0; x <= 4; x++) for (int y = 0; y <= 4; y++) for (int z = 0; z <= 4; z++) {
						BlockPos at = cell.offset(x, y, z);
						if (!chunkBox.isInside(at)) continue;
						if (y == 0) {
							if (level.getBlockState(at).isAir()) level.setBlock(at, shield(ChromaShieldTypes.CRACKS), 2);
						}
						else if (y == 4) {
							if (level.getBlockState(at).isAir()) level.setBlock(at,
									ChromaBlocks.HOVER.get().defaultBlockState()
											.setValue(BlockHoverBlock.TYPE, HoverType.DAMPER)
											.setValue(BlockHoverBlock.DECAY, Decay.PERMANENT), 2);
						}
						else {
							boolean interior = (cx > -room.radius() || x > 0)
									&& (cx < room.radius() || x < 4)
									&& (cz > -room.radius() || z > 0)
									&& (cz < room.radius() || z < 4);
							if (interior) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
							else if (level.getBlockState(at).isAir())
								placeConnectedIronBars(level, at);
						}
					}
				}
			if (room.radius() == 2) placeLargeRoomCenter(level, chunkBox, center, room.chromaRing());
			if (room.chest()) placeRoomChest(level, chunkBox, center.offset(2, 2, 2), room);
		}
	}

	/**
	 * Structure placement uses flag 2 and therefore does not run the ordinary neighbour update which
	 * a hand-placed iron bar receives. Refresh the new bar and every existing horizontal neighbour so
	 * chunk-order cannot leave either half of a 3x3 window disconnected.
	 */
	public static void placeConnectedIronBars(LevelAccessor level, BlockPos at) {
		level.setBlock(at, Blocks.IRON_BARS.defaultBlockState(), 2);
		refreshIronBar(level, at);
		for (Direction direction : new Direction[] {
				Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST})
			refreshIronBar(level, at.relative(direction));
	}

	private static void refreshIronBar(LevelAccessor level, BlockPos at) {
		BlockState state = level.getBlockState(at);
		if (!state.is(Blocks.IRON_BARS))
			return;
		BlockState connected = Block.updateFromNeighbourShapes(state, level, at);
		if (connected != state)
			level.setBlock(at, connected, 2);
	}

	private static void placeLargeRoomCenter(WorldGenLevel level, BoundingBox chunkBox,
			BlockPos center, boolean chromaRing) {
		if (chromaRing) {
			for (int x = -2; x <= 6; x++) for (int z = -2; z <= 6; z++) {
				BlockPos at = center.offset(x, 1, z);
				if (!chunkBox.isInside(at)) continue;
				BlockState state = x == -2 || x == 6 || z == -2 || z == 6 ? shield(ChromaShieldTypes.STONE)
						: x == -1 || x == 5 || z == -1 || z == 5
								? ChromaBlocks.CHROMA.get().defaultBlockState()
								: x == 0 || x == 4 || z == 0 || z == 4 ? shield(ChromaShieldTypes.STONE) : null;
				if (state != null) level.setBlock(at, state, 2);
			}
		}
		else {
			for (int x = -1; x <= 5; x++) for (int z = -1; z <= 5; z++) {
				if (x != -1 && x != 5 && z != -1 && z != 5) continue;
				for (BlockPos at : new BlockPos[] {center.offset(x, 1, z), center.offset(x, 2, z),
						center.offset(x + 1, 1, z), center.offset(x - 1, 1, z),
						center.offset(x, 1, z + 1), center.offset(x, 1, z - 1)})
					if (chunkBox.isInside(at)) level.setBlock(at, Blocks.GLOWSTONE.defaultBlockState(), 2);
			}
		}
	}

	private static void placeRoomChest(WorldGenLevel level, BoundingBox chunkBox, BlockPos at,
			ThreeDMazeLayout.Room room) {
		if (!chunkBox.isInside(at)) return;
		Direction facing = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}
				[Math.floorMod(room.facing(), 4)];
		level.setBlock(at, ChromaBlocks.LOOT_CHEST.get().defaultBlockState()
				.setValue(BlockLootChest.FACING, facing), 2);
		if (!(level.getBlockEntity(at) instanceof TileEntityLootChest chest)) return;
		chest.setLootTable(room.desertLoot() ? BuiltInLootTables.DESERT_PYRAMID
				: BuiltInLootTables.SIMPLE_DUNGEON, room.chestSeed());
		chest.unpackLootTable(null);
		ItemStack bonus = switch (room.bonus()) {
			case BOOSTED_SHARD -> ChromaItems.boostedShardStack(
					CrystalElement.elements[Math.floorMod(room.bonusColor(), CrystalElement.elements.length)],
					room.bonusCount());
			case RAW_CRYSTAL -> new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.RAW_CRYSTAL).get(),
					room.bonusCount());
			case COMPLEX_INGOT -> new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.COMPLEX_INGOT).get(),
					room.bonusCount());
		};
		for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).isEmpty()) {
			chest.setItem(slot, bonus);
			break;
		}
	}

	private void placeReward(WorldGenLevel level, BoundingBox chunkBox, ThreeDMazeLayout layout) {
		int rewardCenterY = baseY - 4 * (layout.height() + 1) + 2;
		BlockPos entry = entry(layout);
		BlockPos anchor = new BlockPos(entry.getX() - 5, rewardCenterY - 8, entry.getZ() - 5);
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_TD_MAZE_LOOT, anchor);
		BlockPos corePos = new BlockPos(entry.getX(), rewardCenterY - 3, entry.getZ());
		if (!chunkBox.isInside(corePos)) return;
		level.setBlock(corePos, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
		if (level.getBlockEntity(corePos) instanceof TileEntityDimensionCore core) {
			StructureGeneratorBase live = findLiveGenerator();
			if (live != null) core.setStructure(new StructurePair(live, color));
		}
	}

	private void placeEntrance(WorldGenLevel level, BoundingBox chunkBox, ThreeDMazeLayout layout) {
		BlockPos entry = entry(layout);
		for (int y = baseY; y <= surfaceY; y++)
			placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_TD_MAZE_SHAFT,
					new BlockPos(entry.getX() - 2, y, entry.getZ() - 2));
		for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
			if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue;
			BlockPos floor = new BlockPos(entry.getX() + x, surfaceY - 1, entry.getZ() + z);
			if (chunkBox.isInside(floor)) level.setBlock(floor, shield(ChromaShieldTypes.STONE), 2);
			for (int y = surfaceY - 2; y > baseY; y--) {
				BlockPos fill = new BlockPos(floor.getX(), y, floor.getZ());
				if (chunkBox.isInside(fill) && level.getBlockState(fill).isAir())
					level.setBlock(fill, Blocks.DIRT.defaultBlockState(), 2);
			}
		}
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_TD_MAZE_ENTRANCE,
				new BlockPos(entry.getX() - 6, surfaceY, entry.getZ() - 6));
	}

	private BlockPos cellOrigin(ThreeDMazeLayout layout, int x, int y, int z) {
		return new BlockPos(origin.getX() + x * 4, baseY + y * 4 - layout.height() * 4,
				origin.getZ() + z * 4);
	}

	private BlockPos entry(ThreeDMazeLayout layout) {
		return new BlockPos(origin.getX() + layout.width() * 2 + 2, baseY,
				origin.getZ() + layout.width() * 2 + 2);
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id, BlockPos anchor) {
		level.getLevel().getStructureManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Three-Dimensional Maze template " + id))
				.placeInWorld(level, anchor, anchor, new StructurePlaceSettings().setRotation(Rotation.NONE)
						.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(mazeSeed ^ anchor.asLong()), 2);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.TDMAZE
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base)
				return base;
		return null;
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
	}

	private static int bit(Direction direction) {
		return 1 << direction.ordinal();
	}
}
