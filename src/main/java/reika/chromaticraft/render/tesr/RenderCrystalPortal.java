package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockChromaPortal;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Faithful 26.2 submit-pipeline port of V33a's Portal Rift renderer. */
public final class RenderCrystalPortal implements
		BlockEntityRenderer<TileEntityCrystalPortal, RenderCrystalPortal.State> {

	private static final Identifier END_PORTAL = Identifier.withDefaultNamespace(
			"textures/entity/end_portal/end_portal.png");
	private static final Identifier RIFT = texture("block/icons/rift.png");
	private static final Identifier RINGS = texture("block/icons/ringrow_fade.png");
	private static final Identifier FLOOR_BEAM = texture("effect/beam2.png");
	private static final Identifier ARCHES = texture("effect/arches2.png");
	private static final Sheet RIFT_SHEET = new Sheet(80, 2);
	private static final Sheet RING_SHEET = new Sheet(180, 1);

	public RenderCrystalPortal(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityCrystalPortal portal, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(portal, state, partialTick, cameraPosition,
				breakProgress);
		state.tick = portal.getTicksExisted() + partialTick;
		state.complete = portal.isComplete();
		state.charge = portal.getCharge();
		state.centre = portal.isPadCentre();
		state.fullPad = portal.getLevel() != null
				&& BlockChromaPortal.isFullPad(portal.getLevel(), portal.getBlockPos());
		state.cameraYaw = (float)Math.toDegrees(Math.atan2(
				cameraPosition.x - portal.getBlockPos().getX() - 0.5,
				cameraPosition.z - portal.getBlockPos().getZ() - 0.5));
		state.exposed = 0;
		if (portal.getLevel() != null) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				if (!(portal.getLevel().getBlockState(portal.getBlockPos().relative(direction)).getBlock()
						instanceof BlockChromaPortal))
					state.exposed |= 1 << direction.ordinal();
			}
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		PoseStack skinPose = copy(poseStack);
		// The portal skin is an opaque V33a pass. Putting it in the translucent target both weakened
		// the texture and made it compete with the rift effects/water; cutout restores depth writing.
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(END_PORTAL),
				(ignored, out) -> renderSkin(skinPose.last(), out, state));
		if (!state.centre || !state.fullPad)
			return;

		PoseStack focusPose = copy(poseStack);
		submitAfterTerrain(focusPose, collector, ChromaRenderPipelines.additiveSprite(RIFT),
				(ignored, out) -> renderFocus(focusPose.last(), out, state));
		if (!state.complete)
			return;

		PoseStack floorPose = copy(poseStack);
		submitAfterTerrain(floorPose, collector, ChromaRenderPipelines.additiveSprite(FLOOR_BEAM),
				(ignored, out) -> renderCompletedFloor(floorPose.last(), out, state));
		PoseStack ringsPose = copy(poseStack);
		submitAfterTerrain(ringsPose, collector, ChromaRenderPipelines.additiveSprite(RINGS),
				(ignored, out) -> renderRings(ringsPose.last(), out, state));

		// A transport beam must mean "ready". The V33a half-charge visual was misleading now that the
		// modern portal gives an explicit rejection result, so expose it only at the usable threshold.
		if (state.charge >= TileEntityCrystalPortal.MINCHARGE) {
			PoseStack archPose = copy(poseStack);
			submitAfterTerrain(archPose, collector, ChromaRenderPipelines.additiveSprite(ARCHES),
					(ignored, out) -> renderArches(archPose.last(), out, state));
		}
	}

	/** V33a's first pass: moving vanilla End-portal texture on exposed pad faces. */
	private static void renderSkin(PoseStack.Pose pose, VertexConsumer out, State state) {
		double u = Math.sin(Math.toRadians(state.tick % 360));
		double v = state.tick % 256 / 256D + Math.cos(Math.toRadians((state.tick / 2D) % 360));
		double du = u + 0.25;
		double dv = v + 0.25;
		double o = 0.001;
		double h = 1 - o;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if ((state.exposed & 1 << direction.ordinal()) != 0)
				face(pose, out, direction, o, h, 0xffffffff,
						(float)u, (float)v, (float)du, (float)dv);
		}
		if (!state.fullPad)
			face(pose, out, Direction.UP, o, h, 0xffffffff,
					(float)u, (float)v, (float)du, (float)dv);
		face(pose, out, Direction.DOWN, o, h, 0xffffffff,
				(float)u, (float)v, (float)du, (float)dv);
	}

	private static void renderFocus(PoseStack.Pose pose, VertexConsumer out, State state) {
		Frame frame = RIFT_SHEET.frame(state.tick);
		int color = 0xff000000 | ReikaColorAPI.getModifiedHue(0x0000ff,
				(int)(240 + 40 * Math.sin(state.tick / 8D)));
		quad(pose, out, -1,1.002,2, 2,1.002,2, 2,1.002,-1, -1,1.002,-1,
				color, 0,frame.v0,1,frame.v1);
	}

	private static void renderCompletedFloor(PoseStack.Pose pose, VertexConsumer out, State state) {
		double millis = System.currentTimeMillis();
		float u = (float)(2 * Math.sin(millis / 3200D));
		float v = (float)Math.cos(90 + millis / 1600D);
		int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(
				CrystalElement.getBlendedColor((int)state.tick, 20), 0.0625F);
		quad(pose, out, -1,1.001,2, 2,1.001,2, 2,1.001,-1, -1,1.001,-1,
				color, u,v,u+1,v+1);
	}

	private static void renderRings(PoseStack.Pose pose, VertexConsumer out, State state) {
		Frame frame = RING_SHEET.frame(state.tick);
		boolean half = state.charge >= TileEntityCrystalPortal.MINCHARGE / 2;
		double visible = half ? 2D : 4D * Math.max(0, state.charge)
				/ TileEntityCrystalPortal.MINCHARGE;
		for (double y = 0; y <= visible + 1.0E-6; y += 0.25) {
			double radius = 1.5 - y / 2;
			int grey = (int)((255 - 64 * y)
					* (0.75 + 0.25 * Math.sin((state.tick + y * 96) / 4F)));
			int color = 0xff000000 | ReikaColorAPI.GStoHex(Math.max(0, grey));
			double yy = y + 1.002;
			quad(pose, out, 0.5-radius,yy,0.5+radius, 0.5+radius,yy,0.5+radius,
					0.5+radius,yy,0.5-radius, 0.5-radius,yy,0.5-radius,
					color, 0,frame.v0,1,frame.v1);
		}
	}

	private static void renderArches(PoseStack.Pose pose, VertexConsumer out, State state) {
		float frame = (System.currentTimeMillis() / 50 % 32) / 32F;
		for (int i = 0; i < 3; i++) {
			double radians = Math.toRadians(i * 60 - state.cameraYaw);
			double dx = Math.cos(radians) * 0.5;
			double dz = Math.sin(radians) * 0.5;
			for (float lane = 0; lane <= 0.5F; lane += 0.5F) {
				float u = lane + frame;
				quad(pose, out, 0.5-dx,2,0.5-dz, 0.5+dx,2,0.5+dz,
						0.5+dx,9,0.5+dz, 0.5-dx,9,0.5-dz,
						0xffffffff, u,0,u+1F/32F,1);
			}
		}
	}

	private static void face(PoseStack.Pose pose, VertexConsumer out, Direction direction,
			double min, double max, int color, float u0, float v0, float u1, float v1) {
		switch (direction) {
			case DOWN -> litQuad(pose,out, max,min,min, max,min,max, min,min,max, min,min,min,color,u0,v0,u1,v1,0,-1,0);
			case UP -> litQuad(pose,out, min,max,min, min,max,max, max,max,max, max,max,min,color,u0,v0,u1,v1,0,1,0);
			case NORTH -> litQuad(pose,out, max,min,min, min,min,min, min,max,min, max,max,min,color,u0,v0,u1,v1,0,0,-1);
			case SOUTH -> litQuad(pose,out, min,min,max, max,min,max, max,max,max, min,max,max,color,u0,v0,u1,v1,0,0,1);
			case WEST -> litQuad(pose,out, min,min,min, min,min,max, min,max,max, min,max,min,color,u0,v0,u1,v1,-1,0,0);
			case EAST -> litQuad(pose,out, max,min,max, max,min,min, max,max,min, max,max,max,color,u0,v0,u1,v1,1,0,0);
		}
	}

	/** The vanilla entity pipeline needs its complete vertex; leaving light/normal unset made the
	 * otherwise-correct End Portal skin black or invisible on some render paths. */
	private static void litQuad(PoseStack.Pose pose, VertexConsumer out,
			double x1,double y1,double z1, double x2,double y2,double z2,
			double x3,double y3,double z3, double x4,double y4,double z4,
			int color, float u0,float v0,float u1,float v1, float nx,float ny,float nz) {
		litVertex(pose,out,x1,y1,z1,color,u0,v1,nx,ny,nz);
		litVertex(pose,out,x2,y2,z2,color,u1,v1,nx,ny,nz);
		litVertex(pose,out,x3,y3,z3,color,u1,v0,nx,ny,nz);
		litVertex(pose,out,x4,y4,z4,color,u0,v0,nx,ny,nz);
	}

	private static void litVertex(PoseStack.Pose pose, VertexConsumer out,
			double x,double y,double z, int color,float u,float v, float nx,float ny,float nz) {
		out.addVertex(pose, (float)x, (float)y, (float)z).setUv(u, v).setColor(color)
				.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out,
			double x1,double y1,double z1, double x2,double y2,double z2,
			double x3,double y3,double z3, double x4,double y4,double z4,
			int color, float u0,float v0,float u1,float v1) {
		vertex(pose,out,x1,y1,z1,color,u0,v1); vertex(pose,out,x2,y2,z2,color,u1,v1);
		vertex(pose,out,x3,y3,z3,color,u1,v0); vertex(pose,out,x4,y4,z4,color,u0,v0);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer out, double x,double y,double z,
			int color,float u,float v) {
		out.addVertex(pose, (float)x, (float)y, (float)z).setUv(u, v).setColor(color);
	}

	private static void submitAfterTerrain(PoseStack poseStack, SubmitNodeCollector collector,
			RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
		CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
				poseStack.last().copy(), renderType, renderer);
		((OrderedSubmitNodeCollectorExtension)collector.order(0))
				.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, submit);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/" + path);
	}

	@Override
	public AABB getRenderBoundingBox(TileEntityCrystalPortal portal) {
		return new AABB(portal.getBlockPos()).inflate(3, 1, 3).expandTowards(0, 9, 0);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

	private record Sheet(int frames, int frameTime) {
		private Frame frame(float tick) {
			int index = Math.floorMod((int)Math.floor(tick / frameTime), frames);
			return new Frame(index / (float)frames, (index + 1) / (float)frames);
		}
	}

	private record Frame(float v0, float v1) {}

	public static final class State extends BlockEntityRenderState {
		private float tick;
		private float cameraYaw;
		private int charge;
		private int exposed;
		private boolean complete;
		private boolean centre;
		private boolean fullPad;
	}
}
