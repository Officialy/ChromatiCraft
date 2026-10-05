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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.transport.TileEntityRift;

/**
 * V33a {@code RenderRift}: an unlit black box (the animated rift texture at 62.5% grey), then in ADDITIVEDARK the
 * pair's colour as five halo shells stepping out to 0.05 blocks and, on the face the Manipulator picked for items, a
 * quarter-block lime glow rune. The item draws the box with thinner (0.025) shells in a hue that cycles every fifteen
 * seconds, as V33a's out-of-world branch did.
 */
public final class RenderRift implements BlockEntityRenderer<TileEntityRift, RenderRift.State> {

	private static final int FULL_BRIGHT = 0xF000F0;

	public RenderRift(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityRift te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.color = te.getColor();
		state.singleDirection = te.getSingleDirection();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		submitBlackBox(poseStack, collector);
		if (state.singleDirection != null)
			submitSideHighlight(poseStack, collector, state.singleDirection);
		submitAura(poseStack, collector, state.color, 0.05F);
	}

	private static TextureAtlasSprite sprite(Identifier id) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	/** V33a {@code drawBlackBox}: each face with the rift texture, colour 0.625, brightness 240, both sides. */
	public static void submitBlackBox(PoseStack poseStack, SubmitNodeCollector collector) {
		TextureAtlasSprite ico = sprite(ChromaIcons.RIFT.getIcon());
		PoseStack.Pose pose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.unlitEntityCutout(TextureAtlas.LOCATION_BLOCKS),
				(unused, v5) -> {
					float u = ico.getU0();
					float du = ico.getU1();
					float v = ico.getV0();
					float dv = ico.getV1();
					int c = 0xff9f9f9f;
					box(v5, pose, c, u, du, v, dv, 0);
				});
	}

	private static void box(VertexConsumer v5, PoseStack.Pose pose, int c, float u, float du, float v, float dv, float out) {
		float lo = -out;
		float hi = 1+out;
		quad(v5, pose, c, lo, hi, lo, u, v, hi, hi, lo, du, v, hi, lo, lo, du, dv, lo, lo, lo, u, dv);
		quad(v5, pose, c, lo, lo, hi, u, v, hi, lo, hi, du, v, hi, hi, hi, du, dv, lo, hi, hi, u, dv);
		quad(v5, pose, c, lo, lo, lo, u, v, hi, lo, lo, du, v, hi, lo, hi, du, dv, lo, lo, hi, u, dv);
		quad(v5, pose, c, lo, hi, hi, u, v, hi, hi, hi, du, v, hi, hi, lo, du, dv, lo, hi, lo, u, dv);
		quad(v5, pose, c, lo, lo, lo, u, v, lo, lo, hi, du, v, lo, hi, hi, du, dv, lo, hi, lo, u, dv);
		quad(v5, pose, c, hi, hi, lo, u, v, hi, hi, hi, du, v, hi, lo, hi, du, dv, hi, lo, lo, u, dv);
	}

	private static void quad(VertexConsumer v5, PoseStack.Pose pose, int c,
			float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3, float x4, float y4, float z4, float u4, float v4) {
		entity(v5, pose, c, x1, y1, z1, u1, v1);
		entity(v5, pose, c, x2, y2, z2, u2, v2);
		entity(v5, pose, c, x3, y3, z3, u3, v3);
		entity(v5, pose, c, x4, y4, z4, u4, v4);
	}

	private static void entity(VertexConsumer v5, PoseStack.Pose pose, int c, float x, float y, float z, float u, float v) {
		v5.addVertex(pose, x, y, z).setColor(c).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
				.setNormal(pose, 0, 1, 0);
	}

	/** V33a {@code renderAura}: shells from 0 to {@code max} in 0.0125 steps, the halo texture in {@code color}. */
	public static void submitAura(PoseStack poseStack, SubmitNodeCollector collector, int color, float max) {
		TextureAtlasSprite ico = sprite(ChromaIcons.RIFTHALO.getIcon());
		PoseStack.Pose pose = poseStack.last().copy();
		int c = 0xff000000 | color;
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, v5) -> {
					float u = ico.getU0();
					float du = ico.getU1();
					float v = ico.getV0();
					float dv = ico.getV1();
					float incr = 0.0125F;
					for (float out = 0; out <= max; out += incr) {
						float lo = -out;
						float hi = 1+out;
						additiveQuad(v5, pose, c, lo, hi, lo, u, v, hi, hi, lo, du, v, hi, lo, lo, du, dv, lo, lo, lo, u, dv);
						additiveQuad(v5, pose, c, lo, lo, hi, u, v, hi, lo, hi, du, v, hi, hi, hi, du, dv, lo, hi, hi, u, dv);
						additiveQuad(v5, pose, c, lo, lo, lo, u, v, hi, lo, lo, du, v, hi, lo, hi, du, dv, lo, lo, hi, u, dv);
						additiveQuad(v5, pose, c, lo, hi, hi, u, v, hi, hi, hi, du, v, hi, hi, lo, du, dv, lo, hi, lo, u, dv);
						additiveQuad(v5, pose, c, lo, lo, lo, u, v, lo, lo, hi, du, v, lo, hi, hi, du, dv, lo, hi, lo, u, dv);
						additiveQuad(v5, pose, c, hi, hi, lo, u, v, hi, hi, hi, du, v, hi, lo, hi, du, dv, hi, lo, lo, u, dv);
					}
				});
	}

	/** V33a {@code renderSideHighlight}: the lime glow rune, half a block wide, just off the chosen face. */
	private static void submitSideHighlight(PoseStack poseStack, SubmitNodeCollector collector, Direction dir) {
		TextureAtlasSprite ico = sprite(CrystalElement.LIME.getIcon());
		PoseStack.Pose pose = poseStack.last().copy();
		collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
				(unused, v5) -> {
					float d = 0.501F;
					float x = 0.5F+dir.getStepX()*d;
					float y = 0.5F+dir.getStepY()*d;
					float z = 0.5F+dir.getStepZ()*d;
					float u = ico.getU0();
					float du = ico.getU1();
					float v = ico.getV0();
					float dv = ico.getV1();
					float h = 0.25F;
					if (dir.getStepZ() < 0 || dir.getStepY() < 0 || dir.getStepX() > 0) {
						float p = du;
						du = u;
						u = p;
					}
					int c = 0xffffffff;
					if (dir.getStepX() != 0) {
						additiveQuad(v5, pose, c, x, y-h, z-h, u, dv, x, y-h, z+h, du, dv, x, y+h, z+h, du, v, x, y+h, z-h, u, v);
					}
					else if (dir.getStepY() != 0) {
						additiveQuad(v5, pose, c, x-h, y, z-h, u, dv, x-h, y, z+h, du, dv, x+h, y, z+h, du, v, x+h, y, z-h, u, v);
					}
					else {
						additiveQuad(v5, pose, c, x-h, y-h, z, u, dv, x+h, y-h, z, du, dv, x+h, y+h, z, du, v, x-h, y+h, z, u, v);
					}
				});
	}

	private static void additiveQuad(VertexConsumer v5, PoseStack.Pose pose, int c,
			float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3, float x4, float y4, float z4, float u4, float v4) {
		v5.addVertex(pose, x1, y1, z1).setUv(u1, v1).setColor(c);
		v5.addVertex(pose, x2, y2, z2).setUv(u2, v2).setColor(c);
		v5.addVertex(pose, x3, y3, z3).setUv(u3, v3).setColor(c);
		v5.addVertex(pose, x4, y4, z4).setUv(u4, v4).setColor(c);
	}

	public static final class State extends BlockEntityRenderState {
		private int color = 0xffffff;
		private @Nullable Direction singleDirection;
	}

}
