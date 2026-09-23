package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

public final class RenderBallLightning extends EntityRenderer<EntityBallLightning, RenderBallLightning.State> {

	private static final Identifier ROUND_FLARE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "block/icons/roundflare");

	public RenderBallLightning(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	@Override public State createRenderState() { return new State(); }

	@Override
	public void extractRenderState(EntityBallLightning entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.color = entity.getRenderColor();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ROUND_FLARE);
		poseStack.pushPose();
		poseStack.mulPose(camera.orientation);
		PoseStack renderPose = new PoseStack();
		renderPose.last().set(poseStack.last());
		int color = state.color;
		collector.submitCustomGeometry(poseStack,
				ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(ignored, vertices) -> {
					quad(renderPose.last(), vertices, sprite, 1F, color, 0);
					int bright = ReikaColorAPI.getColorWithBrightnessMultiplier(
							color | 0x4c4c4c, 1.5F);
					quad(renderPose.last(), vertices, sprite, 0.67F, bright, 0.001F);
					quad(renderPose.last(), vertices, sprite, 0.33F, 0xffffff, 0.002F);
				});
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vertices, TextureAtlasSprite sprite,
			float radius, int color, float depth) {
		vertices.addVertex(pose, -radius, -radius, depth).setUv(sprite.getU0(), sprite.getV0()).setColor(0xff000000 | color);
		vertices.addVertex(pose, radius, -radius, depth).setUv(sprite.getU1(), sprite.getV0()).setColor(0xff000000 | color);
		vertices.addVertex(pose, radius, radius, depth).setUv(sprite.getU1(), sprite.getV1()).setColor(0xff000000 | color);
		vertices.addVertex(pose, -radius, radius, depth).setUv(sprite.getU0(), sprite.getV1()).setColor(0xff000000 | color);
	}

	public static final class State extends EntityRenderState {
		private int color;
	}
}
