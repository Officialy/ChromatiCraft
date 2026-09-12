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

public final class RenderFunctionRelay implements BlockEntityRenderer<TileEntityFunctionRelay, RenderFunctionRelay.State> {
    public static final Identifier SPRITE = Identifier.fromNamespaceAndPath("chromaticraft", "block/icons/cellflare");
    private static final ColorBlendList COLORS = new ColorBlendList(25, 0x22aaff, 0x20a020, 0xffffff, 0xf0c020);

    public RenderFunctionRelay(BlockEntityRendererProvider.Context context) {}
    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(TileEntityFunctionRelay tile, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, cameraPosition, breakProgress);
        state.color = colorAt((long)tile.getTicksExisted() + tile.hashCode());
    }

    @Override public AABB getRenderBoundingBox(TileEntityFunctionRelay tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }

    @Override public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.875F, -0.875F, 0.875F);
        submitSprite(poseStack, collector, state.color);
        poseStack.popPose();
    }

    public static void submitItem(PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1, -1, -1);
        poseStack.mulPose(Axis.YP.rotationDegrees(-45));
        poseStack.mulPose(Axis.XP.rotationDegrees(-30));
        poseStack.scale(1.125F, 1.125F, 1.125F);
        submitSprite(poseStack, collector, colorAt(System.currentTimeMillis() / 50));
        poseStack.popPose();
    }

    public static int colorAt(long time) {
        return 0xff000000 | COLORS.getColor(Math.floorMod(time, 100));
    }

    private static void submitSprite(PoseStack source, SubmitNodeCollector collector, int color) {
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(SPRITE);
        PoseStack pose = new PoseStack();
        pose.last().pose().set(source.last().pose());
        pose.last().normal().set(source.last().normal());
        collector.submitCustomGeometry(pose, ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
                (unused, out) -> {
                    out.addVertex(pose.last(), -1, -1, 0).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
                    out.addVertex(pose.last(), 1, -1, 0).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
                    out.addVertex(pose.last(), 1, 1, 0).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
                    out.addVertex(pose.last(), -1, 1, 0).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
                });
    }

    public static final class State extends BlockEntityRenderState {
        private int color;
    }
}
