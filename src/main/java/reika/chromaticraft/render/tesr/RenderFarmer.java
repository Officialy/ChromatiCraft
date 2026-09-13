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

import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.models.ModelFarmer;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.tileentity.TileEntityFarmer;

public final class RenderFarmer implements BlockEntityRenderer<TileEntityFarmer, RenderFarmer.State> {

    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(id("farmer"), "main");
    public static final Identifier TEXTURE = id("textures/entity/farmer.png");
    private final ModelFarmer model;

    public RenderFarmer(BlockEntityRendererProvider.Context context) {
        model = new ModelFarmer(context.bakeLayer(MODEL_LAYER));
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileEntityFarmer farmer, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(farmer, state, partialTick, cameraPosition, breakProgress);
        state.facing = farmer.getFacing();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        float angle = switch (state.facing) {
            case EAST -> 90;
            case NORTH -> 180;
            case WEST -> -90;
            default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5, 0, -0.5);
        submitMachine(pose, collector, model, state.lightCoords);
        pose.popPose();
    }

    public static void submitMachine(PoseStack pose, SubmitNodeCollector collector, ModelFarmer model, int light) {
        PoseStack body = copy(pose);
        body.translate(0.5, 1.5, 0.5);
        body.scale(1, -1, -1);
        collector.submitCustomGeometry(body, RenderTypes.entityCutout(TEXTURE),
                (unused, output) -> model.render(body, output, light, OverlayTexture.NO_OVERLAY));
        PoseStack effects = copy(pose);
        collector.submitCustomGeometry(effects,
                ChromaRenderPipelines.legacyAdditiveSprite(TextureAtlas.LOCATION_BLOCKS),
                (unused, output) -> renderEffects(effects.last(), output));
    }

    private static void renderEffects(PoseStack.Pose pose, VertexConsumer output) {
        TextureAtlasSprite fire = sprite("bluefire");
        front(pose, output, fire, 0.95F, 0, 0, 1, 0, 1, 1, 0, 1);
        front(pose, output, fire, 0.95F, 1, 1, 0, 1, 0, 0, 1, 0);
        front(pose, output, fire, 0.95F, 0, 1, 0, 0, 1, 0, 1, 1);
        front(pose, output, fire, 0.95F, 1, 0, 1, 1, 0, 1, 0, 0);
        sides(pose, output, sprite("rift_halo"), 0.995F);
        TextureAtlasSprite sparkle = sprite("sparkle2");
        sides(pose, output, sparkle, 0.9975F);
        front(pose, output, sparkle, 0.9975F, 0, 0, 1, 0, 1, 1, 0, 1);
    }

    private static void front(PoseStack.Pose pose, VertexConsumer output, TextureAtlasSprite sprite,
            float depth, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        vertex(pose, output, sprite, 0, 0, depth, u0, v0);
        vertex(pose, output, sprite, 1, 0, depth, u1, v1);
        vertex(pose, output, sprite, 1, 1, depth, u2, v2);
        vertex(pose, output, sprite, 0, 1, depth, u3, v3);
    }

    private static void sides(PoseStack.Pose pose, VertexConsumer output, TextureAtlasSprite sprite, float depth) {
        vertex(pose, output, sprite, 1 - depth, 0.1875F, 0.125F, 0, 0);
        vertex(pose, output, sprite, 1 - depth, 0.1875F, 0.875F, 1, 0);
        vertex(pose, output, sprite, 1 - depth, 0.8125F, 0.875F, 1, 1);
        vertex(pose, output, sprite, 1 - depth, 0.8125F, 0.125F, 0, 1);
        vertex(pose, output, sprite, depth, 0.8125F, 0.125F, 0, 1);
        vertex(pose, output, sprite, depth, 0.8125F, 0.875F, 1, 1);
        vertex(pose, output, sprite, depth, 0.1875F, 0.875F, 1, 0);
        vertex(pose, output, sprite, depth, 0.1875F, 0.125F, 0, 0);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer output, TextureAtlasSprite sprite,
            float x, float y, float z, float u, float v) {
        output.addVertex(pose, x, y, z).setColor(0xff000000 | CrystalElement.GREEN.getColor())
                .setUv(sprite.getU0() + u * (sprite.getU1() - sprite.getU0()),
                        sprite.getV0() + v * (sprite.getV1() - sprite.getV0()))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(id("block/icons/" + name));
    }

    private static PoseStack copy(PoseStack source) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(source.last().pose());
        copy.last().normal().set(source.last().normal());
        return copy;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path); }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.SOUTH;
    }
}
