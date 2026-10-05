package reika.chromaticraft.tileentity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonController26;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.world.dimension.structure.PistonTapePlan;
import reika.dragonapi.libraries.level.ReikaBlockHelper;

/**
 * V33a {@code BlockPistonController}'s four tiles, told apart by {@link BlockPistonController26#TYPE}:
 * <ul>
 * <li>0, playback ({@code TilePistonPlayback}): plays the whole tape, firing the read beams at every door step
 * and stepping the tape between them every 70 ticks, then winding it round to the start;</li>
 * <li>1, trigger ({@code TilePistonTrigger}): fires the read beams once for the current step;</li>
 * <li>2, cycler ({@code TilePistonCycler}): advances the tape by one half-step;</li>
 * <li>3, display ({@code TilePistonDisplay}): shows every door's expected colours and number.</li>
 * </ul>
 *
 * <p>V33a held the stage (piston positions, emitters, door keys, the tape's half-cycle state) in the generator.
 * Each controller here stores the plan's seed and origin and rebuilds the same {@link PistonTapePlan}; the
 * tape's runtime state lives in the playback tile and is saved, so a stage survives a restart mid-cycle.
 */
public final class TileEntityPistonController extends BlockEntity {

	private static final int DELAY = 70;
	private static final Map<String, PistonTapePlan> PLANS = new ConcurrentHashMap<>();

	private int stageIndex;
	private int difficulty = 3;
	private long seed;
	private int originX;
	private int originZ;
	private int currentDoor;
	private boolean configured;

	// TYPE 0
	private boolean isPlaying;
	private int tickUntilNextDoor = DELAY;
	private long[] lastFire = new long[0];
	private boolean[] firedVerticalLast = new boolean[0];
	private boolean[] isHalfCycled = new boolean[0];

	// TYPE 3
	private boolean active;

