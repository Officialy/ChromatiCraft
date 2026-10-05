package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityRotatingLock;

/** Persistent planner for V33a's Channeled Flow (WATER) structure. */
public final class WaterStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private long puzzleSeed;
	private WaterPuzzleLayout layout;

	@Override
	protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		puzzleSeed = random.nextLong();
		layout = WaterPuzzleLayout.create(difficulty, puzzleSeed);
		posX = x;
		posZ = z;
		// V33a moves posY to the top floor once the floors are laid.
		posY = layout.topY();
		int[] corner = layout.lootCorner(layout.coreCorner());
		placeCore(x + corner[0], layout.floorY(layout.floors().size() - 1), z + corner[1]);
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public WaterPuzzleLayout layout() { return layout; }

	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	/** V33a {@code hasBeenSolved}: every floor's checkpoints hold fluid. */
	@Override
	protected boolean hasBeenSolved(Level level) {
		if (layout == null)
			return false;
		for (WaterPuzzleLayout.Floor floor : layout.floors())
			if (!TileEntityRotatingLock.isFloorSolved(level, floorOrigin(floor), floor.radius()))
				return false;
		return true;
	}

	/** V33a {@code openStructure}: every floor's trapdoors open. */
	@Override
	protected void openStructure(Level level) {
		if (layout == null)
			return;
		for (WaterPuzzleLayout.Floor floor : layout.floors())
			TileEntityRotatingLock.setFloorDoors(level, floorOrigin(floor), floor.width(), true);
	}

	public BlockPos floorOrigin(WaterPuzzleLayout.Floor floor) {
		return new BlockPos(posX, layout.floorY(floor.index()), posZ);
	}

	@Override
	protected void clearCaches() {
		difficulty = 0;
		puzzleSeed = 0;
		layout = null;
	}
}
