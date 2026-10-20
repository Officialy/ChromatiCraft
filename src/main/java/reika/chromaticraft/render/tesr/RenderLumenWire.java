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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelLumenWire;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import org.joml.Quaternionf;
import reika.chromaticraft.tileentity.TileEntityLumenWire;
import reika.dragonapi.instantiable.rendering.FXCollection;

/**
 * V33a {@code RenderLumenWire}: the wire model ({@code lumenwire.png}) turned to stand out of its support face, and in
 * the translucent pass the tile's own glints (its {@link FXCollection}), additive.
 */
public final class RenderLumenWire implements BlockEntityRenderer<TileEntityLumenWire, RenderLumenWire.State> {

	public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "lumen_wire"), "main");
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/entity/lumen_wire.png");

	private final ModelLumenWire model;

	public RenderLumenWire(BlockEntityRendererProvider.Context context) {
		model = new ModelLumenWire(context.bakeLayer(MODEL_LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityLumenWire te, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTick, cameraPosition, breakProgress);
		state.facing = te.getFacing();
		state.inWorld = te.hasLevel();
		state.particles = te.particles == null ? FXCollection.Snapshot.EMPTY : te.particles.snapshot();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		int rot = 0;
		int rotx = 0;
		int dx = 0;
		int dz = 0;
		int dy = 1;
        if (!state.inWorld) {
            poseStack.scale(2.5F,2.5F,2.5F);
            poseStack.translate(0,.3125F,0);
        } else {
		switch(state.facing.getOpposite()) {
			case EAST -> {
				rot = 270;
				dx = 1;
			}
			case WEST -> {
				rot = 90;
				dz = 1;
			}
			case SOUTH -> {
				rot = 180;
				dx = 1;
				dz = 1;
			}
			case NORTH -> rot = 0;
			case DOWN -> {
				rotx = 270;
				dy = 0;
			}
			case UP -> {
				rotx = 90;
				dy = 1;
				dz = 1;
			}
		}
		poseStack.translate(dx, dy, dz);
		poseStack.rotate(Axis.YP.rotationDegrees(rot));
		poseStack.rotate(Axis.XP.rotationDegrees(90));
		poseStack.rotate(Axis.XP.rotationDegrees(rotx));
        }
		submitModel(model, poseStack, collector, state.lightCoords);
		poseStack.popPose();

		FXCollection.Snapshot particles = state.particles;
		if (state.inWorld && !particles.isEmpty()) {
            var orientation = new Quaternionf(camera.orientation);
			ScreenParticleRendering.submitGeometry(collector, poseStack, ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
					(pose, v5) -> particles.render(v5, pose, orientation));
		}
	}

	/** V33a {@code renderModel}: the model's origin at the block's top centre, flipped. */
	public static void submitModel(ModelLumenWire model, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 1.5F, 0.5F);
		poseStack.scale(1F, -1F, -1F);
		PoseStack modelPose = new PoseStack();
		modelPose.last().set(poseStack.last());
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (unused, vertices) ->
				model.renderBody(modelPose, vertices, light, OverlayTexture.NO_OVERLAY));
        collector.submitCustomGeometry(poseStack,EntityLighting.FULL_BRIGHT.cutout(TEXTURE,true),(captured,vertices)->{
            var corePose=new PoseStack();corePose.last().set(captured);
            model.renderCore(corePose,vertices,OverlayTexture.NO_OVERLAY);
        });
		poseStack.popPose();
	}

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityLumenWire tile) {
        return tile.getRenderBoundingBox();
    }

	public static final class State extends BlockEntityRenderState {
		private boolean inWorld;
		private Direction facing = Direction.UP;
		private FXCollection.Snapshot particles = FXCollection.Snapshot.EMPTY;
	}

}
