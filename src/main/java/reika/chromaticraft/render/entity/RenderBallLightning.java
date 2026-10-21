package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;

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
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
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
		state.alive = !entity.isRemoved();
		state.bearingPosition = entity.position();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ROUND_FLARE);
		poseStack.pushPose();
		// V33a uses the entity-to-camera bearing, rather than the camera's rotation/roll.
		double dx = state.bearingPosition.x-camera.pos.x;
		double dy = state.bearingPosition.y-camera.pos.y;
		double dz = state.bearingPosition.z-camera.pos.z;
		if (state.alive && dx*dx+dy*dy+dz*dz > 1e-12) {
			double[] angles = ReikaPhysicsHelper.cartesianToPolar(dx, dy, dz);
			poseStack.rotate(Axis.YP.rotationDegrees((float)angles[2]));
			poseStack.rotate(Axis.XP.rotationDegrees((float)(90-angles[1])));
		}
		PoseStack.Pose renderPose = poseStack.last().copy();
		int color = state.color & 0xffffff;
		ScreenParticleRendering.submitGeometry(collector, poseStack,
				EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS, false),
				(ignored, vertices) -> {
					quad(renderPose, vertices, sprite, 1F, color, 0);
					int bright = ReikaColorAPI.getColorWithBrightnessMultiplier(
							color | 0x4c4c4c, 1.5F);
					quad(renderPose, vertices, sprite, 0.67F, bright, 0.001F);
					quad(renderPose, vertices, sprite, 0.33F, 0xffffff, 0.002F);
				});
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vertices, TextureAtlasSprite sprite,
			float radius, int color, float depth) {
		vertices.addVertex(pose, -radius, -radius, depth).setUv(sprite.getU0(), sprite.getV0()).setColor(0xff000000 | color)
				.setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 1, 0);
		vertices.addVertex(pose, radius, -radius, depth).setUv(sprite.getU1(), sprite.getV0()).setColor(0xff000000 | color)
				.setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 1, 0);
		vertices.addVertex(pose, radius, radius, depth).setUv(sprite.getU1(), sprite.getV1()).setColor(0xff000000 | color)
				.setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 1, 0);
		vertices.addVertex(pose, -radius, radius, depth).setUv(sprite.getU0(), sprite.getV1()).setColor(0xff000000 | color)
				.setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 1, 0);
	}

	public static final class State extends EntityRenderState {
		private int color;
		private boolean alive;
		private Vec3 bearingPosition = Vec3.ZERO;
	}
}
