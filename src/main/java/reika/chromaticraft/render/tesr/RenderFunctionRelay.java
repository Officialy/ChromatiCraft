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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.client.ScreenParticleRendering;

public final class RenderFunctionRelay implements BlockEntityRenderer<TileEntityFunctionRelay, RenderFunctionRelay.State> {
    public static final Identifier SPRITE = Identifier.fromNamespaceAndPath("chromaticraft", "block/icons/cellflare");
    private static final ColorBlendList COLORS = new ColorBlendList(25, 0x22aaff, 0x20a020, 0xffffff, 0xf0c020);

    public RenderFunctionRelay(BlockEntityRendererProvider.Context context) {}
    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(TileEntityFunctionRelay tile, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        state.inWorld = tile.hasLevel();
        state.color = state.inWorld ? colorAt((long)tile.getTicksExisted() + tile.hashCode())
                : colorAt(System.currentTimeMillis() / 50);
    }

    @Override public AABB getRenderBoundingBox(TileEntityFunctionRelay tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }

    @Override public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.inWorld) {
            submitItem(poseStack, collector, state.color);
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, 1, 1);
        poseStack.scale(1, -1, -1);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.rotate(Axis.YP.rotationDegrees(camera.yRot));
        poseStack.rotate(Axis.XP.rotationDegrees(camera.xRot));
        submitSprite(poseStack, collector, state.color, true);
        poseStack.popPose();
    }

    public static void submitItem(PoseStack poseStack, SubmitNodeCollector collector) {
        submitItem(poseStack, collector, colorAt(System.currentTimeMillis() / 50));
    }

    private static void submitItem(PoseStack poseStack, SubmitNodeCollector collector, int color) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1, -1, -1);
        poseStack.rotate(Axis.YP.rotationDegrees(-45));
        poseStack.rotate(Axis.XP.rotationDegrees(-30));
        poseStack.scale(1.125F, 1.125F, 1.125F);
        submitSprite(poseStack, collector, color, false);
        poseStack.popPose();
    }

    public static int colorAt(long time) {
        return 0xff000000 | COLORS.getColor(Math.floorMod(time, 100));
    }

    private static void submitSprite(PoseStack pose, SubmitNodeCollector collector, int color, boolean inWorld) {
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(SPRITE);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        SubmitNodeCollector.CustomGeometryRenderer draw = (captured, out) -> {
            out.addVertex(captured, -1, -1, 0).setUv(u0, v0).setColor(color);
            out.addVertex(captured, 1, -1, 0).setUv(u1, v0).setColor(color);
            out.addVertex(captured, 1, 1, 0).setUv(u1, v1).setColor(color);
            out.addVertex(captured, -1, 1, 0).setUv(u0, v1).setColor(color);
        };
        var type = ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS);
        if (inWorld) ScreenParticleRendering.submitGeometry(collector, pose, type, draw);
        else collector.submitCustomGeometry(pose, type, draw);
    }

    public static final class State extends BlockEntityRenderState {
        private int color;
        private boolean inWorld;
    }
}
