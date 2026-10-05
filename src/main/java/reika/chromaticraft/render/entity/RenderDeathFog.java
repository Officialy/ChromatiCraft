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
import reika.chromaticraft.entity.EntityDeathFog;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.RayTracer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RenderDeathFog extends EntityRenderer<EntityDeathFog,RenderDeathFog.State> {
    private static final RayTracer.RayTracerWithCache<?> LOS=RayTracer.getVisualLOSForRenderCulling();
    public RenderDeathFog(EntityRendererProvider.Context context) { super(context); shadowRadius=0; }
    public State createRenderState() { return new State(); }
    @Override public void extractRenderState(EntityDeathFog entity,State state,float partial) {
        super.extractRenderState(entity,state,partial); state.quads=List.of();
        var player=Minecraft.getInstance().player; if (player==null) return;
        LOS.setOrigins(entity.getX(),entity.getY(),entity.getZ(),player.getX(),player.getY(),player.getZ());
        if (!LOS.isClearLineOfSight(entity)) return;
        Random rand=new Random(System.identityHashCode(entity)); rand.nextBoolean(); rand.nextBoolean();
        double base=entity.isEnhanced()?2.25:1.75; int count=entity.isEnhanced()?1+rand.nextInt(4):1;
        List<Quad> quads=new ArrayList<>();
        for (int i=0;i<count;i++) {
            float x=(float)((rand.nextDouble()-.5)*base),y=(float)((rand.nextDouble()-.5)*base),a=rand.nextFloat()+.5F;
            float life=entity.getRemainingLife()-rand.nextInt(Math.max(1,Math.min(entity.getRemainingLife(),10)))-partial;
            float fade=Math.min(1,life/20),size=(float)(base*fade*(.5+rand.nextDouble()*.5));
            if (fade>0) quads.add(new Quad(x,y,size,Math.clamp((int)(fade*255*a),0,255)<<24));
        }
        state.quads=List.copyOf(quads);
    }
    @Override public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        var icon=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.FADE_BASICBLEND.getIcon());
        List<Quad> quads=state.quads; stack.pushPose(); stack.rotate(camera.orientation);
        collector.submitCustomGeometry(stack,ChromaRenderPipelines.translucentSpriteThroughWall(TextureAtlas.LOCATION_BLOCKS),(pose,out)-> {
            for (Quad q:quads) {
                out.addVertex(pose,q.x-q.size,q.y-q.size,0).setColor(q.color).setUv(icon.getU0(),icon.getV0());
                out.addVertex(pose,q.x+q.size,q.y-q.size,0).setColor(q.color).setUv(icon.getU1(),icon.getV0());
                out.addVertex(pose,q.x+q.size,q.y+q.size,0).setColor(q.color).setUv(icon.getU1(),icon.getV1());
                out.addVertex(pose,q.x-q.size,q.y+q.size,0).setColor(q.color).setUv(icon.getU0(),icon.getV1());
            }
        }); stack.popPose();
    }
    private record Quad(float x,float y,float size,int color) {}
    public static final class State extends EntityRenderState { List<Quad> quads=List.of(); }
}
