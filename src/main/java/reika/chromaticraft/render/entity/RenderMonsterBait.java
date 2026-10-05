/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import reika.chromaticraft.entity.EntityMonsterBait;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;

/** V33a's nine horizontal rose-flare layers, with their per-layer color and drift. */
public final class RenderMonsterBait extends EntityRenderer<EntityMonsterBait,RenderMonsterBait.State> {
    public RenderMonsterBait(EntityRendererProvider.Context context) { super(context); shadowRadius=0; }
    public State createRenderState() { return new State(); }
    @Override public void extractRenderState(EntityMonsterBait entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        for (int i=0;i<9;i++) state.colors[i]=entity.getRenderColor(-.0625+i*.015625);
    }
    @Override public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        var icon=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.ROSEFLARE.getIcon());
        int[] colors=state.colors.clone(); double age=state.ageInTicks;
        collector.submitCustomGeometry(stack,ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),(pose,out)-> {
            for (int i=0;i<9;i++) {
                double y=-.0625+i*.015625,t=age/10+y*40,dx=.0625*Math.sin(t*.46),dz=.0625*Math.sin(t*.78);
                int color=0xff000000|colors[i];
                out.addVertex(pose,(float)(dx-.5),(float)y,(float)(dz-.5)).setColor(color).setUv(icon.getU0(),icon.getV0());
                out.addVertex(pose,(float)(dx+.5),(float)y,(float)(dz-.5)).setColor(color).setUv(icon.getU1(),icon.getV0());
                out.addVertex(pose,(float)(dx+.5),(float)y,(float)(dz+.5)).setColor(color).setUv(icon.getU1(),icon.getV1());
                out.addVertex(pose,(float)(dx-.5),(float)y,(float)(dz+.5)).setColor(color).setUv(icon.getU0(),icon.getV1());
            }
        });
    }
    public static final class State extends EntityRenderState { final int[] colors=new int[9]; }
}
