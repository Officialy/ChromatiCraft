package reika.chromaticraft.world.dimension.structure;

import reika.chromaticraft.auxiliary.BiomeStructureMelodies;
import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

import java.util.*;

/** Seeded, reload-safe melody plan for V33a's Crystal Music structure. */
public final class MusicPuzzleLayout {

	private final ArrayList<Room> rooms = new ArrayList<>();

	private MusicPuzzleLayout(int difficulty, Random random) {
		int count = roomCount(difficulty);
		HashSet<Integer> usedPrefabs = new HashSet<>();
		List<BiomeStructureMelodies.Melody> prefabs = BiomeStructureMelodies.prefabs();
		for (int room = 0; room < count; room++) {
			int requestedLength = puzzleLength(difficulty, room);
			BiomeStructureMelodies.Melody prefab = null;
			if (usedPrefabs.size() < prefabs.size() && random.nextInt(5) == 0) {
				ArrayList<Integer> valid = new ArrayList<>();
				int maximum = requestedLength * 5 / 2;
				for (int i = 0; i < prefabs.size(); i++)
					if (!usedPrefabs.contains(i) && prefabs.get(i).notes().size() <= maximum) valid.add(i);
				if (!valid.isEmpty()) {
					int index = valid.get(random.nextInt(valid.size()));
					usedPrefabs.add(index);
					prefab = prefabs.get(index);
				}
			}
			rooms.add(prefab != null
					? new Room(prefab.notes(), prefab.playbackRate() * 10 / 8)
					: randomRoom(requestedLength, random));
		}
	}

	public static MusicPuzzleLayout create(int difficulty, long seed) {
		return new MusicPuzzleLayout(difficulty, new Random(seed));
	}

	public static int roomCount(int difficulty) {
		if (difficulty < 1 || difficulty > 3)
			throw new IllegalArgumentException("Crystal Music difficulty " + difficulty);
		return 4 + difficulty * 2;
	}

	public static int puzzleLength(int difficulty, int room) {
		return Math.max(6, 3 * difficulty + room - 9);
	}

	private static Room randomRoom(int length, Random random) {
		CrystalElement center = CrystalElement.elements[random.nextInt(CrystalElement.elements.length)];
		ArrayList<MusicKey> valid = new ArrayList<>(CrystalMusicManager.instance.getValidNotesToMixWith(center));
		// HashSet iteration was JVM-dependent upstream. Sorting preserves the same allowed-note set
		// while making a serialized structure seed stable across processes.
		Collections.sort(valid);
		ArrayList<MusicKey> notes = new ArrayList<>(length);
		MusicKey last = null;
		while (notes.size() < length) {
			MusicKey key = valid.get(random.nextInt(valid.size()));
			if (last != null) {
				int difference = key.ordinal() - last.ordinal();
				int interval = difference % 12;
				if (difference > 12 || difference < -12 || interval == 11 || interval == 6) continue;
			}
			notes.add(key);
			last = key;
		}
		return new Room(notes, 10);
	}

	public List<Room> rooms() { return Collections.unmodifiableList(rooms); }

	/** Playback delay is in ticks, after V33a's {@code rate * BASE_DELAY / 8}. */
	public record Room(List<MusicKey> melody, int playbackDelay) {
		public Room {
			Objects.requireNonNull(melody, "melody");
			// A null entry is an authored rest. List.copyOf rejects nulls, so freezing a prefab
			// with it would make Proxima layout generation fail nondeterministically whenever
			// the one-in-five prefab roll selected one of V33a's resting melodies.
			melody = Collections.unmodifiableList(new ArrayList<>(melody));
		}
	}
}
