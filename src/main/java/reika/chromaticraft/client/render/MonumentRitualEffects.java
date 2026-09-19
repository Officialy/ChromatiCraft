package reika.chromaticraft.client.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

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
 * {@code ViewportEvent.ComputeCameraAngles}; position comes from a private client-only Marker used as
 * the camera entity. Moving Camera directly in that event does not work in 26.2 because
 * {@code Camera.alignWithEntity} assigns the entity position immediately after posting the event.
 *
 * <p>Everything borrowed is given back in {@link #stop()}, and every way a ritual can end routes through
 * it — the server saying so, the ceremony completing, and the client leaving the level. That last path
 * is the one that matters: a player who logs out mid-ritual would otherwise come back with no HUD.
 *
 * <p>The V33a general/chord screen shaders are combined into one 26.2 post pass. Its sixteen changing
 * core positions, colours and fades travel through a ring-buffered std140 uniform block; the post
 * chain itself remains declarative and only names the scene targets.
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
	private final List<SoundInstance> playingTracks = new ArrayList<>();
	private final List<ScheduledRay> scheduledRays = new ArrayList<>();

	private float vortexSize;
	private boolean vortexGrowing;

	/** V33a colorFade: how lit each element's core currently is, one per element. */
	private final float[] colorFade = new float[16];
	/** The key currently sounding, which decides which colours swell. */
	private reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey activeKey;

	/**
	 * The options the ceremony borrows, captured when it starts. These are the reason
	 * {@link #stop()} must be reachable from every way a ritual can end and not only from completion:
	 * a player who logs out or dies mid-ceremony would otherwise be left with no GUI and no view bob.
	 */
	private Boolean restoreHideGui;
	private Boolean restoreBobbing;
	private CameraType restoreCameraType;
	private Entity restoreCameraEntity;
	private Marker cameraAnchor;

	private MonumentRitualEffects(BlockPos pos, boolean inProxima) {
		this.pos = pos;
		this.inProxima = inProxima;
		this.pending = new ArrayList<>(MonumentRitualScore.buildSchedule(inProxima));
	}

	/** Started by the server's MONUMENTSTART. */
	public static void start(BlockPos pos, boolean inProxima) {
		// A repeated start packet must first return anything borrowed by the previous ceremony.
		stop();
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

	/** V33a writes {@code core.shaderScale = 1 + colorFade * 5} during the score. */
	public static float getDimensionCoreShaderScale(CrystalElement element) {
		return active == null ? 1F : 1F + active.colorFade[element.ordinal()] * 5F;
	}

	/**
	 * V33a MONUMENTCOMPLETE. This is deliberately not {@link #stop()}: the server retains the final
	 * shot for three more seconds before replacing the controller and sending the stopped state.
	 */
	public static void complete(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		ChromaParticle.spawnMonumentCompletion(mc.level, pos);
		if (active != null && active.pos.equals(pos)) {
			active.activeKey = null;
			active.scheduledRays.clear();
		}
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

		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			restoreCameraEntity = mc.getCameraEntity();
			cameraAnchor = new Marker(EntityTypes.MARKER, mc.level);
			CameraPose pose = this.cameraPose(0);
			cameraAnchor.absSnapTo(pose.position.x, pose.position.y, pose.position.z,
					pose.yaw, pose.pitch);
			mc.setCameraEntity(cameraAnchor);
			// V33a explicitly stops the dimension music before beginning its first score segment.
			mc.getMusicManager().stopPlaying();
			// startClient() calls stepSound() immediately; do not make the first recording wait for the
			// next client tick (which is noticeable if activation lands on a stalled frame).
			this.stepTrack(mc.level);
		}
	}

	private void tickEffects() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			stop();
			return;
		}
		long time = System.currentTimeMillis();
		// Client ticks continue while an integrated game is sitting in a pausing screen. Vanilla
		// pauses sounds which already exist, but any sound started from one of those later client ticks
		// begins after SoundManager's pause pass and can therefore be heard over the menu. Freeze the
		// complete ceremony clock here: no camera/event/particle work and, crucially, no newly scheduled
		// lightning or score sounds. Account for the whole pause so resuming cannot catch the timeline up.
		if (mc.isPaused()) {
			pauseTotal += Math.max(0, time - lastTickTime);
			lastTickTime = time;
			return;
		}
		long step = time - lastTickTime;
		// Same correction the server makes: a dropped frame delays the ceremony rather than skipping it.
		if (step > 50)
			pauseTotal += step - 50;
		runTime = time - (startTime + pauseTotal);
		lastTickTime = time;
		tick++;

		this.manipulateCamera(mc);
		// Detaching the camera already prevents LocalPlayer from applying keyboard movement. Clear any
		// momentum which existed before the packet as well; the server independently holds the ritual
		// owner at the activation point, so neither side can drift.
		mc.player.setDeltaMovement(Vec3.ZERO);
		mc.player.setSprinting(false);
		// Do not let vanilla/dimension music restart under the six-part monument recording.
		mc.getMusicManager().stopPlaying();
		this.updateColorFade();
		this.stepTrack(mc.level);
		this.drawVortex(mc.level);
		this.drawRing(mc.level);
		this.fireDueEvents(mc.level);
		this.fireScheduledRays(mc.level);
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
			restoreCameraType = mc.options.getCameraType();
		}
		if (!mc.gui.hud.isHidden())
			mc.gui.hud.toggle();
		mc.options.bobView().set(false);
		mc.options.setCameraType(CameraType.FIRST_PERSON);
	}

	/** Unconditional, and called from {@link #stop()} so every ending path goes through it. */
	private void restoreSettings() {
		Minecraft mc = Minecraft.getInstance();
		for (SoundInstance sound : playingTracks)
			mc.getSoundManager().stop(sound);
		playingTracks.clear();
		scheduledRays.clear();

		if (cameraAnchor != null && mc.getCameraEntity() == cameraAnchor) {
			Entity camera = restoreCameraEntity;
			if (camera == null || camera.level() != mc.level || camera.isRemoved())
				camera = mc.player;
			mc.setCameraEntity(camera);
		}
		cameraAnchor = null;
		restoreCameraEntity = null;

		if (restoreHideGui != null) {
			if (mc.gui.hud.isHidden() != restoreHideGui)
				mc.gui.hud.toggle();
			mc.options.bobView().set(restoreBobbing);
			mc.options.setCameraType(restoreCameraType);
		}
		restoreHideGui = null;
		restoreBobbing = null;
		restoreCameraType = null;
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
		if (mc.player == null || cameraAnchor == null)
			return;
		if (mc.getCameraEntity() != cameraAnchor)
			mc.setCameraEntity(cameraAnchor);
		CameraPose pose = this.cameraPose(partialTick);

		/*
		 * ComputeCameraAngles is posted inside Camera.alignWithEntity immediately before vanilla assigns
		 * the camera entity's interpolated position. Moving Camera itself here is therefore overwritten
		 * later in the same method. Move the private client-only view entity instead: the assignment which
		 * follows this event then installs the orbit position, while the real player remains untouched.
		 */
		cameraAnchor.absSnapTo(pose.position.x, pose.position.y, pose.position.z,
				pose.yaw, pose.pitch);
		event.setYaw(pose.yaw);
		event.setPitch(pose.pitch);
	}

	private CameraPose cameraPose(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		// Exact V33a phase: half the world tick plus the activating player's UUID-derived offset.
		double angle = (mc.level.getGameTime() + mc.player.getUUID().hashCode() % 8000 + partialTick) / 2D;
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

		return new CameraPose(new Vec3(cx, cy, cz), yaw, pitch);
	}

	private record CameraPose(Vec3 position, float yaw, float pitch) {}

	/**
	 * The ritual's screen effect: V33a's {@code general.frag} grade plus {@code chords.frag}'s core
	 * glow, resolved in one pass.
	 *
	 * <p>The sixteen cores are projected to screen space here rather than in the shader, because a
	 * PostChain bakes declared uniforms when the chain compiles and these change every frame — the same
	 * constraint, and the same answer, as RotaryCraft's heat ripple.
	 *
	 * <p>Each core's alpha is its own fade: a colour swells while its note is sounding and decays
	 * afterwards, which is what makes the ring answer the music rather than pulse with it.
	 */
	public static void renderScreenEffect(org.joml.Matrix4fc modelView,
			org.joml.Matrix4fc projection, net.minecraft.world.phys.Vec3 eye) {
		if (active != null)
			active.grade(modelView, projection, eye);
	}

	/** V33a's intensity ramp: the grade fades in over the opening and holds for the ceremony. */
	private float gradeIntensity() {
		return (float)Math.min(1, runTime / 8000D);
	}

	/**
	 * The per-core fade V33a keeps in {@code colorFade}: a colour rises while its key is sounding and
	 * falls away at a third of that rate, so the ring answers each note and then lets it go.
	 */
	private void updateColorFade() {
		var sounding = activeKey == null ? null
				: reika.chromaticraft.auxiliary.CrystalMusicManager.instance
						.getColorsWithKeyAnyOctave(activeKey);
		for (int i = 0; i < colorFade.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			boolean lit = sounding != null && sounding.contains(e);
			colorFade[i] = lit ? Math.min(1, colorFade[i] + 0.04F) : Math.max(0, colorFade[i] - 0.015F);
		}
	}

	private static final int MAX_CORES = 16;
	/** std140: ivec4 CoreCount, vec4 Intensity, then vec4 Focus[16] and vec4 CoreColor[16]. */
	private static final int UBO_SIZE = 16 + 16 + MAX_CORES * 16 * 2;
	private static final int UBO_USAGE = com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE
			| com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM;
	private static final net.minecraft.resources.Identifier EFFECT_ID =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(
					reika.chromaticraft.ChromatiCraft.MODID, "monument");
	private static final net.minecraft.resources.Identifier GRADED_TARGET_ID =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(
					reika.chromaticraft.ChromatiCraft.MODID, "monument_graded");
	private static final java.util.Set<net.minecraft.resources.Identifier> ALLOWED_TARGETS =
			java.util.Set.of(net.minecraft.client.renderer.PostChain.MAIN_TARGET_ID, GRADED_TARGET_ID);

	private static net.minecraft.client.renderer.MappableRingBuffer coreUbo;

	private void grade(org.joml.Matrix4fc modelView, org.joml.Matrix4fc projection,
			net.minecraft.world.phys.Vec3 eye) {
		Minecraft mc = Minecraft.getInstance();
		var chain = mc.getShaderManager().getPostChain(EFFECT_ID, ALLOWED_TARGETS);
		if (chain == null || mc.player == null)
			return;
		com.mojang.blaze3d.pipeline.RenderTarget main = mc.gameRenderer.mainRenderTarget();
		int width = main.width;
		int height = main.height;

		this.uploadCores(modelView, projection, eye);

		// The pass cannot read and write the main target at once, so the grade goes into an offscreen
		// target and the chain blits it back -- the shape RotaryCraft's heat ripple established.
		var frame = new com.mojang.blaze3d.framegraph.FrameGraphBuilder();
		var mainHandle = frame.importExternal("main", main);
		var gradedHandle = frame.createInternal("chromaticraft_monument_graded",
				new com.mojang.blaze3d.resource.RenderTargetDescriptor(width, height, false,
						new org.joml.Vector4f(0, 0, 0, 0), com.mojang.blaze3d.GpuFormat.RGBA8_UNORM));
		var pass = frame.addPass("chromaticraft_monument_grade");
		pass.reads(mainHandle);
		var gradedOut = pass.readsAndWrites(gradedHandle);
		pass.executes(() -> drawGrade(mainHandle.get(), gradedOut.get()));
		chain.addToFrame(frame, width, height, new GradeTargetBundle(mainHandle, gradedOut));
		frame.execute(com.mojang.blaze3d.resource.GraphicsResourceAllocator.UNPOOLED);
		coreUbo.rotate();
	}

	/**
	 * Projects each core to screen space and uploads the set. A core behind the near plane is dropped
	 * rather than clamped: a glow anchored to a point behind the camera would smear across the frame.
	 */
	private void uploadCores(org.joml.Matrix4fc modelView, org.joml.Matrix4fc projection,
			net.minecraft.world.phys.Vec3 eye) {
		if (coreUbo == null)
			coreUbo = new net.minecraft.client.renderer.MappableRingBuffer(
					() -> "ChromatiCraft MonumentCores", UBO_USAGE, UBO_SIZE);

		java.util.List<float[]> focus = new ArrayList<>();
		java.util.List<float[]> colors = new ArrayList<>();
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			if (colorFade[i] <= 0)
				continue;
			var offset = reika.chromaticraft.tileentity.technical.TileEntityDimensionCore.getLocation(e);
			double cx = pos.getX() + offset.getX() + 0.5;
			double cy = pos.getY() + offset.getY() + 0.5;
			double cz = pos.getZ() + offset.getZ() + 0.5;
			org.joml.Vector4f v = new org.joml.Vector4f((float)(cx - eye.x), (float)(cy - eye.y),
					(float)(cz - eye.z), 1);
			v.mul(modelView);
			v.mul(projection);
			if (v.w <= 1.0E-4F)
				continue;
			focus.add(new float[] {v.x / v.w * 0.5F + 0.5F, v.y / v.w * 0.5F + 0.5F,
					(float)Math.max(0.01, eye.distanceToSqr(cx, cy, cz)), colorFade[i] * 0.7F});
			int rgb = e.getColor();
			colors.add(new float[] {((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F,
					(rgb & 0xFF) / 255F});
		}

		try (var view = coreUbo.currentBuffer().map(false, true)) {
			var builder = com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(view.data());
			builder.putIVec4(focus.size(), 0, 0, 0);
			builder.putVec4(this.gradeIntensity(), 0, 0, 0);
			for (float[] f : focus)
				builder.putVec4(f[0], f[1], f[2], f[3]);
			// Padded: CoreColor starts at a fixed offset regardless of how many cores are lit.
			for (int i = focus.size(); i < MAX_CORES; i++)
				builder.putVec4(0, 0, 0, 0);
			for (float[] c : colors)
				builder.putVec4(c[0], c[1], c[2], 0);
		}
	}

	private static void drawGrade(com.mojang.blaze3d.pipeline.RenderTarget source,
			com.mojang.blaze3d.pipeline.RenderTarget target) {
		var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
		try (var pass = encoder.createRenderPass(() -> "ChromatiCraft monument grade",
				target.getColorTextureView(), java.util.Optional.empty())) {
			pass.setPipeline(reika.chromaticraft.render.ChromaRenderPipelines.MONUMENT_GRADE);
			com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("MonumentCores", coreUbo.currentBuffer());
			pass.bindTexture("InSampler", source.getColorTextureView(),
					com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
							.getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	/** Supplies the chain with the main target plus our graded scene. */
	private record GradeTargetBundle(
			com.mojang.blaze3d.resource.ResourceHandle<com.mojang.blaze3d.pipeline.RenderTarget> main,
			com.mojang.blaze3d.resource.ResourceHandle<com.mojang.blaze3d.pipeline.RenderTarget> graded)
			implements net.minecraft.client.renderer.PostChain.TargetBundle {

		@Override
		public void replace(net.minecraft.resources.Identifier id,
				com.mojang.blaze3d.resource.ResourceHandle<com.mojang.blaze3d.pipeline.RenderTarget> handle) {
		}

		@Override
		public com.mojang.blaze3d.resource.ResourceHandle<com.mojang.blaze3d.pipeline.RenderTarget> get(
				net.minecraft.resources.Identifier id) {
			return id.equals(net.minecraft.client.renderer.PostChain.MAIN_TARGET_ID) ? main : graded;
		}
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
		SoundInstance sound = this.playClientSound(track, 1, 1);
		playingTracks.add(sound);
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
		activeKey = e.ray().key();
		var colors = reika.chromaticraft.auxiliary.CrystalMusicManager.instance
				.getColorsWithKeyAnyOctave(e.ray().key());
		if (colors == null)
			return;
		float pitch = (float)reika.chromaticraft.auxiliary.CrystalMusicManager.instance
				.getPitchFactor(e.ray().key());
		int lastTick = Math.max(10,
				MonumentRitualScore.BEAT_LENGTH * e.ray().length() / 50 - 15);
		for (CrystalElement color : colors) {
			int delay = 10 + level.getRandom().nextInt(lastTick - 10 + 1);
			scheduledRays.add(new ScheduledRay(runTime + delay * 50L, color, pitch));
		}
	}

	/** V33a ScheduledRayEvent: each compatible core answers at a random point during the note. */
	private void fireScheduledRays(ClientLevel level) {
		for (Iterator<ScheduledRay> it = scheduledRays.iterator(); it.hasNext();) {
			ScheduledRay ray = it.next();
			if (ray.millis > runTime)
				continue;
			ChromaParticle.spawnMonumentRay(level, pos, ray.color);
			float volume = 0.4F + level.getRandom().nextFloat() * 0.4F;
			this.playClientSound(ChromaSounds.MONUMENTRAY, volume, ray.pitch);
			it.remove();
		}
	}

	private SoundInstance playClientSound(ChromaSounds sound, float volume, float pitch) {
		SoundInstance instance = new SimpleSoundInstance(
				sound.getSoundEvent().location(), sound.getCategory(), volume, pitch,
				SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, false);
		Minecraft.getInstance().getSoundManager().play(instance);
		return instance;
	}

	private record ScheduledRay(long millis, CrystalElement color, float pitch) {}

	private void fireEvent(ClientLevel level, EventType type) {
		switch (type) {
			case VORTEXGROW -> vortexGrowing = true;
			default -> ChromaParticle.spawnMonumentEvent(level, pos, type.ordinal());
		}
	}
}
