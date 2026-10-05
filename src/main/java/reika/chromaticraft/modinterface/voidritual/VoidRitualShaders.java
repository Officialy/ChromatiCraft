package reika.chromaticraft.modinterface.voidritual;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector4f;

import java.util.*;

/** V33a sphere/trap screen shaders and terrain deformation on every native 26.3 chunk draw path. */
public final class VoidRitualShaders {
    private static final BindGroupLayout POST_LAYOUT=BindGroupLayout.builder().withUniform("InSampler",UniformType.COMBINED_IMAGE_SAMPLER).withUniform("RitualPost",UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout TERRAIN_LAYOUT=BindGroupLayout.builder().withUniform("RitualTerrain",UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline POST=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET).withLocation(id("pipeline/void_ritual_post")).withVertexShader("core/screenquad").withFragmentShader(id("post/void_ritual")).withBindGroupLayout(POST_LAYOUT).withColorTargetState(ColorTargetState.DEFAULT).build();
    private static final Map<RenderPipeline,RenderPipeline> TERRAIN=new IdentityHashMap<>();
    private static MappableRingBuffer postUniform,terrainUniform;
    private static boolean terrainUploaded;
    private static Vec3 trapFocus;
    private static float trapIntensity,trapDistance,trapRotation;
    private VoidRitualShaders() {}
    private static net.minecraft.world.entity.Entity findEntity(UUID uuid) {
        var world=Minecraft.getInstance().level; if (world==null) return null;
        for (var entity:world.entitiesForRendering()) if (entity.getUUID().equals(uuid)) return entity;
        return null;
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("chromaticraft",path); }
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(POST);
        for (boolean multi:new boolean[]{false,true}) {
            for (ChunkSectionLayer layer:ChunkSectionLayer.values()) {
                var builder=RenderPipeline.builder(multi?RenderPipelines.MULTIDRAW_TERRAIN_SNIPPET:RenderPipelines.TERRAIN_SNIPPET)
                    .withLocation(id("pipeline/void_ritual_"+layer.label()+(multi?"_multi":""))).withVertexShader(id("core/void_ritual_terrain")).withBindGroupLayout(TERRAIN_LAYOUT);
                builder.withColorTargetState(layer==ChunkSectionLayer.TRANSLUCENT?new ColorTargetState(BlendFunction.TRANSLUCENT):ColorTargetState.DEFAULT);
                if (layer!=ChunkSectionLayer.SOLID) builder.withShaderDefine("ALPHA_CUTOUT",layer==ChunkSectionLayer.CUTOUT?.5F:.1F);
                var pipeline=builder.build(); TERRAIN.put(layer.pipeline(multi),pipeline); event.registerPipeline(pipeline);
            }
            var builder=RenderPipeline.builder(RenderPipelines.GENERIC_BLOCKS_SNIPPET).withVertexShader(id("core/void_ritual_terrain")).withFragmentShader("core/terrain")
                .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO).withBindGroupLayout(TERRAIN_LAYOUT).withShaderDefine("ALPHA_CUTOUT",.1F);
            if (multi) builder.withVertexBinding(1,DefaultVertexFormat.CHUNK_DATA_INSTANCED).withShaderDefine("MULTIDRAW_TERRAIN"); else builder.withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION);
            OitPipelineSet set=OitPipelineSet.builder(id("void_ritual_terrain"+(multi?"_multi":"")),builder).withAccumulateModifier(b->b.withBindGroupLayout(BindGroupLayouts.SAMPLER2)).build();
            OitPipelineSet original=multi?RenderPipelines.OIT_TERRAIN_MULTIDRAW:RenderPipelines.OIT_TERRAIN;
            for (OitStage stage:new OitStage[]{OitStage.DEPTH_BOUNDS,OitStage.TRANSMITTANCE,OitStage.ACCUMULATE}) { var pipeline=set.getPipeline(stage); TERRAIN.put(original.getPipeline(stage),pipeline); event.registerPipeline(pipeline); }
        }
    }
    public static RenderPipeline terrainPipeline(RenderPipeline original) { return terrainActive()?TERRAIN.getOrDefault(original,original):original; }
    public static boolean terrainActive() { return VoidMonsterRitualClientEffects.visuals().stream().anyMatch(v->v.cause!=VoidMonsterDestructionRitual.Effects.COLLAPSING_SPHERE && v.intensity>0); }
    public static void bindTerrain(RenderPass pass) {
        if (!terrainActive()) return;
        if (terrainUniform==null) terrainUniform=new MappableRingBuffer(()->"Void ritual terrain",GpuBuffer.USAGE_MAP_WRITE|GpuBuffer.USAGE_UNIFORM,64);
        if (!terrainUploaded) {
            Vec3 focus=Vec3.ZERO; float intensity=0,wave=0,thick=4,amplitude=0,curlX=0,curlY=0,stretch=1,radius=1000,application=0;
            var mc=Minecraft.getInstance();
            for (var visual:VoidMonsterRitualClientEffects.visuals()) if (visual.intensity>0 && visual.cause!=VoidMonsterDestructionRitual.Effects.COLLAPSING_SPHERE) {
                var entity=mc.level==null?null:findEntity(visual.focus); if (entity==null) continue;
                focus=entity.position().subtract(mc.gameRenderer.mainCamera().position()); intensity=Math.max(intensity,visual.intensity);
                switch (visual.cause) {
                    case WAVE -> { wave=visual.waveRadius; thick=visual.waveThickness; amplitude=visual.waveAmplitude; }
                    case CURL -> { curlX=visual.curlXZ; curlY=visual.curlY; }
                    case STRETCH -> { stretch=visual.stretchFactor; radius=visual.stretchRadius; application=visual.stretchApplication; }
                    default -> { }
                }
            }
            try (var mapping=terrainUniform.currentBuffer().map(false,true)) {
                Std140Builder.intoBuffer(mapping.data()).putVec4((float)focus.x,(float)focus.y,(float)focus.z,intensity)
                    .putVec4(wave,thick,amplitude,0).putVec4(curlX,curlY,stretch,radius).putVec4(application,0,0,0);
            }
            terrainUploaded=true;
        }
        pass.setUniform("RitualTerrain",terrainUniform.currentBuffer());
    }
    public static void markTrap(Vec3 center,float intensity,double distance,float rotation) { trapFocus=center; trapIntensity=intensity; trapDistance=(float)distance; trapRotation=rotation; }
    public static void reset() { trapFocus=null; trapIntensity=0; terrainUploaded=false; }
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        try { renderPost(event); }
        finally { trapFocus=null; trapIntensity=0; terrainUploaded=false; if (terrainUniform!=null) terrainUniform.rotate(); }
    }
    private static Vector4f projected(Vec3 focus,RenderLevelStageEvent.AfterLevel event) {
        var camera=event.getLevelRenderState().cameraRenderState;
        var point=new Vector4f((float)(focus.x-camera.pos.x),(float)(focus.y-camera.pos.y),(float)(focus.z-camera.pos.z),1).mul(event.getModelViewMatrix()).mul(camera.projectionMatrix);
        return point.w<=.0001F?new Vector4f(0,0,0,0):new Vector4f(point.x/point.w*.5F+.5F,point.y/point.w*.5F+.5F,0,1);
    }
    private static void renderPost(RenderLevelStageEvent.AfterLevel event) {
        var mc=Minecraft.getInstance(); Vector4f sphere=new Vector4f(),trap=new Vector4f(); float size=0,thickness=1;
        for (var visual:VoidMonsterRitualClientEffects.visuals()) if (visual.cause==VoidMonsterDestructionRitual.Effects.COLLAPSING_SPHERE && visual.intensity>0) {
            var entity=mc.level==null?null:findEntity(visual.focus); if (entity==null) continue;
            sphere=projected(entity.position(),event); sphere.w*=visual.intensity; size=visual.size; thickness=visual.thickness;
        }
        if (trapFocus!=null && trapIntensity>0) { trap=projected(trapFocus,event); trap.w*=trapIntensity; }
        if (sphere.w==0 && trap.w==0) return;
        var chain=mc.getShaderManager().getPostChain(id("void_ritual"),Set.of(PostChain.MAIN_TARGET_ID,id("void_ritual_warped"))); if (chain==null) return;
        if (postUniform==null) postUniform=new MappableRingBuffer(()->"Void ritual screen distortion",GpuBuffer.USAGE_MAP_WRITE|GpuBuffer.USAGE_UNIFORM,64);
        try (var mapping=postUniform.currentBuffer().map(false,true)) {
            Std140Builder.intoBuffer(mapping.data()).putVec4(sphere.x,sphere.y,size,sphere.w).putVec4(thickness,0,0,0).putVec4(trap.x,trap.y,trapDistance,trap.w).putVec4(trapRotation,0,0,0);
        }
        RenderTarget main=mc.gameRenderer.mainRenderTarget(); var frame=new FrameGraphBuilder(); var source=frame.importExternal("main",main);
        var warped=frame.createInternal("void_ritual_warped",new RenderTargetDescriptor(main.width,main.height,new RenderTargetDescriptor.TextureProperties(new Vector4f(),GpuFormat.RGBA8_UNORM),null));
        var pass=frame.addPass("void_ritual_distortion"); pass.reads(source); var output=pass.readsAndWrites(warped);
        pass.executes(()-> { var encoder=RenderSystem.getDevice().createCommandEncoder();
            try (var draw=encoder.createRenderPass(()->"Void ritual distortion",output.get().getColorTextureView(),Optional.empty())) {
                draw.setPipeline(RenderSystem.getCompiledPipeline(POST)); RenderSystem.bindDefaultUniforms(draw);
                draw.setUniform("RitualPost",postUniform.currentBuffer()); draw.setUniform("InSampler",source.get().getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)); draw.draw(3,1,0,0);
            }
        });
        Map<Identifier,ResourceHandle<RenderTarget>> targets=new HashMap<>(); targets.put(PostChain.MAIN_TARGET_ID,source); targets.put(id("void_ritual_warped"),output);
        chain.addToFrame(frame,main.width,main.height,new PostChain.TargetBundle() {
            public void replace(Identifier id,ResourceHandle<RenderTarget> handle) { targets.put(id,handle); }
            public ResourceHandle<RenderTarget> get(Identifier id) { return targets.get(id); }
        }); frame.execute(GraphicsResourceAllocator.UNPOOLED); postUniform.rotate();
    }
}
