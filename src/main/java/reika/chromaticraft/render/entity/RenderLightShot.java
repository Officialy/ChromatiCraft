/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.auxiliary.render.ChromaColorTiles;
import reika.chromaticraft.entity.EntityLightShot;
import reika.chromaticraft.registry.ChromaIcons;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderLightShot}: a white chroma spark (0.25 across each way) over a larger spinning flare (0.6), the
 * flare cycling through the chroma colour ramp and pulsing, both turned to face the camera by the camera-to-shot
 * bearing as V33a did. The spark is alpha-blended, the flare additive-dark, neither writing depth.
 *
 * <p>Both source icons use the managed animated block atlas. The spark is drawn before the flare,
 * with independent ordinary-alpha and RGB-screen blends on the composed world color.
 */
public final class RenderLightShot extends EntityRenderer<EntityLightShot, RenderLightShot.State> {

	private static final ColorBlendList colors = new ColorBlendList(5, ChromaColorTiles.getChromaColorTiles());

	public RenderLightShot(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EntityLightShot e, State state, float partialTick) {
		super.extractRenderState(e, state, partialTick);
		state.flareColor = flareColor(e.tickCount, e.getId(), System.currentTimeMillis());
		state.bearingPosition = e.position();
		state.alive = !e.isRemoved();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		Vec3 bearing = state.bearingPosition.subtract(camera.pos);
		if (state.alive && bearing.lengthSqr() > 1e-12) {
			double[] angs = ReikaPhysicsHelper.cartesianToPolar(bearing.x, bearing.y, bearing.z);
			poseStack.rotate(Axis.YP.rotationDegrees((float)angs[2]));
			poseStack.rotate(Axis.XP.rotationDegrees((float)(90-angs[1])));
		}
		PoseStack.Pose pose = poseStack.last().copy();
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite spark = atlas.getSprite(ChromaIcons.CHROMA.getIcon());
		TextureAtlasSprite flare = atlas.getSprite(ChromaIcons.SPINFLARE.getIcon());
		// Explicit collector orders retain DEFAULT then ADDITIVEDARK even when native batching groups render types.
		collector.order(0).submitSpecial(ScreenParticleRendering.PHASE, new CustomFeatureRenderer.Submit(
				pose.copy(), EntityLighting.LIGHTMAP_ONLY.alphaSprite(TextureAtlas.LOCATION_BLOCKS, false),
				(ignored, out) -> quad(pose, out, 0.25F, 0, spark.getU0(), spark.getV0(), spark.getU1(), spark.getV1(), 0xffffffff)));
		int color = 0xff000000 | state.flareColor;
		collector.order(1).submitSpecial(ScreenParticleRendering.PHASE, new CustomFeatureRenderer.Submit(
				pose.copy(), EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS, false),
				(ignored, out) -> quad(pose, out, 0.6F, -0.05F, flare.getU0(), flare.getV0(), flare.getU1(), flare.getV1(), color)));
		poseStack.popPose();
	}

	/** The original whole-tick palette cycle and wall-clock pulse, sampled once during extraction. */
	public static int flareColor(int ticks, int entityId, long millis) {
		double mod = 15 + entityId % 15;
		float pulse = (float)(0.8 + 0.2 * Math.sin((millis / mod) % 360));
		return ReikaColorAPI.getColorWithBrightnessMultiplier(colors.getColor(ticks), pulse);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v5, float s, float z, float u, float v, float du, float dv,
			int color) {
		v5.addVertex(pose, -s, -s, z).setUv(u, v).setColor(color).setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 0, 1);
		v5.addVertex(pose, s, -s, z).setUv(du, v).setColor(color).setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 0, 1);
		v5.addVertex(pose, s, s, z).setUv(du, dv).setColor(color).setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 0, 1);
		v5.addVertex(pose, -s, s, z).setUv(u, dv).setColor(color).setLight(240).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(pose, 0, 0, 1);
	}

	public static final class State extends EntityRenderState {
		private int flareColor;
		private boolean alive;
		private Vec3 bearingPosition = Vec3.ZERO;
	}

}
