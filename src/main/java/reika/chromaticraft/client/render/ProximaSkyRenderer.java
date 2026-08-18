package reika.chromaticraft.client.render;

import java.util.Random;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;

import org.joml.Matrix4fc;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code ChromaSkyRenderer}: Proxima's sky.
 *
 * <p>Proxima has no sun, no moon and no horizon glow. Its dimension type declares {@code Skybox.NONE},
 * so vanilla draws nothing and this draws everything: a field of five to seven and a half thousand
 * stars, drifting nebulae, and a scattering of planets, all billboarded onto a sphere around the viewer.
 *
 * <h2>The parts that carry the feel</h2>
 *
 * <ul>
 * <li><b>The star count breathes.</b> {@code 5000 + 2500*sin(t/24000)}, so the field slowly thickens
 *     and thins rather than being a fixed backdrop.</li>
 * <li><b>Each star twinkles on its own clock,</b> at a speed between 0.125 and 4 with an amplitude
 *     between 0.0625 and 0.375, offset by its own index so they never pulse together.</li>
 * <li><b>The whole sky turns with the viewer.</b> An eighth of a degree per block on each axis, plus a
 *     slow spin with time — standing still the sky rotates, walking, it wheels.</li>
 * <li><b>It fades in with altitude.</b> Nothing below y 18, full by y 30, and immediately full if the
 *     viewer can see the sky, so a player underground is not looking at stars through stone.</li>
 * </ul>
 *
 * <p>Placement is vanilla's own star-billboard construction, which is what upstream copied: a random
 * direction on the unit sphere, rejected unless its squared length lands between 0.01 and 1, then a
 * rotation basis built from its two spherical angles and a random roll.
 *
 * <h2>What is not here</h2>
 *
 * <p>Upstream also keeps up to thirty supernovae alive at once, each an animated sprite advancing
 * through frames and holding at its midpoint before expiring. That animation is driven from a sheet
 * with its own per-frame timing and is a piece of work in itself, so the field is drawn without them.
 *
 * <h2>How this is reached</h2>
 *
 * <p>Through NeoForge's {@link CustomSkyboxRenderer} hook, named by the {@code neoforge:custom_skybox}
 * environment attribute on Proxima's dimension type, and <em>not</em> through
 * {@code RenderLevelStageEvent.AfterSky}. Both of those live inside the sky frame pass, and that pass
 * is only added when the dimension's skybox is not {@code NONE}. Proxima declaring {@code NONE} — the
 * obvious reading of "vanilla draws no sky here" — meant neither ever ran. Returning true from
 * {@link #renderSky} is what actually suppresses vanilla's sun, moon, stars and horizon.
 */
public final class ProximaSkyRenderer implements CustomSkyboxRenderer {

	public static final ProximaSkyRenderer INSTANCE = new ProximaSkyRenderer();

	private static final Identifier STARS = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_stars.png");
	private static final Identifier NEBULAE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_nebulae.png");
	private static final Identifier PLANETS = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_planets.png");

	/** V33a BASE_STARS and STARS_VARIATION. */
	private static final int BASE_STARS = 5000;
	private static final int STARS_VARIATION = 2500;

	// Sheet sizes in pixels, needed only to inset each tile's UVs by half a texel. V33a's stars.png,
	// stars2.png and planets2.png, carried over unchanged; the nebula sheet's last six tiles are blank,
	// which upstream also draws and relies on the alpha test to throw away.
	private static final int STARS_SHEET = 128;
	private static final int NEBULA_SHEET = 512;
	private static final int PLANET_SHEET = 140;

	/** V33a's star palette. */
	private static final int[] STAR_COLOURS = {
			0xFFFFFF, 0xFFF4EA, 0xFFD2A1, 0xFFCC6F, 0xFFC46F, 0xAABFFF, 0xCAD7FF, 0xF8F7FF};

	private static final Star[] STAR_FIELD = new Star[BASE_STARS + STARS_VARIATION];
	private static final SkyQuad[] NEBULA_FIELD = new SkyQuad[16];
	private static final SkyQuad[] PLANET_FIELD = new SkyQuad[32];

	/** The furthest anything in the field reaches: the nebula and planet shells, plus their spread. */
	private static final float SKY_RADIUS = 420;
	/** How much of the far plane the sky is allowed to occupy once it has to be pulled in. */
	private static final float SKY_DEPTH_HEADROOM = 0.9F;

	// One pass per field rather than one shared between them. Each holds its own vertex buffer, so the
	// three batches of a frame never write over each other's geometry between draws, and each shows up
	// under its own name in a GPU capture.
	private static final WorldGeometryPass NEBULA_PASS = new WorldGeometryPass("ChromatiCraft Proxima nebulae");
	private static final WorldGeometryPass PLANET_PASS = new WorldGeometryPass("ChromatiCraft Proxima planets");
	private static final WorldGeometryPass STAR_PASS = new WorldGeometryPass("ChromatiCraft Proxima stars");

	static {
		// Built once from a fixed sequence, so Proxima's sky is the same every session rather than
		// reshuffling itself on each world load.
		Random random = new Random(10842L);
		for (int i = 0; i < STAR_FIELD.length; i++) {
			int colour = STAR_COLOURS[random.nextInt(STAR_COLOURS.length)];
			double twinkleSpeed = 0.125 + random.nextDouble() * (4 - 0.125);
			double twinkleAmplitude = 0.0625 + random.nextDouble() * (0.375 - 0.0625);
			STAR_FIELD[i] = Star.randomized(colour, random.nextInt(16), twinkleSpeed,
					twinkleAmplitude, i, random);
		}
		for (int i = 0; i < NEBULA_FIELD.length; i++)
			NEBULA_FIELD[i] = SkyQuad.randomized(i, 4, NEBULA_SHEET, 175, random);
		for (int i = 0; i < PLANET_FIELD.length; i++)
			PLANET_FIELD[i] = SkyQuad.randomized(i, 2, PLANET_SHEET, 30, random);
	}

	/**
	 * Which of the ways this can show nothing has already been reported. A sky that fails silently is
	 * indistinguishable from a sky that is simply not drawn, which is exactly what cost this fault
	 * several sessions of static analysis; each cause says so once rather than every frame.
	 */
	private static final java.util.Set<String> reported = new java.util.HashSet<>();

	private ProximaSkyRenderer() {}

	private static void reportOnce(String cause, String detail) {
		if (reported.add(cause))
			ChromatiCraft.LOGGER.info("Proxima sky: {} ({})", cause, detail);
	}

	@Override
	public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState,
			Matrix4fc modelViewMatrix, Runnable setupFog) {
		setupFog.run();
		reportOnce("reached", "the custom skybox hook is selected and firing");
		// True is returned on every path below, including the ones that draw nothing: this renderer is
		// selected for Proxima and Proxima has no sun or moon, so falling through to vanilla when the
		// viewer is underground would put a sun in the sky through the stone.
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return true;

		double y = minecraft.player.getY();
		float fade = y <= 18 ? 0 : y >= 30 ? 1 : (float)((y - 18) / 12F);
		if (minecraft.level.canSeeSky(minecraft.player.blockPosition()))
			fade = 1;
		if (fade <= 0) {
			reportOnce("faded out", "at y " + (int)y + " with no view of the sky, so nothing is drawn");
			return true;
		}

		double time = System.currentTimeMillis();
		var camera = levelRenderState.cameraRenderState.pos;
		// V33a turns the sky by an eighth of a degree per block travelled on each axis, then adds a
		// slow spin with time. The sky is drawn around the viewer, so no camera translation is applied.
		org.joml.Matrix4f matrix = new org.joml.Matrix4f(modelViewMatrix);
		// The sky is built at a fixed radius, and the far plane is not fixed: it is four times the
		// render distance, so at a short render distance the whole field would sit behind it and be
		// clipped away. Scaling the sky uniformly pulls it inside without changing how it looks --
		// every vertex, centre offset and corner alike, scales by the same factor, so the angular size
		// of each star is untouched. Vanilla sidesteps this by drawing its own stars at radius 100.
		float depthFar = levelRenderState.cameraRenderState.depthFar;
		if (depthFar > 0 && depthFar < SKY_RADIUS / SKY_DEPTH_HEADROOM)
			matrix.scale(depthFar * SKY_DEPTH_HEADROOM / SKY_RADIUS);
		// The planets do not turn with the viewer. Upstream calls renderPlanets from its own
		// push/popMatrix in render(), outside the block that applies the position spin, and calls
		// renderNebulae from inside renderStars after it -- so stars and nebulae wheel as you walk and
		// the planets keep their own slow orbits regardless. Hence two matrices.
		org.joml.Matrix4f planetMatrix = new org.joml.Matrix4f(matrix);
		matrix.rotateX((float)Math.toRadians(camera.x * 0.125));
		matrix.rotateY((float)Math.toRadians(camera.y * 0.125));
		matrix.rotateZ((float)Math.toRadians(camera.z * 0.125));
		matrix.rotateZ((float)Math.toRadians(time / 12000D % 360D));

		final float alpha = fade;
		int washColour = ReikaColorAPI.GStoHex((int)(255 * alpha));
		NEBULA_PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, NEBULAE, matrix, buffer -> {
			for (int i = 0; i < NEBULA_FIELD.length; i++)
				if (NEBULA_FIELD[i] != null)
					NEBULA_FIELD[i].emit(buffer, 380, washColour, nebulaOrientation(i));
		});
		PLANET_PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, PLANETS, planetMatrix, buffer -> {
			for (int i = 0; i < PLANET_FIELD.length; i++)
				if (PLANET_FIELD[i] != null)
					PLANET_FIELD[i].emit(buffer, 380, washColour, planetOrientation(i, time));
		});
		STAR_PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, STARS, matrix, buffer -> {
			int count = starCount(time);
			for (int i = 0; i < count; i++) {
				Star star = STAR_FIELD[i];
				if (star == null)
					continue;
				// V33a dims the stars nearest the end of the list, so the field thins at its edge
				// rather than new stars appearing at full brightness as the count breathes.
				float brightness = (float)Math.min(count - i, 24 * alpha);
				star.emitStar(buffer, 320 - i / 10D / count, brightness, time);
			}
		});
		reportOnce("drawing", starCount(time) + " stars at y " + (int)y);
		return true;
	}

	/**
	 * V33a's per-nebula {@code glRotate} chain. It is fixed, so the nebulae hang where they are put;
	 * without it the whole field sits wherever the shared construction seed happened to place it, which
	 * is a much smaller patch of sky than upstream's.
	 */
	private static org.joml.Matrix4f nebulaOrientation(int index) {
		return new org.joml.Matrix4f()
				.rotateX((float)Math.toRadians(index * 8))
				.rotateY((float)Math.toRadians(index % 4 * 32))
				.rotateZ((float)Math.toRadians(-index * 15 + 90));
	}

	/**
	 * V33a's per-planet chain, which unlike the nebulae's advances with time on all three axes at three
	 * very different rates -- a quarter-minute, a sixteenth of that, and one so slow it is measured in
	 * days. Planets drift across Proxima's sky; they do not hang in it.
	 */
	private static org.joml.Matrix4f planetOrientation(int index, double time) {
		int sub = index % 4;
		double x = index * 8 + time / 4000D % 360D + sub * 90D * Math.signum(index % 2 - 0.5);
		double y = sub / 2 * 180 + time / 16000D % 360D - sub * 30D;
		double z = index % 4 * 60 + time / 240000D % 360D * (1 + index % 2);
		return new org.joml.Matrix4f()
				.rotateX((float)Math.toRadians(x))
				.rotateY((float)Math.toRadians(y))
				.rotateZ((float)Math.toRadians(z));
	}

	/** V33a getStarCount: the field breathes between 2500 and 7500 over a day. */
	private static int starCount(double time) {
		return BASE_STARS + (int)(STARS_VARIATION * Math.sin(time / 24000D));
	}

	/**
	 * One billboarded sprite on the sky sphere. The construction is vanilla's own star maths, which is
	 * what upstream built on: a random unit-sphere direction, and a rotation basis from its two
	 * spherical angles plus a random roll.
	 */
	private static class SkyQuad {

		protected final double dirX;
		protected final double dirY;
		protected final double dirZ;
		protected final double spread;
		protected final double sinYaw;
		protected final double cosYaw;
		protected final double sinPitch;
		protected final double cosPitch;
		protected final double roll;
		protected final double size;
		protected final float[] u = new float[4];
		protected final float[] v = new float[4];

		protected SkyQuad(int textureIndex, int rowWidth, int sheetPixels, double size, double[] basis) {
			this.size = size;
			this.dirX = basis[0];
			this.dirY = basis[1];
			this.dirZ = basis[2];
			this.spread = basis[3];
			this.sinYaw = basis[4];
			this.cosYaw = basis[5];
			this.sinPitch = basis[6];
			this.cosPitch = basis[7];
			this.roll = basis[8];
			int frames = rowWidth * rowWidth;
			int index = Math.floorMod(textureIndex, frames);
			float du = (index % rowWidth) / (float)rowWidth;
			float dv = (index / rowWidth) / (float)rowWidth;
			float step = 1F / rowWidth;
			// Half a texel in from every side. Upstream's coordinates are the tile's exact bounds, which
			// is fine under 1.7.10's nearest sampling but not here: filtering at a shared edge blends
			// the neighbouring tile in, and on the nebula sheet the neighbour is often the blank corner
			// of the sheet -- opaque white with zero alpha. The blend produces a partly-transparent
			// white fringe, which survives the shader's `discard` on exactly zero alpha and draws as a
			// bright outline. That outline is the visible box; the tiles themselves fade to black at
			// their own edges, so with the inset there is nothing to see.
			float inset = 0.5F / sheetPixels;
			u[0] = du + inset;
			u[1] = du + inset;
			u[2] = du + step - inset;
			u[3] = du + step - inset;
			v[0] = dv + inset;
			v[1] = dv + step - inset;
			v[2] = dv + step - inset;
			v[3] = dv + inset;
		}

		static SkyQuad randomized(int textureIndex, int rowWidth, int sheetPixels, double size,
				Random random) {
			double[] basis = sphere(random);
			return basis == null ? null : new SkyQuad(textureIndex, rowWidth, sheetPixels, size, basis);
		}

		/**
		 * A random direction on the unit sphere with its rotation basis, or null if the draw landed
		 * outside the sphere or too near its centre. Upstream rejects those rather than normalising
		 * them, which is why a field of 7500 stars legitimately contains gaps.
		 */
		protected static double[] sphere(Random random) {
			double x = random.nextFloat() * 2 - 1;
			double y = random.nextFloat() * 2 - 1;
			double z = random.nextFloat() * 2 - 1;
			double spread = 0.15F + random.nextFloat() * 0.1F;
			double lengthSq = x * x + y * y + z * z;
			if (lengthSq >= 1 || lengthSq <= 0.01)
				return null;
			double inverse = 1 / Math.sqrt(lengthSq);
			x *= inverse;
			y *= inverse;
			z *= inverse;
			double yaw = Math.atan2(x, z);
			double pitch = Math.atan2(Math.sqrt(x * x + z * z), y);
			double rollAngle = random.nextDouble() * Math.PI * 2;
			return new double[] {x, y, z, spread, Math.sin(yaw), Math.cos(yaw), Math.sin(pitch),
					Math.cos(pitch), Math.cos(rollAngle)};
		}

		void emit(BufferBuilder buffer, double distance, int colour) {
			this.emit(buffer, distance, colour, null);
		}

		/**
		 * @param orientation upstream's per-quad {@code glRotate} chain, or null for none. It is applied
		 *                    on the CPU rather than as a matrix per draw: upstream issues one
		 *                    Tessellator draw per quad, and forty-eight render passes a frame to say
		 *                    the same thing is not worth it.
		 */
		void emit(BufferBuilder buffer, double distance, int colour,
				org.joml.Matrix4f orientation) {
			double cx = dirX * distance;
			double cy = dirY * distance;
			double cz = dirZ * distance;
			org.joml.Vector3f vertex = new org.joml.Vector3f();
			for (int corner = 0; corner < 4; corner++) {
				double ox = ((corner & 2) - 1) * spread;
				double oy = ((corner + 1 & 2) - 1) * spread;
				double rx = ox * roll - oy * size;
				double ry = oy * roll + ox * size;
				double px = rx * sinPitch;
				double pz = -rx * cosPitch;
				vertex.set((float)(cx + pz * sinYaw - ry * cosYaw), (float)(cy + px),
						(float)(cz + ry * sinYaw + pz * cosYaw));
				if (orientation != null)
					orientation.transformPosition(vertex);
				buffer.addVertex(vertex.x, vertex.y, vertex.z)
						.setUv(u[corner], v[corner])
						.setColor(0xFF000000 | colour);
			}
		}
	}

	/** A star: a sky quad that twinkles on its own clock. */
	private static final class Star extends SkyQuad {

		private final int colour;
		private final double twinkleSpeed;
		private final double twinkleAmplitude;
		private final double twinkleOffset;

		private Star(int colour, int textureIndex, double size, double twinkleSpeed,
				double twinkleAmplitude, double twinkleOffset, double[] basis) {
			super(textureIndex, 4, STARS_SHEET, size, basis);
			this.colour = colour;
			this.twinkleSpeed = twinkleSpeed;
			this.twinkleAmplitude = twinkleAmplitude;
			this.twinkleOffset = twinkleOffset;
		}

		static Star randomized(int colour, int textureIndex, double twinkleSpeed,
				double twinkleAmplitude, int offset, Random random) {
			double[] basis = sphere(random);
			if (basis == null)
				return null;
			// Upstream's own size roll, kept because it is what gives the field its range of sizes.
			double size = (1 + Math.sin(random.nextDouble() * Math.PI * 2)
					* (random.nextDouble() * 4)) * 12;
			return new Star(colour, textureIndex, size, twinkleSpeed, twinkleAmplitude, offset, basis);
		}

		void emitStar(BufferBuilder buffer, double distance, float brightness, double time) {
			if (brightness <= 0)
				return;
			double base = 1 - twinkleAmplitude;
			int twinkled = ReikaColorAPI.getColorWithBrightnessMultiplier(colour,
					(float)(base + twinkleAmplitude
							* Math.sin(twinkleOffset + time * twinkleSpeed / 500D)));
			int finalColour = brightness >= 24 ? twinkled
					: ReikaColorAPI.getColorWithBrightnessMultiplier(twinkled, brightness / 24F);
			this.emit(buffer, distance, finalColour);
		}
	}
}
