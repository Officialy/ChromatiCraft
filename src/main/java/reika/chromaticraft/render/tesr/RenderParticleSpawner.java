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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.auxiliary.HoldingChecks;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.decoration.TileEntityParticleSpawner;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code RenderParticleSpawner}: the otherwise invisible spawner shows as a small cross of six quarter-block planes
 * around its centre, gentle caustics under a dimmer glow frame, additive and unlit. It fades in (1/8 a frame) while the
 * player holds a Manipulator or looks at it from within 4.5 blocks, and out again (1/40 a frame) otherwise.
 */
public final class RenderParticleSpawner implements BlockEntityRenderer<TileEntityParticleSpawner, RenderParticleSpawner.State> {

	public RenderParticleSpawner(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityParticleSpawner te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		if (te.isInWorld()) {
			if (isRevealed(te.getBlockPos()))
				te.renderOpacity = Math.min(1, te.renderOpacity+0.125F);
			else
				te.renderOpacity = Math.max(0, te.renderOpacity-0.025F);
		}
		state.opacity = te.renderOpacity;
	}

	private static boolean isRevealed(BlockPos pos) {
		if (HoldingChecks.MANIPULATOR.isClientHolding())
			return true;
		var player = Minecraft.getInstance().player;
		if (player != null && player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= 21) {
			BlockHitResult mov = ReikaPlayerAPI.getLookedAtBlockClient(4.5, false);
			return mov != null && mov.getType() == BlockHitResult.Type.BLOCK && mov.getBlockPos().equals(pos);
		}
		return false;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.opacity <= 0)
			return;
		submitFrame(poseStack, collector, state.opacity);
	}

	/** The caustics then the glow frame, each on the same six planes; also the item's rendering, at full opacity. */
	public static void submitFrame(PoseStack poseStack, SubmitNodeCollector collector, float opacity) {
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite[] icons = {
				atlas.getSprite(ChromaIcons.CAUSTICS_GENTLE.getIcon()),
				atlas.getSprite(ChromaIcons.GLOWFRAME.getIcon()),
		};
		for (int n = 0; n < icons.length; n++) {
			TextureAtlasSprite ico = icons[n];
			int color = 0xff000000 | ReikaColorAPI.getColorWithBrightnessMultiplier(n >= 1 ? 0x3f3f3f : 0xffffff, opacity);
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
					(pose, v5) -> planes(pose, v5, ico, color));
		}
	}

	private static void planes(PoseStack.Pose pose, VertexConsumer v5, TextureAtlasSprite ico, int color) {
		float u = ico.getU0();
		float v = ico.getV0();
		float du = ico.getU1();
		float dv = ico.getV1();

		float o = 0.4375F;
		float i = 0.3125F;

		vertex(v5, pose, i, o, i, u, v, color);
		vertex(v5, pose, 1-i, o, i, du, v, color);
		vertex(v5, pose, 1-i, o, 1-i, du, dv, color);
		vertex(v5, pose, i, o, 1-i, u, dv, color);

		vertex(v5, pose, i, 1-o, i, u, v, color);
		vertex(v5, pose, 1-i, 1-o, i, du, v, color);
		vertex(v5, pose, 1-i, 1-o, 1-i, du, dv, color);
		vertex(v5, pose, i, 1-o, 1-i, u, dv, color);

		vertex(v5, pose, o, i, i, u, v, color);
		vertex(v5, pose, o, 1-i, i, du, v, color);
		vertex(v5, pose, o, 1-i, 1-i, du, dv, color);
		vertex(v5, pose, o, i, 1-i, u, dv, color);

		vertex(v5, pose, 1-o, i, i, u, v, color);
		vertex(v5, pose, 1-o, 1-i, i, du, v, color);
		vertex(v5, pose, 1-o, 1-i, 1-i, du, dv, color);
		vertex(v5, pose, 1-o, i, 1-i, u, dv, color);

		vertex(v5, pose, i, i, o, u, v, color);
		vertex(v5, pose, 1-i, i, o, du, v, color);
		vertex(v5, pose, 1-i, 1-i, o, du, dv, color);
		vertex(v5, pose, i, 1-i, o, u, dv, color);

		vertex(v5, pose, i, i, 1-o, u, v, color);
		vertex(v5, pose, 1-i, i, 1-o, du, v, color);
		vertex(v5, pose, 1-i, 1-i, 1-o, du, dv, color);
		vertex(v5, pose, i, 1-i, 1-o, u, dv, color);
	}

	private static void vertex(VertexConsumer v5, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
		v5.addVertex(pose, x, y, z).setUv(u, v).setColor(color);
	}

	public static final class State extends BlockEntityRenderState {
		private float opacity;
	}

}
