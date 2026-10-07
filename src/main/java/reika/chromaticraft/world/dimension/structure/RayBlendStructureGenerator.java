package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityRayblendFloor;

/**
 * Persistent planner for V33a's Crystal Interference ({@code RayBlendGenerator}). The plan is {@link RayBlendLayout},
 * rebuilt from the stored seed; the puzzle state is the world itself (see {@link TileEntityRayblendFloor}).
 *
 * <p>V33a's {@code updateTick} only ticked the mix points, which its {@code getValidMix} never creates, and its
 * {@code onBlockUpdate} hook is commented out, so neither has anything to port.
 */
public final class RayBlendStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private long puzzleSeed;
	private RayBlendLayout layout;

	@Override
	protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		puzzleSeed = random.nextLong();
		layout = RayBlendLayout.create(difficulty, puzzleSeed, x, z);
		posX = x;
		posZ = z;
		posY = layout.posY();
		BlockPos core = layout.core();
		placeCore(core.getX(), core.getY(), core.getZ());
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public RayBlendLayout layout() { return layout; }

	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	/** V33a {@code hasBeenSolved}: every room's {@code isComplete}. */
	@Override
	protected boolean hasBeenSolved(Level level) {
		if (layout == null)
			return false;
		for (RayBlendLayout.Room room : layout.rooms())
			if (!TileEntityRayblendFloor.isComplete(level, room))
				return false;
		return true;
	}

	/** V33a {@code openStructure}: {@code forceOpen} every room. */
	@Override
	protected void openStructure(Level level) {
		if (layout == null)
			return;
		for (RayBlendLayout.Room room : layout.rooms())
			TileEntityRayblendFloor.forceOpen(level, room);
	}

	@Override
	protected void clearCaches() {
		difficulty = 0;
		puzzleSeed = 0;
		layout = null;
	}
}
