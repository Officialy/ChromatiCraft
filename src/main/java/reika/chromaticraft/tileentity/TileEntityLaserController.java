package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.RoomBlueprint;
import reika.chromaticraft.world.dimension.structure.laser.LaserRoomState;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** One persistent room authority for the switch, targets, emitters and east gate. */
public final class TileEntityLaserController extends BlockEntity {

	private @Nullable LaserRoomState room;
	private long lastClick = Long.MIN_VALUE;
	private int minX;
	private int maxX;
	private int minZ;
	private int maxZ;
	private int effectorY;
	private int effectorMaxY;

	public TileEntityLaserController(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LASER_CONTROLLER.get(), pos, state);
	}

	public void configure(RoomBlueprint blueprint, BlockPos effectorOrigin) {
		room = LaserRoomState.fromBlueprint(blueprint, effectorOrigin);
		minX = effectorOrigin.getX(); maxX = minX + blueprint.sizeX();
		minZ = effectorOrigin.getZ(); maxZ = minZ + blueprint.sizeZ();
		effectorY = effectorOrigin.getY();
		effectorMaxY = effectorY + blueprint.effectors().stream()
				.mapToInt(device -> device.position().getY()).max().orElse(0);
		setChanged();
	}

	public void toggle() {
		if (room == null || level == null || level.isClientSide()) return;
		long time = level.getGameTime();
		if (lastClick != Long.MIN_VALUE && time - lastClick < 5) return;
		lastClick = time;
		for (BlockPos target : room.targets())
			if (level.getBlockEntity(target) instanceof TileEntityLaserEffector effector)
				effector.resetTarget();
		apply(room.toggle());
	}

	public void targetTriggered(BlockPos target, boolean on) {
		if (room == null || level == null || level.isClientSide()
				|| !room.targets().contains(target)) return;
		apply(room.targetTriggered(target, on));
	}

	public void forceComplete() {
		if (room != null && level != null) apply(room.forceComplete());
	}

	public boolean isFiring() { return room != null && room.lasersInPlay(); }
	public boolean isSolved() { return room != null && room.solved(); }
	public @Nullable LaserRoomState room() { return room; }

	private void apply(LaserRoomState.Change change) {
		if (room == null || level == null) return;
		for (BlockPos emitter : room.emitters())
			if (level.getBlockEntity(emitter) instanceof TileEntityLaserEffector effector)
				effector.setFiring(change.lasersInPlay());
		if (change.clearPulses()) {
			AABB box = new AABB(minX - 1, effectorY - 1, minZ - 1,
					maxX + 2, effectorMaxY + 2, maxZ + 2);
			for (EntityLaserPulse pulse : level.getEntitiesOfClass(EntityLaserPulse.class, box))
				pulse.discard();
		}
		BlockPos door = room.doorCenter();
		for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
			BlockPos at = door.offset(0, dy, dz);
			BlockState state = level.getBlockState(at);
			if (state.is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, state.setValue(BlockChromaDoor.OPEN, change.doorOpen()),
						Block.UPDATE_ALL);
		}
		BlockLightPanel.activate(level, worldPosition.below(), change.lasersInPlay());
		setChanged();
	}

	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (room == null) return;
		output.putString("room", room.name());
		output.putLong("door", room.doorCenter().asLong());
		output.putString("emitters", encode(room.emitters()));
		output.putString("targets", encode(room.targets()));
		output.putString("activeTargets", encode(room.activeTargets()));
		output.putBoolean("firing", room.lasersInPlay());
		output.putBoolean("solved", room.solved());
		output.putLong("lastClick", lastClick);
		output.putInt("minX", minX); output.putInt("maxX", maxX);
		output.putInt("minZ", minZ); output.putInt("maxZ", maxZ);
		output.putInt("effectorY", effectorY);
		output.putInt("effectorMaxY", effectorMaxY);
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String name = input.getStringOr("room", "");
		Set<BlockPos> emitters = decode(input.getStringOr("emitters", ""));
		Set<BlockPos> targets = decode(input.getStringOr("targets", ""));
		room = name.isEmpty() || emitters.isEmpty() || targets.isEmpty() ? null
				: LaserRoomState.restore(name, BlockPos.of(input.getLongOr("door", 0)),
					emitters, targets, decode(input.getStringOr("activeTargets", "")),
					input.getBooleanOr("firing", false), input.getBooleanOr("solved", false));
		lastClick = input.getLongOr("lastClick", Long.MIN_VALUE);
		minX = input.getIntOr("minX", 0); maxX = input.getIntOr("maxX", 0);
		minZ = input.getIntOr("minZ", 0); maxZ = input.getIntOr("maxZ", 0);
		effectorY = input.getIntOr("effectorY", 0);
		effectorMaxY = input.getIntOr("effectorMaxY", effectorY);
	}

	private static String encode(Set<BlockPos> positions) {
		return positions.stream().map(pos -> Long.toString(pos.asLong()))
				.collect(Collectors.joining(","));
	}

	private static Set<BlockPos> decode(String encoded) {
		if (encoded.isEmpty()) return Set.of();
		return Arrays.stream(encoded.split(",")).map(Long::parseLong).map(BlockPos::of)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
