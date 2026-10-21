/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.IrisCompatibility;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.render.ShaderCompatibleRenderTypes;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;
import reika.dragonapi.interfaces.PlayerRenderObj;

/** Original detached pylon: a full-bright round-flare card, rather than the placed pylon's halo/beams. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class DonatorPylonRender implements PlayerRenderObj {
    public static final DonatorPylonRender instance = new DonatorPylonRender();
    private static final Identifier FLARE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/roundflare");
    private TileEntityCrystalPylon preview;
    private DonatorPylonRender() {}

    @SubscribeEvent public static void load(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> PlayerSpecificRenderer.instance.registerDonatorRenderer(instance));
    }

    @Override public void extractRenderState(PoseStack stack, Player player, float partialTick,
            PlayerSpecificRenderer.PlayerRotationData rotation, SubmitNodeCollector collector) {
        if (IrisCompatibility.shadowPass()) return;
        if (preview == null) preview = new TileEntityCrystalPylon(BlockPos.ZERO, ChromaBlocks.PYLON.get().defaultBlockState());
        preview.animateItem();
        int color = 0xff000000 | preview.getColor().color();
        boolean shaders = IrisCompatibility.shadersEnabled();
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(shaders ? FLARE.withSuffix("_alpha") : FLARE);
        stack.pushPose();
        try {
            stack.translate(0, 2.25, 0);
            stack.rotate(Axis.XP.rotationDegrees(45));
            stack.rotate(Axis.XP.rotationDegrees(rotation.getRenderPitch()));
            stack.rotate(Axis.YP.rotationDegrees(-45));
            stack.scale(0.375F, 0.375F, 0.375F);
            // Original RenderCrystalPylon's detached-card branch.
            stack.rotate(Axis.YP.rotationDegrees(45));
            stack.rotate(Axis.XP.rotationDegrees(-45));
            var submit = new CustomFeatureRenderer.Submit(stack.last().copy(),
                    shaders ? ShaderCompatibleRenderTypes.pylon(TextureAtlas.LOCATION_BLOCKS, false)
                            : ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
                    (pose, vertices) -> {
                        for (int i = 0; i < 4; i++) {
                            float x = i == 0 || i == 3 ? -1 : 1;
                            float y = i < 2 ? -1 : 1;
                            vertices.addVertex(pose, x, y, 0).setColor(color)
                                    .setUv(x < 0 ? sprite.getU0() : sprite.getU1(), y < 0 ? sprite.getV0() : sprite.getV1())
                                    .setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT)
                                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                                    .setNormal(pose, 0, 0, 1);
                        }
                    });
            collector.order(0).submitSpecial(ScreenParticleRendering.PHASE, submit);
        } finally { stack.popPose(); }
    }
    @Override public int getRenderPriority() { return Integer.MAX_VALUE; }
}
