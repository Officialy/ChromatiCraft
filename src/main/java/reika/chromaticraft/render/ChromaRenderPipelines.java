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
 * Depth writes let Minecraft 26.2 sort later translucent targets around the resulting glow. */
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
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
            .build();

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
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
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
    }
}

