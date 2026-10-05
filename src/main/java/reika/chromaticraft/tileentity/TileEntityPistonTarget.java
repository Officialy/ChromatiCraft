package reika.chromaticraft.tileentity;

import java.util.List;
import java.util.Random;

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

import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTarget26;
import reika.chromaticraft.entity.EntityPistonSpline;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.instantiable.RGBColorData;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;

/**
 * V33a {@code PistonEmitterTile} (TYPE 0, the read head) and {@code PistonDoorTile} (TYPE 1, a door's colour
 * receiver), which shared {@code PistonTargetTile}'s stage/door/bus bookkeeping.
 *
 * <p>A read head that is hit along its facing by the filtered beam sends an {@link EntityPistonSpline} of that
 * colour along the path its stage set for the current step. A door receiver hit by a spline of its own colour
 * lights for 50 ticks and marks its door validated; when every bus receiver of the door is lit at once the
 * door opens, and it closes again when one goes dark. V33a kept the validation in the generator's memory; it is
 * stored here, so a stage's solved state survives a restart.
 */
public final class TileEntityPistonTarget extends BlockEntity {

	private static final int DURATION = 50;

	private int doorBusWidth;
	private int stageIndex;
	private int doorIndex;
	private int doorColorIndex;
	private Direction facing = Direction.NORTH;
	private RGBColorData color = RGBColorData.white();
	private BlockPos door = BlockPos.ZERO;
	private int active;
	private boolean validated;
	/** Not saved, as upstream: set by the stage each time it fires the emitters. */
	private transient @Nullable Spline path;

