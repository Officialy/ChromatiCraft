/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code CrystalPlantRenderer}: once a bloom buds it grows a pointed pod of {@code crystal/bloom} over the
 * cross, in its element's colour at three-quarter saturation; while budding the pod is at 0.625 scale, off to one
 * side. A ripe bloom adds a camera-facing glow ({@code plant_glow}) and sparkle ({@code sparkle2}) above the pod
 * in its element's colour, ADDITIVEDARK with the source lightmap.
 *
 * <p>V33a drew in a frame flipped by {@code glScalef(1, -1, -1)} from {@code (x, y+1, z+1)} and nudged by 1/16;
 * every vertex here is that frame's point carried into block space, so the geometry is V33a's exactly.
 */
public final class CrystalPlantRenderer implements BlockEntityRenderer<TileEntityCrystalPlant, CrystalPlantRenderer.State> {

	private static final Identifier BULB = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/bloom");

	/** V33a drawBulb: half-width, body height and point height. */
	private static final double S = 0.1;
	private static final double H = 0.3;
	private static final double PH = 0.15;

	/**
	 * V33a drawBulb, as (x, y, z, u-slot, v-slot) per vertex in its flipped frame. The slots name V33a's texture
	 * coordinates: 0 = min, 1 = mid, 2 = max.
	 */
	private static final double[][] BULB_VERTICES = {
			//Bottom
			{0.5-S, 0, 0.5+S, 0, 2}, {0.5+S, 0, 0.5+S, 2, 2}, {0.5+S, 0, 0.5-S, 2, 0}, {0.5-S, 0, 0.5-S, 0, 0},
			//Top point
			{0.5-S, -H, 0.5-S, 0, 2}, {0.5, -H-PH/2, 0.5-S, 1, 2}, {0.5, -H-PH, 0.5, 1, 1}, {0.5-S, -H-PH/2, 0.5, 0, 1},
			{0.5-S, -H-PH/2, 0.5, 0, 1}, {0.5, -H-PH, 0.5, 1, 1}, {0.5, -H-PH/2, 0.5+S, 1, 0}, {0.5-S, -H, 0.5+S, 0, 0},
			{0.5+S, -H-PH/2, 0.5, 0, 1}, {0.5, -H-PH, 0.5, 1, 1}, {0.5, -H-PH/2, 0.5-S, 1, 0}, {0.5+S, -H, 0.5-S, 0, 0},
			{0.5+S, -H, 0.5+S, 0, 2}, {0.5, -H-PH/2, 0.5+S, 1, 2}, {0.5, -H-PH, 0.5, 1, 1}, {0.5+S, -H-PH/2, 0.5, 0, 1},
			//Sides
			{0.5+S, 0, 0.5, 0, 1}, {0.5+S, -H-PH/2, 0.5, 2, 1}, {0.5+S, -H, 0.5-S, 2, 0}, {0.5+S, 0, 0.5-S, 0, 0},
			{0.5+S, 0, 0.5+S, 0, 2}, {0.5+S, -H, 0.5+S, 2, 2}, {0.5+S, -H-PH/2, 0.5, 2, 1}, {0.5+S, 0, 0.5, 0, 1},
			{0.5-S, 0, 0.5, 0, 1}, {0.5-S, -H-PH/2, 0.5, 2, 1}, {0.5-S, -H, 0.5+S, 2, 2}, {0.5-S, 0, 0.5+S, 0, 2},
			{0.5-S, 0, 0.5-S, 0, 0}, {0.5-S, -H, 0.5-S, 2, 0}, {0.5-S, -H-PH/2, 0.5, 2, 1}, {0.5-S, 0, 0.5, 0, 1},
			{0.5, 0, 0.5+S, 0, 1}, {0.5, -H-PH/2, 0.5+S, 2, 1}, {0.5+S, -H, 0.5+S, 2, 2}, {0.5+S, 0, 0.5+S, 0, 2},
			{0.5-S, 0, 0.5+S, 0, 0}, {0.5-S, -H, 0.5+S, 2, 0}, {0.5, -H-PH/2, 0.5+S, 2, 1}, {0.5, 0, 0.5+S, 0, 1},
			{0.5+S, 0, 0.5-S, 0, 2}, {0.5+S, -H, 0.5-S, 2, 2}, {0.5, -H-PH/2, 0.5-S, 2, 1}, {0.5, 0, 0.5-S, 0, 1},
			{0.5, 0, 0.5-S, 0, 1}, {0.5, -H-PH/2, 0.5-S, 2, 1}, {0.5-S, -H, 0.5-S, 2, 0}, {0.5-S, 0, 0.5-S, 0, 0},
	};

