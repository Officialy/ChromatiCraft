package reika.chromaticraft.render;

import java.util.function.Function;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import reika.chromaticraft.ChromatiCraft;

/** Additive, full-bright pipelines matching V33a's direct-framebuffer ADDITIVEDARK passes.
 *
 * <p>Neither pipeline writes depth — see the note above {@link #ADDITIVE_PARTICLE}. Ordering against
 * water and clouds is handled by <em>when</em> the glow is submitted (after the translucent chunk
 * layer), not by stamping it into the depth buffer. */
public final class ChromaRenderPipelines {

    /** V33a {@code BlendMode.ADDITIVEDARK}: GL_ONE, GL_ONE_MINUS_SRC_COLOR. */
    private static final BlendFunction ADDITIVE_DARK =
            new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR);

    public static final RenderPipeline ADDITIVE_SPRITE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_sprite"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /*
     * Depth WRITE is deliberately off on both additive pipelines. They draw into the main colour and
     * depth targets so V33a's ADDITIVEDARK screen blend can be evaluated against the already-rendered
     * world colour; vanilla's own TRANSLUCENT_PARTICLE does write depth, but it writes into the
     * separate particle target that the post-chain composites, not the main one. Writing depth here
     * instead stamps the glow quads into the scene depth, and the translucent terrain drawn
     * afterwards then fails its depth test against them -- which showed up as square holes punched
     * through water wherever a pylon particle was in front of it. Depth TEST stays on, so solid
     * terrain still occludes the glow.
     *
     * This was re-learned the hard way: ADDITIVE_SPRITE was briefly given depth write back, on the
     * theory that the cloud compositor needed something to sort against. It does not, and the result
     * was that the pylon glow occluded both water and clouds outright. Ordering is the submission
     * phase's job (TRANSLUCENT_CUSTOM_GEOMETRY, i.e. after the translucent chunk layer); a glow that
     * adds light to whatever is behind it must never claim depth. Do not set this to true again.
     */
    public static final RenderPipeline ADDITIVE_PARTICLE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/particle")
            .withFragmentShader("core/particle")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /**
     * The monument ritual's screen grade and core glow, in one pass. Its sixteen cores arrive as a live
     * UBO rather than as declared uniforms, because a PostChain bakes those when the chain compiles and
     * these change every frame; see {@code MonumentRitualEffects}.
     */
    public static final RenderPipeline MONUMENT_GRADE = RenderPipeline.builder(
                    net.minecraft.client.renderer.RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/monument"))
            // The String overloads assume the minecraft namespace, so ours must be an explicit
            // Identifier or it resolves as "minecraft:chromaticraft:post/monument".
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "post/monument"))
            .withBindGroupLayout(com.mojang.blaze3d.pipeline.BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("MonumentCores",
                            com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                    .build())
            .build();

    private static final Function<Identifier, RenderType> ADDITIVE_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_additive_sprite", RenderSetup.builder(ADDITIVE_SPRITE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload().createRenderSetup()));

    private ChromaRenderPipelines() {}

    public static RenderType additiveSprite(Identifier texture) {
        return ADDITIVE_TYPES.apply(texture);
    }

    public static void register(IEventBus bus) {
        bus.addListener(ChromaRenderPipelines::registerPipelines);
    }

    private static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE_SPRITE);
        event.registerPipeline(ADDITIVE_PARTICLE);
        event.registerPipeline(MONUMENT_GRADE);
    }
}

