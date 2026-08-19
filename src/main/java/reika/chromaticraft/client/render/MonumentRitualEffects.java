package reika.chromaticraft.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.magic.MonumentRitualScore;
import reika.chromaticraft.magic.MonumentRitualScore.EventType;
import reika.chromaticraft.magic.MonumentRitualScore.TimedEvent;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * The client half of the monument completion ritual: everything the server timeline deliberately does
 * not touch.
 *
 * <p>The two halves never share an object. The server runs its own copy of the same score and decides
 * when the ceremony ends; this runs a second copy purely to know what to draw and when, started by a
 * packet and stopped by one. That means the visuals cannot hold the ceremony up and a stalled client
 * cannot desynchronise it — the price is that both sides read the same clock, which is why
 * {@link MonumentRitualScore} is wall-clock and not tick-based.
 *
 * <p>The pause correction is here too, and for the same reason as on the server: a client that drops
 * frames should have the ceremony wait for it rather than skip through its own music.
 *
 * <h2>What each effect is</h2>
 *
 * <ul>
 * <li><b>The vortex</b> — twelve blurs a tick on a ring below the monument, each drifting inward and
 *     up, the ring turning with the wall clock. It breathes: it grows to full over the opening, and
 *     each {@code VORTEXGROW} in the score doubles it before it eases back.</li>
 * <li><b>The ring</b> — a circle of thirty-two blocks' radius at the monument's shoulder height, its
 *     points lit three at a time and travelling round, so light appears to run the circumference.</li>
 * <li><b>The rays</b> — one per note, thrown from the core whose colour can sound that note.</li>
 * </ul>
 *
 * <h2>Not yet carried</h2>
 *
 * <p>The camera. V33a walks it around an epitrochoid — {@code R = 40, r = 32.5, d = 12.19} — through
 * {@code ReikaRenderHelper.setCameraPosition}, hides the GUI and disables view bob for the duration.
 * That needs a 26.2 camera-override hook, and with it must come an <em>unconditional</em> restore of
 * both settings: on logout, on death and on server stop, not only on completion, or a player who leaves
 * mid-ritual is left with no GUI. Nothing here touches those settings, so nothing here can strand them.
 *
 * <p>The two shader programs are also absent; per this port's shader notes their sixteen per-frame
 * core positions and colours have to arrive as a texture rather than as uniforms.
 */
public final class MonumentRitualEffects {

	private static MonumentRitualEffects active;

	private final BlockPos pos;
	private final boolean inProxima;
	private final List<TimedEvent> pending;

	private long startTime;
	private long lastTickTime;
	private long pauseTotal;
	private long runTime;
	private int tick;

	private int currentTrack = -1;
	private long nextTrackTime;

	private float vortexSize;
	private boolean vortexGrowing;

	private MonumentRitualEffects(BlockPos pos, boolean inProxima) {
		this.pos = pos;
		this.inProxima = inProxima;
		this.pending = new ArrayList<>(MonumentRitualScore.buildSchedule(inProxima));
	}

	/** Started by the server's MONUMENTSTART. */
	public static void start(BlockPos pos, boolean inProxima) {
		active = new MonumentRitualEffects(pos, inProxima);
		active.begin();
	}

	/** Stopped by MONUMENTEND or RESETMONUMENT; safe to call when nothing is running. */
	public static void stop() {
		active = null;
	}

	public static boolean isRunning() {
		return active != null;
	}

	/** Called once a client tick. */
	public static void tickClient() {
		if (active != null)
			active.tickEffects();
	}

	private void begin() {
		startTime = System.currentTimeMillis();
		lastTickTime = startTime;
		pauseTotal = 0;
		runTime = 0;
		tick = 0;
		currentTrack = -1;
		nextTrackTime = 0;
		vortexSize = 0;
		vortexGrowing = false;
	}

	private void tickEffects() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			stop();
			return;
		}
		long time = System.currentTimeMillis();
		long step = time - lastTickTime;
		// Same correction the server makes: a dropped frame delays the ceremony rather than skipping it.
		if (step > 50)
			pauseTotal += step - 50;
		runTime = time - (startTime + pauseTotal);
		lastTickTime = time;
		tick++;

		this.stepTrack(mc.level);
		this.drawVortex(mc.level);
		this.drawRing(mc.level);
		this.fireDueEvents(mc.level);
	}

	/**
	 * V33a stepSound: the six tracks are started in turn, each at its own offset from the ritual's
	 * start, so they play as one continuous piece rather than being cued off each other's length.
	 */
	private void stepTrack(ClientLevel level) {
		if (runTime < nextTrackTime)
			return;
		int next = currentTrack + 1;
		ChromaSounds track = ChromaSounds.monumentTrack(next);
		if (track == null) {
			nextTrackTime = Long.MAX_VALUE;
			return;
		}
		track.playSound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 1);
		currentTrack = next;
		nextTrackTime = next + 1 >= MonumentRitualScore.SOUND_TIMINGS.length ? Long.MAX_VALUE
				: MonumentRitualScore.SOUND_TIMINGS[next + 1];
	}

	/**
	 * V33a doVortexFX. The size curve is upstream's: up to one over the opening, doubled by each growth
	 * event, then eased back down multiplicatively rather than linearly so it settles rather than snaps.
	 */
	private void drawVortex(ClientLevel level) {
		ChromaParticle.spawnMonumentVortex(level, pos, vortexSize);
		if (vortexSize < 1)
			vortexSize = Math.min(1, vortexSize + 0.02F);
		else if (vortexGrowing) {
			vortexSize = Math.min(2, vortexSize + 0.05F);
			vortexGrowing = vortexSize < 2;
		}
		else if (vortexSize > 1)
			vortexSize = Math.max(1, vortexSize * 0.998F - 0.01F);
	}

	/** V33a doRingFX: three lit points travelling round a ring of a hundred and eighty. */
	private void drawRing(ClientLevel level) {
		ChromaParticle.spawnMonumentRing(level, pos, tick);
	}

	private void fireDueEvents(ClientLevel level) {
		while (!pending.isEmpty() && pending.get(0).millis() <= runTime) {
			TimedEvent e = pending.remove(0);
			if (e.isRay())
				this.fireRay(level, e);
			else
				this.fireEvent(level, e.type());
		}
	}

	/**
	 * V33a doRays: the note is thrown from whichever cores can sound it, so the monument answers its own
	 * music in the colours that music is written in.
	 */
	private void fireRay(ClientLevel level, TimedEvent e) {
		var colors = reika.chromaticraft.auxiliary.CrystalMusicManager.instance
				.getColorsWithKeyAnyOctave(e.ray().key());
		if (colors == null)
			return;
		for (CrystalElement color : colors)
			ChromaParticle.spawnMonumentRay(level, pos, color);
		ChromaSounds.MONUMENTRAY.playSound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				1, 1);
	}

	private void fireEvent(ClientLevel level, EventType type) {
		switch (type) {
			case VORTEXGROW -> vortexGrowing = true;
			default -> ChromaParticle.spawnMonumentEvent(level, pos, type.ordinal());
		}
	}
}
