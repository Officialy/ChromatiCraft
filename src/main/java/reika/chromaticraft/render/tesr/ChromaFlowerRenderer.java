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

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.tileentity.plants.TileEntityChromaFlower;

/**
 * V33a {@code ChromaFlowerRenderer}: three vertical planes through the block's centre, 60 degrees apart, carry the stem
 * ({@code plant/0_4}, untinted, the solid pass) and then three petal layers ({@code plant/0_1..0_3}, alpha-blended),
 * each tinted with its own cycling hue at full saturation and half brightness. V33a drew it unlit and unculled.
 */
public final class ChromaFlowerRenderer implements BlockEntityRenderer<TileEntityChromaFlower, ChromaFlowerRenderer.State> {

	private static final Identifier STEM = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/plant/0_4");
	private static final Identifier[] PETALS = {
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/plant/0_1"),
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/plant/0_2"),
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/plant/0_3"),
	};

	public ChromaFlowerRenderer(BlockEntityRendererProvider.Context context) {

	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityChromaFlower te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.hues = new int[] {te.getHue1(), te.getHue2(), te.getHue3()};
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		submitFlower(poseStack, collector, state.hues, state.lightCoords,
				RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
				RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), false);
	}

	/**
	 * The whole flower, stem then petals. {@code doubleSided} emits each plane in both windings, for render types that
	 * cull (the item pipeline); the entity types used in the world do not.
	 */
	public static void submitFlower(PoseStack poseStack, SubmitNodeCollector collector, int[] hues, int light,
			RenderType solid, RenderType translucent, boolean doubleSided) {
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite stem = atlas.getSprite(STEM);
		collector.submitCustomGeometry(poseStack, solid, (pose, vertices) ->
				planes(pose, vertices, stem, 0xFFFFFFFF, light, doubleSided));
		for (int k = 0; k < 3; k++) {
			TextureAtlasSprite petal = atlas.getSprite(PETALS[k]);
			int color = 0xFF000000 | Color.HSBtoRGB(hues[k]/360F, 1, 0.5F);
			collector.submitCustomGeometry(poseStack, translucent, (pose, vertices) ->
					planes(pose, vertices, petal, color, light, doubleSided));
		}
	}

	private static void planes(PoseStack.Pose base, VertexConsumer v5, TextureAtlasSprite ico, int color, int light,
			boolean doubleSided) {
		float u = ico.getU0();
		float du = ico.getU1();
		float v = ico.getV0();
		float dv = ico.getV1();
		for (int i = 0; i < 3; i++) {
			PoseStack stack = new PoseStack();
			stack.last().set(base);
			stack.translate(0.5, 0, 0.5);
			stack.mulPose(Axis.YP.rotationDegrees(60*i));
			stack.translate(-0.5, 0, -0.5);
			PoseStack.Pose pose = stack.last();
			vertex(v5, pose, 0.5F, 0, 1, du, dv, color, light);
			vertex(v5, pose, 0.5F, 1, 1, du, v, color, light);
			vertex(v5, pose, 0.5F, 1, 0, u, v, color, light);
			vertex(v5, pose, 0.5F, 0, 0, u, dv, color, light);
			if (doubleSided) {
				vertex(v5, pose, 0.5F, 0, 0, u, dv, color, light);
				vertex(v5, pose, 0.5F, 1, 0, u, v, color, light);
				vertex(v5, pose, 0.5F, 1, 1, du, v, color, light);
				vertex(v5, pose, 0.5F, 0, 1, du, dv, color, light);
			}
		}
	}

	/** Upward normals stand in for V33a's disabled lighting: every plane gets the full, unshaded light. */
	private static void vertex(VertexConsumer v5, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color,
			int light) {
		v5.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, 0, 1, 0);
	}

	public static final class State extends BlockEntityRenderState {
		private int[] hues = {0, 0, 0};
	}

}