	public TileEntityPistonController(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PISTON_CONTROLLER.get(), pos, state);
	}

	public int type() {
		return getBlockState().getValue(BlockPistonController26.TYPE);
	}

	public Direction facing() {
		return getBlockState().getValue(BlockPistonController26.FACING);
	}

	/** V33a {@code setData(stage, dir)}, plus what the port needs to rebuild the stage. */
	public void configure(int stage, int difficulty, long seed, int originX, int originZ) {
		stageIndex = stage;
		this.difficulty = difficulty;
		this.seed = seed;
		this.originX = originX;
		this.originZ = originZ;
		configured = true;
		setChanged();
	}

	public static PistonTapePlan plan(int difficulty, long seed, int x, int z) {
		return PLANS.computeIfAbsent(difficulty + ":" + seed + ":" + x + ":" + z,
				k -> PistonTapePlan.create(difficulty, seed, x, z));
	}

	public @Nullable PistonTapePlan.Stage stage() {
		if (!configured)
			return null;
		PistonTapePlan plan = plan(difficulty, seed, originX, originZ);
		return stageIndex < plan.stages().size() ? plan.stages().get(stageIndex) : null;
	}

	public int currentDoor() { return currentDoor; }
	public boolean isPlaying() { return isPlaying; }
	public boolean isDisplayActive() { return active; }
	/** V33a {@code getActiveDoor}. */
	public int activeDoor() { return currentDoor - 1; }

	/** V33a {@code onRightClick}, per tile type. */
	public void onRightClick() {
		if (level == null || level.isClientSide())
			return;
		switch (type()) {
			case 0 -> {
				setDoor(0);
				setPlaying(true);
				fire();
				tickUntilNextDoor = DELAY;
			}
			case 1 -> fire();
			case 2 -> {
				setDoor(currentDoor + 1);
				if (level.getBlockEntity(worldPosition.above()) instanceof TileEntityPistonController te)
					te.setDoor(currentDoor);
				cycle();
			}
			default -> {
				// V33a TilePistonDisplay.read(): the list is always the stage's door keys.
				sync();
			}
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPistonController te) {
		if (te.type() != 0 || !te.isPlaying)
			return;
		PistonTapePlan.Stage stage = te.stage();
		if (stage == null)
			return;
		if (te.tickUntilNextDoor > 0) {
			te.tickUntilNextDoor--;
		}
		else if (te.cycle()) {
			te.setDoor(te.currentDoor + 1);
			if (te.currentDoor == stage.doorCount)
				te.tickUntilNextDoor = DELAY;
			if (te.currentDoor >= stage.doorCount) {
				if (te.currentDoor >= stage.totalLength())
					te.setPlaying(false);
			}
			else {
				te.fire();
				te.tickUntilNextDoor = DELAY;
			}
		}
	}

	private void setPlaying(boolean play) {
		isPlaying = play;
		TileEntityPistonController display = display();
		if (display != null) {
			display.active = play;
			display.sync();
		}
		setChanged();
	}

	/** The playback tile's display: two blocks behind along the hall, one block down. */
	private @Nullable TileEntityPistonController display() {
		if (level == null)
			return null;
		Direction dir = facing().getCounterClockWise();
		return level.getBlockEntity(worldPosition.relative(dir, 2).below()) instanceof TileEntityPistonController te
				&& te.type() == 3 ? te : null;
	}

	private void setDoor(int door) {
		currentDoor = door;
		if (type() == 0) {
			TileEntityPistonController display = display();
			if (display != null) {
				display.currentDoor = door;
				display.sync();
			}
		}
		sync();
	}

	private @Nullable TileEntityPistonController playback() {
		PistonTapePlan.Stage stage = stage();
		if (level == null || stage == null)
			return null;
		return level.getBlockEntity(stage.playback()) instanceof TileEntityPistonController te && te.type() == 0 ? te : null;
	}

	/** V33a {@code TapeStage.fireEmitters(world, currentDoor)}. */
	private void fire() {
		PistonTapePlan.Stage stage = stage();
		if (level == null || stage == null || currentDoor >= stage.doorCount)
			return;
		PistonTapePlan.DoorKey door = stage.doors().get(currentDoor);
		for (int i = 0; i < stage.bitsPerDoor; i++) {
			PistonTapePlan.Slice slice = stage.slice(i);
			if (level.getBlockEntity(slice.emitter()) instanceof TileEntityLaserEffector emitter)
				emitter.fire();
			if (level.getBlockEntity(slice.target()) instanceof TileEntityPistonTarget head)
				head.setTarget(currentDoor, door.target(i), stage.mainDirection, stage.hallSplinePoints(i));
		}
	}

	/** V33a {@code TapeStage.cycle}, run on the playback tile that owns the tape's state. */
	private boolean cycle() {
		TileEntityPistonController owner = type() == 0 ? this : playback();
		return owner != null && owner.cycleTape();
	}

	/** V33a {@code PistonTapeLoop.cycle}: every slice steps, and the tape is ready when all completed a step. */
	private boolean cycleTape() {
		PistonTapePlan.Stage stage = stage();
		if (level == null || stage == null)
			return false;
		ensureSliceState(stage.sliceCount());
		boolean ready = true;
		for (int i = 0; i < stage.sliceCount(); i++)
			ready &= cycleSlice(stage, stage.slice(i), i);
		setChanged();
		return ready;
	}

	/**
	 * V33a {@code PistonTapeSlice.cycle}: at most once every 6 ticks, alternately fire the vertical pair (the front
	 * column up, the back column down) and the horizontal pair (the top row back, the bottom row forward). Two
	 * half-steps move the tape one cell; only the second reports it ready.
	 */
	private boolean cycleSlice(PistonTapePlan.Stage stage, PistonTapePlan.Slice slice, int i) {
		long now = level.getGameTime();
		if (now - lastFire[i] < 6)
			return false;
		lastFire[i] = now;
		if (firedVerticalLast[i]) {
			ReikaBlockHelper.extendPiston(level, slice.piston(stage.facing));
			ReikaBlockHelper.extendPiston(level, slice.piston(stage.facing.getOpposite()));
		}
		else {
			ReikaBlockHelper.extendPiston(level, slice.piston(Direction.UP));
			ReikaBlockHelper.extendPiston(level, slice.piston(Direction.DOWN));
		}
		firedVerticalLast[i] = !firedVerticalLast[i];
		isHalfCycled[i] = !isHalfCycled[i];
		return !isHalfCycled[i];
	}

	private void ensureSliceState(int n) {
		if (lastFire.length != n) {
			lastFire = new long[n];
			java.util.Arrays.fill(lastFire, -1000);
			firedVerticalLast = new boolean[n];
			isHalfCycled = new boolean[n];
		}
	}

	/** The door keys shown by a display tile. */
	public List<PistonTapePlan.DoorKey> displayList() {
		PistonTapePlan.Stage stage = stage();
		return stage == null ? List.of() : stage.doors();
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("stage", stageIndex);
		output.putInt("door", currentDoor);
		output.putInt("difficulty", difficulty);
		output.putLong("seed", seed);
		output.putInt("originX", originX);
		output.putInt("originZ", originZ);
		output.putBoolean("configured", configured);
		output.putBoolean("playing", isPlaying);
		output.putInt("delay", tickUntilNextDoor);
		output.putBoolean("active", active);
		output.putInt("slices", lastFire.length);
		for (int i = 0; i < lastFire.length; i++) {
			output.putLong("lastFire" + i, lastFire[i]);
			output.putBoolean("vertical" + i, firedVerticalLast[i]);
			output.putBoolean("half" + i, isHalfCycled[i]);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		stageIndex = input.getIntOr("stage", 0);
		currentDoor = input.getIntOr("door", 0);
		difficulty = input.getIntOr("difficulty", 3);
		seed = input.getLongOr("seed", 0);
		originX = input.getIntOr("originX", 0);
		originZ = input.getIntOr("originZ", 0);
		configured = input.getBooleanOr("configured", false);
		isPlaying = input.getBooleanOr("playing", false);
		tickUntilNextDoor = input.getIntOr("delay", DELAY);
		active = input.getBooleanOr("active", false);
		int n = input.getIntOr("slices", 0);
		lastFire = new long[n];
		firedVerticalLast = new boolean[n];
		isHalfCycled = new boolean[n];
		for (int i = 0; i < n; i++) {
			lastFire[i] = input.getLongOr("lastFire" + i, -1000);
			firedVerticalLast[i] = input.getBooleanOr("vertical" + i, false);
			isHalfCycled[i] = input.getBooleanOr("half" + i, false);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		return saveCustomOnly(provider);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
