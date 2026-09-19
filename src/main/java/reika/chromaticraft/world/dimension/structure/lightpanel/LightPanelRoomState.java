package reika.chromaticraft.world.dimension.structure.lightpanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.KeySignature;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.Note;

/** Runtime identity and persistent state of one NBT-owned Glowing Logic room. */
public final class LightPanelRoomState {

	public static final int DEPTH = 18;
	private final int levelIndex;
	private final BlockPos origin;
	private final LightPanelPuzzleState puzzle;
	private final int[] pitchOrdinals;

	public LightPanelRoomState(int levelIndex, BlockPos origin, FixedLightPattern pattern, Random random) {
		this.levelIndex = levelIndex;
		this.origin = origin.immutable();
		puzzle = new LightPanelPuzzleState(pattern, random);
		pitchOrdinals = createPitches(puzzle, random);
	}

	private LightPanelRoomState(int levelIndex, BlockPos origin, LightPanelPuzzleState puzzle,
			int[] pitches) {
		this.levelIndex = levelIndex;
		this.origin = origin.immutable();
		this.puzzle = puzzle;
		pitchOrdinals = new int[puzzle.switchCount()];
		java.util.Arrays.fill(pitchOrdinals, -1);
		System.arraycopy(pitches, 0, pitchOrdinals, 0,
				Math.min(pitches.length, pitchOrdinals.length));
		for (int i = 0; i < pitchOrdinals.length; i++)
			if (MusicKey.getByIndex(pitchOrdinals[i]) == null) pitchOrdinals[i] = -1;
	}

	public int levelIndex() { return levelIndex; }
	public BlockPos origin() { return origin; }
	public LightPanelPuzzleState puzzle() { return puzzle; }

	public BlockPos panelPosition(int row, LightType type) {
		return origin.offset(DEPTH * 2 / 3, 6 + row, -LightType.list.length + type.ordinal() * 2 + 1);
	}

	public BlockPos switchPosition(int channel) {
		return origin.offset(DEPTH / 3, 2, -puzzle.switchCount() + channel * 2 + 1);
	}

	public BlockPos doorPosition() {
		return origin.offset(DEPTH * 2 / 3, 1, 0);
	}

	public MusicKey pitch(int channel) {
		if (channel < 0 || channel >= pitchOrdinals.length)
			throw new IllegalArgumentException("Glowing Logic switch " + channel);
		return MusicKey.getByIndex(pitchOrdinals[channel]);
	}

	public void save(ValueOutput output) {
		output.putInt("level", levelIndex);
		output.putLong("origin", origin.asLong());
		output.putIntArray("pitches", pitchOrdinals);
		puzzle.save(output.child("puzzle"));
	}

	public static LightPanelRoomState load(ValueInput input) {
		int level = input.getIntOr("level", 0);
		BlockPos origin = BlockPos.of(input.getLongOr("origin", BlockPos.ZERO.asLong()));
		LightPanelPuzzleState puzzle = input.child("puzzle").map(LightPanelPuzzleState::load)
				.orElseThrow(() -> new IllegalArgumentException("Glowing Logic room has no puzzle state"));
		int[] pitches = input.getIntArray("pitches").orElseGet(() -> new int[0]);
		return new LightPanelRoomState(level, origin, puzzle, pitches);
	}

	private static int[] createPitches(LightPanelPuzzleState puzzle, Random random) {
		KeySignature signature = KeySignature.keys[random.nextInt(KeySignature.keys.length)];
		List<MusicKey> available = new ArrayList<>();
		for (Note note : signature.getScale()) available.add(MusicKey.C5.getInterval(note.ordinal()));
		available.add(MusicKey.C6);
		int[] pitches = new int[puzzle.switchCount()];
		java.util.Arrays.fill(pitches, -1);
		for (int sw = 0; sw < pitches.length && !available.isEmpty(); sw++) {
			if (!puzzle.hasConnections(sw)) continue;
			MusicKey key = available.remove(random.nextInt(available.size()));
			pitches[sw] = key.ordinal();
		}
		return pitches;
	}
}
