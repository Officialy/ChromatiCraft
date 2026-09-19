package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.dimension.structure.gol.BlockGOLController;
import reika.chromaticraft.block.dimension.structure.gol.BlockGOLTile;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.world.dimension.structure.GOLPuzzleLayout;

/**
 * Single persistent authority for V33a's Cellular Automata chamber. The original attached a ticker
 * to every floor cell; this performs the same simultaneous B3/S23 generations from one controller,
 * avoiding thousands of independently ticking block entities while retaining cell-local bindings.
 */
public final class TileEntityGOLController extends BlockEntity {

	public static final int ROOM_HEIGHT = 12;
	private int minX;
	private int maxX;
	private int minZ;
	private int maxZ;
	private int floorY;
	private int maxSelected;
	private int requiredTrail;
	private @Nullable BlockPos doorCenter;
	private boolean configured;
	private boolean running;
	private boolean solved;
	private boolean[] nextGeneration = new boolean[0];
	private boolean generationPrepared;

	public TileEntityGOLController(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.GOL_CONTROLLER.get(), pos, state);
	}

	public void configure(int x1, int x2, int z1, int z2, int y, int selectionLimit,
			int trailThreshold, BlockPos door) {
		if (Math.abs(x2 - x1) != Math.abs(z2 - z1))
			throw new IllegalArgumentException("Cellular Automata board must be square");
		minX = Math.min(x1, x2);
		maxX = Math.max(x1, x2);
		minZ = Math.min(z1, z2);
		maxZ = Math.max(z1, z2);
		floorY = y;
		maxSelected = Math.max(1, selectionLimit);
		requiredTrail = Math.max(1, trailThreshold);
		doorCenter = door.immutable();
		configured = true;
		nextGeneration = new boolean[cellCount()];
		setChanged();
	}

	public void toggleCell(BlockPos pos) {
		if (!configured || running || level == null || pos.getY() != floorY || !contains(pos)) {
			if (level != null) ChromaSounds.ERROR.playSoundAtBlock(level, pos);
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (!isFloorCell(state)) return;
		boolean active = state.getValue(BlockGOLTile.ACTIVE);
		if (!active && selectedCount() >= maxSelected) {
			ChromaSounds.ERROR.playSoundAtBlock(level, pos);
			return;
		}
		setFloorCell(pos, !active, true);
		level.playSound(null, pos, active ? SoundEvents.STONE_BUTTON_CLICK_OFF
				: SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.75F,
				active ? 0.65F : 0.75F);
	}

	public void toggleSimulation() {
		if (!configured || level == null) return;
		if (running) stopAndEvaluate();
		else start();
	}

	private void start() {
		running = true;
		generationPrepared = false;
		updateControllerState();
		setChanged();
	}

	private void stopAndEvaluate() {
		running = false;
		int trail = trailCount();
		if (trail >= requiredTrail) {
			solved = true;
			openDoor();
			ChromaSounds.CAST.playSoundAtBlock(level, worldPosition, 2, 1);
		}
		else ChromaSounds.ERROR.playSoundAtBlock(level, worldPosition, 2, 1);
		resetBoard();
		updateControllerState();
		setChanged();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state,
			TileEntityGOLController controller) {
		if (!controller.running || !controller.configured) return;
		int phase = Math.floorMod(level.getGameTime(), 5);
		if (phase == 0) controller.calculateNextGeneration();
		else if (phase == 1) controller.applyNextGeneration();
	}

	private void calculateNextGeneration() {
		ensureBuffer();
		boolean[] current = new boolean[cellCount()];
		for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++)
			current[index(x, z)] = isActive(level.getBlockState(new BlockPos(x, floorY, z)));
		nextGeneration = new GOLPuzzleLayout((width() - 1) / 2, width(), maxSelected, requiredTrail)
				.step(current);
		generationPrepared = true;
	}

	private void applyNextGeneration() {
		if (!generationPrepared) return;
		ensureBuffer();
		for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++)
			setFloorCell(new BlockPos(x, floorY, z), nextGeneration[index(x, z)], false);
		generationPrepared = false;
		setChanged();
	}

	private void setFloorCell(BlockPos pos, boolean active, boolean clearMemoryWhenOff) {
		BlockState state = level.getBlockState(pos);
		if (!isFloorCell(state)) return;
		if (state.getValue(BlockGOLTile.ACTIVE) != active)
			level.setBlock(pos, state.setValue(BlockGOLTile.ACTIVE, active), Block.UPDATE_ALL);
		BlockPos memoryPos = pos.above(ROOM_HEIGHT);
		BlockState memory = level.getBlockState(memoryPos);
		if (memory.is(ChromaBlocks.GOL_TILE.get()) && memory.getValue(BlockGOLTile.MEMORY)) {
			// During simulation, dead cells leave their ceiling pixel lit: this accumulated trail is the
			// actual V33a completion metric. Manual deselection and board reset clear it.
			boolean memoryActive = active || (!clearMemoryWhenOff && memory.getValue(BlockGOLTile.ACTIVE));
			if (memory.getValue(BlockGOLTile.ACTIVE) != memoryActive)
				level.setBlock(memoryPos, memory.setValue(BlockGOLTile.ACTIVE, memoryActive), Block.UPDATE_ALL);
		}
	}

	private void resetBoard() {
		for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++)
			setFloorCell(new BlockPos(x, floorY, z), false, true);
	}

	private int selectedCount() {
		int count = 0;
		for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++)
			if (isActive(level.getBlockState(new BlockPos(x, floorY, z)))) count++;
		return count;
	}

	private int trailCount() {
		int count = 0;
		for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
			BlockState state = level.getBlockState(new BlockPos(x, floorY + ROOM_HEIGHT, z));
			if (state.is(ChromaBlocks.GOL_TILE.get()) && state.getValue(BlockGOLTile.MEMORY)
					&& state.getValue(BlockGOLTile.ACTIVE)) count++;
		}
		return count;
	}

	private void openDoor() {
		if (level == null || doorCenter == null) return;
		for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
			BlockPos pos = doorCenter.offset(0, dy, dz);
			BlockState state = level.getBlockState(pos);
			if (state.is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(pos, state.setValue(BlockChromaDoor.OPEN, true), Block.UPDATE_ALL);
		}
	}

	public void forceComplete() {
		running = false;
		solved = true;
		openDoor();
		resetBoard();
		updateControllerState();
		setChanged();
	}

	public boolean isRunning() { return running; }
	public boolean isSolved() { return solved; }
	public int requiredTrail() { return requiredTrail; }
	public int maxSelected() { return maxSelected; }

	private boolean contains(BlockPos pos) {
		return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
	}

	private static boolean isFloorCell(BlockState state) {
		return state.is(ChromaBlocks.GOL_TILE.get()) && !state.getValue(BlockGOLTile.MEMORY);
	}

	private static boolean isActive(BlockState state) {
		return isFloorCell(state) && state.getValue(BlockGOLTile.ACTIVE);
	}

	private int width() { return maxX - minX + 1; }
	private int depth() { return maxZ - minZ + 1; }
	private int cellCount() { return configured ? width() * depth() : 0; }
	private int index(int x, int z) { return (z - minZ) * width() + x - minX; }

	private void ensureBuffer() {
		if (nextGeneration.length != cellCount()) nextGeneration = new boolean[cellCount()];
	}

	private void updateControllerState() {
		if (level == null) return;
		BlockState state = getBlockState();
		if (state.getValue(BlockGOLController.ACTIVE) != running)
			level.setBlock(worldPosition, state.setValue(BlockGOLController.ACTIVE, running), Block.UPDATE_ALL);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("minX", minX);
		output.putInt("maxX", maxX);
		output.putInt("minZ", minZ);
		output.putInt("maxZ", maxZ);
		output.putInt("floorY", floorY);
		output.putInt("maxSelected", maxSelected);
		output.putInt("requiredTrail", requiredTrail);
		if (doorCenter != null) output.putLong("door", doorCenter.asLong());
		output.putBoolean("configured", configured);
		output.putBoolean("running", running);
		output.putBoolean("solved", solved);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		minX = input.getIntOr("minX", 0);
		maxX = input.getIntOr("maxX", -1);
		minZ = input.getIntOr("minZ", 0);
		maxZ = input.getIntOr("maxZ", -1);
		floorY = input.getIntOr("floorY", 0);
		maxSelected = Math.max(1, input.getIntOr("maxSelected", 1));
		requiredTrail = Math.max(1, input.getIntOr("requiredTrail", 1));
		doorCenter = input.getLong("door").map(BlockPos::of).orElse(null);
		configured = input.getBooleanOr("configured", false) && maxX >= minX && maxZ >= minZ;
		running = input.getBooleanOr("running", false) && configured;
		solved = input.getBooleanOr("solved", false);
		nextGeneration = new boolean[cellCount()];
		generationPrepared = false;
	}
}
