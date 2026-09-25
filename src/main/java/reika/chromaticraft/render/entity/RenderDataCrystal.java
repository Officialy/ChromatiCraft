package reika.chromaticraft.render.entity;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.ArtealloyEffects;
import reika.chromaticraft.entity.EntityDataCrystal;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.RayTracer;

/**
 * V33a {@code DataCrystalRenderer}: the dropped Memory Crystal as a tilted lore-tower prism with a flare.
 *
 * <p>Sizes follow the 1.7.10 path exactly: Forge drew a {@code BLOCK_3D} custom item renderer at half scale,
 * so the renderer's {@code glScaled(3)} is 1.5 blocks, and a 1.7.10 item's render origin was 0.125 above its
 * bounding box (its {@code yOffset}), where 26.2 draws from the box's base. The prism pivots about its middle
 * and does not spin on its own axis (V33a passed tick 0). The flare is drawn through walls, and only while the
 * viewer has line of sight to the crystal.
 *
 * <p>A crystal in the Artifact Distribution Map alloy turns orange, loses its flare, and instead focuses the
 * ARTEALLOY screen shaders ({@link ArtealloyEffects}) on a point below it, where V33a's matrices stood when it
 * set them.
 */
public final class RenderDataCrystal extends EntityRenderer<EntityDataCrystal, RenderDataCrystal.State> {

	private static final Identifier NODE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/data_node.png");
	private static final Identifier NODE_ORANGE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/data_node_orange.png");
	private static final Identifier FLARE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/flare7.png");

	/** 1.7.10 EntityItem {@code yOffset}: half its 0.25 height. */
	private static final double LEGACY_Y_OFFSET = 0.125;
	/** The renderer's {@code glScaled(3)} under Forge's 0.5 BLOCK_3D entity-item scale. */
	private static final float SCALE = 1.5F;
	private static final double PRISM_HEIGHT = 1.25;
	private static final double FLARE_SIZE = 3;

	private static final RayTracer.RayTracerWithCache<?> LOS =
			RayTracer.getMultipointVisualLOSForRenderCulling(RenderDataCrystal::isClearLineOfSight);

