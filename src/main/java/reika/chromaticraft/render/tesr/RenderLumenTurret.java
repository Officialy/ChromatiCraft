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
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.client.ScreenParticleRendering;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import reika.chromaticraft.models.ModelTurret;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.aoe.defence.TileEntityLumenTurret;

/**
 * V33a {@code RenderLumenTurret}: the turret model (texture {@code turret.png}), and in the translucent pass a small
 * camera-facing spark at its tip -- a glow-section sprite under a centre blur, additive and fullbright.
 */
public final class RenderLumenTurret implements BlockEntityRenderer<TileEntityLumenTurret, RenderLumenTurret.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "lumen_turret"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/lumen_turret.png");

	private final ModelTurret model;

	public RenderLumenTurret(BlockEntityRendererProvider.Context context) {
		model = new ModelTurret(context.bakeLayer(MODEL_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
    public void extractRenderState(TileEntityLumenTurret tile, State state, float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partialTick,cameraPosition,breaking);
        state.inWorld=tile.hasLevel();
    }

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if(!state.inWorld) {
            poseStack.scale(1.75F,1.75F,1.75F);
            poseStack.translate(0,.1875F,0);
        }
		submitModel(model, poseStack, collector, state.lightCoords);
        poseStack.popPose();
        if(!state.inWorld)return;

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.625F, 0.5F);
		poseStack.rotate(camera.orientation);
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		ChromaIcons[] icons = {
				ChromaIcons.GLOWSECTION,
				ChromaIcons.CENTER
		};
		for (int i = 0; i < icons.length; i++) {
			TextureAtlasSprite ico = atlas.getSprite(icons[i].getIcon());
			float s = 0.125F-0.03125F*i;
			ScreenParticleRendering.submitGeometry(collector, poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
					(pose, v5) -> quad(pose, v5, ico, s));
		}
		poseStack.popPose();
	}

	/** V33a {@code renderModel}: the model's origin at the block's top centre, flipped. */
	public static void submitModel(ModelTurret model, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) ->
				model.render(modelPose, vertices, light, OverlayTexture.NO_OVERLAY));
		poseStack.popPose();
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v5, TextureAtlasSprite ico, float s) {
		v5.addVertex(pose, -s, s, 0).setUv(ico.getU0(), ico.getV1()).setColor(0xffffffff);
		v5.addVertex(pose, s, s, 0).setUv(ico.getU1(), ico.getV1()).setColor(0xffffffff);
		v5.addVertex(pose, s, -s, 0).setUv(ico.getU1(), ico.getV0()).setColor(0xffffffff);
		v5.addVertex(pose, -s, -s, 0).setUv(ico.getU0(), ico.getV0()).setColor(0xffffffff);
	}

	public static final class State extends BlockEntityRenderState {
        private boolean inWorld;
	}

}
