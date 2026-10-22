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
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;
import reika.chromaticraft.render.ChromaBeam;
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
        state.nether=tile.isNether(); state.structure=tile.hasStructure(); state.inWorld=tile.getLevel()!=null;
        state.millis=System.currentTimeMillis(); state.time=state.inWorld?tile.getTicksExisted()+partial:state.millis/50D; state.flash=tile.getFlashBrightness(); state.beams=List.of();
        state.structurePreview=reika.dragonapi.instantiable.rendering.structure.StructureRenderer.isRenderingTiles();
        state.preview=!state.inWorld; state.previewNether=tile.isPreviewNether();
        if (state.structurePreview) {
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
            double radius=.25+.5*(1-tether.getDistance()/16D); double phase=tile.getTicksExisted()/5D;
            for (var spline:tether.getSplines()) {
                var points=spline.getPoints(8);
                for (int i=0;i<points.size()-1;i++) {
                    var a=points.get(i); var b=points.get(i+1); int color=0xff000000|ReikaColorAPI.mixColors(0xffffff,0xda50ff,(float)(.5+.5*Math.sin(phase)));
                    beams.add(new Beam(legacyPoint(new Vec3(a.xCoord(),a.yCoord(),a.zCoord()).subtract(origin)),legacyPoint(new Vec3(b.xCoord(),b.yCoord(),b.zCoord()).subtract(origin)),radius,color)); phase+=.4;
                }
                phase+=.9;
            }
        }
        state.beams=List.copyOf(beams);
    }
    @Override public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        drawInner(state,stack,collector,camera);
        if (state.beams.isEmpty()) return;
        var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.LASER.getIcon());
        stack.pushPose(); outerTransform(stack);
        int order=3;
        for (var beam:state.beams) {
            // V33a draws each core before that segment's planes. Preserve this across native type batching.
            submitGeometry(state,stack,collector,order++,ChromaRenderPipelines.ADDITIVE_LINES_TYPE,(pose,out)-> {
                Vec3 direction=beam.end.subtract(beam.start).normalize();
                if (direction.lengthSqr()==0) direction=new Vec3(0,1,0);
                for (Vec3 point:new Vec3[]{beam.start,beam.end})
                    out.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(beam.color)
                        .setNormal(pose,(float)direction.x,(float)direction.y,(float)direction.z).setLineWidth(1);
            });
            submitGeometry(state,stack,collector,order++,ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),
                    (pose,out)->ChromaBeam.planes(pose,out,sprite,beam.start,beam.end,255,beam.radius,beam.color,beam.color));
        }
        stack.popPose();
    }
    /** Inverse of V33a's outer translate(0,1,1)/scale(1,-1,-1), including the beam width axes. */
    private static Vec3 legacyPoint(Vec3 point) { return new Vec3(point.x,1-point.y,1-point.z); }
    private static void outerTransform(PoseStack stack) { stack.translate(0,1,1); stack.scale(1,-1,-1); }
    private static void submitGeometry(State state,PoseStack stack,SubmitNodeCollector collector,int order,
            RenderType type,SubmitNodeCollector.CustomGeometryRenderer geometry) {
        var captured=stack.last().copy();
        SubmitNodeCollector.CustomGeometryRenderer replay=(ignored,out)->geometry.render(captured,out);
        if (state.inWorld && !state.structurePreview)
            collector.order(order).submitSpecial(ScreenParticleRendering.PHASE,new CustomFeatureRenderer.Submit(captured,type,replay));
        else collector.order(order).submitCustomGeometry(stack,type,replay);
    }
    public static void drawInner(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        Icon[] icons=state.structure||state.structurePreview?((state.previewNether||state.nether)?NETHER:NORMAL):!state.inWorld?NORMAL:BASE;
        stack.pushPose(); outerTransform(stack); stack.translate(.5,.5,.5);
        if (state.structurePreview) { stack.rotate(Axis.YP.rotationDegrees(state.previewRY)); stack.rotate(Axis.XP.rotationDegrees(-state.previewRX)); }
        else if (!state.inWorld) { stack.rotate(Axis.YP.rotationDegrees(-45)); stack.rotate(Axis.XP.rotationDegrees(-30)); } else { stack.rotate(Axis.YP.rotationDegrees(camera.yRot)); stack.rotate(Axis.XP.rotationDegrees(camera.xRot)); }
        float scale=state.inWorld?.4375F:.5F; stack.scale(scale,scale,scale); double time=state.time,swell=2+.5*Math.sin(time/90);
        for (int i=0;i<icons.length;i++) {
            Icon entry=icons[i]; int color=ReikaColorAPI.getModifiedHue(0xff0000,265+(int)(15*Math.sin(time/30+i)));
            color=ReikaColorAPI.getModifiedSat(color,.9F+.1F*(float)Math.sin(time/20+i/4D)); color=ReikaColorAPI.getModifiedSat(color,1-.15F*i);
            if (state.flash>0) color=ReikaColorAPI.getModifiedSat(color,1-state.flash);
            color=0xff000000|ReikaColorAPI.getColorWithBrightnessMultiplier(color,entry.brightness);
            var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(entry.sprite.getIcon());
            stack.pushPose(); float size=(float)(swell*(state.inWorld?entry.worldSize:entry.previewSize)*(state.structurePreview?.625:1)); stack.scale(size,size,size);
            double angle=state.nether&&i==0?(state.millis/12D)%360:!state.nether&&i==1?(-state.millis/50D)%360:0;
            stack.rotate(Axis.ZP.rotationDegrees((float)angle)); int tint=color;
            submitGeometry(state,stack,collector,i,ChromaRenderPipelines.additiveSprite(TextureAtlas.LOCATION_BLOCKS),(pose,out)-> {
                out.addVertex(pose,-1,-1,0).setColor(tint).setUv(sprite.getU0(),sprite.getV0()); out.addVertex(pose,1,-1,0).setColor(tint).setUv(sprite.getU1(),sprite.getV0());
                out.addVertex(pose,1,1,0).setColor(tint).setUv(sprite.getU1(),sprite.getV1()); out.addVertex(pose,-1,1,0).setColor(tint).setUv(sprite.getU0(),sprite.getV1());
            }); stack.popPose();
        } stack.popPose();
    }
    private record Beam(Vec3 start,Vec3 end,double radius,int color) {}
    public static final class State extends BlockEntityRenderState {
        public boolean preview,nether,structure,structurePreview,inWorld,previewNether;
        public float previewRX,previewRY;
        public double time;
        public long millis;
        public float flash;
        private List<Beam> beams=List.of();
    }
}
