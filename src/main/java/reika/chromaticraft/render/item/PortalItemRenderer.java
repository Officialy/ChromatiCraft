package reika.chromaticraft.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.ChromaRenderPipelines;

import java.awt.*;
import java.util.function.Consumer;

/** V33a Portal Rift item: an unlit End-portal cube with a cycling additive flare on all faces. */
public final class PortalItemRenderer implements NoDataSpecialModelRenderer {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"portal_rift");
	private static final Identifier END_PORTAL = Identifier.withDefaultNamespace(
			"textures/entity/end_portal/end_portal.png");
	private static final Identifier BIG_FLARE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/block/icons/bigflare.png");

	/**
	 * V33a's BIGFLARE was an atlas icon, which the atlas animated: 80 square frames of 256x256, two ticks each
	 * (bigflare.png.mcmeta). Bound as a raw texture, the whole 256x20480 strip must be stepped by hand, or every
	 * frame is squashed onto the cube face at once.
	 */
	private static final int FLARE_FRAMES = 80;
	private static final int FLARE_FRAME_TICKS = 2;

	private PortalItemRenderer() {}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		poseStack.translate(0, -0.125, 0);
		PoseStack skinPose = copy(poseStack);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(END_PORTAL),
				(unused, vertices) -> renderLitCube(skinPose.last(), vertices));

		PoseStack flarePose = copy(poseStack);
		int color = 0xff000000 | (Color.HSBtoRGB(
				(System.currentTimeMillis() % 15_000L) / 15_000F, 1, 1) & 0xffffff);
		int frame = (int)(System.currentTimeMillis() / (50L * FLARE_FRAME_TICKS) % FLARE_FRAMES);
		float v0 = frame / (float)FLARE_FRAMES;
		float v1 = (frame + 1) / (float)FLARE_FRAMES;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(BIG_FLARE),
				(unused, vertices) -> renderFlareCube(flarePose.last(), vertices, color, v0, v1));
		poseStack.popPose();
	}

	private static void renderLitCube(PoseStack.Pose pose, VertexConsumer out) {
		litFace(pose, out, 0,0,1, 1,0,1, 1,1,1, 0,1,1, 0,0,1);
		litFace(pose, out, 0,1,0, 1,1,0, 1,0,0, 0,0,0, 0,0,-1);
		litFace(pose, out, 1,1,0, 1,1,1, 1,0,1, 1,0,0, 1,0,0);
		litFace(pose, out, 0,0,0, 0,0,1, 0,1,1, 0,1,0, -1,0,0);
		litFace(pose, out, 0,1,0, 0,1,1, 1,1,1, 1,1,0, 0,1,0);
		litFace(pose, out, 1,0,0, 1,0,1, 0,0,1, 0,0,0, 0,-1,0);
	}

	private static void renderFlareCube(PoseStack.Pose pose, VertexConsumer out, int color, float v0, float v1) {
		face(pose, out, 0,0,1, 1,0,1, 1,1,1, 0,1,1, color, v0, v1);
		face(pose, out, 0,1,0, 1,1,0, 1,0,0, 0,0,0, color, v0, v1);
		face(pose, out, 1,1,0, 1,1,1, 1,0,1, 1,0,0, color, v0, v1);
		face(pose, out, 0,0,0, 0,0,1, 0,1,1, 0,1,0, color, v0, v1);
		face(pose, out, 0,1,0, 0,1,1, 1,1,1, 1,1,0, color, v0, v1);
		face(pose, out, 1,0,0, 1,0,1, 0,0,1, 0,0,0, color, v0, v1);
	}

	private static void litFace(PoseStack.Pose pose, VertexConsumer out,
			float x1,float y1,float z1, float x2,float y2,float z2,
			float x3,float y3,float z3, float x4,float y4,float z4,
			float nx,float ny,float nz) {
		litVertex(pose,out,x1,y1,z1,0,0,nx,ny,nz);
		litVertex(pose,out,x2,y2,z2,1,0,nx,ny,nz);
		litVertex(pose,out,x3,y3,z3,1,1,nx,ny,nz);
		litVertex(pose,out,x4,y4,z4,0,1,nx,ny,nz);
	}

	private static void litVertex(PoseStack.Pose pose, VertexConsumer out,
			float x,float y,float z, float u,float v, float nx,float ny,float nz) {
		out.addVertex(pose, x, y, z).setUv(u, v).setColor(0xffffffff)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	private static void face(PoseStack.Pose pose, VertexConsumer out,
			float x1,float y1,float z1, float x2,float y2,float z2,
			float x3,float y3,float z3, float x4,float y4,float z4, int color, float v0, float v1) {
		out.addVertex(pose,x1,y1,z1).setUv(0,v0).setColor(color);
		out.addVertex(pose,x2,y2,z2).setUv(1,v0).setColor(color);
		out.addVertex(pose,x3,y3,z3).setUv(1,v1).setColor(color);
		out.addVertex(pose,x4,y4,z4).setUv(0,v1).setColor(color);
	}

	private static PoseStack copy(PoseStack source) {
		PoseStack copy = new PoseStack();
		copy.last().set(source.last());
		return copy;
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, -0.125F, 0));
		output.accept(new Vector3f(1, 0.875F, 1));
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
		@Override public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() { return MAP_CODEC; }
		@Override public PortalItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new PortalItemRenderer();
		}
	}
}