	public RenderDataCrystal(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	/** V33a {@code isClearLineOfSight}: any of nine points around the crystal can see the viewer. */
	private static boolean isClearLineOfSight(EntityDataCrystal ei, RayTracer trace, Level world) {
		Player ep = Minecraft.getInstance().player;
		if (ep == null)
			return false;
		Vec3 eye = ep.getEyePosition();
		double r = 0.5;
		for (double i = -r; i <= r; i += r) {
			for (double k = -r; k <= r; k += r) {
				trace.setOrigins(ei.getX() + i, ei.getY() + LEGACY_Y_OFFSET, ei.getZ() + k, eye.x, eye.y, eye.z);
				if (trace.isClearLineOfSight(world))
					return true;
			}
		}
		return false;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EntityDataCrystal crystal, State state, float partialTick) {
		super.extractRenderState(crystal, state, partialTick);
		state.airborneOwned = crystal.getOwner() != null && !crystal.onGround();
		state.identity = System.identityHashCode(crystal);
		state.artealloy = crystal.isArtealloy();
		state.lineOfSight = !state.artealloy && LOS.isClearLineOfSight(crystal);
		if (state.artealloy) {
			Player ep = Minecraft.getInstance().player;
			if (ep != null) {
				Vec3 pos = crystal.getPosition(partialTick);
				Vec3 focus = pos.add(0, LEGACY_Y_OFFSET + SCALE * (0.125 - 0.75), 0);
				ArtealloyEffects.markCrystal(focus, crystal.distanceToSqr(ep));
			}
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0, LEGACY_Y_OFFSET, 0);
		poseStack.scale(SCALE, SCALE, SCALE);
		poseStack.translate(0, 0.125, 0);

		PoseStack prismPose = copy(poseStack);
		prismPose.mulPose(Axis.ZP.rotationDegrees(27.5F));
		float rotation = 90;
		if (state.airborneOwned)
			rotation = state.ageInTicks * 90F / (20 + Math.floorMod(state.identity, 10));
		prismPose.mulPose(Axis.XP.rotationDegrees(rotation));
		prismPose.translate(0, -PRISM_HEIGHT / 2, 0);
		boolean orange = state.artealloy;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(orange ? NODE_ORANGE : NODE),
				(pose, out) -> prism(prismPose.last(), out, orange));

		if (state.lineOfSight) {
			PoseStack flarePose = copy(poseStack);
			flarePose.mulPose(camera.orientation);
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.legacyAdditiveSpriteThroughWall(FLARE),
					(pose, out) -> flare(flarePose.last(), out));
		}
		poseStack.popPose();
	}

	/** V33a {@code RenderDataNode.renderPrism(0, v5, 1, 1.25, 0, orangeglow)}. */
	private static void prism(PoseStack.Pose pose, VertexConsumer out, boolean orange) {
		double s = 0.125 * 0.875;
		double r1 = 2 * s * 0.875;
		double r2 = 2 * s;
		double[] da = {90, 15, 15};
		double[] ra = {r1, r2, r1};
		int c1 = orange ? 0xffffffff : 0xffa0e0ff;
		int c2 = 0xffffffff;
		double h = PRISM_HEIGHT;
		List<Vec3> li = new ArrayList<>();
		int i = 0;
		double a0 = 45;
		for (double a = a0; a <= a0 + 360; a += da[i]) {
			li.add(new Vec3(ra[i] * Math.cos(Math.toRadians(a)), 0, ra[i] * Math.sin(Math.toRadians(a))));
			i = (i + 1) % da.length;
		}
		// The caps were untextured smooth-shaded fans; each triangle is a degenerate quad on this QUADS pipeline.
		for (int idx = 0; idx < li.size() - 1; idx++) {
			Vec3 a = li.get(idx);
			Vec3 b = li.get(idx + 1);
			fanVertex(pose, out, 0, 0, 0, c1);
			fanVertex(pose, out, a.x, 0, a.z, c2);
			fanVertex(pose, out, b.x, 0, b.z, c2);
			fanVertex(pose, out, 0, 0, 0, c1);
			fanVertex(pose, out, 0, h, 0, 0xffa0e0ff);
			fanVertex(pose, out, b.x, h, b.z, c2);
			fanVertex(pose, out, a.x, h, a.z, c2);
			fanVertex(pose, out, 0, h, 0, 0xffa0e0ff);
		}
		for (int idx = 0; idx < li.size(); idx++) {
			Vec3 p1 = li.get(idx);
			Vec3 p2 = li.get((idx + 1) % li.size());
			float u = idx % 3 != 2 ? 63F / 64 : 49F / 64;
			float v = 34F / 96;
			float du = 1;
			float dv = 94F / 96;
			out.addVertex(pose, (float)p1.x, 0, (float)p1.z).setUv(u, v).setColor(c2);
			out.addVertex(pose, (float)p1.x, (float)h, (float)p1.z).setUv(u, dv).setColor(c2);
			out.addVertex(pose, (float)p2.x, (float)h, (float)p2.z).setUv(du, dv).setColor(c2);
			out.addVertex(pose, (float)p2.x, 0, (float)p2.z).setUv(du, v).setColor(c2);
		}
	}

	/** Cap vertices sample a flat white patch of the node texture, standing in for GL_TEXTURE_2D being off. */
	private static void fanVertex(PoseStack.Pose pose, VertexConsumer out, double x, double y, double z, int color) {
		out.addVertex(pose, (float)x, (float)y, (float)z).setUv(0.88F, 0.245F).setColor(color);
	}

	/** V33a {@code RenderDataNode.renderFlare(v5, 1, true)}: a ±3 billboard in 0xb0e0ff. */
	private static void flare(PoseStack.Pose pose, VertexConsumer out) {
		float s = (float)FLARE_SIZE;
		int color = 0xffb0e0ff;
		out.addVertex(pose, -s, s, 0).setUv(0, 1).setColor(color);
		out.addVertex(pose, s, s, 0).setUv(1, 1).setColor(color);
		out.addVertex(pose, s, -s, 0).setUv(1, 0).setColor(color);
		out.addVertex(pose, -s, -s, 0).setUv(0, 0).setColor(color);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	public static final class State extends EntityRenderState {
		private boolean airborneOwned;
		private int identity;
		private boolean artealloy;
		private boolean lineOfSight;
	}
}
