package reika.chromaticraft.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.world.dimension.SkyRiverGenerator;
import reika.chromaticraft.world.dimension.SkyRiverGenerator.RiverPoint;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code SkyRiverRenderer}: draws Proxima's sky rivers as glowing tubes.
 *
 * <p>Each segment between two river points is a thirty-six sided tube whose radius breathes four
 * blocks either side of the tunnel radius, phase-shifted along the river by {@link #STEP_PER_POINT} so
 * the pulse travels rather than throbbing in place. The hue cycles with position and with time, which
 * is what makes a river read as flowing while standing still, and the texture scrolls along it.
 *
 * <p>Drawn additively with no depth write, so a river glows through what is behind it and occludes
 * nothing, and unlit, because a river is its own light.
 *
 * <p>The two ends of a ray fade almost to black — upstream drops the first and last segment to a
 * hundredth brightness — so a river tapers out instead of stopping dead in mid-air.
 *
 * <h2>What is not here</h2>
 *
 * <p>Upstream also spawns particles at both mouths of every ray, drawn towards the opening by a
 * {@code CollectingPositionController}. That controller and the blur particle it drives are DragonAPI
 * and ChromatiCraft particle machinery which is not ported, so the tubes are drawn without them; the
 * rivers read correctly, the mouths are simply quieter than upstream's.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class SkyRiverRenderer {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/sky_river.png");

	/** V33a STEP_PER_POINT: how far the radius pulse is phase-shifted from one point to the next. */
	private static final double STEP_PER_POINT = 0.75;
	/** How far from the camera rivers are drawn. */
	private static final double RENDER_RANGE = 512;
	/** V33a draws each tube with thirty-six sides. */
	private static final int SIDES = 36;

	private static final WorldGeometryPass PASS = new WorldGeometryPass("ChromatiCraft sky rivers");

	/**
	 * Which of the three ways this can draw nothing has already been reported. Rivers failing silently
	 * is indistinguishable in-game from rivers being nowhere near you, so each cause says so once
	 * rather than every frame.
	 */
	private static final java.util.Set<String> reported = new java.util.HashSet<>();

	private SkyRiverRenderer() {}

	private static void reportOnce(String cause, String detail) {
		if (reported.add(cause))
			ChromatiCraft.LOGGER.info("Sky rivers: {} ({})", cause, detail);
	}

	/**
	 * Drawn after translucent blocks so a river composites over the world. It is not drawn in the
	 * weather or particle stages: those target separate buffers that the post chain composites, and an
	 * additive glow belongs against the finished scene colour.
	 */
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null
				|| minecraft.level.dimension() != ChromaDimensions.PROXIMA)
			return;
		reportOnce("reached", "the render hook fires in Proxima");
		SkyRiverGenerator rivers = ClientSkyRivers.get();
		if (rivers == null) {
			reportOnce("no client copy", "the layout seed packet has not arrived");
			return;
		}
		var points = rivers.getPointsWithin(minecraft.player, RENDER_RANGE);
		if (points.isEmpty()) {
			reportOnce("none in range", "nothing within " + (int)RENDER_RANGE + " of "
					+ minecraft.player.blockPosition() + "; " + rivers.getRays().size()
					+ " rays were generated");
			return;
		}
		reportOnce("drawing", points.size() + " points at " + minecraft.player.blockPosition());

		Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
		double time = System.currentTimeMillis() / 1250D;
		PASS.draw(ChromaRenderPipelines.ADDITIVE_SPRITE, TEXTURE,
				new org.joml.Matrix4f(event.getModelViewMatrix()), buffer -> {
			for (RiverPoint point : points) {
				double radiusFrom = SkyRiverGenerator.RIVER_TUNNEL_RADIUS
						+ 4 * Math.sin(time - point.index() * STEP_PER_POINT);
				double radiusTo = SkyRiverGenerator.RIVER_TUNNEL_RADIUS
						+ 4 * Math.sin(time - (point.index() + 1) * STEP_PER_POINT);
				int colourFrom = hueAt(point.index());
				int colourTo = hueAt(point.index() + 1);
				// V33a fades the very first and last segment so a ray tapers rather than stopping dead.
				if (point.index() == 1)
					colourFrom = ReikaColorAPI.getColorWithBrightnessMultiplier(colourFrom, 0.01F);
				else if (point.index() + 2 >= point.pathLength() - 1)
					colourTo = ReikaColorAPI.getColorWithBrightnessMultiplier(colourTo, 0.01F);
				tube(buffer, point.position(), point.next(), camera, radiusFrom, radiusTo,
						colourFrom, colourTo, point.index());
			}
		});
	}

	/** V33a cycles the hue four degrees a point and with time, so the colour travels along the river. */
	private static int hueAt(int index) {
		return ReikaColorAPI.getModifiedHue(0xFF0000,
				(int)((index * 4 - System.currentTimeMillis() / 500D) % 360D));
	}

	/**
	 * One segment: a tube of {@link #SIDES} quads swept between two river points. The ring basis is
	 * built from whichever axis is least parallel to the segment, which is what stops a vertical river
	 * degenerating into a line.
	 */
	private static void tube(BufferBuilder buffer, DecimalPosition from, DecimalPosition to,
			Vec3 camera, double radiusFrom, double radiusTo, int colourFrom, int colourTo, int index) {
		Vec3 start = new Vec3(from.xCoord, from.yCoord, from.zCoord).subtract(camera);
		Vec3 end = new Vec3(to.xCoord, to.yCoord, to.zCoord).subtract(camera);
		Vec3 axis = end.subtract(start);
		if (axis.lengthSqr() < 1.0E-6)
			return;
		axis = axis.normalize();
		Vec3 reference = Math.abs(axis.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 right = axis.cross(reference).normalize();
		Vec3 up = axis.cross(right).normalize();

		// The texture scrolls along the tube with time; most of the sense of flow comes from this.
		//
		// The u coordinate has to be continuous *along the river*, not per segment. Running it from 0 to
		// 1 on every segment -- which is what this did at first -- restarts the texture at every node,
		// and since node spacing grows with distance from the centre the restarts are visibly uneven:
		// the texture appears to snap back and change density as the river recedes. Keying u to the
		// point's index instead means each segment picks up exactly where the last one left off, so the
		// seam matches and the pattern runs unbroken down the whole ray.
		float scroll = (float)(-(System.currentTimeMillis() % 4000L) / 4000D);
		float u0 = scroll + index;
		float u1 = scroll + index + 1;
		for (int side = 0; side < SIDES; side++) {
			double a1 = side * 2 * Math.PI / SIDES;
			double a2 = (side + 1) * 2 * Math.PI / SIDES;
			float v1 = (float)side / SIDES;
			float v2 = (float)(side + 1) / SIDES;
			Vec3 o1 = right.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
			Vec3 o2 = right.scale(Math.cos(a2)).add(up.scale(Math.sin(a2)));
			vertex(buffer, start.add(o1.scale(radiusFrom)), u0, v1, colourFrom);
			vertex(buffer, start.add(o2.scale(radiusFrom)), u0, v2, colourFrom);
			vertex(buffer, end.add(o2.scale(radiusTo)), u1, v2, colourTo);
			vertex(buffer, end.add(o1.scale(radiusTo)), u1, v1, colourTo);
		}
	}

	private static void vertex(BufferBuilder buffer, Vec3 at, float u, float v, int colour) {
		buffer.addVertex((float)at.x, (float)at.y, (float)at.z).setUv(u, v).setColor(0xFF000000 | colour);
	}
}
