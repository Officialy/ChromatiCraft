package reika.chromaticraft.tileentity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.block.dimension.structure.water.BlockRotatingLock26;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.world.dimension.structure.WaterPuzzleLayout;

/**
 * V33a {@code BlockRotatingLock.TileEntityRotatingLock}: one lock of a Channeled Flow floor.
 *
 * <p>A right-click turns the lock a quarter: the fluid inside its 5x5 channel square is drained, the open
 * arms are plugged with invisible pads while the model turns three degrees per tick, and after 30 ticks the
 * channel pattern that was captured at the start is placed again turned by 90 degrees. Fluid arriving above
 * the lock (or leaving) re-evaluates the floor; when every checkpoint lock of the floor holds fluid, its four
 * corner trapdoors open.
 *
 * <p>V33a kept the floor's checkpoint flags and lock rotations in the generator's memory, which the original
 * rebuilt only partially after a restart. The port keeps everything a floor needs in its lock tiles (each
 * knows its floor's origin, width and grid radius), so the solved state survives reloads exactly.
 */
public final class TileEntityRotatingLock extends BlockEntity {

	private record Captured(BlockPos offset, BlockState state) {
		static final Codec<Captured> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("offset").forGetter(Captured::offset),
				BlockState.CODEC.fieldOf("state").forGetter(Captured::state)
		).apply(i, Captured::new));
	}

	private Direction direction = Direction.EAST;
	private int floor;
	private int lockX;
	private int lockZ;
	private boolean checkpoint;
	private boolean endpoint;
	private boolean hasFluid;
	private final Set<Direction> openEndsAtZero = EnumSet.noneOf(Direction.class);
	private BlockPos floorOrigin = BlockPos.ZERO;
	private int floorWidth;
	private int floorRadius;
	private int rotatingAmount;
	private final Map<BlockPos, BlockState> captured = new LinkedHashMap<>();

	public TileEntityRotatingLock(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ROTATING_LOCK.get(), pos, state);
	}

	/** V33a {@code LockCallback.onTilePlaced}, plus the floor geometry the port keeps per lock. */
	public void configure(WaterPuzzleLayout.Lock lock, int floorIndex, boolean isCheckpoint, boolean isEndpoint,
			BlockPos floorOrigin, int floorWidth, int floorRadius) {
		direction = lock.facing();
		floor = floorIndex;
		lockX = lock.location().x();
		lockZ = lock.location().z();
		checkpoint = isCheckpoint;
		endpoint = isEndpoint;
		openEndsAtZero.clear();
		openEndsAtZero.addAll(lock.openEndsAtZero());
		this.floorOrigin = floorOrigin.immutable();
		this.floorWidth = floorWidth;
		this.floorRadius = floorRadius;
		setChanged();
	}

	public Direction direction() { return direction; }
	public boolean isCheckpoint() { return checkpoint; }
	public boolean isEndpoint() { return endpoint; }
	public boolean hasFluid() { return hasFluid; }
	public int rotationProgress() { return rotatingAmount; }
	public int floorIndex() { return floor; }

	/** V33a {@code Lock.isDirectionOpen} for the lock's current heading. */
	public boolean isOpen(Direction dir) {
		return WaterPuzzleLayout.Lock.isOpen(openEndsAtZero, direction, dir);
	}

	/** V33a {@code getOpenEndsForRender}: the arms that are open in the world right now. */
	public List<Direction> openEnds() {
		List<Direction> list = new ArrayList<>(4);
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (isOpen(dir))
				list.add(dir);
		return list;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRotatingLock lock) {
		if (lock.rotatingAmount > 0) {
			lock.rotatingAmount = Math.min(lock.rotatingAmount + 3, 90);
			if (lock.rotatingAmount == 90) {
				lock.rotatingAmount = 0;
				lock.finishRotating(false);
			}
			else
				lock.sync();
		}
		else if (lock.rotatingAmount < 0) {
			lock.rotatingAmount = Math.max(lock.rotatingAmount - 3, -90);
			if (lock.rotatingAmount == -90) {
				lock.rotatingAmount = 0;
				lock.finishRotating(true);
			}
			else
				lock.sync();
		}
	}

	/** V33a {@code startRotating}: drain the square, capture its channel pattern and plug it with pads. */
	public void startRotating(boolean reverse) {
		if (rotatingAmount != 0 || level == null || level.isClientSide())
			return;
		rotatingAmount = reverse ? -1 : 1;
		sync();
		ChromaSounds.WATERLOCK.playSoundAtBlock(level, worldPosition, 2, 1);
		BlockPos channel = worldPosition.above();
		captured.clear();
		Block fluid = ChromaBlocks.EVER_FLUID.get();
		for (int i = -2; i <= 2; i++) {
			for (int k = -2; k <= 2; k++) {
				BlockPos at = channel.offset(i, 0, k);
				if (level.getBlockState(at).is(fluid))
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				if (i != 0 || k != 0)
					captured.putIfAbsent(new BlockPos(i, 0, k), level.getBlockState(at));
			}
		}
		BlockState pad = ChromaBlocks.ROTATING_LOCK.get().defaultBlockState().setValue(BlockRotatingLock26.PAD, true);
		for (int i = -2; i <= 2; i++) {
			for (int k = -2; k <= 2; k++) {
				if (i == 0 && k == 0)
					continue;
				BlockPos at = channel.offset(i, 0, k);
				BlockState there = level.getBlockState(at);
				if (there.isAir() || there.is(fluid))
					level.setBlock(at, pad, Block.UPDATE_ALL);
			}
		}
		level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		setChanged();
	}

	/**
	 * V33a {@code finishRotating}: turn the heading, then place the captured pattern rotated about the lock
	 * (DragonAPI's {@code rotate90Degrees}: clockwise is (x, z) to (-z, x)).
	 */
	private void finishRotating(boolean reverse) {
		if (level == null || level.isClientSide())
			return;
		direction = reverse ? direction.getCounterClockWise() : direction.getClockWise();
		sync();
		level.playSound(null, worldPosition, Blocks.STONE.defaultBlockState().getSoundType().getBreakSound(),
				SoundSource.BLOCKS, 2, 0);
		if (captured.isEmpty())
			return;
		BlockPos channel = worldPosition.above();
		for (Map.Entry<BlockPos, BlockState> entry : captured.entrySet()) {
			BlockPos o = entry.getKey();
			BlockPos turned = reverse ? new BlockPos(o.getZ(), 0, -o.getX()) : new BlockPos(-o.getZ(), 0, o.getX());
			level.setBlock(channel.offset(turned), entry.getValue(), Block.UPDATE_ALL);
		}
		captured.clear();
		level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		setChanged();
	}

	/** V33a {@code updateHasFluidState}, run on every neighbour change. */
	public void updateHasFluidState() {
		if (level == null || level.isClientSide())
			return;
		boolean fluid = level.getBlockState(worldPosition.above()).is(ChromaBlocks.EVER_FLUID.get());
		ChromaSounds.CAST.playSound(level, worldPosition, 0.4F, 2F, false);
		boolean before = isFloorSolved(level, floorOrigin, floorRadius);
		hasFluid = fluid;
		setChanged();
		boolean after = isFloorSolved(level, floorOrigin, floorRadius);
		if (before != after) {
			if (after)
				ChromaSounds.CASTHARMONIC.playSound(level, floorOrigin, 0.4F, 2F, false);
			setFloorDoors(level, floorOrigin, floorWidth, after);
		}
		sync();
	}

	/** V33a {@code WaterFloor.hasBeenSolved}: every checkpoint lock of the floor holds fluid. */
	public static boolean isFloorSolved(Level level, BlockPos floorOrigin, int radius) {
		for (int i = -radius; i <= radius; i++) {
			for (int k = -radius; k <= radius; k++) {
				BlockPos at = lockPosition(floorOrigin, i, k);
				if (level.getBlockEntity(at) instanceof TileEntityRotatingLock lock && lock.checkpoint && !lock.hasFluid)
					return false;
			}
		}
		return true;
	}

	/** Lock tile position {@code (i, k)} of a floor whose base is {@code floorOrigin}. */
	public static BlockPos lockPosition(BlockPos floorOrigin, int i, int k) {
		return floorOrigin.offset(i * WaterPuzzleLayout.LOCK_SPACING, 1, k * WaterPuzzleLayout.LOCK_SPACING);
	}

	/** V33a {@code WaterFloor.setDoorStates}: the four 3x3 corner trapdoors at floor height 2. */
	public static void setFloorDoors(Level level, BlockPos floorOrigin, int width, boolean open) {
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				for (int i = width - 3; i <= width - 1; i++) {
					for (int k = width - 3; k <= width - 1; k++) {
						BlockPos at = floorOrigin.offset(sx * i, 2, sz * k);
						if (level.getBlockEntity(at) instanceof TileEntityChromaDoor door) {
							if (open)
								door.open(-1);
							else
								door.close();
						}
					}
				}
			}
		}
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("dir", direction.get2DDataValue());
		output.putInt("lvl", floor);
		output.putInt("lockX", lockX);
		output.putInt("lockY", lockZ);
		output.putBoolean("check", checkpoint);
		output.putBoolean("end", endpoint);
		output.putBoolean("fluid", hasFluid);
		int ends = 0;
		for (Direction d : openEndsAtZero)
			ends |= 1 << d.get2DDataValue();
		output.putInt("ends", ends);
		output.store("floorOrigin", BlockPos.CODEC, floorOrigin);
		output.putInt("floorWidth", floorWidth);
		output.putInt("floorRadius", floorRadius);
		output.putInt("rotation", rotatingAmount);
		List<Captured> list = new ArrayList<>(captured.size());
		captured.forEach((offset, state) -> list.add(new Captured(offset, state)));
		output.store("captured", Captured.CODEC.listOf(), list);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		direction = Direction.from2DDataValue(input.getIntOr("dir", Direction.EAST.get2DDataValue()));
		floor = input.getIntOr("lvl", 0);
		lockX = input.getIntOr("lockX", 0);
		lockZ = input.getIntOr("lockY", 0);
		checkpoint = input.getBooleanOr("check", false);
		endpoint = input.getBooleanOr("end", false);
		hasFluid = input.getBooleanOr("fluid", false);
		int ends = input.getIntOr("ends", 0);
		openEndsAtZero.clear();
		for (Direction d : Direction.Plane.HORIZONTAL)
			if ((ends & 1 << d.get2DDataValue()) != 0)
				openEndsAtZero.add(d);
		floorOrigin = input.read("floorOrigin", BlockPos.CODEC).orElse(BlockPos.ZERO);
		floorWidth = input.getIntOr("floorWidth", 0);
		floorRadius = input.getIntOr("floorRadius", 0);
		rotatingAmount = input.getIntOr("rotation", 0);
		captured.clear();
		for (Captured c : input.read("captured", Captured.CODEC.listOf()).orElse(List.of()))
			captured.put(c.offset(), c.state());
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
