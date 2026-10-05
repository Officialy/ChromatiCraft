package reika.chromaticraft.world.dimension.structure;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;
import reika.chromaticraft.tileentity.TileEntityPistonTarget;

/** Persistent planner for V33a's Filter Cycle (PISTONTAPE) structure. */
public final class PistonTapeStructureGenerator extends StructureGeneratorBase {

	private int difficulty;
	private long puzzleSeed;
	private PistonTapePlan plan;

	@Override
	protected void calculate(int x, int z, Random random) {
		difficulty = ChromaOptions.getStructureDifficulty();
		puzzleSeed = random.nextLong();
		plan = PistonTapePlan.create(difficulty, puzzleSeed, x, z);
		posX = x;
		posZ = z;
		posY = plan.posY();
		BlockPos core = plan.core();
		placeCore(core.getX(), core.getY(), core.getZ());
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public PistonTapePlan plan() { return plan; }

	@Override protected int getCenterXOffset() { return 0; }
	@Override protected int getCenterZOffset() { return 0; }

	/** V33a {@code hasBeenSolved}: every door of every stage has received its colour at least once. */
	@Override
	protected boolean hasBeenSolved(Level level) {
		if (plan == null)
			return false;
		for (PistonTapePlan.Stage stage : plan.stages()) {
			for (PistonTapePlan.DoorKey door : stage.doors()) {
				boolean validated = false;
				for (int i = 0; i < door.colorCount() && !validated; i++)
					validated = level.getBlockEntity(door.target(i)) instanceof TileEntityPistonTarget te && te.isValidated();
				if (!validated)
					return false;
			}
		}
		return true;
	}

	/** V33a {@code openStructure}: force every door open and mark every step validated. */
	@Override
	protected void openStructure(Level level) {
		if (plan == null)
			return;
		for (PistonTapePlan.Stage stage : plan.stages()) {
			for (PistonTapePlan.DoorKey door : stage.doors()) {
				for (BlockPos panel : door.panels())
					if (level.getBlockEntity(panel) instanceof TileEntityChromaDoor d)
						d.open(-1);
				for (int i = 0; i < door.colorCount(); i++)
					if (level.getBlockEntity(door.target(i)) instanceof TileEntityPistonTarget te)
						te.setValidated(true);
			}
		}
	}

	@Override
	protected void clearCaches() {
		difficulty = 0;
		puzzleSeed = 0;
		plan = null;
	}
}
