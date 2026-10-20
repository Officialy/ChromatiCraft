package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.relay.BlockLumenRelay;
import reika.chromaticraft.block.relay.BlockLumenRelay.TileEntityLumenRelay;
import reika.dragonapi.client.EntityLighting;

/** V33a RelayRenderer: full-bright terrain geometry and ordinary translucent crystal/connectivity. */
public final class RenderLumenRelay implements BlockEntityRenderer<TileEntityLumenRelay,RenderLumenRelay.State> {
    private static final Identifier STONE=sprite("block/pylon/block_0"), CRYSTAL=sprite("block/crystal/crystal_outline"),
            FRAME=sprite("block/icons/glowframe2"), DOT=sprite("block/icons/glowframe_dot2");
    public RenderLumenRelay(BlockEntityRendererProvider.Context context) {}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TileEntityLumenRelay tile,State state,float partial,Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        state.facing=tile.getBlockState().getValue(BlockLumenRelay.FACING);
        state.input=tile.getInput();
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        var stone=body(state.facing,false,atlas(STONE));
        var crystal=body(state.facing,true,atlas(CRYSTAL));
        var connectivity=connectivity(state.facing,state.input,atlas(FRAME),atlas(DOT));
        collector.submitCustomGeometry(pose,EntityLighting.FULL_BRIGHT.cutout(TextureAtlas.LOCATION_BLOCKS,true),
                (captured,out)->emit(stone,captured,out,0xffffffff));
        // Original ISBRH pass 1 inherits standard terrain alpha blending, never ADDITIVEDARK.
        var translucent=EntityLighting.FULL_BRIGHT.translucent(TextureAtlas.LOCATION_BLOCKS,true);
        collector.submitCustomGeometry(pose,translucent,(captured,out)->emit(crystal,captured,out,0xd4ffffff));
        collector.submitCustomGeometry(pose,translucent,(captured,out)->emit(connectivity,captured,out,0xffffffff));
    }
    private record Vertex(double x,double y,double z,double u,double v) {}
    private static List<Vertex> body(Direction dir,boolean crystal,TextureAtlasSprite sprite) {
        double minx=0,miny=0,minz=0,w=.125;
        boolean negative=dir.getStepX()+dir.getStepY()+dir.getStepZ()<0;
        double h=negative?.25:.3125;
        double u=sprite.getU0(),v=sprite.getV0(),du=sprite.getU1(),dv=sprite.getV1();
        var vertices=new ArrayList<Vertex>();
        if(!crystal) {
            vertices.add(new Vertex(minx,miny,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny,minz,du,v));
            vertices.add(new Vertex(minx+w*2,miny,minz+w*2,du,dv));
            vertices.add(new Vertex(minx,miny,minz+w*2,u,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,du,v));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,v));
            vertices.add(new Vertex(minx,miny+h,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h,minz,du,v));
            vertices.add(new Vertex(minx,miny+h,minz,u,v));
            vertices.add(new Vertex(minx,miny,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h,minz+w*2,u,v));
            vertices.add(new Vertex(minx,miny+h,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h,minz,du,v));
            vertices.add(new Vertex(minx+w*2,miny,minz,du,dv));
            vertices.add(new Vertex(minx,miny,minz,u,dv));
            vertices.add(new Vertex(minx,miny,minz,u,dv));
            vertices.add(new Vertex(minx,miny,minz+w*2,du,dv));
            vertices.add(new Vertex(minx,miny+h,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h,minz+w*2,du,v));
            vertices.add(new Vertex(minx+w*2,miny,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny,minz,u,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,du,v));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,v));
            vertices.add(new Vertex(minx,miny+h,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,u,v));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,du,v));
            vertices.add(new Vertex(minx+w*2,miny+h,minz,du,dv));
            vertices.add(new Vertex(minx,miny+h,minz,u,dv));
            vertices.add(new Vertex(minx,miny+h,minz,u,dv));
            vertices.add(new Vertex(minx,miny+h,minz+w*2,du,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx+w*2,miny+h,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h,minz,u,dv));
        } else {
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz,du,v));
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz,u,v));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,u,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz+w*2,u,v));
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz,du,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,du,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz,u,dv));
            vertices.add(new Vertex(minx,miny+h*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx,miny+h*2+w*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz,u,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2+w*2,minz+w*2,du,v));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz+w*2,du,dv));
            vertices.add(new Vertex(minx+w*2,miny+h*2,minz,u,dv));
        }
        // Match TessellatorVertexList's reflections, origin swaps, and winding reversals.
        if(negative) {
            vertices.replaceAll(p->new Vertex(p.x,1-p.y,p.z,p.u,p.v));
            Collections.reverse(vertices);
        }
        if(dir.getStepX()!=0) {
            vertices.replaceAll(p->new Vertex(p.y-.5,p.x+.5-w,p.z-w,p.u,p.v));
            Collections.reverse(vertices);
        } else if(dir.getStepZ()!=0) {
            vertices.replaceAll(p->new Vertex(p.x-w,p.z+.5-w,p.y-.5,p.u,p.v));
            Collections.reverse(vertices);
        } else vertices.replaceAll(p->new Vertex(p.x-w,p.y,p.z-w,p.u,p.v));
        vertices.replaceAll(p->new Vertex(p.x+.5,p.y,p.z+.5,p.u,p.v));
        return List.copyOf(vertices);
    }
    private static List<Vertex> connectivity(Direction facing,Direction input,TextureAtlasSprite frame,TextureAtlasSprite dot) {
        double w=.1875,h=facing.getStepX()+facing.getStepY()+facing.getStepZ()<0?.125:.25;
        var vertices=new ArrayList<Vertex>();
        for(var dir:Direction.values()) {
            var sprite=dir==input?dot:frame;
            double u=sprite.getU0(),v=sprite.getV0(),du=sprite.getU1(),dv=sprite.getV1();
            if(dir==Direction.EAST) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,u,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,u,dv));
            }
            if(dir==Direction.WEST) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,u,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,u,v));
            }
            if(dir==Direction.NORTH) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,u,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,u,dv));
            }
            if(dir==Direction.SOUTH) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,u,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,u,v));
            }
            if(dir==Direction.DOWN) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,u,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5-w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5-w,h*facing.getStepZ()+0.5+w,u,dv));
            }
            if(dir==Direction.UP) {
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,u,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5+w,du,dv));
            vertices.add(new Vertex(h*facing.getStepX()+0.5+w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,du,v));
            vertices.add(new Vertex(h*facing.getStepX()+0.5-w,h*facing.getStepY()+0.5+w,h*facing.getStepZ()+0.5-w,u,v));
            }
        }
        return List.copyOf(vertices);
    }
    private static void emit(List<Vertex> vertices,PoseStack.Pose pose,VertexConsumer out,int color) {
        for(var p:vertices) out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z)
                .setUv((float)p.u,(float)p.v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose,0,1,0);
    }
    private static Identifier sprite(String path){return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,path);}
    private static TextureAtlasSprite atlas(Identifier id){return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);}
    public static final class State extends BlockEntityRenderState {
        private Direction facing=Direction.UP,input=Direction.DOWN;
    }
}
