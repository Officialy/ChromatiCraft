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

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.base.tileentity.TileEntityMassStorage;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;

/**
 * V33a {@code RenderMassStorage}: above the crate, swinging 45 degrees either way, the kind of item it holds -- or, for
 * the catch-all mode, a question mark at half size, additive. V33a drew the item as a dim (0.2) additive ghost; the 26.2
 * item pipeline chooses its own render type, so here it is the item as an item frame would show it, at the crate's light.
 */
public final class RenderMassStorage implements BlockEntityRenderer<TileEntityMassStorage, RenderMassStorage.State> {

	private final ItemModelResolver itemModelResolver;

	public RenderMassStorage(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityMassStorage te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.tick = partialTick+te.getTicksExisted();
		state.item = null;
		ItemStack filter = te.getFilterItemRender();
		if (!filter.isEmpty()) {
			state.item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(state.item, filter, ItemDisplayContext.FIXED, te.getLevel(), null, 0);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 1.25, 0.5);
		float s = 1;
		double ang = 45*Math.sin(Math.toRadians(state.tick*3));
		if (state.item == null)
			s *= 0.5F;
		poseStack.scale(s, s, s);
		poseStack.mulPose(Axis.YP.rotationDegrees((float)ang));
		if (state.item != null) {
			state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}
		else {
			TextureAtlasSprite ico = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
					.getSprite(ChromaIcons.QUESTION.getIcon());
			collector.submitCustomGeometry(poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
					(pose, v5) -> questionMark(pose, v5, ico));
		}
		poseStack.popPose();
	}

	/** V33a {@code renderItemIn2D} of the question icon, centred: one square, unculled. */
	private static void questionMark(PoseStack.Pose pose, VertexConsumer v5, TextureAtlasSprite ico) {
		v5.addVertex(pose, -0.5F, 0, 0).setUv(ico.getU1(), ico.getV1()).setColor(0xffffffff);
		v5.addVertex(pose, 0.5F, 0, 0).setUv(ico.getU0(), ico.getV1()).setColor(0xffffffff);
		v5.addVertex(pose, 0.5F, 1, 0).setUv(ico.getU0(), ico.getV0()).setColor(0xffffffff);
		v5.addVertex(pose, -0.5F, 1, 0).setUv(ico.getU1(), ico.getV0()).setColor(0xffffffff);
	}

	public static final class State extends BlockEntityRenderState {
		private float tick;
		private ItemStackRenderState item;
	}

}
