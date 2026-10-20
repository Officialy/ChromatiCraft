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

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import reika.chromaticraft.tileentity.transport.TileEntityLaunchPad;

/**
 * V33a {@code RenderLaunchPad}: the pad block itself is an ordinary textured cube; over a built pad this draws the charge
 * as a fan sweeping clockwise across the whole 3x3 frame, just above its surface, as far round as the charge has got --
 * additive-dark, unlit, white, with a green fan under it when enhanced. The 1.7.10 triangle fan is emitted as
 * triangles (a quad repeating its last corner) for the quad pipeline.
 */
public final class RenderLaunchPad implements BlockEntityRenderer<TileEntityLaunchPad, RenderLaunchPad.State> {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/misc/launchcharge_additive.png");

	public RenderLaunchPad(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityLaunchPad te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.charge = te.hasLevel() && te.hasStructure() ? te.getChargeFraction() : 0;
		state.enhanced = te.isEnhanced();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			net.minecraft.client.renderer.state.level.CameraRenderState camera) {
		float f = state.charge;
		if (f <= 0)
			return;
		PoseStack.Pose pose = poseStack.last().copy();
		boolean enhanced = state.enhanced;
		ScreenParticleRendering.submitGeometry(collector, poseStack, EntityLighting.FULL_BRIGHT.screen(TEXTURE,true), (ignored, v5) -> {
			if (enhanced) {
				renderFan(pose, v5, f, 0x00ff00, 0x00ff00);
				renderFan(pose, v5, f, 0xffffff, 0x00ff00);
			}
			else {
				renderFan(pose, v5, f, 0xffffff, 0xffffff);
			}
		});
	}

	private static void renderFan(PoseStack.Pose pose, VertexConsumer v5, float f, int color, int centerColor) {
		double s = 1.5;
		double lim = f*360;
		double da = 5;
		float h = 1.001F;
		int c = 0xff000000 | color;

		double px = Double.NaN, pz = 0, pu = 0, pv = 0;
		for (double a = 0; a <= lim; a += da) {
			double ang = Math.toRadians(a);
			double r = s*Math.min(Math.abs(1D/Math.cos(ang)), Math.abs(1D/Math.sin(ang)));
			double dx = r*Math.cos(ang);
			double dz = r*Math.sin(ang);
			double u = 0.5+0.5*dx/s;
			double v = 0.5+0.5*dz/s;
			if (!Double.isNaN(px)) {
                // Convert the source CW fan into native CCW top faces; keep its first anchor color.
                vertex(pose,v5,.5F,h,.5F,.5F,.5F,0xff000000|centerColor);
                vertex(pose,v5,(float)(.5+dx),h,(float)(.5+dz),(float)u,(float)v,c);
                vertex(pose,v5,(float)(.5+px),h,(float)(.5+pz),(float)pu,(float)pv,c);
                vertex(pose,v5,(float)(.5+px),h,(float)(.5+pz),(float)pu,(float)pv,c);
			}
			px = dx;
			pz = dz;
			pu = u;
			pv = v;
		}
	}

    private static void vertex(PoseStack.Pose pose,VertexConsumer out,float x,float y,float z,float u,float v,int color) {
        out.addVertex(pose,x,y,z).setUv(u,v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose,0,1,0);
    }

	public static final class State extends BlockEntityRenderState {
		private float charge;
		private boolean enhanced;
	}

}
