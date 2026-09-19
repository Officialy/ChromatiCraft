package reika.chromaticraft.world.dimension.structure;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import reika.chromaticraft.block.dimension.structure.music.BlockMusicMemory;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityMusicMemory;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/** Chunk-clipped NBT composition and mutable binding for V33a's Crystal Music structure. */
public final class MusicStructurePiece extends StructurePiece {

	private final BlockPos entry;
	private final int baseY;
	private final int surfaceY;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public MusicStructurePiece(StructureCalculator.StructurePlacement placement,
			MusicStructureGenerator plan, int surfaceY) {
		super(ProximaStructurePieces.MUSIC.get(), 0, boundsFor(placement.placement(), plan, surfaceY));
		entry = placement.placement().immutable();
		baseY = plan.getPosY();
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = placement.color;
		generationIndex = placement.generationIndex;
	}

	public MusicStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.MUSIC.get(), tag);
		entry = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		baseY = tag.getIntOr("BaseY", 40);
		surfaceY = tag.getIntOr("SurfaceY", baseY + 20);
		difficulty = tag.getIntOr("Difficulty", 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos entry, MusicStructureGenerator plan, int surfaceY) {
		int finalZ = entry.getZ() + MusicStructureGenerator.FIRST_ROOM_Z
				+ plan.roomCount() * MusicStructureGenerator.ROOM_STRIDE + 7;
		return new BoundingBox(entry.getX() - 2, Math.min(plan.getPosY(), surfaceY - 11), entry.getZ() - 7,
				entry.getX() + 12, Math.max(plan.getPosY() + 6, surfaceY + 6), finalZ);
	}

	@Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", entry.getX()); tag.putInt("Z", entry.getZ());
		tag.putInt("BaseY", baseY); tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty); tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal()); tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		placeEntrance(level, chunkBox);
		MusicPuzzleLayout layout = MusicPuzzleLayout.create(difficulty, puzzleSeed);
		for (int room = 0; room < layout.rooms().size(); room++)
			placeRoom(level, chunkBox, layout.rooms().get(room), room);
		placeReward(level, chunkBox, layout.rooms().size());
	}

	private void placeEntrance(WorldGenLevel level, BoundingBox chunkBox) {
		int funnelY = surfaceY - 6;
		for (int x = -2; x <= 12; x++) for (int z = -7; z <= 7; z++)
			for (int y = surfaceY - 11; y <= surfaceY + 6; y++) {
				BlockPos at = new BlockPos(entry.getX() + x, y, entry.getZ() + z);
				if (!chunkBox.isInside(at)) continue;
				BlockState state = level.getBlockState(at);
				if (y <= funnelY && state.isAir()) level.setBlock(at, Blocks.DIRT.defaultBlockState(), 2);
				else if (y > funnelY && !(state.getBlock() instanceof BlockStructureShield))
					level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			}
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_MUSIC_FUNNEL,
				new BlockPos(entry.getX() - 2, funnelY, entry.getZ() - 7));

		for (int y = baseY; y < surfaceY - 5; y++) for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos at = new BlockPos(entry.getX() + 5 + dx, y, entry.getZ() + dz);
				if (!chunkBox.isInside(at)) continue;
				boolean wall = y == baseY || Math.abs(dx) == 2
						|| Math.abs(dz) == 2 && !(dz == 2 && y <= baseY + 3);
				BlockState state = wall ? shield(ChromaShieldTypes.STONE)
						: (y - baseY) % 12 == 6 && y < surfaceY - 6
								? ChromaBlocks.HOVER.get().defaultBlockState()
										.setValue(BlockHoverBlock.TYPE, HoverType.DAMPER)
										.setValue(BlockHoverBlock.DECAY, Decay.PERMANENT)
								: Blocks.AIR.defaultBlockState();
				level.setBlock(at, state, 2);
			}
	}

	private void placeRoom(WorldGenLevel level, BoundingBox chunkBox, MusicPuzzleLayout.Room room, int index) {
		BlockPos origin = roomOrigin(index);
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_MUSIC_ROOM, origin);
		BlockPos memoryPos = origin.offset(5, 1, 5);
		if (chunkBox.isInside(memoryPos)) {
			level.setBlock(memoryPos, ChromaBlocks.MUSIC_MEMORY.get().defaultBlockState()
					.setValue(BlockMusicMemory.FACING, Direction.NORTH), 2);
			if (level.getBlockEntity(memoryPos) instanceof TileEntityMusicMemory memory)
				memory.program(room.melody(), room.playbackDelay(), index, origin.offset(5, 2, 18));
		}
		if (index == 0) {
			BlockPos passwordPos = origin.offset(5, 2, 5);
			if (chunkBox.isInside(passwordPos)) {
				level.setBlock(passwordPos, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
				if (level.getBlockEntity(passwordPos) instanceof TileEntityStructurePassword password)
					password.setStructure(color, DimensionStructureType.MUSIC, generationIndex);
			}
		}
		for (int side : new int[] {1, 9}) for (int dz = 6; dz <= 13; dz++) {
			BlockPos trigger = origin.offset(side, 1, dz);
			if (chunkBox.isInside(trigger)) level.setBlock(trigger,
					ChromaBlocks.MUSIC_TRIGGER.get().defaultBlockState(), 2);
		}
		for (int dx = 4; dx <= 6; dx++) for (int dy = 1; dy <= 3; dy++) {
			BlockPos door = origin.offset(dx, dy, 18);
			if (chunkBox.isInside(door)) level.setBlock(door,
					BlockChromaDoor.withConnections(ChromaBlocks.CHROMA_DOOR.get().defaultBlockState(), level, door), 2);
		}
		resolveDoor(level, chunkBox, origin.offset(5, 2, 18));
	}

	private void placeReward(WorldGenLevel level, BoundingBox chunkBox, int rooms) {
		BlockPos origin = new BlockPos(entry.getX() + 2, baseY,
				entry.getZ() + MusicStructureGenerator.FIRST_ROOM_Z + rooms * MusicStructureGenerator.ROOM_STRIDE);
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_MUSIC_LOOT, origin);
		BlockPos corePos = origin.offset(3, 2, 5);
		if (!chunkBox.isInside(corePos)) return;
		level.setBlock(corePos, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
		if (level.getBlockEntity(corePos) instanceof TileEntityDimensionCore core) {
			StructureGeneratorBase live = findLiveGenerator();
			if (live != null) core.setStructure(new StructurePair(live, color));
		}
	}

	private BlockPos roomOrigin(int room) {
		return new BlockPos(entry.getX(), baseY, entry.getZ() + MusicStructureGenerator.FIRST_ROOM_Z
				+ room * MusicStructureGenerator.ROOM_STRIDE);
	}

	private static void resolveDoor(WorldGenLevel level, BoundingBox chunkBox, BlockPos center) {
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
			BlockPos at = center.offset(dx, dy, 0);
			if (chunkBox.isInside(at) && level.getBlockState(at).is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, BlockChromaDoor.withConnections(level.getBlockState(at), level, at), 2);
		}
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id, BlockPos anchor) {
		level.getLevel().getStructureManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Crystal Music template " + id))
				.placeInWorld(level, anchor, anchor, new StructurePlaceSettings().setRotation(Rotation.NONE)
						.setIgnoreEntities(true).setBoundingBox(chunkBox),
						RandomSource.create(puzzleSeed ^ anchor.asLong()), 2);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.MUSIC
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base) return base;
		return null;
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
	}
}
