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
import reika.chromaticraft.render.ChromaRenderPipelines;
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
		state.charge = te.hasStructure() ? te.getChargeFraction() : 0;
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
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TEXTURE), (ignored, v5) -> {
			if (enhanced) {
				renderFan(pose, v5, f, 0x00ff00);
				renderFan(pose, v5, f, 0xffffff);
			}
			else {
				renderFan(pose, v5, f, 0xffffff);
			}
		});
	}

	private static void renderFan(PoseStack.Pose pose, VertexConsumer v5, float f, int color) {
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
				v5.addVertex(pose, 0.5F, h, 0.5F).setUv(0.5F, 0.5F).setColor(c);
				v5.addVertex(pose, (float)(0.5+px), h, (float)(0.5+pz)).setUv((float)pu, (float)pv).setColor(c);
				v5.addVertex(pose, (float)(0.5+dx), h, (float)(0.5+dz)).setUv((float)u, (float)v).setColor(c);
				v5.addVertex(pose, (float)(0.5+dx), h, (float)(0.5+dz)).setUv((float)u, (float)v).setColor(c);
			}
			px = dx;
			pz = dz;
			pu = u;
			pv = v;
		}
	}

	public static final class State extends BlockEntityRenderState {
		private float charge;
		private boolean enhanced;
	}

}
