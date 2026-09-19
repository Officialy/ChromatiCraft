package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;

/**
 * Modern planner for V33a {@code LightPanelGenerator} (Glowing Logic).
 *
 * <p>The planner records component anchors, not individual blocks. Rooms, entrance sections and the
 * reward chamber are canonical NBT templates; {@link LightPanelPiece} composes them chunk by chunk.
 * This keeps V33a's layout arithmetic available to the Dimension Core and region mapper while giving
 * the engine ownership of placement, locating and persistence.
 */
public final class LightPanelStructureGenerator extends StructureGeneratorBase {

	public static final int ROOM_DEPTH = 18;
	public static final int ROOM_STRIDE = ROOM_DEPTH + 1;
	public static final int FIRST_ROOM_OFFSET = 11;
	public static final int LOOT_RADIUS = 10;

	private final ArrayList<BlockPos> roomOrigins = new ArrayList<>();
	private int difficulty;
	private long puzzleSeed;
	private BlockPos lootCenter;
	private BlockPos controllerPosition;
	private BlockPos passwordPosition;

	@Override
	protected void calculate(int x, int z, Random random) {
		posY = 20 + random.nextInt(80);
		difficulty = ChromaOptions.getStructureDifficulty();
		puzzleSeed = random.nextLong();
		int roomCount = roomCount(difficulty);
		int roomX = x + FIRST_ROOM_OFFSET;
		for (int room = 0; room < roomCount; room++) {
			roomOrigins.add(new BlockPos(roomX, posY, z));
			roomX += ROOM_STRIDE;
		}
		// V33a advances by LightPanelLoot.WIDTH before passing this coordinate as the room centre.
		lootCenter = new BlockPos(roomX + LOOT_RADIUS, posY, z);
		controllerPosition = lootCenter.offset(0, 21, 0);
		passwordPosition = roomOrigins.get(0).offset(1, 1, 0);
		placeCore(lootCenter.getX(), posY + 6, z);
	}

	public static int roomCount(int difficulty) {
		return switch (difficulty) {
			case 1 -> 5;
			case 2 -> 8;
			case 3 -> 12;
			default -> throw new IllegalArgumentException("Glowing Logic difficulty " + difficulty);
		};
	}

	public int difficulty() { return difficulty; }
	public long puzzleSeed() { return puzzleSeed; }
	public List<BlockPos> roomOrigins() { return Collections.unmodifiableList(roomOrigins); }
	public BlockPos lootCenter() { return lootCenter; }
	public BlockPos controllerPosition() { return controllerPosition; }

	@Override
	protected int getCenterXOffset() {
		return 0;
	}

	@Override
	protected int getCenterZOffset() {
		return 0;
	}

	@Override
	protected boolean hasBeenSolved(Level level) {
		return controllerPosition != null
				&& level.getBlockEntity(controllerPosition) instanceof TileEntityStructureController controller
				&& !controller.getGlowingLogicRooms().isEmpty()
				&& controller.getGlowingLogicRooms().stream().allMatch(room -> room.puzzle().isComplete());
	}

	@Override
	protected void openStructure(Level level) {
		if (controllerPosition == null
				|| !(level.getBlockEntity(controllerPosition) instanceof TileEntityStructureController controller))
			return;
		for (var room : controller.getGlowingLogicRooms()) {
			BlockPos root = room.doorPosition();
			for (int dy = 0; dy < 3; dy++) for (int dz = -2; dz <= 2; dz++) {
				BlockPos at = root.offset(0, dy, dz);
				BlockState state = level.getBlockState(at);
				if (state.hasProperty(BlockChromaDoor.OPEN))
					level.setBlock(at, state.setValue(BlockChromaDoor.OPEN, true), 3);
			}
		}
	}

	@Override
	protected void clearCaches() {
		roomOrigins.clear();
		lootCenter = null;
		controllerPosition = null;
		passwordPosition = null;
		difficulty = 0;
		puzzleSeed = 0;
	}
}