	public CrystalPlantRenderer(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityCrystalPlant te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.pod = te.renderPod();
		state.ripe = te.emitsLight();
		state.color = te.getColor().color();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.pod)
			return;
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		Uv bulb = Uv.capture(atlas.getSprite(BULB));
		int bulbColor = 0xFF000000 | ReikaColorAPI.getModifiedSat(state.color, 0.75F);
		boolean ripe = state.ripe;
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, EntityLighting.LIGHTMAP_ONLY.translucent(TextureAtlas.LOCATION_BLOCKS,true),
                (pose,vertices)->bulb(pose,vertices,bulb,bulbColor,ripe,light));

		if (ripe) {
			poseStack.pushPose();
			poseStack.translate(0.5, 1.25, 0.5);
			poseStack.scale(1,-1,-1);
            poseStack.rotate(Axis.YP.rotationDegrees(camera.yRot));
            poseStack.rotate(Axis.XP.rotationDegrees(camera.xRot));
			int glowColor = 0xFF000000 | state.color;
			Uv glow = Uv.capture(atlas.getSprite(ChromaIcons.RADIATE.getIcon()));
			Uv sparkle = Uv.capture(atlas.getSprite(ChromaIcons.SPARKLE.getIcon()));
			ScreenParticleRendering.submitGeometry(collector,poseStack,EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS,true),
					(pose, vertices) -> {
						billboard(pose,vertices,glow,glowColor,light);
						billboard(pose,vertices,sparkle,glowColor,light);
					});
			poseStack.popPose();
		}
	}

	private static void bulb(PoseStack.Pose pose, VertexConsumer out, Uv ico, int color, boolean ripe, int light) {
		float[] us = {ico.u0, ico.u0+(ico.u1-ico.u0)/2, ico.u1};
		float[] vs = {ico.v0, ico.v0+(ico.v1-ico.v0)/2, ico.v1};
		for (double[] v : BULB_VERTICES) {
			double x = v[0];
			double y = v[1];
			double z = v[2];
			if (!ripe) {
				// V33a: glTranslated(0.2, 0, 0.2) then glScaled(0.625) while the pod is still budding.
				x = 0.2+0.625*x;
				y = 0.625*y;
				z = 0.2+0.625*z;
			}
			y += 0.0625;
			// The flipped frame: glTranslatef(x, y+1, z+1), glScalef(1, -1, -1).
			out.addVertex(pose, (float)x, (float)(1-y), (float)(1-z))
					.setUv(us[(int)v[3]], vs[(int)v[4]]).setColor(color)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,1,0);
		}
	}

	/** V33a's unit quad at a quarter scale, facing the camera. */
    private static void billboard(PoseStack.Pose pose,VertexConsumer out,Uv ico,int color,int light) {
        float s=.25F;
        vertex(pose,out,-s,-s,ico.u0,ico.v0,color,light);
        vertex(pose,out,s,-s,ico.u1,ico.v0,color,light);
        vertex(pose,out,s,s,ico.u1,ico.v1,color,light);
        vertex(pose,out,-s,s,ico.u0,ico.v1,color,light);
    }
    private static void vertex(PoseStack.Pose pose,VertexConsumer out,float x,float y,float u,float v,int color,int light) {
        out.addVertex(pose,x,y,0).setUv(u,v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose,0,1,0);
    }
    private record Uv(float u0,float u1,float v0,float v1) {
        static Uv capture(TextureAtlasSprite sprite){return new Uv(sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1());}
    }

	/** The pod and its glow rise half a block above the plant's own cube. */
	@Override
	public AABB getRenderBoundingBox(TileEntityCrystalPlant te) {
		return new AABB(te.getBlockPos()).expandTowards(0, 0.5, 0);
	}

	public static final class State extends BlockEntityRenderState {
		private boolean pod;
		private boolean ripe;
		private int color;
	}

}
