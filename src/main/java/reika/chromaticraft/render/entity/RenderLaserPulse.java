package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityLaserPulse;

/** Fullbright camera-facing V33a laser pulse using its original fade-star sprite. */
public final class RenderLaserPulse extends EntityRenderer<EntityLaserPulse, RenderLaserPulse.State> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/fade_star.png");
	private static final float SIZE = 0.1875F;

	public RenderLaserPulse(EntityRendererProvider.Context context) { super(context); }
	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(EntityLaserPulse entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.color = 0xFF000000 | entity.color().renderColor();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.mulPose(camera.orientation);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE),
				(pose, buffer) -> quad(pose, buffer, state.color));
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		int light = 0xF000F0;
		buffer.addVertex(pose, -SIZE, -SIZE, 0).setColor(color).setUv(0, 1)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, SIZE, -SIZE, 0).setColor(color).setUv(1, 1)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, SIZE, SIZE, 0).setColor(color).setUv(1, 0)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, -SIZE, SIZE, 0).setColor(color).setUv(0, 0)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
	}

	public static final class State extends EntityRenderState {
		private int color = 0xFFFFFFFF;
	}
}
