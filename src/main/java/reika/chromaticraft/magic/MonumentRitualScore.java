package reika.chromaticraft.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

/**
 * V33a's monument ritual score: the melody, and the event schedule derived from it.
 *
 * <p>This is separated from the ritual itself because it is pure data and pure arithmetic — no world,
 * no side, no player — and because getting it wrong is silent. Every timing here is in milliseconds
 * against real elapsed time, not ticks, because the whole thing is synchronised to six recorded audio
 * tracks; see {@code MonumentCompletionRitual}.
 *
 * <h2>The schedule is not the melody</h2>
 *
 * <p>Each note produces a ray a quarter-second before its own beat, an effect chosen by the note's
 * length, and — if it opens a phrase — a burst of six particle rings at fifty-millisecond spacing.
 * Percussive and bell notes add their own. On top of that sit five vortex growths on fixed beats.
 *
 * <p>The nudges are the part that reads as musical rather than mechanical, and they are upstream's
 * exactly: a running {@code off} that shifts phrase openings by -250 then +150, a one-shot
 * {@code nextOff} after the long note at index 10, hard resets at 16 and 18, and three cumulative
 * drifts once the elapsed time crosses 85, 101 and 115 seconds. Inside Proxima every event is pulled
 * earlier — half a second for a phrase opening, nine tenths otherwise — which is what keeps it aligned
 * with the dimension's own longer audio.
 */
public final class MonumentRitualScore {

	/** V33a BEAT_LENGTH: two and a half seconds to the quarter, near enough. */
	public static final int BEAT_LENGTH = 2390;

	/** V33a SOUND_TIMINGS: where each of the six tracks begins, in milliseconds. */
	public static final long[] SOUND_TIMINGS = {0, 28000, 66500, 86000, 104800, 123500};

	/** V33a FINAL_SOUND_COMPLETION_DELAY, and the extra Proxima itself adds. */
	public static final int FINAL_SOUND_COMPLETION_DELAY = 20000;
	public static final int FINAL_SOUND_COMPLETION_DELAY_EXTRA = 4000;
	/** V33a COMPLETION_EXTRA: how long the completion pose is held before the point is placed. */
	public static final int COMPLETION_EXTRA = 3000;

	private static final List<RayNote> MELODY = new ArrayList<>();

	static {
		add(MusicKey.A4, 2, true, false, true);
		add(MusicKey.C5, 2);
		add(MusicKey.B4, 2);
		add(MusicKey.G4, 2);
		add(MusicKey.A4, 2);
		add(MusicKey.E4, 6, false, true, false);

		add(MusicKey.A4, 2, true, false, true);
		add(MusicKey.C5, 2);
		add(MusicKey.B4, 2);
		add(MusicKey.G5, 4, false, true, false);
		add(MusicKey.E5, 6);

		add(MusicKey.A5, 2, true, true, true);
		add(MusicKey.G5, 2);
		add(MusicKey.E5, 2);
		add(MusicKey.C5, 2);
		add(MusicKey.D5, 2);
		add(MusicKey.A4, 6, false, true, false);

		add(MusicKey.A4, 2, true, false, true);
		add(MusicKey.E5, 2);
		add(MusicKey.D5, 2);
		add(MusicKey.G4, 2);
		add(MusicKey.A4, 8, true, true, false);
	}

	private MonumentRitualScore() {}

	private static void add(MusicKey key, int length) {
		add(key, length, false, false, false);
	}

	private static void add(MusicKey key, int length, boolean percussion, boolean bell,
			boolean firstInPhrase) {
		// V33a derives each note's start from the running total of what came before it.
		int start = MELODY.isEmpty() ? 0
				: MELODY.get(MELODY.size() - 1).startBeat() + MELODY.get(MELODY.size() - 1).length();
		MELODY.add(new RayNote(key, length, start, percussion, bell, firstInPhrase));
	}

	public static List<RayNote> melody() {
		return Collections.unmodifiableList(MELODY);
	}

