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
 * <h2>The camera, and giving it back</h2>
 *
 * <p>V33a walks the camera around an epitrochoid about the monument, hiding the GUI and disabling view
 * bob for the duration. The orbit needs both halves of a camera override: the angles go through
 * {@code ViewportEvent.ComputeCameraAngles}, but the <em>position</em> is not exposed by any public API,
 * so it goes through {@code CameraAccessor}.
 *
 * <p>Everything borrowed is given back in {@link #stop()}, and every way a ritual can end routes through
 * it — the server saying so, the ceremony completing, and the client leaving the level. That last path
 * is the one that matters: a player who logs out mid-ritual would otherwise come back with no HUD.
 *
 * <p>Still absent: the two shader programs. Per this port's shader notes their sixteen per-frame core
 * positions and colours have to arrive as a texture rather than as uniforms.
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

	/**
	 * The options the ceremony borrows, captured when it starts. These are the reason
	 * {@link #stop()} must be reachable from every way a ritual can end and not only from completion:
	 * a player who logs out or dies mid-ceremony would otherwise be left with no GUI and no view bob.
	 */
	private Boolean restoreHideGui;
	private Boolean restoreBobbing;

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

	/**
	 * Stopped by MONUMENTEND or RESETMONUMENT, by leaving the level, and by the ritual completing.
	 * Safe to call when nothing is running, and safe to call twice.
	 */
	public static void stop() {
		if (active != null)
			active.restoreSettings();
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

		this.manipulateCamera(mc);
		this.stepTrack(mc.level);
		this.drawVortex(mc.level);
		this.drawRing(mc.level);
		this.fireDueEvents(mc.level);
	}

	/**
	 * V33a manipulateCamera, minus the orbit itself, which is applied per frame in
	 * {@link #applyCamera}. This half only borrows the settings the ceremony needs — and captures their
	 * previous values first, so they can be given back whatever ends the ritual.
	 */
	private void manipulateCamera(Minecraft mc) {
		// In 26.2 the HUD's hidden flag lives on Hud with only a toggle, not a setter, so the state is
		// reached by comparing and toggling rather than by assignment.
		if (restoreHideGui == null) {
			restoreHideGui = mc.gui.hud.isHidden();
			restoreBobbing = mc.options.bobView().get();
		}
		if (!mc.gui.hud.isHidden())
			mc.gui.hud.toggle();
		mc.options.bobView().set(false);
	}

	/** Unconditional, and called from {@link #stop()} so every ending path goes through it. */
	private void restoreSettings() {
		if (restoreHideGui == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden() != restoreHideGui)
			mc.gui.hud.toggle();
		mc.options.bobView().set(restoreBobbing);
		restoreHideGui = null;
		restoreBobbing = null;
	}

	/**
	 * V33a's camera orbit: an epitrochoid about the monument, which is what makes the ceremony read as
	 * a shot rather than a spin. R and r are 32 and 26 scaled by a quarter, d is 13 by a quarter by
	 * three quarters, and the height falls off with distance while breathing on a quarter-rate sine.
	 *
	 * <p>Applied at render time rather than on the tick, because a camera set once a tick judders.
	 */
	public static void applyCamera(net.minecraft.client.Camera camera, float partialTick,
			net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
		if (active == null)
			return;
		active.orbit(camera, partialTick, event);
	}

	private void orbit(net.minecraft.client.Camera camera, float partialTick,
			net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		double angle = (runTime / 100D + partialTick / 2D);
		double r = 26 * 1.25;
		double R = 32 * 1.25;
		double d = 13 * 1.25 * 0.75;
		double cx = pos.getX() + 0.5 + (R - r) * Math.cos(Math.toRadians(angle))
				+ d * Math.cos(Math.toRadians((R - r) / r * angle));
		double cz = pos.getZ() + 0.5 + (R - r) * Math.sin(Math.toRadians(angle))
				- d * Math.sin(Math.toRadians((R - r) / r * angle));
		double horizontal = Math.sqrt(Math.pow(cx - pos.getX() - 0.5, 2)
				+ Math.pow(cz - pos.getZ() - 0.5, 2));
		double cy = pos.getY() + 21 - 0.25 * horizontal + 4 * Math.sin(Math.toRadians(angle / 4));

		double dx = pos.getX() + 0.5 - cx;
		double dy = pos.getY() + 0.5 - cy;
		double dz = pos.getZ() + 0.5 - cz;
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float)-Math.toDegrees(Math.atan2(dy, flat));

		// The position needs the accessor; the angles go through the event, which is what the rest of
		// the render pipeline reads them from.
		((reika.dragonapi.mixin.CameraAccessor)camera)
				.dragonapi$setPosition(new net.minecraft.world.phys.Vec3(cx, cy, cz));
		event.setYaw(yaw);
		event.setPitch(pitch);
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
