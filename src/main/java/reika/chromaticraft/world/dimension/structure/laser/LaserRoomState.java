package reika.chromaticraft.world.dimension.structure.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.RoomBlueprint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent V33a Chromatic Beams room state. The switch, emitters and targets all use this one
 * state machine; world effects are returned to the structure piece so a target cannot open a
 * different room's gate or let a stale pulse survive a reset.
 */
public final class LaserRoomState {

	private final String name;
	private final BlockPos doorCenter;
	private final Set<BlockPos> emitters;
	private final Set<BlockPos> targets;
	private final Set<BlockPos> activeTargets = new LinkedHashSet<>();
	private boolean lasersInPlay;
	private boolean solved;

	private LaserRoomState(String name, BlockPos doorCenter, Set<BlockPos> emitters,
			Set<BlockPos> targets) {
		if (emitters.isEmpty() || targets.isEmpty())
			throw new IllegalArgumentException("Laser room " + name + " needs emitters and targets");
		this.name = name;
		this.doorCenter = doorCenter.immutable();
		this.emitters = Collections.unmodifiableSet(new LinkedHashSet<>(emitters));
		this.targets = Collections.unmodifiableSet(new LinkedHashSet<>(targets));
	}

	/** The origin is the decoded .struct minimum corner, at the generator's base Y. */
	public static LaserRoomState fromBlueprint(RoomBlueprint room, BlockPos origin) {
		Set<BlockPos> emitters = new LinkedHashSet<>();
		Set<BlockPos> targets = new LinkedHashSet<>();
		for (var effector : room.effectors()) {
			BlockPos at = origin.offset(effector.position());
			if (effector.type() == EffectType.EMITTER) emitters.add(at);
			else if (effector.type().isTarget()) targets.add(at);
		}
		BlockPos door = origin.offset(room.sizeX() + 7, 3, (room.sizeZ() + 1) / 2);
		return new LaserRoomState(room.name(), door, emitters, targets);
	}

	/** V33a's room switch: a second click stops firing and resets every target and the door. */
	public Change toggle() {
		activeTargets.clear();
		solved = false;
		if (lasersInPlay) {
			lasersInPlay = false;
			return new Change(List.of(), true, false, false);
		}
		lasersInPlay = true;
		return new Change(List.copyOf(emitters), false, false, true);
	}

	/** V33a's completeTrigger, including pass-through targets and their later auto-reset. */
	public Change targetTriggered(BlockPos target, boolean on) {
		if (!targets.contains(target))
			throw new IllegalArgumentException("Target " + target + " is not in laser room " + name);
		if (on) activeTargets.add(target);
		else activeTargets.remove(target);
		solved = activeTargets.equals(targets);
		return new Change(List.of(), solved, solved, lasersInPlay);
	}

	/** V33a structure-password bypass opens the gate without starting emitters. */
	public Change forceComplete() {
		solved = true;
		return new Change(List.of(), false, true, lasersInPlay);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("level", name);
		tag.put("door", positionTag(doorCenter));
		tag.put("emitters", positionsTag(emitters));
		tag.put("targets", positionsTag(targets));
		tag.put("activeTargets", positionsTag(activeTargets));
		tag.putBoolean("firing", lasersInPlay);
		tag.putBoolean("solved", solved);
		return tag;
	}

	public static LaserRoomState load(CompoundTag tag) {
		LaserRoomState state = new LaserRoomState(tag.getStringOr("level", ""),
				readPosition(tag.getCompoundOrEmpty("door")), readPositions(tag, "emitters"),
				readPositions(tag, "targets"));
		state.activeTargets.addAll(readPositions(tag, "activeTargets"));
		state.activeTargets.retainAll(state.targets);
		state.lasersInPlay = tag.getBooleanOr("firing", false);
		state.solved = tag.getBooleanOr("solved", false);
		return state;
	}

	/** Restores a room through the 26.3 block-entity ValueInput persistence format. */
	public static LaserRoomState restore(String name, BlockPos door, Set<BlockPos> emitters,
			Set<BlockPos> targets, Set<BlockPos> active, boolean firing, boolean solved) {
		LaserRoomState state = new LaserRoomState(name, door, emitters, targets);
		state.activeTargets.addAll(active);
		state.activeTargets.retainAll(targets);
		state.lasersInPlay = firing;
		state.solved = solved;
		return state;
	}

	private static CompoundTag positionTag(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		return tag;
	}

	private static ListTag positionsTag(Set<BlockPos> positions) {
		ListTag list = new ListTag();
		positions.forEach(pos -> list.add(positionTag(pos)));
		return list;
	}

	private static Set<BlockPos> readPositions(CompoundTag tag, String key) {
		Set<BlockPos> positions = new LinkedHashSet<>();
		tag.getListOrEmpty(key).compoundStream().map(LaserRoomState::readPosition)
				.forEach(positions::add);
		return positions;
	}

	private static BlockPos readPosition(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
	}

	public String name() { return name; }
	public BlockPos doorCenter() { return doorCenter; }
	public Set<BlockPos> emitters() { return emitters; }
	public Set<BlockPos> targets() { return targets; }
	public Set<BlockPos> activeTargets() { return Set.copyOf(activeTargets); }
	public boolean lasersInPlay() { return lasersInPlay; }
	public boolean solved() { return solved; }

	/** Side effects prescribed by one state transition, applied in the caller's ServerLevel. */
	public record Change(List<BlockPos> fireEmitters, boolean clearPulses, boolean doorOpen,
			boolean lasersInPlay) {
		public Change { fireEmitters = List.copyOf(fireEmitters); }
	}
}