	/** One note of the ray melody. {@code length} is in quarters. */
	public record RayNote(MusicKey key, int length, int startBeat, boolean percussion, boolean bell,
			boolean firstInPhrase) {}

	public enum EventType {
		FLARES, PARTICLECLOUD, PARTICLERING, TWIRL, PINWHEEL, VORTEXGROW
	}

	/** A scheduled effect, at a millisecond offset from the ritual's start. */
	public record TimedEvent(EventType type, long millis, RayNote ray) implements Comparable<TimedEvent> {

		public TimedEvent(EventType type, long millis) {
			this(type, millis, null);
		}

		public boolean isRay() {
			return ray != null;
		}

		@Override
		public int compareTo(TimedEvent o) {
			return Long.compare(millis, o.millis);
		}
	}

	/**
	 * Builds the whole schedule, in order.
	 *
	 * @param inProxima whether the ritual is running in Proxima, which pulls every event earlier
	 */
	public static List<TimedEvent> buildSchedule(boolean inProxima) {
		List<TimedEvent> events = new ArrayList<>();
		events.add(new TimedEvent(EventType.FLARES, 0));

		long t0 = 100;
		long off = 0;
		int nextOff = 0;
		boolean past85 = false;
		boolean past101 = false;
		boolean past115 = false;

		List<RayNote> melody = MELODY;
		for (int idx = 0; idx < melody.size(); idx++) {
			RayNote n = melody.get(idx);
			// The effect a note draws is chosen by how long it is held.
			EventType e = n.length() >= 6 ? EventType.PARTICLECLOUD
					: n.length() >= 4 ? EventType.TWIRL : EventType.FLARES;

			if (n.firstInPhrase() && n.startBeat() > 0) {
				if (off == 0)
					off -= 250;
				else if (off == -250)
					off += 400;
				if (off == 150)
					nextOff = 200;
			}

			// Upstream's three hard corrections, by note index: the long non-bell note at 10, then a
			// reset in and out at 16 and 18.
			if (idx == 10)
				nextOff = 250;
			if (idx == 16)
				off = 250;
			if (idx == 18)
				off = 0;

			long t = t0 + off + nextOff;
			nextOff = 0;
			if (inProxima)
				t -= n.firstInPhrase() ? 500 : 900;

			events.add(new TimedEvent(e, t));
			// The ray leads its own note by a quarter second.
			events.add(new TimedEvent(e, t - 250, n));
			if (n.percussion() && n.startBeat() > 0)
				events.add(new TimedEvent(EventType.PINWHEEL, t));
			if (n.bell() && n.startBeat() > 0)
				events.add(new TimedEvent(EventType.TWIRL, t));
			if (n.firstInPhrase())
				for (int i = 0; i <= 250; i += 50)
					events.add(new TimedEvent(EventType.PARTICLERING, t + i));

			t0 += (long)n.length() * BEAT_LENGTH;
			// Three cumulative drifts, each applied once, as the piece passes its own landmarks.
			if (t0 >= 85000 && !past85) {
				t0 += 500;
				past85 = true;
			}
			if (t0 >= 101000 && !past101) {
				t0 += 750;
				past101 = true;
			}
			if (t0 >= 115000 && !past115) {
				t0 -= 1200;
				past115 = true;
			}
		}

		for (int beat : new int[] {12, 14, 24, 28, 44})
			events.add(new TimedEvent(EventType.VORTEXGROW, (long)beat * BEAT_LENGTH));

		Collections.sort(events);
		return events;
	}

	/** V33a isReadyToComplete: how long after the last track begins the ritual finishes. */
	public static long completionTime(boolean inProxima) {
		return SOUND_TIMINGS[SOUND_TIMINGS.length - 1] + FINAL_SOUND_COMPLETION_DELAY
				- (inProxima ? FINAL_SOUND_COMPLETION_DELAY_EXTRA : 0);
	}
}
