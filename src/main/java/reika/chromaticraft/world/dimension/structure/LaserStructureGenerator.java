package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityLaserController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Persistent planner for V33a's complete Chromatic Beams corridor. */
public final class LaserStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private long puzzleSeed;
	private LaserPuzzleLayout puzzle;
	private final List<BlockPos> controllers = new ArrayList<>();

	@Override protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		posY = 10 + random.nextInt(70);
		puzzleSeed = random.nextLong();
		puzzle = LaserPuzzleLayout.create(difficulty, puzzleSeed);
		for (LaserPuzzleLayout.PlacedRoom placed : puzzle.rooms()) {
			LaserPuzzleLayout.RoomBlueprint room = placed.blueprint();
			int originZ = z - (room.sizeZ() + 1) / 2;
			controllers.add(new BlockPos(x + placed.x() + (room.sizeX() + 1) / 2,
					posY + 4, originZ - 3));
		}
		placeCore(x + puzzle.lootStartX() + 10, posY + 4, z);
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public LaserPuzzleLayout puzzle() { return puzzle; }
	public List<BlockPos> controllers() { return List.copyOf(controllers); }
	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	@Override protected boolean hasBeenSolved(Level level) {
		if (controllers.isEmpty()) return false;
		for (BlockPos pos : controllers)
			if (!(level.getBlockEntity(pos) instanceof TileEntityLaserController controller)
					|| !controller.isSolved()) return false;
		return true;
	}

	@Override protected void openStructure(Level level) {
		for (BlockPos pos : controllers)
			if (level.getBlockEntity(pos) instanceof TileEntityLaserController controller)
				controller.forceComplete();
	}

	@Override protected void clearCaches() {
		controllers.clear();
		puzzle = null;
		puzzleSeed = 0;
		difficulty = 0;
	}
}