	public TileEntityPistonTarget(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PISTON_TARGET.get(), pos, state);
	}

	public boolean isDoorReceiver() {
		return getBlockState().getValue(BlockPistonTarget26.TYPE) == 1;
	}

	/** V33a {@code setData(facing, stage, door, index, width)}. */
	public void configure(Direction facing, int stage, int door, int busIndex, int busWidth) {
		this.facing = facing;
		stageIndex = stage;
		doorIndex = door;
		doorColorIndex = busIndex;
		doorBusWidth = busWidth;
		setChanged();
	}

	public void configureDoor(RGBColorData color, BlockPos doorPanel) {
		this.color = color.copy();
		door = doorPanel.immutable();
		setChanged();
	}

	public Direction facing() { return facing; }
	public int stageIndex() { return stageIndex; }
	public int doorIndex() { return doorIndex; }
	public RGBColorData color() { return color.copy(); }
	public boolean isActive() { return active > 0; }
	public boolean isValidated() { return validated; }

	public void setValidated(boolean flag) {
		validated = flag;
		setChanged();
	}

	// ----------------------------------------------------------------------------- read head (TYPE 0)

	/** V33a {@code PistonEmitterTile.fire}: send the read colour down the current step's path. */
	public void fire(RGBColorData clr) {
		if (level == null || level.isClientSide())
			return;
		if (path == null) {
			ChromaSounds.ERROR.playSoundAtBlock(this);
			return;
		}
		level.addFreshEntity(new EntityPistonSpline(level, path, clr));
	}

	/**
	 * V33a {@code PistonEmitterTile.setTarget}: a chordal spline from this head, through a jittered point in each
	 * hall waypoint, then every {@code 3+busWidth} blocks down the door tunnel until it passes the target, to
	 * the target itself. The jitter is seeded by this head's position, so a step always takes the same path.
	 */
	public void setTarget(int door, BlockPos tgt, Direction hallDir, List<BlockPos> hall) {
		Direction dir = hallDir.getCounterClockWise();
		Random rand = new Random(worldPosition.hashCode());
		rand.nextBoolean();
		doorIndex = door;
		Spline spline = new Spline(Spline.SplineType.CHORDAL);
		spline.addPoint(new Spline.BasicSplinePoint(new DecimalPosition(this)));
		for (BlockPos c : hall)
			spline.addPoint(new Spline.BasicSplinePoint(DecimalPosition.getRandomWithin(c, rand)));
		BlockPos last = hall.getLast();
		int dl = 3 + doorBusWidth;
		int dx = last.getX() + dl * hallDir.getStepX();
		int dz = last.getZ() + dl * hallDir.getStepZ();
		int tx = tgt.getX() + dir.getStepX() * 3;
		int tz = tgt.getZ() + dir.getStepZ() * 3;
		int diff = hallDir.getStepX() * (tx - dx) + hallDir.getStepZ() * (tz - dz);
		while (diff >= 0) {
			double rx = dx + 0.5 - 1.25 + rand.nextDouble() * 2.5;
			double rz = dz + 0.5 - 1.25 + rand.nextDouble() * 2.5;
			double ry = tgt.getY() + 0.5 - 1.25 + rand.nextDouble() * 2.5;
			spline.addPoint(new Spline.BasicSplinePoint(new DecimalPosition(rx, ry, rz)));
			dx += hallDir.getStepX() * dl;
			dz += hallDir.getStepZ() * dl;
			diff = hallDir.getStepX() * (tx - dx) + hallDir.getStepZ() * (tz - dz);
		}
		spline.addPoint(new Spline.BasicSplinePoint(new DecimalPosition(tgt.getX() + 0.5, tgt.getY() + 0.5, tgt.getZ() + 0.5)));
		path = spline;
	}

	// ----------------------------------------------------------------------- door receiver (TYPE 1)

	/** V33a {@code PistonDoorTile.receive}. */
	public void receive() {
		if (level == null)
			return;
		setActive(true);
		validated = true;
		ChromaSounds.CAST.playSoundAtBlock(this);
		level.blockEvent(worldPosition, getBlockState().getBlock(), RECEIVE_EVENT, 0);
	}

	private void setActive(boolean on) {
		active = on ? DURATION : 0;
		setChanged();
		sync();
		checkDoor();
	}

	/** V33a {@code checkDoor}: the door is open exactly while every bus receiver of it is lit. */
	private void checkDoor() {
		if (level == null)
			return;
		boolean open = true;
		Direction left = facing.getCounterClockWise();
		BlockPos root = worldPosition.relative(left, -doorColorIndex);
		for (int i = 0; i < doorBusWidth; i++) {
			if (!(level.getBlockEntity(root.relative(left, i)) instanceof TileEntityPistonTarget te) || !te.isActive()) {
				open = false;
				break;
			}
		}
		if (level.getBlockEntity(door) instanceof TileEntityChromaDoor panel) {
			if (open)
				panel.open(-1);
			else
				panel.close();
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPistonTarget te) {
		if (te.active > 0) {
			te.active--;
			if (te.active == 0)
				te.setActive(false);
		}
	}

	private static final int RECEIVE_EVENT = 1;

	/** V33a {@code receptionParticles}, delivered by block event because reception is decided on the server. */
	@Override
	public boolean triggerEvent(int id, int value) {
		if (id == RECEIVE_EVENT) {
			if (level != null && level.isClientSide())
				reika.chromaticraft.render.particle.ChromaParticle.spawnPistonDoorReception(level, worldPosition,
						facing, color.getRenderColor());
			return true;
		}
		return super.triggerEvent(id, value);
	}

	private void sync() {
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("index", doorColorIndex);
		output.putInt("stage", stageIndex);
		output.putInt("door", doorIndex);
		output.putInt("bus", doorBusWidth);
		output.putInt("facing", facing.get3DDataValue());
		output.putBoolean("red", color.red);
		output.putBoolean("green", color.green);
		output.putBoolean("blue", color.blue);
		output.store("doorLoc", BlockPos.CODEC, door);
		output.putInt("active", active);
		output.putBoolean("validated", validated);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		doorColorIndex = input.getIntOr("index", 0);
		stageIndex = input.getIntOr("stage", 0);
		doorIndex = input.getIntOr("door", 0);
		doorBusWidth = input.getIntOr("bus", 0);
		facing = Direction.from3DDataValue(input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
		color = new RGBColorData(input.getBooleanOr("red", true), input.getBooleanOr("green", true),
				input.getBooleanOr("blue", true));
		door = input.read("doorLoc", BlockPos.CODEC).orElse(BlockPos.ZERO);
		active = input.getIntOr("active", 0);
		validated = input.getBooleanOr("validated", false);
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
