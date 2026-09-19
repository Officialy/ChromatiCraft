package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;

/** Modern planner for V33a's Three-Dimensional Maze. */
public final class ThreeDMazeStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private long mazeSeed;
	private int width;
	private int height;

	@Override
	protected void calculate(int x, int z, Random random) {
		posY = 75;
		difficulty = ChromaOptions.getStructureDifficulty();
		width = ThreeDMazeLayout.widthFor(difficulty);
		height = ThreeDMazeLayout.heightFor(difficulty);
		mazeSeed = random.nextLong();
		// Test the complete topology now, rather than discovering a bad seed during chunk placement.
		ThreeDMazeLayout.create(difficulty, mazeSeed);
		entryX = x + ThreeDMazeLayout.CELL_SIZE * width / 2 + ThreeDMazeLayout.CELL_SIZE / 2;
		entryZ = z + ThreeDMazeLayout.CELL_SIZE * width / 2 + ThreeDMazeLayout.CELL_SIZE / 2;
		int rewardCenterY = posY - ThreeDMazeLayout.CELL_SIZE * (height + 1)
				+ ThreeDMazeLayout.CELL_SIZE / 2;
		placeCore(entryX, rewardCenterY - 3, entryZ);
	}

	public int difficulty() { return difficulty; }
	public long mazeSeed() { return mazeSeed; }
	public int width() { return width; }
	public int height() { return height; }

	@Override
	protected int getCenterXOffset() {
		return ThreeDMazeLayout.CELL_SIZE * (width + 1) / 2;
	}

	@Override
	protected int getCenterZOffset() {
		return ThreeDMazeLayout.CELL_SIZE * (width + 1) / 2;
	}

	/** V33a has no stateful completion detector for a navigational maze. Reaching its core is solving it. */
	@Override
	protected boolean hasBeenSolved(Level level) {
		return true;
	}

	@Override
	protected void openStructure(Level level) {
		// V33a: no door or other state has to be changed after navigating the maze.
	}

	@Override
	protected void clearCaches() {
		difficulty = 0;
		mazeSeed = 0;
		width = 0;
		height = 0;
	}
}
