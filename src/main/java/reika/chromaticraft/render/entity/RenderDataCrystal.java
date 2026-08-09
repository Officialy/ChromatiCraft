package reika.chromaticraft.render.entity;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityDataCrystal;
import reika.chromaticraft.render.ChromaRenderPipelines;

/** V33a Memory Crystal entity pass: the 3x tilted, rotating lore-tower prism and flare. */
public final class RenderDataCrystal extends EntityRenderer<EntityDataCrystal, RenderDataCrystal.State> {

	private static final Identifier NODE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/data_node.png");
	private static final Identifier FLARE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/flare7.png");

	public RenderDataCrystal(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
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
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.scale(3, 3, 3);
		poseStack.translate(0, 0.125, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees(27.5F));
		float rotation = 90;
		if (state.airborneOwned)
			rotation = state.ageInTicks * 90F / (20 + Math.floorMod(state.identity, 10));
		poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
		PoseStack prismPose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(NODE),
				(pose, out) -> prism(prismPose.last(), out, state.ageInTicks));

		PoseStack flarePose = copy(poseStack);
		flarePose.translate(0, 0.625, 0);
		flarePose.mulPose(camera.orientation);
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(FLARE),
				(pose, out) -> flare(flarePose.last(), out));
		poseStack.popPose();
	}

	private static void prism(PoseStack.Pose pose, VertexConsumer out, float tick) {
		List<Vec3> ring = new ArrayList<>();
		double[] steps = {90, 15, 15};
		double[] radii = {0.19140625, 0.21875, 0.19140625};
		int step = 0;
		for (double angle = 45 + tick * 2; angle <= 405 + tick * 2.001; angle += steps[step]) {
			double radius = radii[step];
			ring.add(new Vec3(radius * Math.cos(Math.toRadians(angle)), 0,
					radius * Math.sin(Math.toRadians(angle))));
			step = (step + 1) % 3;
		}
		for (int i = 0; i < ring.size() - 1; i++) {
			Vec3 a = ring.get(i);
			Vec3 b = ring.get(i + 1);
			quad(pose, out, a.x, 0, a.z, a.x, 1.25, a.z, b.x, 1.25, b.z, b.x, 0, b.z,
					0xffa0e0ff, 49F / 64F, 94F / 96F, 1, 34F / 96F);
			quad(pose, out, 0, 0, 0, a.x, 0, a.z, b.x, 0, b.z, 0, 0, 0,
					0xff80c8ff, .78F, .31F, .98F, .18F);
			quad(pose, out, 0, 1.25, 0, b.x, 1.25, b.z, a.x, 1.25, a.z, 0, 1.25, 0,
					0xffffffff, .78F, .31F, .98F, .18F);
		}
	}

	private static void flare(PoseStack.Pose pose, VertexConsumer out) {
		quad(pose, out, -.75, -.75, 0, .75, -.75, 0, .75, .75, 0, -.75, .75, 0,
				0xffffffff, 0, 1, 1, 0);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out,
			double ax, double ay, double az, double bx, double by, double bz,
			double cx, double cy, double cz, double dx, double dy, double dz,
			int color, float u0, float v0, float u1, float v1) {
		out.addVertex(pose, (float)ax, (float)ay, (float)az).setUv(u0, v0).setColor(color);
		out.addVertex(pose, (float)bx, (float)by, (float)bz).setUv(u1, v0).setColor(color);
		out.addVertex(pose, (float)cx, (float)cy, (float)cz).setUv(u1, v1).setColor(color);
		out.addVertex(pose, (float)dx, (float)dy, (float)dz).setUv(u0, v1).setColor(color);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	public static final class State extends EntityRenderState {
		private boolean airborneOwned;
		private int identity;
	}
}
