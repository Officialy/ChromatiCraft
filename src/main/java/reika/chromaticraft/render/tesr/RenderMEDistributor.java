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
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelMEDistributor;
import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;

/**
 * V33a {@code RenderMEDistributor}: the frame model on medistrib.png, turned from its UP orientation to the side it
 * was placed on (V33a's metadata switch, one case per side).
 */
public final class RenderMEDistributor implements BlockEntityRenderer<TileEntityMEDistributor, RenderMEDistributor.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "me_distributor"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/me_distributor.png");

	private final ModelMEDistributor model;

	public RenderMEDistributor(BlockEntityRendererProvider.Context context) {
		model = new ModelMEDistributor(context.bakeLayer(MODEL_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityMEDistributor te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.facing = te.getFacing();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		switch (state.facing) {
			case DOWN -> {
				poseStack.translate(0, 1, 1);
				poseStack.rotate(Axis.XP.rotationDegrees(180));
			}
			case NORTH -> {
				poseStack.translate(0, 0, 1);
				poseStack.rotate(Axis.XP.rotationDegrees(-90));
			}
			case SOUTH -> {
				poseStack.translate(0, 1, 0);
				poseStack.rotate(Axis.XP.rotationDegrees(90));
			}
			case WEST -> {
				poseStack.translate(1, 0, 0);
				poseStack.rotate(Axis.ZP.rotationDegrees(90));
			}
			case EAST -> {
				poseStack.translate(0, 1, 0);
				poseStack.rotate(Axis.ZP.rotationDegrees(-90));
			}
			case UP -> {}
		}
		submitModel(model, poseStack, collector, state.lightCoords, true);
		poseStack.popPose();
	}

	/** V33a {@code renderModel}: the model's origin at the block's top centre, flipped. */
	public static void submitModel(ModelMEDistributor model, PoseStack poseStack, SubmitNodeCollector collector, int light,
			boolean inWorld) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) ->
				model.render(modelPose, vertices, light, OverlayTexture.NO_OVERLAY, inWorld));
		poseStack.popPose();
	}

	public static final class State extends BlockEntityRenderState {
		private Direction facing = Direction.UP;
	}

}
