package reika.chromaticraft.tileentity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;

/** Persistent UUID/owner state shared logically by every connected Chroma Door cell. */
public final class TileEntityChromaDoor extends BlockEntity implements SneakPop {

	private @Nullable UUID uid;
	private @Nullable UUID placer;
	private boolean autoOpen;
	private int lastAutoOpenDuration = 20;

	public TileEntityChromaDoor(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CHROMA_DOOR.get(), pos, state);
	}

	public boolean isOwner(Player player) {
		return player != null && placer != null && placer.equals(player.getUUID());
	}

	public void setPlacer(UUID player) { placer = player; setChanged(); }
	public @Nullable UUID getDoorID() { return uid; }
	public boolean isAutoOpen() { return autoOpen; }
	public boolean canOpen(UUID key) { return key != null && key.equals(uid); }

	/** V33a automatic-key mode, evaluated once by the north/west/bottom root of a connected door. */
	public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChromaDoor door) {
		boolean opened = false;
		if (door.autoOpen && !state.getValue(BlockChromaDoor.OPEN)
				&& !level.getBlockState(pos.below()).is(state.getBlock())
				&& !level.getBlockState(pos.west()).is(state.getBlock())
				&& !level.getBlockState(pos.north()).is(state.getBlock())
				&& door.placer != null) {
			Player player = level.getPlayerByUUID(door.placer);
			if (player != null) {
				double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				if (Math.abs(player.getY() - pos.getY()) < 1 && distance < 9
						&& (distance < 2 || lookingAt(player, pos, 0.5) || lookingAt(player, pos, -1.5))) {
					door.lastAutoOpenDuration = Math.min(door.lastAutoOpenDuration + 10, 200);
					door.open(door.lastAutoOpenDuration);
					opened = true;
				}
			}
		}
		if (!opened) door.lastAutoOpenDuration = Math.max(20, door.lastAutoOpenDuration - 10);
	}

	private static boolean lookingAt(Player player, BlockPos pos, double yOffset) {
		net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
		net.minecraft.world.phys.Vec3 target = new net.minecraft.world.phys.Vec3(
				pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
		net.minecraft.world.phys.Vec3 delta = target.subtract(eye);
		return delta.lengthSqr() > 0 && player.getLookAngle().dot(delta.normalize()) >= 0.95;
	}

	public void bindUUID(@Nullable Player player, UUID id, boolean automatic) {
		for (BlockPos pos : connectedDoor()) {
			if (level.getBlockEntity(pos) instanceof TileEntityChromaDoor door
					&& (player == null || door.isOwner(player))) {
				door.uid = id;
				door.autoOpen = automatic;
				door.setChanged();
			}
		}
	}

	public void openClick() { open(50); }

	public void open(int delay) {
		setOpen(true);
		if (level != null) {
			ChromaSounds.ITEMSTAND.playSoundAtBlock(level, worldPosition, 1, 2F);
			ChromaSounds.ITEMSTAND.playSoundAtBlock(level, worldPosition, 1, 1F);
			if (delay > 0 && !getBlockState().getValue(BlockChromaDoor.STAY_OPEN))
				level.scheduleTick(worldPosition, getBlockState().getBlock(), delay);
		}
	}

	public void close() {
		if (level == null || !getBlockState().getValue(BlockChromaDoor.OPEN))
			return;
		setOpen(false);
		ChromaSounds.ITEMSTAND.playSoundAtBlock(level, worldPosition, 1, 0.5F);
	}

	private void setOpen(boolean open) {
		for (BlockPos pos : connectedDoor()) {
			if (level.getBlockEntity(pos) instanceof TileEntityChromaDoor door && sameID(door))
				level.setBlock(pos, level.getBlockState(pos).setValue(BlockChromaDoor.OPEN, open), 3);
		}
	}

	private boolean sameID(TileEntityChromaDoor other) {
		return uid == null ? other.uid == null : uid.equals(other.uid);
	}

	private Set<BlockPos> connectedDoor() {
		Set<BlockPos> found = new HashSet<>();
		if (level == null) return found;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(worldPosition);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.removeFirst();
			if (!found.add(pos)) continue;
			for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (Math.abs(next.getX() - worldPosition.getX()) <= 8
						&& Math.abs(next.getY() - worldPosition.getY()) <= 8
						&& Math.abs(next.getZ() - worldPosition.getZ()) <= 8
						&& level.getBlockState(next).is(getBlockState().getBlock()) && !found.contains(next))
					queue.add(next);
			}
		}
		return found;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (uid != null) output.putString("uid", uid.toString());
		if (placer != null) output.putString("placer", placer.toString());
		output.putBoolean("auto", autoOpen);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		uid = parse(input, "uid");
		placer = parse(input, "placer");
		autoOpen = input.getBooleanOr("auto", false);
	}

	private static @Nullable UUID parse(ValueInput input, String key) {
		return input.getString(key).flatMap(value -> {
			try { return java.util.Optional.of(UUID.fromString(value)); }
			catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
		}).orElse(null);
	}

	@Override
	public void drop() {
		if (level == null) return;
		Block.popResource(level, worldPosition, new ItemStack(ChromaBlocks.CHROMA_DOOR.get()));
		level.destroyBlock(worldPosition, false);
	}

	@Override
	public boolean canDrop(Player player) {
		return isOwner(player);
	}

	@Override
	public boolean allowMining(Player player) {
		return isOwner(player);
	}
}
