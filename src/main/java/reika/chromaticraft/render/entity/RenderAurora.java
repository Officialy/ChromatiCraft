package reika.chromaticraft.render.entity;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.Aurora;
import reika.chromaticraft.entity.EntityAurora;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderAurora}: draws one aurora curtain.
 *
 * <p>The curtain is a strip of quads following the ribbon's drifting spline, twenty-four blocks tall,
 * gradiented from the first colour at its base to the second at its top and faded out over the last
 * thirty-two steps at each end so it dissolves rather than stopping square. It is drawn additively with
 * no depth write, which is why an aurora glows through cloud and never occludes anything behind it.
 *
 * <p>The frame animation is upstream's, including a quirk worth naming rather than tidying: the sheet is
 * eight frames across and four down, but V33a indexes it with {@code u = (f % 4) * 0.125} and
 * {@code v = (f / 4) * 0.25} over {@code f = (time / 150) % 32}. So the column only ever walks the first
 * four of eight, while the row runs to 1.75 and wraps twice. Reproducing it keeps the animation upstream
 * paced and upstream looking; "fixing" the indices would change how an aurora reads.
 *
 * <h2>What differs from upstream, and why</h2>
 *
 * <p>Upstream's Proxima early-return is deliberately <em>not</em> reinstated. V33a skips this renderer
 * inside Proxima and lets the dimension's sky renderer draw aurorae instead, because 1.7.10's fixed
 * function pipeline needed them composited with the sky to sort correctly against it. 26.2 does not:
 * this draws through the entity submit pipeline, which already orders against the world, and the sky is
 * drawn far behind everything at a fixed distance. Skipping it here would simply lose the aurorae,
 * since the sky pass has no entity list to draw from.
 */
public final class RenderAurora extends EntityRenderer<EntityAurora, RenderAurora.State> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/aurora.png");

	/** V33a's frame clock: one of thirty-two frames every hundred and fifty milliseconds. */
	private static final double FRAME_MILLIS = 150;
	private static final int FRAMES = 32;
	/** How many steps at each end the curtain fades over. */
	private static final float FADE_STEPS = 32;

	public RenderAurora(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EntityAurora entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		// The curtain is built and wobbled per client; the entity only carries the endpoints and colours.
		// Identity, not equality, is the right test: it asks whether the entity has been handed a new
		// data object, which is the only thing that invalidates an existing curtain.
		if (state.aurora == null || state.aurora.data() != entity.getAuroraData()) {
			state.aurora = new Aurora(entity.getAuroraData());
			state.lastTick = -1;
		}
		// V33a drives the drift from the entity's onUpdate, so once per tick. This method runs once per
		// frame, so advancing it here unconditionally would make the curtain wave at the frame rate --
		// three times too fast at sixty frames a second, and different on every machine.
		if (entity.tickCount != state.lastTick) {
			state.lastTick = entity.tickCount;
			state.aurora.update();
		}
		state.curve = state.aurora.curve();
		state.entityPosition = entity.position();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		if (state.curve == null || state.curve.size() < 2)
			return;
		PoseStack curtainPose = new PoseStack();
		curtainPose.last().set(poseStack.last());
		// The spline is in absolute world coordinates while the pose is centred on the entity, so the
		// entity's own position is subtracted back out.
		Vec3 origin = state.entityPosition;
		List<DecimalPosition> curve = state.curve;
		int colorFrom = state.aurora.data().colorFrom();
		int colorTo = state.aurora.data().colorTo();
		double baseFrom = state.aurora.data().from().y;
		double baseTo = state.aurora.data().to().y;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TEXTURE),
				(pose, out) -> curtain(curtainPose.last(), out, curve, origin, colorFrom, colorTo,
						baseFrom, baseTo));
	}

	private static void curtain(PoseStack.Pose pose, VertexConsumer out, List<DecimalPosition> curve,
			Vec3 origin, int colorFrom, int colorTo, double baseFrom, double baseTo) {
		int frame = (int)(System.currentTimeMillis() / FRAME_MILLIS % FRAMES);
		// Upstream's indexing: see the class documentation for why the column only walks four of eight.
		double u = (frame % 4) * 0.125;
		double v = (frame / 4) * 0.25;
		double uEnd = u + 0.125;
		double vEnd = v + 0.25;

		int size = curve.size();
		for (int i = 0; i < size - 1; i++) {
			DecimalPosition first = curve.get(i);
			DecimalPosition second = curve.get(i + 1);
			// The baseline runs straight between the endpoint heights; only x and z follow the spline.
			double y1 = baseFrom + (baseTo - baseFrom) * (i / (double)size);
			double y2 = baseFrom + (baseTo - baseFrom) * ((i + 1) / (double)size);

			int column = (i - 1) % Aurora.STEPS;
			double u1 = u + column * (uEnd - u) / Aurora.STEPS;
			double u2 = u1 + (uEnd - u) / Aurora.STEPS;

			float fadeA = fade(i, size);
			float fadeB = fade(i + 1, size);
			int lowA = ReikaColorAPI.getColorWithBrightnessMultiplier(colorFrom, fadeA);
			int lowB = ReikaColorAPI.getColorWithBrightnessMultiplier(colorFrom, fadeB);
			int highA = ReikaColorAPI.getColorWithBrightnessMultiplier(colorTo, fadeA);
			int highB = ReikaColorAPI.getColorWithBrightnessMultiplier(colorTo, fadeB);

			// V33a maps v to the base and dv to the top, so the sheet is used upside down; kept as-is.
			quadVertex(pose, out, first.xCoord - origin.x, y1 - origin.y, first.zCoord - origin.z,
					u1, vEnd, lowA);
			quadVertex(pose, out, first.xCoord - origin.x, y1 + Aurora.HEIGHT - origin.y,
					first.zCoord - origin.z, u1, v, highA);
			quadVertex(pose, out, second.xCoord - origin.x, y2 + Aurora.HEIGHT - origin.y,
					second.zCoord - origin.z, u2, v, highB);
			quadVertex(pose, out, second.xCoord - origin.x, y2 - origin.y, second.zCoord - origin.z,
					u2, vEnd, lowB);
		}
	}

	/** V33a fades the first and last thirty-two steps to nothing so a curtain has no hard ends. */
	private static float fade(int index, int size) {
		if (index < FADE_STEPS)
			return index / FADE_STEPS;
		if (size - index < FADE_STEPS)
			return (size - index - 1) / FADE_STEPS;
		return 1;
	}

	private static void quadVertex(PoseStack.Pose pose, VertexConsumer out, double x, double y, double z,
			double u, double v, int color) {
		out.addVertex(pose, (float)x, (float)y, (float)z)
				.setColor(0xFF000000 | color)
				.setUv((float)u, (float)v)
				// V33a sets brightness 240 and disables entity lighting: a curtain is its own light.
				.setLight(0xF000F0)
				.setNormal(0, 1, 0);
	}

	public static final class State extends EntityRenderState {
		private Aurora aurora;
		private List<DecimalPosition> curve;
		private Vec3 entityPosition = Vec3.ZERO;
		private int lastTick = -1;
	}
}
