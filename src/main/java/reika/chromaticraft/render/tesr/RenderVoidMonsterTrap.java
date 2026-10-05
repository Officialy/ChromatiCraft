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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.modinterface.voidritual.TileEntityVoidMonsterTrap;
import reika.chromaticraft.modinterface.voidritual.VoidRitualShaders;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.RayTracer;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.List;

/** Original stacked glows and pulsing spline tethers, snapshotted for native geometry submission. */
public final class RenderVoidMonsterTrap implements BlockEntityRenderer<TileEntityVoidMonsterTrap,RenderVoidMonsterTrap.State> {
    private record Icon(ChromaIcons sprite,double worldSize,double previewSize,float brightness) {}
    private static final Icon[] NORMAL={new Icon(ChromaIcons.SUNFLARE,4,1.25,1),new Icon(ChromaIcons.CELLFLARE,2.75,1.25,1),new Icon(ChromaIcons.ECLIPSEFLARE,3,.9375,.75F)};
    private static final Icon[] NETHER={new Icon(ChromaIcons.WHITEHOLE,2,1.25,1),new Icon(ChromaIcons.PORTALRING,1.25,1.25,2),new Icon(ChromaIcons.HEXFLARE2,2,1.25,.5F)};
    private static final Icon[] BASE={new Icon(ChromaIcons.CELLFLARE,1.5,1,1),new Icon(ChromaIcons.PORTALRING,.75,1,1),new Icon(ChromaIcons.PORTALRING,1,1,1)};
    private static final RayTracer LOS=RayTracer.getVisualLOS();
    public RenderVoidMonsterTrap(BlockEntityRendererProvider.Context context) { }
    public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileEntityVoidMonsterTrap tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        state.nether=tile.isNether(); state.structure=tile.hasStructure(); state.time=tile.getTicksExisted()+partial; state.flash=tile.getFlashBrightness(); state.beams=List.of();
        state.structurePreview=reika.dragonapi.instantiable.rendering.structure.StructureRenderer.isRenderingTiles();
        state.preview=state.structurePreview;
        if (state.structurePreview) {
            state.nether=tile.isPreviewNether(); state.time=System.currentTimeMillis()/50D;
            state.previewRY=(float)reika.dragonapi.instantiable.rendering.structure.StructureRenderer.getRenderRY();
            state.previewRX=(float)reika.dragonapi.instantiable.rendering.structure.StructureRenderer.getRenderRX();
            return;
        }
        if (tile.getLevel()==null) return;
        var player=Minecraft.getInstance().player; Vec3 center=Vec3.atCenterOf(tile.getBlockPos());
        if (state.structure && player!=null) {
            LOS.setOrigins(center.x,center.y,center.z,player.getX(),player.getY(),player.getZ());
            if (LOS.isClearLineOfSight(tile.getLevel())) { double d=player.position().distanceTo(center); float intensity=d<=16?1:d<=32?1-(float)((d-16)/16):0; VoidRitualShaders.markTrap(center,intensity,d*d,tile.getShaderRotation()); }
        }
        List<Beam> beams=new ArrayList<>(); Vec3 origin=Vec3.atLowerCornerOf(tile.getBlockPos());
        for (var tether:tile.getTethers()) {
            float radius=(float)(.25+.5*(1-tether.getDistance()/16)); double phase=tile.getTicksExisted()/5D;
            for (var spline:tether.getSplines()) {
                var points=spline.getPoints(8);
                for (int i=0;i<points.size()-1;i++) {
                    var a=points.get(i); var b=points.get(i+1); int color=0xff000000|ReikaColorAPI.mixColors(0xffffff,0xda50ff,(float)(.5+.5*Math.sin(phase)));
                    beams.add(new Beam(new Vec3(a.xCoord(), a.yCoord(), a.zCoord()).subtract(origin),new Vec3(b.xCoord(), b.yCoord(), b.zCoord()).subtract(origin),radius,color)); phase+=.4;
                }
                phase+=.9;
            }
        }
        state.beams=List.copyOf(beams);
    }
    @Override public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        drawInner(state,stack,collector,camera);
        var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.LASER.getIcon());
        var beams=state.beams;
        collector.submitCustomGeometry(stack,ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),(pose,out)-> { for (var beam:beams) beam(pose,out,sprite,beam); });
        collector.submitCustomGeometry(stack,ChromaRenderPipelines.translucentColorLines(),(pose,out)-> {
            for (var beam:beams) {
                var direction=beam.end.subtract(beam.start).normalize();
                for (Vec3 point:new Vec3[]{beam.start,beam.end})
                    out.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(beam.color)
                        .setNormal(pose,(float)direction.x,(float)direction.y,(float)direction.z).setLineWidth(1);
            }
        });
    }
    public static void drawInner(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        Icon[] icons=state.preview||state.structure?(state.nether?NETHER:NORMAL):BASE;
        stack.pushPose(); stack.translate(.5,.5,.5);
        if (state.structurePreview) { stack.rotate(Axis.YP.rotationDegrees(state.previewRY)); stack.rotate(Axis.XP.rotationDegrees(-state.previewRX)); }
        else if (state.preview) { stack.rotate(Axis.YP.rotationDegrees(-45)); stack.rotate(Axis.XP.rotationDegrees(-30)); } else stack.rotate(camera.orientation);
        float scale=state.structurePreview?.33F*.625F:state.preview?.5F:.4375F; stack.scale(scale,scale,scale); double time=state.time,swell=2+.5*Math.sin(time/90);
        for (int i=0;i<icons.length;i++) {
            Icon entry=icons[i]; int color=ReikaColorAPI.getModifiedHue(0xff0000,265+(int)(15*Math.sin(time/30+i)));
            color=ReikaColorAPI.getModifiedSat(color,.9F+.1F*(float)Math.sin(time/20+i/4D)); color=ReikaColorAPI.getModifiedSat(color,1-.15F*i);
            if (state.flash>0) color=ReikaColorAPI.getModifiedSat(color,1-state.flash);
            color=0xff000000|ReikaColorAPI.getColorWithBrightnessMultiplier(color,entry.brightness);
            var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(entry.sprite.getIcon());
            stack.pushPose(); float size=(float)(swell*(state.preview?entry.previewSize:entry.worldSize)); stack.scale(size,size,size);
            double angle=state.nether&&i==0?(System.currentTimeMillis()/12D)%360:!state.nether&&i==1?(-System.currentTimeMillis()/50D)%360:0;
            stack.rotate(Axis.ZP.rotationDegrees((float)angle)); int tint=color;
            collector.submitCustomGeometry(stack,ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),(pose,out)-> {
                out.addVertex(pose,-1,-1,0).setColor(tint).setUv(sprite.getU0(),sprite.getV0()); out.addVertex(pose,1,-1,0).setColor(tint).setUv(sprite.getU1(),sprite.getV0());
                out.addVertex(pose,1,1,0).setColor(tint).setUv(sprite.getU1(),sprite.getV1()); out.addVertex(pose,-1,1,0).setColor(tint).setUv(sprite.getU0(),sprite.getV1());
            }); stack.popPose();
        } stack.popPose();
    }
    private static void beam(PoseStack.Pose pose,VertexConsumer out,TextureAtlasSprite sprite,Beam beam) {
        int sections=Math.max(1,(int)Math.ceil(beam.start.distanceTo(beam.end)));
        for (int section=0;section<sections;section++) {
            Vec3 start=beam.start.lerp(beam.end,section/(double)sections),end=beam.start.lerp(beam.end,(section+1)/(double)sections);
            for (Vec3 axis:new Vec3[]{new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1)}) {
                Vec3 offset=axis.scale(beam.radius); vertex(out,pose,start.subtract(offset),sprite.getU0(),sprite.getV0(),beam.color); vertex(out,pose,start.add(offset),sprite.getU0(),sprite.getV1(),beam.color);
                vertex(out,pose,end.add(offset),sprite.getU1(),sprite.getV1(),beam.color); vertex(out,pose,end.subtract(offset),sprite.getU1(),sprite.getV0(),beam.color);
            }
        }
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,int color) { out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(u,v); }
    private record Beam(Vec3 start,Vec3 end,float radius,int color) {}
    public static final class State extends BlockEntityRenderState { public boolean preview,nether,structure,structurePreview; public float previewRX,previewRY; public double time; public float flash; private List<Beam> beams=List.of(); }
}
