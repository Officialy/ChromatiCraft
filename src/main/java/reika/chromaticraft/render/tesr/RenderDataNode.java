package reika.chromaticraft.render.tesr;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * Submit-pipeline port of V33a's deployable lore tower. The renderer retains the three telescoping
 * stages, threefold tower arms, rotating tower glyphs, flare/prism and the post-scan sky beam; only
 * the obsolete immediate-mode/shader setup has been replaced.
 */
public final class RenderDataNode implements BlockEntityRenderer<TileEntityDataNode, RenderDataNode.State> {

	private static final Identifier NODE = texture("entity/data_node.png");
	private static final Identifier SYMBOLS = texture("entity/tower_symbols.png");
	private static final Identifier MOSS = texture("block/shield/moss.png");
	private static final Identifier STONE = texture("block/shield/stone.png");
	private static final Identifier FLARE = texture("block/icons/flare7.png");
	private static final int FULL_BRIGHT = 0x00f000f0;

	public RenderDataNode(BlockEntityRendererProvider.Context context) {}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityDataNode node, State state, float partialTick,
			Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(node, state, partialTick, cameraPosition, breakProgress);
		state.extension0 = node.getExtension0();
		state.extension1 = node.getExtension1();
		state.extension2 = node.getExtension2();
		state.rotation = node.getRotation() + partialTick;
		state.tick = node.getTicksExisted() + partialTick;
		state.scanProgress = node.getScanProgress();
		state.towerTexture = node.getTower() != null ? node.getTower().textureIndex : -1;
		Player player = Minecraft.getInstance().player;
		state.scanned = player != null && node.hasBeenScanned(player);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);

		PoseStack basePose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(MOSS),
				(pose, vertices) -> renderBase(basePose.last(), vertices, state));

		PoseStack towerPose = copy(poseStack);
		towerPose.mulPose(Axis.YP.rotationDegrees((float)state.rotation));
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(NODE),
				(pose, vertices) -> renderTower(towerPose.last(), vertices, state));
		PoseStack collarPose = copy(towerPose);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(STONE),
				(pose, vertices) -> renderCollar(collarPose.last(), vertices, state));

		double deployed = deployment(state);
		if (deployed > 0) {
			if (state.towerTexture >= 0)
				submitSymbols(state, poseStack, collector, deployed);
			submitFlare(state, poseStack, collector, camera, deployed);
			submitPrism(state, poseStack, collector, deployed);
		}
		if (state.scanned)
			submitSkyBeam(state, poseStack, collector);
		poseStack.popPose();
	}

	/** V33a's moss-clad first-stage telescoping pedestal. */
	private static void renderBase(PoseStack.Pose pose, VertexConsumer out, State state) {
		double top = state.extension0;
		// The source exposes nested skins as each whole block of the lower stage rises.
		int layers = Math.max(1, (int)Math.ceil(top + 1));
		for (int i = 0; i < layers; i++) {
			double y0 = top - i - 0.5;
			double inset = (i & 1) == 0 ? -0.0625 : 0.0625;
			box(pose, out, -0.5 - inset, y0, -0.5 - inset,
					0.5 + inset, y0 + 1, 0.5 + inset, 0xffffffff, state.lightCoords);
		}
		// Sloped source cap, represented by the exact 1/8-block inset and drop.
		frustum(pose, out, -0.5625, top + 0.5, 0.5625,
				-0.375, top + 0.375, 0.375, 0xffffffff, state.lightCoords, 0, 1);
	}

	/** The two complete six-faced V33a rows, repeated with its 120-degree symmetry. */
	private static void renderTower(PoseStack.Pose pose, VertexConsumer out, State state) {
		for (int arm = 0; arm < 3; arm++) {
			double angle = Math.toRadians(arm * 120D);
			// Source dy = 0.5+extension1-1; the previous approximation was half a block high.
			towerRow(pose, out, angle, state.extension1 - 0.5, 2.5,
					0.125 / Math.sin(Math.toRadians(60)) * 1.5,
					0.125 / Math.sin(Math.toRadians(60)), -0.5, -0.375,
					state.lightCoords, 3F/64F, 34F/96F, 45F/64F, 94F/96F);
			// Source dy2 = 0.5+extension1+extension2.
			towerRow(pose, out, angle, 0.5 + state.extension1 + state.extension2, 1.5,
					0.125 / Math.sin(Math.toRadians(60)),
					0.0625 / Math.sin(Math.toRadians(60)), -0.3671875, -0.2421875,
					state.lightCoords, 17F/64F, 34F/96F, 31F/64F, 94F/96F);
		}
	}

	private static void towerRow(PoseStack.Pose pose, VertexConsumer out, double angle,
			double y0, double height, double backWidth, double frontWidth,
			double backZ, double frontZ, int light,
			float u0, float v0, float u1, float v1) {
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		double y1 = y0 + height;
		Vec3 bl = rotate(-backWidth,y0,backZ,sin,cos), br = rotate(backWidth,y0,backZ,sin,cos);
		Vec3 tl = rotate(-backWidth,y1,backZ,sin,cos), tr = rotate(backWidth,y1,backZ,sin,cos);
		Vec3 fbl = rotate(-frontWidth,y0,frontZ,sin,cos), fbr = rotate(frontWidth,y0,frontZ,sin,cos);
		Vec3 ftl = rotate(-frontWidth,y1,frontZ,sin,cos), ftr = rotate(frontWidth,y1,frontZ,sin,cos);
		pointQuad(pose,out,tl,tr,br,bl,0xffacacac,light,u0,v0,u1,v1);
		pointQuad(pose,out,ftr,ftl,fbl,fbr,0xffacacac,light,u0,v0,u1,v1);
		pointQuad(pose,out,ftl,ftr,tr,tl,0xffffffff,light,u0,v0,u1,v1);
		pointQuad(pose,out,bl,br,fbr,fbl,0xff808080,light,u0,v0,u1,v1);
		pointQuad(pose,out,tl,bl,fbl,ftl,0xffacacac,light,u0,v0,u1,v1);
		pointQuad(pose,out,ftr,fbr,br,tr,0xffacacac,light,u0,v0,u1,v1);
	}

	/** Source pylon-stone square sleeve between the two extending rows. */
	private static void renderCollar(PoseStack.Pose pose, VertexConsumer out, State state) {
		double top = state.extension1 + 1;
		double bottom = top - 0.1875;
		double outer = 0.40625;
		double inner = 0.21875;
		squareRing(pose, out, outer, inner, bottom, top, 0xffffffff, state.lightCoords);
	}

	private static void submitSymbols(State state, PoseStack poseStack, SubmitNodeCollector collector,
			double deployed) {
		float brightness = (float)Math.pow(deployed, 6);
		if (brightness <= 0.002F) return;
		int cell = state.towerTexture;
		float u0 = (cell % 8) / 8F;
		float v0 = (cell / 8) / 8F;
		float u1 = u0 + 1F / 8F;
		float v1 = v0 + 1F / 8F;
		int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(0xffffff, brightness);
		double y = state.extension0 + state.extension1 + state.extension2 - 0.625;
		double rotation = -(state.tick / 1.5D) % 360;
		for (int i = 0; i < 6; i++) {
			PoseStack glyphPose = copy(poseStack);
			glyphPose.translate(0, y, 0);
			glyphPose.mulPose(Axis.YP.rotationDegrees((float)(rotation + i * 60)));
			glyphPose.translate(0, 0, 1.75);
			PoseStack.Pose matrix = glyphPose.last();
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(SYMBOLS),
					(pose, out) -> quad(matrix, out, -1.5, 0, 1.5, 3, color, u0, v1, u1, v0));
		}
	}

	private static void submitFlare(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, double deployed) {
		float factor = (float)(0.125 + 0.875 * ((state.extension1 + state.extension2)
				/ (TileEntityDataNode.EXTENSION_LIMIT_1 + TileEntityDataNode.EXTENSION_LIMIT_2)));
		int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(0xb0e0ff, factor);
		PoseStack flarePose = copy(poseStack);
		flarePose.translate(0, state.extension0 + state.extension1 + state.extension2 + 0.75, 0);
		flarePose.mulPose(camera.orientation);
		PoseStack.Pose matrix = flarePose.last();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(FLARE),
				(pose, out) -> quad(matrix, out, -3, -3, 3, 3, color, 0, 1, 1, 0));
	}

	/** Exact alternating 90/15/15-degree V33a prism outline and texture strip. */
	private static void submitPrism(State state, PoseStack poseStack, SubmitNodeCollector collector,
			double deployed) {
		PoseStack prismPose = copy(poseStack);
		double y = state.extension1 + state.extension2
				+ 0.0625 * Math.sin((state.tick / 8D) % (Math.PI * 2));
		prismPose.translate(0, y, 0);
		PoseStack.Pose matrix = prismPose.last();
		float factor = (float)(0.5 + 0.5 * deployed);
		int blue = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(0xa0e0ff, factor);
		int white = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(0xffffff, factor);
		double angle = 45 + state.tick * 2;
		List<Vec3> ring = new ArrayList<>();
		double[] steps = {90, 15, 15};
		double[] radii = {0.19140625, 0.21875, 0.19140625};
		int step = 0;
		for (double a = angle; a <= angle + 360.001; a += steps[step]) {
			double r = radii[step];
			ring.add(new Vec3(r * Math.cos(Math.toRadians(a)), 0, r * Math.sin(Math.toRadians(a))));
			step = (step + 1) % 3;
		}
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(NODE),
				(pose, out) -> prism(matrix, out, ring, 1.5, blue, white));
	}

	private static void submitSkyBeam(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		PoseStack beamPose = copy(poseStack);
		PoseStack.Pose matrix = beamPose.last();
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(),
				(pose, out) -> twistingBeam(matrix, out, state.tick));
	}

	private static void twistingBeam(PoseStack.Pose pose, VertexConsumer out, double tick) {
		// V33a is a 12-sided twisting cage in six-block sections, not two long crossed ribbons.
		// Closing each ring and both half-segment surfaces keeps it coherent at long range.
		double lowerPhase = tick * 3.2;
		double upperPhase = tick * 4.1;
		int c1 = 0xb8d8f2ff;
		int c2 = 0xa873dcff;
		for (double dy = 0; dy < 128; dy += 6) {
			double y0 = dy + 0.5;
			double y1 = y0 + 6;
			double ym = (y0 + y1) * 0.5;
			for (int side = 0; side < 12; side++) {
				double a0 = Math.toRadians(side * 30 + lowerPhase);
				double a0n = Math.toRadians((side + 1) * 30 + lowerPhase);
				double a1 = Math.toRadians(side * 30 + upperPhase);
				double a1n = Math.toRadians((side + 1) * 30 + upperPhase);
				Vec3 lo = radial(a0, y0), lon = radial(a0n, y0);
				Vec3 hi = radial(a1, y1), hin = radial(a1n, y1);
				Vec3 mid = lo.add(hi).scale(0.5), midn = lon.add(hin).scale(0.5);
				mid = new Vec3(mid.x, ym, mid.z);
				midn = new Vec3(midn.x, ym, midn.z);
				colorQuad(pose,out,lo,lon,midn,mid,c1);
				colorQuad(pose,out,mid,midn,hin,hi,c2);
				// Source also joins each end ring, making the helix readable instead of dashed.
				colorQuad(pose,out,lo,lon,
						new Vec3(lon.x * 0.82, y0, lon.z * 0.82),
						new Vec3(lo.x * 0.82, y0, lo.z * 0.82),c1);
			}
		}
	}

	private static Vec3 radial(double angle, double y) {
		return new Vec3(0.25 * Math.cos(angle), y, 0.25 * Math.sin(angle));
	}

	private static void colorQuad(PoseStack.Pose pose, VertexConsumer out,
			Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		out.addVertex(pose,(float)a.x,(float)a.y,(float)a.z).setColor(color);
		out.addVertex(pose,(float)b.x,(float)b.y,(float)b.z).setColor(color);
		out.addVertex(pose,(float)c.x,(float)c.y,(float)c.z).setColor(color);
		out.addVertex(pose,(float)d.x,(float)d.y,(float)d.z).setColor(color);
	}

	private static void prism(PoseStack.Pose pose, VertexConsumer out, List<Vec3> ring,
			double height, int bottom, int top) {
		for (int i = 0; i < ring.size() - 1; i++) {
			Vec3 a = ring.get(i), b = ring.get(i + 1);
			quad(pose, out, a.x, 0, a.z, a.x, height, a.z,
					b.x, height, b.z, b.x, 0, b.z, top, 49F / 64F, 94F / 96F, 1, 34F / 96F);
			// Triangulated caps encoded as degenerate quads for the registered QUADS pipeline.
			quad(pose, out, 0, 0, 0, a.x, 0, a.z, b.x, 0, b.z, 0, 0, 0, bottom, 0.78F, 0.31F, 0.98F, 0.18F);
			quad(pose, out, 0, height, 0, b.x, height, b.z, a.x, height, a.z,
					0, height, 0, top, 0.78F, 0.31F, 0.98F, 0.18F);
		}
	}

	private static void box(PoseStack.Pose pose, VertexConsumer out, double x0, double y0, double z0,
			double x1, double y1, double z1, int color, int light) {
		quadLit(pose, out, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, color,light, 0,-1,0);
		quadLit(pose, out, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, color,light, 0,1,0);
		quadLit(pose, out, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, color,light, 0,0,1);
		quadLit(pose, out, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, color,light, 0,0,-1);
		quadLit(pose, out, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, color,light, -1,0,0);
		quadLit(pose, out, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, color,light, 1,0,0);
	}

	private static void frustum(PoseStack.Pose pose, VertexConsumer out,
			double x0, double y0, double x1, double ix0, double iy, double ix1,
			int color, int light, float u, float v) {
		quadLit(pose,out,x0,y0,x0,x1,y0,x0,ix1,iy,ix0,ix0,iy,ix0,color,light,0,0,-1);
		quadLit(pose,out,x1,y0,x1,x0,y0,x1,ix0,iy,ix1,ix1,iy,ix1,color,light,0,0,1);
		quadLit(pose,out,x0,y0,x1,x0,y0,x0,ix0,iy,ix0,ix0,iy,ix1,color,light,-1,0,0);
		quadLit(pose,out,x1,y0,x0,x1,y0,x1,ix1,iy,ix1,ix1,iy,ix0,color,light,1,0,0);
	}

	private static void squareRing(PoseStack.Pose pose, VertexConsumer out, double outer,
			double inner, double bottom, double top, int color, int light) {
		// Four top and bottom ring strips.
		quadLit(pose,out,-outer,top,-outer, outer,top,-outer, inner,top,-inner,-inner,top,-inner,color,light,0,1,0);
		quadLit(pose,out, outer,top,-outer, outer,top, outer, inner,top, inner, inner,top,-inner,color,light,0,1,0);
		quadLit(pose,out, outer,top, outer,-outer,top, outer,-inner,top, inner, inner,top, inner,color,light,0,1,0);
		quadLit(pose,out,-outer,top, outer,-outer,top,-outer,-inner,top,-inner,-inner,top, inner,color,light,0,1,0);
		quadLit(pose,out,-inner,bottom,-inner, inner,bottom,-inner, outer,bottom,-outer,-outer,bottom,-outer,color,light,0,-1,0);
		quadLit(pose,out, inner,bottom,-inner, inner,bottom, inner, outer,bottom, outer, outer,bottom,-outer,color,light,0,-1,0);
		quadLit(pose,out, inner,bottom, inner,-inner,bottom, inner,-outer,bottom, outer, outer,bottom, outer,color,light,0,-1,0);
		quadLit(pose,out,-inner,bottom, inner,-inner,bottom,-inner,-outer,bottom,-outer,-outer,bottom, outer,color,light,0,-1,0);
		// Outer and inner sleeve walls; inner winding faces the cavity.
		quadLit(pose,out,-outer,bottom,-outer, outer,bottom,-outer, outer,top,-outer,-outer,top,-outer,color,light,0,0,-1);
		quadLit(pose,out, outer,bottom,-outer, outer,bottom, outer, outer,top, outer, outer,top,-outer,color,light,1,0,0);
		quadLit(pose,out, outer,bottom, outer,-outer,bottom, outer,-outer,top, outer, outer,top, outer,color,light,0,0,1);
		quadLit(pose,out,-outer,bottom, outer,-outer,bottom,-outer,-outer,top,-outer,-outer,top, outer,color,light,-1,0,0);
		quadLit(pose,out, inner,bottom,-inner,-inner,bottom,-inner,-inner,top,-inner, inner,top,-inner,color,light,0,0,1);
		quadLit(pose,out, inner,bottom, inner, inner,bottom,-inner, inner,top,-inner, inner,top, inner,color,light,-1,0,0);
		quadLit(pose,out,-inner,bottom, inner, inner,bottom, inner, inner,top, inner,-inner,top, inner,color,light,0,0,-1);
		quadLit(pose,out,-inner,bottom,-inner,-inner,bottom, inner,-inner,top, inner,-inner,top,-inner,color,light,1,0,0);
	}

	private static Vec3 rotate(double x, double y, double z, double sin, double cos) {
		return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
	}

	private static void pointQuad(PoseStack.Pose pose, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
			int color, int light, float u0, float v0, float u1, float v1) {
		quadLit(pose, out, a.x,a.y,a.z, b.x,b.y,b.z, c.x,c.y,c.z, d.x,d.y,d.z,
				color, light, 0, 1, 0, u0,v0,u1,v1);
	}

	private static void quadLit(PoseStack.Pose pose, VertexConsumer out,
			double ax,double ay,double az,double bx,double by,double bz,
			double cx,double cy,double cz,double dx,double dy,double dz,
			int color,int light,float nx,float ny,float nz) {
		quadLit(pose,out,ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz,color,light,nx,ny,nz,0,1,1,0);
	}

	private static void quadLit(PoseStack.Pose pose, VertexConsumer out,
			double ax,double ay,double az,double bx,double by,double bz,
			double cx,double cy,double cz,double dx,double dy,double dz,
			int color,int light,float nx,float ny,float nz,float u0,float v0,float u1,float v1) {
		lit(out,pose,ax,ay,az,color,u0,v0,light,nx,ny,nz);
		lit(out,pose,bx,by,bz,color,u1,v0,light,nx,ny,nz);
		lit(out,pose,cx,cy,cz,color,u1,v1,light,nx,ny,nz);
		lit(out,pose,dx,dy,dz,color,u0,v1,light,nx,ny,nz);
	}

	private static void lit(VertexConsumer out, PoseStack.Pose pose, double x,double y,double z,
			int color,float u,float v,int light,float nx,float ny,float nz) {
		out.addVertex(pose,(float)x,(float)y,(float)z).setColor(color).setUv(u,v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out, double minX,double minY,
			double maxX,double maxY,int color,float u0,float v0,float u1,float v1) {
		quad(pose,out,minX,minY,0,maxX,minY,0,maxX,maxY,0,minX,maxY,0,color,u0,v0,u1,v1);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out,
			double ax,double ay,double az,double bx,double by,double bz,
			double cx,double cy,double cz,double dx,double dy,double dz,
			int color,float u0,float v0,float u1,float v1) {
		out.addVertex(pose,(float)ax,(float)ay,(float)az).setUv(u0,v0).setColor(color);
		out.addVertex(pose,(float)bx,(float)by,(float)bz).setUv(u1,v0).setColor(color);
		out.addVertex(pose,(float)cx,(float)cy,(float)cz).setUv(u1,v1).setColor(color);
		out.addVertex(pose,(float)dx,(float)dy,(float)dz).setUv(u0,v1).setColor(color);
	}

	private static double deployment(State state) {
		return (state.extension0 + state.extension1 + state.extension2)
				/ (TileEntityDataNode.EXTENSION_LIMIT_0 + TileEntityDataNode.EXTENSION_LIMIT_1
						+ TileEntityDataNode.EXTENSION_LIMIT_2);
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
	public int getViewDistance() {
		return 256;
	}

	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityDataNode node) {
		BlockPos pos = node.getBlockPos();
		// Includes the deployed body, six rotating symbols/flare and the complete 128-block beam.
		// The default one-block box caused the renderer to vanish as soon as its controller left the
		// camera frustum even while the tower or beam remained plainly visible.
		return new net.minecraft.world.phys.AABB(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4,
				pos.getX() + 5, pos.getY() + 130, pos.getZ() + 5);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	public static final class State extends BlockEntityRenderState {
		private double extension0;
		private double extension1;
		private double extension2;
		private double rotation;
		private float tick;
		private float scanProgress;
		private int towerTexture = -1;
		private boolean scanned;
	}
}
