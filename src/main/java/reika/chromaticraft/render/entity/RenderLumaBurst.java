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
import reika.chromaticraft.entity.EntityLumaBurst;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a RenderLumaBurst: two nested camera-facing, fullbright, additive quads (an opaque outer flare
 * and a smaller inner core mixed toward white), on the same flare sprite/RenderType template as the
 * accepted {@link RenderPylonOverloadShock}. V33a oriented the quad by the polar angle from the
 * render camera to the entity; the modern {@code camera.orientation} billboard used here produces
 * the same camera-facing result.
 */
public class RenderLumaBurst extends EntityRenderer<EntityLumaBurst, RenderLumaBurst.State> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/block/icons/flare.png");

	/** V33a: s1 = 0.1875 * 1.125 (outer), halved for the inner core. */
	private static final float OUTER_SIZE = (float)(0.1875 * 1.125);
	private static final float INNER_SIZE = OUTER_SIZE * 0.5F;

	public RenderLumaBurst(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EntityLumaBurst entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.color = 0xFF000000 | entity.getColor().getColor();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.mulPose(camera.orientation);
		int innerRgb = ReikaColorAPI.mixColors(state.color & 0xFFFFFF, 0xFFFFFF, 0.25F) & 0xFFFFFF;
		int inner = 0xFF000000 | innerRgb;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE),
				(pose, buffer) -> {
					quad(pose, buffer, state.color, OUTER_SIZE);
					quad(pose, buffer, inner, INNER_SIZE);
				});
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, float size) {
		int light = 0xF000F0;
		buffer.addVertex(pose, -size, -size, 0).setColor(color).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, size, -size, 0).setColor(color).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, size, size, 0).setColor(color).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
		buffer.addVertex(pose, -size, size, 0).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
	}

	public static final class State extends EntityRenderState {
		private int color = 0xFFFFFFFF;
	}
}
