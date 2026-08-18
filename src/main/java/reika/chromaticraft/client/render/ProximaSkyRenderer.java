package reika.chromaticraft.client.render;

import java.util.Random;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaDimensions;
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
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ProximaSkyRenderer {

	private static final Identifier STARS = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_stars.png");
	private static final Identifier NEBULAE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_nebulae.png");
	private static final Identifier PLANETS = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/environment/proxima_planets.png");

	/** V33a BASE_STARS and STARS_VARIATION. */
	private static final int BASE_STARS = 5000;
	private static final int STARS_VARIATION = 2500;

	/** V33a's star palette. */
	private static final int[] STAR_COLOURS = {
			0xFFFFFF, 0xFFF4EA, 0xFFD2A1, 0xFFCC6F, 0xFFC46F, 0xAABFFF, 0xCAD7FF, 0xF8F7FF};

	private static final Star[] STAR_FIELD = new Star[BASE_STARS + STARS_VARIATION];
	private static final SkyQuad[] NEBULA_FIELD = new SkyQuad[16];
	private static final SkyQuad[] PLANET_FIELD = new SkyQuad[32];

	private static final WorldGeometryPass PASS = new WorldGeometryPass("ChromatiCraft Proxima sky");

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
			NEBULA_FIELD[i] = SkyQuad.randomized(i, 4, 175, random);
		for (int i = 0; i < PLANET_FIELD.length; i++)
			PLANET_FIELD[i] = SkyQuad.randomized(i, 2, 30, random);
	}

	private ProximaSkyRenderer() {}

	@SubscribeEvent
	public static void onRenderSky(RenderLevelStageEvent.AfterSky event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null
				|| minecraft.level.dimension() != ChromaDimensions.PROXIMA)
			return;

		double y = minecraft.player.getY();
		float fade = y <= 18 ? 0 : y >= 30 ? 1 : (float)((y - 18) / 12F);
		if (minecraft.level.canSeeSky(minecraft.player.blockPosition()))
			fade = 1;
		if (fade <= 0)
			return;

		double time = System.currentTimeMillis();
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		// V33a turns the sky by an eighth of a degree per block travelled on each axis, then adds a
		// slow spin with time. The sky is drawn around the viewer, so no camera translation is applied.
		org.joml.Matrix4f matrix = new org.joml.Matrix4f(event.getModelViewMatrix());
		matrix.rotateX((float)Math.toRadians(camera.x * 0.125));
		matrix.rotateY((float)Math.toRadians(camera.y * 0.125));
		matrix.rotateZ((float)Math.toRadians(camera.z * 0.125));
		matrix.rotateZ((float)Math.toRadians(time / 12000D % 360D));

		final float alpha = fade;
		int washColour = ReikaColorAPI.GStoHex((int)(255 * alpha));
		PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, NEBULAE, matrix, buffer -> {
			for (SkyQuad nebula : NEBULA_FIELD)
				if (nebula != null)
					nebula.emit(buffer, 380, washColour);
		});
		PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, PLANETS, matrix, buffer -> {
			for (SkyQuad planet : PLANET_FIELD)
				if (planet != null)
					planet.emit(buffer, 380, washColour);
		});
		PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, STARS, matrix, buffer -> {
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

		protected SkyQuad(int textureIndex, int rowWidth, double size, double[] basis) {
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
			u[0] = du;
			u[1] = du;
			u[2] = du + step;
			u[3] = du + step;
			v[0] = dv;
			v[1] = dv + step;
			v[2] = dv + step;
			v[3] = dv;
		}

		static SkyQuad randomized(int textureIndex, int rowWidth, double size, Random random) {
			double[] basis = sphere(random);
			return basis == null ? null : new SkyQuad(textureIndex, rowWidth, size, basis);
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
			double cx = dirX * distance;
			double cy = dirY * distance;
			double cz = dirZ * distance;
			for (int corner = 0; corner < 4; corner++) {
				double ox = ((corner & 2) - 1) * spread;
				double oy = ((corner + 1 & 2) - 1) * spread;
				double rx = ox * roll - oy * size;
				double ry = oy * roll + ox * size;
				double px = rx * sinPitch;
				double pz = -rx * cosPitch;
				buffer.addVertex((float)(cx + pz * sinYaw - ry * cosYaw), (float)(cy + px),
								(float)(cz + ry * sinYaw + pz * cosYaw))
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
			super(textureIndex, 4, size, basis);
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
