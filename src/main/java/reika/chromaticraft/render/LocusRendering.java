package reika.chromaticraft.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.dragonapi.extras.shader.IrisCompat;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Explicit V33a Locus draw state: source-alpha addition, screen glow and ordinary-alpha lines. */
public final class LocusRendering {
    private LocusRendering() {}
    public static final int CORE_LIGHT=240;
    private static final RenderPipeline[] CORE=new RenderPipeline[2], SCREEN=new RenderPipeline[2], LINES=new RenderPipeline[2];
    private static final RenderType[] CORE_TYPES=new RenderType[2], SCREEN_TYPES=new RenderType[2], LINE_TYPES=new RenderType[2];
    static {
        for(int i=0;i<2;i++) {
            boolean depthTest=i==0;
            var depth=new DepthStencilState(depthTest?CompareOp.GREATER_THAN_OR_EQUAL:CompareOp.ALWAYS_PASS,false);
            String suffix=depthTest?"":"_no_depth";
            CORE[i]=sprite("core"+suffix,new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE),depth);
            SCREEN[i]=sprite("screen"+suffix,new BlendFunction(BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_COLOR),depth);
            LINES[i]=RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.FOG)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                    .withVertexShader(Identifier.fromNamespaceAndPath("chromaticraft","core/adjacency_lines"))
                    .withFragmentShader("core/rendertype_lines").withCull(false)
                    .withVertexBinding(0,ChromaRenderPipelines.ADJACENCY_LINE_FORMAT).withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withLocation(id("lines"+suffix)).withDepthStencilState(depth)
                    .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE_MINUS_SRC_ALPHA))).build();
            CORE_TYPES[i]=RenderType.create("locus_core"+suffix,RenderSetup.builder(CORE[i]).withTexture("Sampler0",reika.chromaticraft.render.tesr.RenderDimensionCore.TEXTURE).useLightmap().createRenderSetup());
            SCREEN_TYPES[i]=RenderType.create("locus_screen"+suffix,RenderSetup.builder(SCREEN[i]).withTexture("Sampler0",TextureAtlas.LOCATION_BLOCKS).useLightmap().createRenderSetup());
            LINE_TYPES[i]=RenderType.create("locus_lines"+suffix,RenderSetup.builder(LINES[i]).useLightmap().createRenderSetup());
        }
    }
    private static Identifier id(String name) {return Identifier.fromNamespaceAndPath("chromaticraft","locus/"+name);}
    private static RenderPipeline sprite(String name,BlendFunction blend,DepthStencilState depth) {
        return RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET).withLocation(id(name))
                .withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                .withCull(false).withDepthStencilState(depth).withColorTargetState(new ColorTargetState(blend)).build();
    }
    public static RenderType core(boolean depthTest) {return CORE_TYPES[depthTest?0:1];}
    public static RenderType screen(boolean depthTest) {return SCREEN_TYPES[depthTest?0:1];}
    public static RenderType lines(boolean depthTest) {return LINE_TYPES[depthTest?0:1];}
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(LocusGuiItems.ADDITIVE_BLIT);
        for(var pipelines:new RenderPipeline[][]{CORE,SCREEN,LINES})for(var pipeline:pipelines) {
            event.registerPipeline(pipeline);
            IrisCompat.assignWithoutShadow(pipeline,pipelines==LINES?"LINES":"ENTITIES");
        }
    }
    public static void submit(PoseStack pose,SubmitNodeCollector collector,RenderType type,
            SubmitNodeCollector.CustomGeometryRenderer draw,boolean world) {
        if(world)ScreenParticleRendering.submitGeometry(collector,pose,type,draw);
        else collector.submitCustomGeometry(pose,type,draw);
    }
    /** Caller provides the original billboard transform; every layer keeps the same position-based roll. */
    public static void coreLayers(PoseStack pose,SubmitNodeCollector collector,int baseColor,int frame,
            float roll,boolean world,boolean depthTest,int light) {
        float u=frame%8/8F,v=frame/8/10F;
        for(int layer=0;layer<3;layer++) {
            int alpha=255-layer*64;
            int color=alpha<<24|ReikaColorAPI.getColorWithBrightnessMultiplier(baseColor&0xffffff,alpha/255F);
            float scale=1-layer*.25F;
            pose.pushPose();pose.scale(scale,scale,scale);pose.rotate(Axis.ZP.rotationDegrees(roll));
            quad(pose,collector,core(depthTest),world,-1,-1,1,1,0,u,v,u+1/8F,v+1/10F,color,light);
            pose.popPose();
        }
    }
    /** Immutable scalar UV/color/light capture, in the original bottom-left-first UV order. */
    public static void quad(PoseStack pose,SubmitNodeCollector collector,RenderType type,boolean world,
            float x0,float y0,float x1,float y1,float z,float u0,float v0,float u1,float v1,int color,int light) {
        submit(pose,collector,type,(captured,out)->{
            vertex(captured,out,x0,y0,z,u0,v0,color,light);vertex(captured,out,x1,y0,z,u1,v0,color,light);
            vertex(captured,out,x1,y1,z,u1,v1,color,light);vertex(captured,out,x0,y1,z,u0,v1,color,light);
        },world);
    }
    private static void vertex(PoseStack.Pose pose,VertexConsumer out,float x,float y,float z,float u,float v,int color,int light) {
        out.addVertex(pose,x,y,z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,1);
    }
}
