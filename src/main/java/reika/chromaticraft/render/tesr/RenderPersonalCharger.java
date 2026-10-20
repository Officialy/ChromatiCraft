package reika.chromaticraft.render.tesr;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityPersonalCharger;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;

/** V33a Personal Charger: source pulse, billboard layers, preview overrides and lightmapped item. */
public final class RenderPersonalCharger implements BlockEntityRenderer<TileEntityPersonalCharger,RenderPersonalCharger.State> {
    private static final Identifier CENTER=sprite("centerblur3"), ROSES_WHITE=sprite("roses_w"),
            BIG_FLARE=sprite("bigflare"), ROSES=sprite("roses");
    public RenderPersonalCharger(BlockEntityRendererProvider.Context context) {}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TileEntityPersonalCharger tile,State state,float partial,Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breaking);
        state.inWorld=tile.hasLevel();
        state.preview=StructureRenderer.isRenderingTiles();
        state.active=tile.canConduct() || state.preview;
        long now=System.currentTimeMillis();
        state.phase=(tile.hashCode()+now/5000D)%360;
        state.color=state.preview && !StructureRenderer.isRenderingRealTiles()
                ?CrystalElement.elements[(int)((now/4000)%16)].color():tile.getRenderColor();
        state.yaw=(float)-StructureRenderer.getRenderRY();
        state.pitch=(float)-StructureRenderer.getRenderRX();
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        pose.pushPose();
        if(!state.inWorld) {
            pose.rotate(Axis.YP.rotationDegrees(45));
            pose.rotate(Axis.XP.rotationDegrees(-45));
            submitItem(pose,collector,state.lightCoords);
        } else {
            pose.translate(.5,.5,.5);
            float yaw=state.preview?state.yaw:-camera.yRot, pitch=state.preview?state.pitch:camera.xRot;
            double scale=state.active?pulseScale(state.phase):.5;
            int count=state.active?3:1;
            Identifier[] icons=state.active?new Identifier[]{CENTER,ROSES_WHITE,BIG_FLARE}:new Identifier[]{CENTER};
            for(int i=0;i<count;i++) {
                boolean last=i==count-1;
                pose.pushPose();
                float s=(float)(last?scale*.75:scale);
                pose.scale(s,s,s);
                pose.rotate(Axis.YP.rotationDegrees(yaw));
                pose.rotate(Axis.XP.rotationDegrees(pitch));
                submitLayer(pose,collector,icons[i],1,(float)(-.005*i),last?0xffffffff:0xff000000|state.color,
                        LightCoordsUtil.FULL_BRIGHT,true);
                pose.popPose();
            }
        }
        pose.popPose();
    }
    /** The original phase is radians after Java remainder by 360, including negative hashes. */
    public static double pulseScale(double phase) {
        return .875+.25*Math.sin(phase)+.125*Math.sin(phase*4)+.0625*Math.sin(phase*16);
    }
    public static void submitItem(PoseStack pose,SubmitNodeCollector collector) {
        submitItem(pose,collector,LightCoordsUtil.FULL_BRIGHT);
    }
    /** Caller establishes item rotations; source inventory leaves the lightmap and depth writes enabled. */
    public static void submitItem(PoseStack pose,SubmitNodeCollector collector,int light) {
        submitLayer(pose,collector,ROSES,.875F,0,0xffffffff,light,false);
    }
    private static void submitLayer(PoseStack pose,SubmitNodeCollector collector,Identifier id,float scale,float z,
            int color,int light,boolean world) {
        var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
        float u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV0(),v1=sprite.getV1();
        SubmitNodeCollector.CustomGeometryRenderer draw=(captured,out)->{
            vertex(out,captured,-scale,-scale,z,u0,v0,color,light);
            vertex(out,captured,scale,-scale,z,u1,v0,color,light);
            vertex(out,captured,scale,scale,z,u1,v1,color,light);
            vertex(out,captured,-scale,scale,z,u0,v1,color,light);
        };
        var type=world?EntityLighting.FULL_BRIGHT.screen(TextureAtlas.LOCATION_BLOCKS,false)
                :EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS,false,true);
        if(world)ScreenParticleRendering.submitGeometry(collector,pose,type,draw);
        else collector.submitCustomGeometry(pose,type,draw);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,int color,int light) {
        out.addVertex(pose,x,y,z).setUv(u,v).setColor(color).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose,0,0,1);
    }
    private static Identifier sprite(String name){return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,"block/icons/"+name);}
    @Override public AABB getRenderBoundingBox(TileEntityPersonalCharger tile){return new AABB(tile.getBlockPos()).inflate(2);}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 96;}
    public static final class State extends BlockEntityRenderState {
        private boolean inWorld,preview,active;
        private int color;
        private double phase;
        private float yaw,pitch;
    }
}
