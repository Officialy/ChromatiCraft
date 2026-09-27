package reika.chromaticraft.auxiliary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.RandomSource;

import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/** The exact prefab melody catalog shared by V33a's music and Biome Fragment puzzles. */
public final class BiomeStructureMelodies {

	private static final List<Melody> PREFABS = List.of(
			melody(8, "C5 - G4 - A4 - E4 - F4 - C4 - F4 - G4 -"),
			melody(8, "G5 A5 B5 D6 C6 C6 E6 D6 D6 G6 Fs6 G6 D6 B5 G5 A5 B5 C6 D6 E6 D6 C6 B5 A5 B5 G5 Fs5 G5 A5 D5"),
			melody(8, "F5 C5 F5 C5 F5 Cs5 Eb5 G5 Ab5 G5 Eb5 F5"),
			melody(8, "A4 A4 C5 G4 E4 A4 C5 G4"),
			melody(8, "A4 B4 Cs5 E5 A4 B4 Cs5 E5 A4 B4 C5 D5 B4"),
			melody(8, "A4 B4 D5 B4 E5 Fs5 D5 B4 D5 B4 Cs5"),
			melody(8, "G5 E5 D5 B4 E5 C5 G5 - B4 D5 G5 A5 Fs5"),
			melody(8, "G4 D4 A4 D4 Bb4 D4 C5 D5 Bb4 Eb4 C5 Eb4 A4 Eb4 F4 D4"),
			melody(8, "C5 D5 Eb5 C5 G4 Ab4 Bb4 G4 F4 G4 Ab4 Bb4 F4"),
			melody(8, "Ab5 Eb5 Ab5 Eb6 B5 Ab5 B5 Ab5 B5 Eb5 Eb6 Ab5 Eb5 B5 Eb6 B5 B5 Ab5 B5 E6 Fs5 E6 B5 B5 Fs5 Bb5 Eb5 Cs6 B5"),
			melody(8, "G5 D5 G5 D5 G5 Eb5 G5 Eb5 A5 Eb5 Bb5 Eb5 F5 D5 F5 D5 G5 D5 A5 D5 A5 D5 A5 F5 G5"),
			melody(8, "E5 E5 F5 G5 G5 F5 E5 D5 C5 C5 D5 E5 E5 - D5 D5 - E5 E5 F5 G5 G5 F5 E5 D5 C5 C5 D5 E5 D5 - C5 C5"),
			melody(7, "D5 - A5 - Cs6 - - A5 D6 Cs6 B5 A5 B5 Cs6 A5 - E5 - A5 - Cs6 - - A5 D6 Cs6 B5 A5 B5 Cs6 E6 Cs6 A5"),
			melody(5, "G5 - - D5 - - G5 - - D5 - - G5 - A5 - G5 - - Fs5 - - G5 - - Fs5 - - C6 - B5 - G5 - - D5 - - B4 - - G5 - - C5 - B4 - A5 - - B5 - - A5 - - B5 - - A5 B5 D6 B5 G5"),
			melody(7, "E5 - - B5 - - A5 G5 Fs5 G5 - E5 Fs5 - G5 Fs5 - D5 E5 - - - - D5 E5 - - B5 - - A5 G5 Fs5 G5 - E5 G5 - A5 B5 - C6 B5")
	);

	private BiomeStructureMelodies() {
	}

	public static Melody random(RandomSource random) {
		return PREFABS.get(random.nextInt(PREFABS.size()));
	}

	public static List<Melody> prefabs() {
		return PREFABS;
	}

	/**
	 * V33a {@code MusicPuzzleGenerator.addPrefabMelody}: the authored notes are transposed into the
	 * nearest key the crystals can voice before the prefab is stored. Only an upward shift is applied, as
	 * upstream does ({@code if (shift > 0)}); the second melody's only fit is five down, so it keeps its
	 * authored pitches, and at thirty notes it is too long for any Crystal Music room.
	 */
	private static Melody melody(int playbackRate, String serialized) {
		ArrayList<MusicKey> notes = new ArrayList<>();
		for (String token : serialized.split(" "))
			notes.add(token.equals("-") ? null : MusicKey.valueOf(token));
		int shift = verify(notes);
		if (shift > 0) {
			for (int i = 0; i < notes.size(); i++) {
				MusicKey key = notes.get(i);
				if (key != null)
					notes.set(i, key.getInterval(shift));
			}
		}
		return new Melody(playbackRate, Collections.unmodifiableList(notes));
	}

	/** V33a {@code verify}: the first shift, trying 0..11 up then 1..11 down, at which every note is playable. */
	private static int verify(List<MusicKey> notes) {
		for (int d = 0; d < 12; d++)
			if (fits(notes, d))
				return d;
		for (int d = -1; d > -12; d--)
			if (fits(notes, d))
				return d;
		throw new IllegalStateException("Invalid prefab melody! " + notes);
	}

	private static boolean fits(List<MusicKey> notes, int shift) {
		for (MusicKey key : notes) {
			if (key == null)
				continue;
			// getInterval clamps at the ends of the key range; a clamped note is not a transposition.
			if ((shift != 0 && key.getInterval(shift) == key)
					|| !CrystalMusicManager.instance.canPlayKey(key.getInterval(shift)))
				return false;
		}
		return true;
	}

	public record Melody(int playbackRate, List<MusicKey> notes) {
	}
}
