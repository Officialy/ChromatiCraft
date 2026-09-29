package reika.chromaticraft.client.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code CliffFogRenderer}: drifting banks of mist over the Luminous Cliffs.
 *
 * <p>While the player is in the cliffs, up to 48 fog clouds are kept alive around them: each is spawned
 * somewhere within 96 blocks (only over the cliffs), 60-256 blocks up, 24-96 blocks across, and lives
 * 40-240 steps. A cloud is a camera-facing sheet cut into an 8x8 grid whose alpha falls off to the edges
 * and rises and falls over its life, textured with a slowly scrolling, slowly stretching window onto
 * Reika's fog texture. All of them drift together on a wind that turns full circle over a sixth of a
 * day. Clouds already alive outlast the player leaving.
 *
 * <p>Upstream stepped the clouds once per rendered frame, so their speed and lifetime followed the frame
 * rate. They are stepped here at a fixed 60 per second, the rate that behaviour was tuned against.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class LuminousCliffsFogRenderer {

	private static final int CLOUD_COUNT = 48;
	/** Reika's {@code Textures/biomefog_alpha.png}. */
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/environment/biomefog_alpha.png");
	private static final WorldGeometryPass PASS = new WorldGeometryPass("ChromatiCraft Luminous Cliffs fog");
	private static final double STEP_NANOS = 1_000_000_000D / 60;

	private static final List<Cloud> clouds = new ArrayList<>();
	private static final Random rand = new Random();
	private static long lastStep = -1;
	private static Object lastLevel;

	private LuminousCliffsFogRenderer() {}

	private static boolean isLuminousCliffs(double x, double z) {
		var level = Minecraft.getInstance().level;
		int bx = (int)Math.floor(x);
		int bz = (int)Math.floor(z);
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
		return level.getBiome(new BlockPos(bx, y, bz)).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS);
	}

	/** The geometry uploader needs the completed level, after Minecraft closes its translucent pass. */
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterLevel event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.level != lastLevel) {
			clouds.clear();
			lastStep = -1;
			lastLevel = mc.level;
			if (mc.level == null || mc.player == null)
				return;
		}
		Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
		Vec3 player = mc.player.position();

		long now = System.nanoTime();
		long owed = lastStep < 0 ? 1 : (long)((now - lastStep) / STEP_NANOS);
		int steps = (int)Math.min(20, owed);
		if (lastStep < 0 || owed > steps)
			lastStep = now; // after a stall, drop the backlog rather than fast-forward through it
		else if (steps > 0)
			lastStep += (long)(steps * STEP_NANOS);

		double v = 0.125;
		double a = ((4 * mc.level.getGameTime()) % 24000) / 24000D * (Math.PI * 2);
		double vx = v * Math.cos(a);
		double vz = v * Math.sin(a);
		for (int i = 0; i < steps; i++) {
			if (isLuminousCliffs(player.x, player.z) && clouds.size() < CLOUD_COUNT && rand.nextInt(4) == 0) {
				Cloud c = createCloud(player.x, player.y, player.z);
				if (c != null)
					clouds.add(c);
			}
			Iterator<Cloud> it = clouds.iterator();
			while (it.hasNext())
				if (it.next().update(vx, 0, vz))
					it.remove();
		}
		if (clouds.isEmpty())
			return;

		Quaternionf rotation = mc.gameRenderer.mainCamera().rotation();
		PASS.draw(ChromaRenderPipelines.TRANSLUCENT_SPRITE, TEXTURE, new org.joml.Matrix4f(event.getModelViewMatrix()),
				true, buffer -> {
					for (Cloud c : clouds)
						c.render(buffer, rotation, camera);
				});
	}

	/** V33a {@code createCloud}. */
	private static Cloud createCloud(double x, double y, double z) {
		double dx = ReikaRandomHelper.getRandomPlusMinus(x, 96, rand);
		double dz = ReikaRandomHelper.getRandomPlusMinus(z, 96, rand);
		if (!isLuminousCliffs(dx, dz))
			return null;
		double dy = ReikaRandomHelper.getRandomBetween(60D, 256D, rand);
		int l = 40 + rand.nextInt(200);
		double s = ReikaRandomHelper.getRandomBetween(24D, 96D, rand);
		float sx = (float)ReikaRandomHelper.getRandomBetween(0.0625, 0.375, rand);
		float sy = (float)ReikaRandomHelper.getRandomBetween(0.0625, 0.375, rand);
		float tx = (float)ReikaRandomHelper.getRandomPlusMinus(0, 0.03125D / 128, rand);
		float ty = (float)ReikaRandomHelper.getRandomPlusMinus(0, 0.03125D / 128, rand);
		float fx = (float)ReikaRandomHelper.getRandomPlusMinus(0, 0.03125D / 32, rand);
		float fy = (float)ReikaRandomHelper.getRandomPlusMinus(0, 0.03125D / 32, rand);
		Cloud c = new Cloud(rand.nextFloat(), rand.nextFloat(), sx, sy, s, l, dx, dy, dz);
		c.textureXSpeed = tx;
		c.textureYSpeed = ty;
		c.sizeXSpeed = fx;
		c.sizeYSpeed = fy;
		c.scaleSpeed = ReikaRandomHelper.getRandomPlusMinus(0, 0.03125D / 16, rand);
		return c;
	}

	private static final class Cloud {

		private float textureX;
		private float textureY;
		private float sizeX;
		private float sizeY;
		private double scale;

		private float textureXSpeed;
		private float textureYSpeed;
		private float sizeXSpeed;
		private float sizeYSpeed;
		private double scaleSpeed;

		private final int lifespan;
		private int age;

		private double posX;
		private double posY;
		private double posZ;

		private Cloud(float tx, float ty, float sx, float sy, double s, int l, double x, double y, double z) {
			textureX = tx;
			textureY = ty;
			sizeX = sx;
			sizeY = sy;
			scale = s;
			lifespan = l;
			posX = x;
			posY = y;
			posZ = z;
		}

		private boolean update(double vx, double vy, double vz) {
			posX += vx;
			posY += vy;
			posZ += vz;
			textureX += textureXSpeed;
			textureY += textureYSpeed;
			sizeX += sizeXSpeed;
			sizeY += sizeYSpeed;
			scale += scaleSpeed;
			age++;
			return age >= lifespan;
		}

		private float getAlpha() {
			return (float)Math.sin(Math.toRadians(180D * age / lifespan));
		}

		/** V33a {@code Cloud.render}: the 8x8 grid, each corner's alpha the life alpha times a tent falloff. */
		private void render(BufferBuilder buffer, Quaternionf rotation, Vec3 camera) {
			float f = this.getAlpha();
			double x1 = -scale * sizeX;
			double x2 = scale * sizeX;
			double y1 = -scale * sizeY;
			double y2 = scale * sizeY;
			double ds = 0.125;
			for (double d1 = 0; d1 < 1; d1 += ds) {
				for (double d2 = 0; d2 < 1; d2 += ds) {
					double dx = x1 + (x2 - x1) * d1;
					double dy = y1 + (y2 - y1) * d2;
					double dx2 = dx + ds * sizeX * scale * 2;
					double dy2 = dy + ds * sizeY * scale * 2;
					float u1 = (float)(textureX + d1 * sizeX);
					float u2 = (float)(u1 + ds * sizeX);
					float v1 = (float)(textureY + d2 * sizeY);
					float v2 = (float)(v1 + ds * sizeY);
					this.vertex(buffer, rotation, camera, dx, dy, u1, v1, f * tent(d1) * tent(d2));
					this.vertex(buffer, rotation, camera, dx2, dy, u2, v1, f * tent(d1 + ds) * tent(d2));
					this.vertex(buffer, rotation, camera, dx2, dy2, u2, v2, f * tent(d1 + ds) * tent(d2 + ds));
					this.vertex(buffer, rotation, camera, dx, dy2, u1, v2, f * tent(d1) * tent(d2 + ds));
				}
			}
		}

		private static float tent(double d) {
			return (float)(1 - Math.abs(d * 2 - 1));
		}

		private void vertex(BufferBuilder buffer, Quaternionf rotation, Vec3 camera, double lx, double ly,
				float u, float v, float alpha) {
			Vector3f p = rotation.transform(new Vector3f((float)lx, (float)ly, 0));
			int a = Math.max(0, Math.min(255, (int)(255 * alpha)));
			buffer.addVertex((float)(posX - camera.x + p.x()), (float)(posY - camera.y + p.y()), (float)(posZ - camera.z + p.z()))
					.setUv(u, v).setColor(255, 255, 255, a);
		}
	}
}
