package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityMusicMemory;

/** Modern persistent planner for V33a's Crystal Music structure. */
public final class MusicStructureGenerator extends StructureGeneratorBase {

	public static final int FIRST_ROOM_Z = 3;
	public static final int ROOM_STRIDE = 20;

	private final ArrayList<BlockPos> memoryPositions = new ArrayList<>();
	private int difficulty;
	private long puzzleSeed;
	private int roomCount;

	@Override
	protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		roomCount = MusicPuzzleLayout.roomCount(difficulty);
		puzzleSeed = random.nextLong();
		MusicPuzzleLayout.create(difficulty, puzzleSeed);
		posY = 20 + random.nextInt(80);
		for (int room = 0; room < roomCount; room++)
			memoryPositions.add(new BlockPos(x + 5, posY + 1, z + FIRST_ROOM_Z + room * ROOM_STRIDE + 5));
		int lootZ = z + FIRST_ROOM_Z + roomCount * ROOM_STRIDE;
		placeCore(x + 5, posY + 2, lootZ + 5);
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public int roomCount() { return roomCount; }
	public List<BlockPos> memoryPositions() { return Collections.unmodifiableList(memoryPositions); }

	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	@Override
	protected boolean hasBeenSolved(Level level) {
		return !memoryPositions.isEmpty() && memoryPositions.stream().allMatch(pos ->
				level.getBlockEntity(pos) instanceof TileEntityMusicMemory memory && memory.isSolved());
	}

	@Override
	protected void openStructure(Level level) {
		for (BlockPos pos : memoryPositions)
			if (level.getBlockEntity(pos) instanceof TileEntityMusicMemory memory) memory.forceComplete();
	}

	@Override
	protected void clearCaches() {
		memoryPositions.clear();
		difficulty = 0;
		puzzleSeed = 0;
		roomCount = 0;
	}
}
