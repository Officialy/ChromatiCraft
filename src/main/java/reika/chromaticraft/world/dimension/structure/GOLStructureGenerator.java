package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityGOLController;

/** Persistent planner for V33a's Cellular Automata structure. */
public final class GOLStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private int floorY;
	private int radius;
	private BlockPos controllerPos;

	@Override
	protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		GOLPuzzleLayout layout = GOLPuzzleLayout.forDifficulty(difficulty);
		radius = layout.radius();
		posY = 30 + random.nextInt(40);
		floorY = posY + 1;
		controllerPos = new BlockPos(x - radius - 3, floorY + 1, z);
		placeCore(x + radius + 8, floorY + 2, z);
		// V33a GOLEntrance offsets the generator's region/biome entry marker by ten blocks.
		offsetEntry(-10, 0);
	}

	public int difficulty() { return difficulty; }
	public int floorY() { return floorY; }
	public int radius() { return radius; }
	public BlockPos controllerPos() { return controllerPos; }

	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	@Override
	protected boolean hasBeenSolved(Level level) {
		return controllerPos != null && level.getBlockEntity(controllerPos)
				instanceof TileEntityGOLController controller && controller.isSolved();
	}

	@Override
	protected void openStructure(Level level) {
		if (controllerPos != null && level.getBlockEntity(controllerPos)
				instanceof TileEntityGOLController controller) controller.forceComplete();
	}

	@Override
	protected void clearCaches() {
		difficulty = 0;
		floorY = 0;
		radius = 0;
		controllerPos = null;
	}
}
