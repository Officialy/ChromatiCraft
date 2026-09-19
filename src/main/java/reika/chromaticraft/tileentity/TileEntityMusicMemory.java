package reika.chromaticraft.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.decoration.BlockMusicTrigger;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/** Persistent modern owner for one V33a Crystal Music room. */
public final class TileEntityMusicMemory extends BlockEntity implements BlockMusicTrigger.Handler {

	private final ArrayList<MusicKey> melody = new ArrayList<>();
	private int playbackDelay = 10;
	private int roomIndex;
	private @Nullable BlockPos doorCenter;
	private int correctIndex = -1;
	private int playbackIndex;
	private int playbackTick;
	private boolean playing;
	private boolean solved;

	public TileEntityMusicMemory(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.MUSIC_MEMORY.get(), pos, state);
	}

	public void program(List<MusicKey> notes, int delay, int room, BlockPos door) {
		melody.clear();
		melody.addAll(notes);
		playbackDelay = Math.max(1, delay);
		roomIndex = room;
		doorCenter = door.immutable();
		setChanged();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state,
			TileEntityMusicMemory memory) {
		if (!memory.playing || memory.melody.isEmpty()) return;
		if (--memory.playbackTick > 0) return;
		if (memory.playbackIndex >= memory.melody.size()) {
			memory.playing = false;
			memory.playbackIndex = 0;
			memory.setChanged();
			return;
		}
		MusicKey key = memory.melody.get(memory.playbackIndex++);
		if (key != null) memory.playKey(key);
		memory.playbackTick = memory.playbackDelay;
		memory.setChanged();
	}

	public void play() {
		if (melody.isEmpty()) return;
		playing = true;
		playbackIndex = 0;
		playbackTick = playbackDelay;
		setChanged();
	}

	private void playKey(MusicKey key) {
		if (level instanceof ServerLevel server)
			reika.chromaticraft.network.ChromaNetwork.sendMusicMemoryNote(server, worldPosition, key);
	}

	@Override
	public void onMusicTrigger(BlockPos triggerPos, CrystalElement element, MusicKey key,
			@Nullable Player player) {
		if (solved || melody.isEmpty()) return;
		int next = correctIndex + 1;
		if (next < melody.size() && key == melody.get(next)) {
			correctIndex = next;
			while (correctIndex < melody.size() - 1 && melody.get(correctIndex + 1) == null)
				correctIndex++;
			if (correctIndex == melody.size() - 1) complete();
			else setChanged();
		}
		else {
			if (correctIndex != -1 && level != null) ChromaSounds.ERROR.playSoundAtBlock(level, worldPosition);
			correctIndex = -1;
			setChanged();
		}
	}

	public void complete() {
		solved = true;
		correctIndex = -1;
		playing = false;
		openDoor();
		if (level != null) ChromaSounds.CAST.playSoundAtBlock(level, worldPosition);
		setChanged();
	}

	/** V33a password/creative bypass opens a room without playing six-to-ten CAST sounds at once. */
	public void forceComplete() {
		solved = true;
		correctIndex = -1;
		playing = false;
		openDoor();
		setChanged();
	}

	public boolean isSolved() { return solved; }
	/** Returns an immutable snapshot while retaining null entries, which are authored rests. */
	public List<MusicKey> melody() { return java.util.Collections.unmodifiableList(new ArrayList<>(melody)); }
	public int playbackDelay() { return playbackDelay; }
	public int roomIndex() { return roomIndex; }

	private void openDoor() {
		if (level == null || doorCenter == null) return;
		for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
			BlockPos at = doorCenter.offset(dx, dy, 0);
			BlockState state = level.getBlockState(at);
			if (state.is(ChromaBlocks.CHROMA_DOOR.get()))
				level.setBlock(at, state.setValue(BlockChromaDoor.OPEN, true), 3);
		}
	}

	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray("melody", melody.stream().mapToInt(key -> key == null ? -1 : key.ordinal()).toArray());
		output.putInt("delay", playbackDelay);
		output.putInt("room", roomIndex);
		if (doorCenter != null) output.putLong("door", doorCenter.asLong());
		output.putInt("correct", correctIndex);
		output.putInt("playIndex", playbackIndex);
		output.putInt("playTick", playbackTick);
		output.putBoolean("playing", playing);
		output.putBoolean("solved", solved);
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		melody.clear();
		for (int ordinal : input.getIntArray("melody").orElse(new int[0]))
			melody.add(ordinal < 0 ? null : MusicKey.getByIndex(ordinal));
		playbackDelay = Math.max(1, input.getIntOr("delay", 10));
		roomIndex = Math.max(0, input.getIntOr("room", 0));
		doorCenter = input.getLong("door").map(BlockPos::of).orElse(null);
		correctIndex = input.getIntOr("correct", -1);
		playbackIndex = Math.clamp(input.getIntOr("playIndex", 0), 0, melody.size());
		playbackTick = Math.max(0, input.getIntOr("playTick", 0));
		playing = input.getBooleanOr("playing", false) && !melody.isEmpty();
		solved = input.getBooleanOr("solved", false);
	}
}
