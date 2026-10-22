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
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.client.SolidModelRendering;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.block.decoration.BlockColoredAltar.TileEntityColoredAltar;
import reika.chromaticraft.models.ModelColoredAltar;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.dragonapi.instantiable.Orbit;
import reika.dragonapi.instantiable.effects.TruncatedCube;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.instantiable.rendering.ColorVariance;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

/** The authored metal cup, 24 independently shimmering crystal parts, and twelve orbiting cube trails. */
public final class RenderColoredAltar implements BlockEntityRenderer<TileEntityColoredAltar,RenderColoredAltar.State> {
    public static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("chromaticraft","textures/entity/colored_altar.png");
    public static final ModelLayerLocation MODEL_LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("chromaticraft","colored_altar"),"main");
    private static final EnumMap<CrystalElement,ColorBlendList[]> COLORS=new EnumMap<>(CrystalElement.class);
    private static final EnumMap<CrystalElement,List<Orbit>> ORBITS=new EnumMap<>(CrystalElement.class);
    private static final TruncatedCube CUBE=new TruncatedCube(.03125,.03125*1.875);
    private static final List<TruncatedCube.Surface> SURFACES=CUBE.surfaces(0,0,0);
    private final ModelColoredAltar model;
    static {
        Random random=new Random();
        for (var color:CrystalElement.elements) {
            ColorBlendList[] blends=new ColorBlendList[24];
            for (int i=0;i<24;i++) { var variance=new ColorVariance(color.color(),10,0,48); variance.whiteVariation=.25F; variance.alphaRoot=171; variance.alphaVariation=21; blends[i]=variance.getBlends(8,10+random.nextInt(61)); }
            COLORS.put(color,blends); var orbits=new ArrayList<Orbit>();
            for (int i=0;i<12;i++) orbits.add(new Orbit(.25+random.nextDouble()*.5,random.nextDouble()*.4,random.nextInt(90),random.nextInt(360),random.nextInt(360),random.nextInt(360)));
            ORBITS.put(color,List.copyOf(orbits));
        }
    }
    public static int[] crystalColors(CrystalElement color,double time) {
        int[] result=new int[24]; for (int i=0;i<24;i++) result[i]=COLORS.get(color)[i].getColor(time); return result;
    }
    public RenderColoredAltar(BlockEntityRendererProvider.Context context) { model=new ModelColoredAltar(context.bakeLayer(MODEL_LAYER)); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityColoredAltar tile) {
        var pos=tile.getBlockPos();double radius=.75*1.4+Math.sqrt(3)*CUBE.mainSize;
        return new net.minecraft.world.phys.AABB(pos.getX()+.5-radius,pos.getY(),pos.getZ()+.5-radius,
            pos.getX()+.5+radius,pos.getY()+Math.max(1,1.25+radius),pos.getZ()+.5+radius);
    }
    public State createRenderState() { return new State(); }
    @Override public void extractRenderState(TileEntityColoredAltar tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        state.color=tile.getColor(); double tick=tile.getRenderTick()+partial;
        for (int i=0;i<24;i++) state.crystals[i]=COLORS.get(state.color)[i].getColor(tick);
        var mc=Minecraft.getInstance();state.inWorld=tile.getLevel()!=null;state.preview=StructureRenderer.isRenderingTiles();
        double distanceSquared=mc.player==null?Double.POSITIVE_INFINITY:mc.player.position().distanceToSqr(Vec3.atCenterOf(tile.getBlockPos()));
        float distance=(float)Math.sqrt(distanceSquared);state.distance=distance;state.cubes=List.of();
        state.metalMask=ModelColoredAltar.ALL_METAL;state.crystalMask=ModelColoredAltar.ALL_CRYSTALS;
        if(state.inWorld&&!state.preview) {
            boolean control=(org.lwjgl.sdl.SDLKeyboard.SDL_GetModState()&net.minecraft.client.input.InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)!=0;
            var visible=model.visibility(distanceSquared,mc.options.renderDistance().get(),control);state.metalMask=visible.metal();state.crystalMask=visible.crystals();
        }
        if(!state.inWorld||state.preview)return;
        int setting=mc.options.particles().get().ordinal();
        double step=4*(1+setting/2D); if (distance>8) step=Math.min(12,step*2); if (distance>16) step=Math.min(16,step*2);
        List<Cube> cubes=new ArrayList<>();
        for (int i=0;i<12/(1+setting);i++) {
            var orbit=ORBITS.get(state.color).get(i); var rotation=tile.cubeRotations[i];
            for (double tail=-24;tail<=0;tail+=step) {
                var point=orbit.getPosition(.5,1.25,.5,tile.getRenderTick()*4+tail);
                cubes.add(new Cube(new Vec3(point.xCoord(), point.yCoord(), point.zCoord()),(float)rotation.angX,(float)rotation.angY,(float)rotation.angZ,tail));
            }
            double t=tile.getTicksExisted()+partial,v=.375;
            rotation.rvX=v*(1+.5*Math.sin(Math.toRadians(t)))*(1+.5*Math.cos(Math.toRadians(90+t)));
            rotation.rvY=v*(1+.25*Math.sin(Math.toRadians(2*t)))*(1+.75*Math.cos(Math.toRadians(t)));
            rotation.rvZ=v*(1+.75*Math.sin(Math.toRadians(90+t)))*(1+.25*Math.cos(Math.toRadians(2*t)));
            rotation.angX+=rotation.rvX; rotation.angY+=rotation.rvY; rotation.angZ+=rotation.rvZ;
        }
        state.cubes=List.copyOf(cubes);
    }
    @Override public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera) {
        stack.pushPose();modelTransform(stack);
        var captured=new PoseStack();captured.last().set(stack.last());int light=state.lightCoords;int[] colors=state.crystals.clone();
        long metalMask=state.metalMask,crystalMask=state.crystalMask;
        var metalType=EntityLighting.LIGHTMAP_ONLY.alphaModel(TEXTURE,true,true);
        SubmitNodeCollector.CustomGeometryRenderer metal=(pose,out)->model.renderMetal(captured,out,light,OverlayTexture.NO_OVERLAY,metalMask);
        if(metalMask!=0) {
            if(state.inWorld&&!state.preview)SolidModelRendering.submitGeometry(collector,stack,metalType,metal);
            else collector.order(0).submitCustomGeometry(stack,metalType,metal);
        }
        var crystalType=EntityLighting.LIGHTMAP_ONLY.alphaModel(TEXTURE,false,!state.inWorld||state.preview);
        if(crystalMask!=0)submitAlpha(state,stack,collector,1,crystalType,(pose,out)->model.renderCrystals(captured,out,colors,light,OverlayTexture.NO_OVERLAY,crystalMask));
        stack.popPose();
        int base=ReikaColorAPI.mixColors(0xffffff,state.color.color(),.125F),edge=ReikaColorAPI.mixColors(0xffffff,state.color.color(),.375F)|150<<24;
        float distance=state.distance;int order=2;
        for(Cube cube:state.cubes) {
            stack.pushPose();stack.translate(cube.point.x,cube.point.y,cube.point.z);
            stack.rotate(Axis.XP.rotationDegrees(cube.x));stack.rotate(Axis.YP.rotationDegrees(cube.y));stack.rotate(Axis.ZP.rotationDegrees(cube.z));
            var pose=stack.last().copy();int fill=base|(int)(190*(1+cube.tail/24))<<24;
            if(cube.tail==0) {
                // Ordinary alpha is order-dependent: V33a pairs every face/corner fill with its outline.
                for(var surface:SURFACES) {
                    submitAlpha(state,stack,collector,order++,ChromaRenderPipelines.LIGHTMAPPED_ALPHA_TRIANGLES_TYPE,
                        (ignored,out)->TruncatedCube.renderSurfaceFaces(pose,out,surface,fill,240));
                    submitAlpha(state,stack,collector,order++,ChromaRenderPipelines.LIGHTMAPPED_ALPHA_LINES_TYPE,
                        (ignored,out)->TruncatedCube.renderSurfaceEdges(pose,out,surface,edge,distance,240));
                }
            } else submitAlpha(state,stack,collector,order++,ChromaRenderPipelines.LIGHTMAPPED_ALPHA_TRIANGLES_TYPE,
                (ignored,out)->CUBE.renderFaces(pose,out,0,0,0,fill,240));
            stack.popPose();
        }
    }
    public static void modelTransform(PoseStack stack) {stack.translate(.5,1.5,.5);stack.scale(1,-1,-1);}
    private static void submitAlpha(State state,PoseStack stack,SubmitNodeCollector collector,int order,RenderType type,
            SubmitNodeCollector.CustomGeometryRenderer geometry) {
        if(state.inWorld&&!state.preview)collector.order(order).submitSpecial(ScreenParticleRendering.PHASE,new CustomFeatureRenderer.Submit(stack.last().copy(),type,geometry));
        else collector.order(order).submitCustomGeometry(stack,type,geometry);
    }
    private record Cube(Vec3 point,float x,float y,float z,double tail) {}
    public static final class State extends BlockEntityRenderState {
        CrystalElement color=CrystalElement.WHITE;final int[] crystals=new int[24];List<Cube> cubes=List.of();float distance;
        boolean inWorld,preview;long metalMask=ModelColoredAltar.ALL_METAL,crystalMask=ModelColoredAltar.ALL_CRYSTALS;
    }
}
