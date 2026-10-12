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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.tileentity.recipe.TileEntityChromaCrafter;

/** V33a three-block fluid column and ingredient orbits; the shield body uses data-owned connected models. */
public final class RenderChromaCrafter implements BlockEntityRenderer<TileEntityChromaCrafter, RenderChromaCrafter.State> {
    private static final Identifier CHROMA = Identifier.fromNamespaceAndPath("chromaticraft", "block/fluid/activechroma");
    private final ItemModelResolver resolver;
    public RenderChromaCrafter(BlockEntityRendererProvider.Context context) { resolver = context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileEntityChromaCrafter tile, State state, float partial,
            Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partial, camera, overlay);
        state.tick = tile.getTicksExisted() + partial;
        state.height = (float)(tile.getChromaLevel()*3D / TileEntityChromaCrafter.CAPACITY);
        state.formed = tile.hasStructure();
        if (!state.formed) state.height = Math.min(1, state.height);
        state.items = new ArrayList<>();
        for (var stack : tile.getRenderItems()) {
            ItemStackRenderState item = new ItemStackRenderState();
            resolver.updateForTopItem(item, stack.copyWithCount(1), ItemDisplayContext.FIXED, tile.getLevel(), null, state.items.size());
            state.items.add(item);
        }
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.height > 0) {
            TextureAtlasSprite icon = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CHROMA);
            PoseStack copy = new PoseStack(); copy.last().set(poses.last());
            collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                    (unused, vertices) -> fluid(vertices, copy.last(), icon, state.height, state.formed));
        }
        for (int i = 0; i < state.items.size(); i++) {
            double angle = Math.toRadians(state.tick*4 + i*360D*4/state.items.size());
            poses.pushPose();
            poses.translate(0.5 + 0.25*Math.cos(angle), 1.25 + 1.25*Math.sin(angle/4), 0.5 + 0.25*Math.sin(angle));
            poses.rotate(Axis.YP.rotationDegrees(state.tick*4));
            poses.scale(0.5F, 0.5F, 0.5F);
            state.items.get(i).submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }
    private static void fluid(VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite icon, float height, boolean formed) {
        float lo = 0.01F, hi = 0.99F;
        quad(vertices, pose, icon, new float[][]{{hi,lo,lo},{hi,lo,hi},{lo,lo,hi},{lo,lo,lo}}, 0, 1, 0,-1,0);
        for (int floor = 0; floor < height; floor++) {
            float portion = Math.min(1, height-floor);
            float bottom = floor + (formed && floor > 0 ? 0 : lo);
            float top = floor + portion - (formed && floor+1 < 3 ? 0 : lo);
            quad(vertices,pose,icon,new float[][]{{lo,top,lo},{hi,top,lo},{hi,bottom,lo},{lo,bottom,lo}},portion,0,0,0,-1);
            quad(vertices,pose,icon,new float[][]{{lo,bottom,hi},{hi,bottom,hi},{hi,top,hi},{lo,top,hi}},0,portion,0,0,1);
            quad(vertices,pose,icon,new float[][]{{hi,top,lo},{hi,top,hi},{hi,bottom,hi},{hi,bottom,lo}},portion,0,1,0,0);
            quad(vertices,pose,icon,new float[][]{{lo,bottom,lo},{lo,bottom,hi},{lo,top,hi},{lo,top,lo}},0,portion,-1,0,0);
        }
        float top = height - (height < 3 && formed ? 0 : lo);
        quad(vertices,pose,icon,new float[][]{{lo,top,lo},{lo,top,hi},{hi,top,hi},{hi,top,lo}},1,0,0,1,0);
    }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite icon,
            float[][] points, float firstV, float lastV, float nx, float ny, float nz) {
        float[] u = {0,1,1,0}, v = {firstV,firstV,lastV,lastV};
        for (int i = 0; i < 4; i++) out.addVertex(pose, points[i][0],points[i][1],points[i][2])
                .setColor(0xffffffff).setUv(icon.getU(u[i]),icon.getV(v[i]))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose,nx,ny,nz);
    }
    public static final class State extends BlockEntityRenderState {
        private float tick, height;
        private boolean formed;
        private List<ItemStackRenderState> items = List.of();
    }
}
