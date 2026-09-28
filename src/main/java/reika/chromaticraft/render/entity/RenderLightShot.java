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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.render.ChromaColorTiles;
import reika.chromaticraft.entity.EntityLightShot;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderLightShot}: a white chroma spark (0.25 across each way) over a larger spinning flare (0.6), the
 * flare cycling through the chroma colour ramp and pulsing, both turned to face the camera by the camera-to-shot
 * bearing as V33a did. The spark is alpha-blended, the flare additive-dark, neither writing depth.
 *
 * <p>V33a's flare, {@code ChromaIcons.SPINFLARE}, is the 180-frame {@code rotating_flare_pulse} strip; it is stepped by
 * hand here, one frame per tick of the clock, as {@code RenderStructureController} does.
 */
public final class RenderLightShot extends EntityRenderer<EntityLightShot, RenderLightShot.State> {

	private static final Identifier CHROMA = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/chroma_particle");
	private static final Identifier SPINFLARE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/block/icons/rotating_flare_pulse.png");
	private static final int FRAMES = 180;

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
		double mod = 15+(e.getId())%15;
		double f = 0.8+0.2*Math.sin((System.currentTimeMillis()/mod)%360);
		int c = colors.getColor(e.tickCount);
		state.flareColor = ReikaColorAPI.getColorWithBrightnessMultiplier(c, (float)f);
		state.alive = !e.isRemoved();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		if (state.alive) {
			Vec3 cam = camera.pos;
			double[] angs = ReikaPhysicsHelper.cartesianToPolar(state.x-cam.x, state.y-cam.y, state.z-cam.z);
			poseStack.rotate(Axis.YP.rotationDegrees((float)angs[2]));
			poseStack.rotate(Axis.XP.rotationDegrees((float)(90-angs[1])));
		}
		PoseStack.Pose pose = poseStack.last().copy();
		TextureAtlasSprite spark = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CHROMA);
		float s1 = 0.25F;
		float s2 = 0.6F;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.translucentSprite(TextureAtlas.LOCATION_BLOCKS),
				(ignored, v5) -> quad(pose, v5, s1, 0, spark.getU0(), spark.getV0(), spark.getU1(), spark.getV1(), 0xffffffff));
		int frame = (int)(System.currentTimeMillis()/50%FRAMES);
		float v0 = frame/(float)FRAMES;
		float v1 = v0+1F/FRAMES;
		int flare = 0xff000000 | state.flareColor;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(SPINFLARE),
				(ignored, v5) -> quad(pose, v5, s2, -0.05F, 0, v0, 1, v1, flare));
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v5, float s, float z, float u, float v, float du, float dv,
			int color) {
		v5.addVertex(pose, -s, -s, z).setUv(u, v).setColor(color);
		v5.addVertex(pose, s, -s, z).setUv(du, v).setColor(color);
		v5.addVertex(pose, s, s, z).setUv(du, dv).setColor(color);
		v5.addVertex(pose, -s, s, z).setUv(u, dv).setColor(color);
	}

	public static final class State extends EntityRenderState {
		private int flareColor;
		private boolean alive;
	}

}
