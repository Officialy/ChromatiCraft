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

/** Additive, full-bright pipelines matching V33a's direct-framebuffer ADDITIVEDARK passes. */
public final class ChromaRenderPipelines {

    /** V33a {@code BlendMode.ADDITIVEDARK}: GL_ONE, GL_ONE_MINUS_SRC_COLOR. */
    private static final BlendFunction ADDITIVE_DARK =
            new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR);

    /**
     * The Fabulous/transparency particle target is later composited by alpha. Preserve V33a's
     * ADDITIVEDARK colour equation while accumulating a conventional premultiplied alpha channel;
     * using the colour factors for alpha as well leaves that target with unusable coverage.
     */
    private static final BlendFunction ADDITIVE_DARK_PARTICLE_TARGET = new BlendFunction(
            // Unlike the old immediate framebuffer, the 26.2 particle target is composited later.
            // Multiplying the source colour by its coverage prevents transparent-but-coloured edge
            // texels from revealing the rectangular particle quad around flares and pylon stars.
            BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_COLOR,
            BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);

    /**
     * V33a's atlas glows predate alpha-backed sprites: black RGB was transparency and the fixed
     * function ADDITIVEDARK pass made it disappear. The legacy particle shader derives coverage
     * from that RGB intensity, so the colour factors can remain exactly ONE/ONE_MINUS_SRC_COLOR
     * while the modern off-screen particle target receives usable alpha coverage.
     */
    private static final BlendFunction LEGACY_ADDITIVE_DARK_PARTICLE_TARGET = new BlendFunction(
            BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR,
            BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);

    /**
     * Modern equivalent of the {@code glDepthMask(false)} surrounding every V33a particle layer.
     * The comparison remains enabled, so solid terrain still occludes particles; only writes back
     * into the depth attachment are disabled. This is essential for densely overlapping effects
     * such as {@code TileEntityDimensionCore.spawnConnectFX}, whose quarter-block blur quads must
     * blend with one another instead of depth-rejecting later quads in the same beam.
     */
    private static final DepthStencilState PARTICLE_DEPTH_NO_WRITE =
            new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);
	/** Alpha-covered additive pass used by V33a's ADDITIVE2 locus/item glows. */
	private static final BlendFunction ADDITIVE_ALPHA = new BlendFunction(
			BlendFactor.SRC_ALPHA, BlendFactor.ONE,
			BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);

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

	public static final RenderPipeline ADDITIVE_ALPHA_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_alpha_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(ADDITIVE_ALPHA))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	/**
	 * Legacy RGB-only sprite sheet on a modern intermediate target. Black was transparency under
	 * V33a's fixed-function additive blend; the fragment shader derives real alpha from luminance so
	 * inventories and GUI item targets do not retain an opaque black rectangle.
	 */
	public static final RenderPipeline LEGACY_ADDITIVE_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"pipeline/legacy_additive_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"core/legacy_additive_sprite"))
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(LEGACY_ADDITIVE_DARK_PARTICLE_TARGET))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	/** V33a RenderLocusPoint disables depth testing while the monument ritual owns the cores. */
	public static final RenderPipeline LEGACY_ADDITIVE_SPRITE_THROUGH_WALL = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"pipeline/legacy_additive_sprite_through_wall"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"core/legacy_additive_sprite"))
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(LEGACY_ADDITIVE_DARK_PARTICLE_TARGET))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.withCull(false)
			.build();

    /** V33a's crystal-network beam pass: textured, full colour, no blending, depth-writing. */
    public static final RenderPipeline ENERGY_BEAM = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/energy_beam"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .build();

    /*
     * SingleQuadParticle's translucent flag routes this pipeline into Minecraft's dedicated particle
     * target so its colour is combined with water/cloud targets by the transparency post-chain.
     * PARTICLE_DEPTH_NO_WRITE deliberately retains the scene-depth test without stamping each glow
     * quad into depth, matching V33a's EffectRenderer/ThrottleableEffectRenderer state exactly.
     */
    public static final RenderPipeline ADDITIVE_PARTICLE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/particle")
            .withFragmentShader("core/particle")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK_PARTICLE_TARGET))
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(PARTICLE_DEPTH_NO_WRITE)
            .withCull(false)
            .build();

    public static final RenderPipeline LEGACY_ADDITIVE_PARTICLE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "pipeline/legacy_additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/particle")
            .withFragmentShader(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "core/legacy_additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withColorTargetState(new ColorTargetState(
                    LEGACY_ADDITIVE_DARK_PARTICLE_TARGET))
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(PARTICLE_DEPTH_NO_WRITE)
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

	/** V33a DIMCORE and AURALOC global-no-GUI shaders, resolved from live locus-point emitters. */
	public static final RenderPipeline LOCUS_POINTS = RenderPipeline.builder(
				net.minecraft.client.renderer.RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/locus_points"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"post/locus_points"))
			.withBindGroupLayout(com.mojang.blaze3d.pipeline.BindGroupLayout.builder()
					.withSampler("InSampler")
					.withUniform("LocusPoints",
							com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
					.build())
			.build();

	/**
	 * V33a Aura Locus knot pass. The old renderer retained depth testing but wrapped all three line
	 * submissions in {@code glDepthMask(false)}. Vanilla 26.2 lines write depth, causing the first
	 * strand to reject the two faint overdraw passes and making the knot look much thinner/smaller.
	 */
	public static final RenderPipeline AURA_LOCUS_LINES = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"pipeline/aura_locus_lines"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withVertexShader("core/rendertype_lines")
			.withFragmentShader("core/rendertype_lines")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build();

	private static final RenderType AURA_LOCUS_LINE_TYPE = RenderType.create(
			"chromaticraft_aura_locus_lines",
			RenderSetup.builder(AURA_LOCUS_LINES).createRenderSetup());

    private static final Function<Identifier, RenderType> ADDITIVE_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_additive_sprite", RenderSetup.builder(ADDITIVE_SPRITE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload().createRenderSetup()));
    private static final Function<Identifier, RenderType> ENERGY_BEAM_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_energy_beam", RenderSetup.builder(ENERGY_BEAM)
                    .withTexture("Sampler0", texture).createRenderSetup()));
	private static final Function<Identifier, RenderType> ADDITIVE_ALPHA_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_additive_alpha_sprite",
					RenderSetup.builder(ADDITIVE_ALPHA_SPRITE)
							.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> LEGACY_ADDITIVE_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_legacy_additive_sprite",
					RenderSetup.builder(LEGACY_ADDITIVE_SPRITE)
							.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> LEGACY_ADDITIVE_THROUGH_WALL_TYPES =
			Util.memoize(texture -> RenderType.create(
					"chromaticraft_legacy_additive_sprite_through_wall",
					RenderSetup.builder(LEGACY_ADDITIVE_SPRITE_THROUGH_WALL)
							.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    private ChromaRenderPipelines() {}

    public static RenderType additiveSprite(Identifier texture) {
        return ADDITIVE_TYPES.apply(texture);
    }

    public static RenderType energyBeam(Identifier texture) {
        return ENERGY_BEAM_TYPES.apply(texture);
    }

	public static RenderType additiveAlphaSprite(Identifier texture) {
		return ADDITIVE_ALPHA_TYPES.apply(texture);
	}

	public static RenderType legacyAdditiveSprite(Identifier texture) {
		return LEGACY_ADDITIVE_TYPES.apply(texture);
	}

	public static RenderType legacyAdditiveSpriteThroughWall(Identifier texture) {
		return LEGACY_ADDITIVE_THROUGH_WALL_TYPES.apply(texture);
	}

	public static RenderType auraLocusLines() {
		return AURA_LOCUS_LINE_TYPE;
	}

    public static void register(IEventBus bus) {
        bus.addListener(ChromaRenderPipelines::registerPipelines);
    }

    private static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE_SPRITE);
		event.registerPipeline(ADDITIVE_ALPHA_SPRITE);
		event.registerPipeline(LEGACY_ADDITIVE_SPRITE);
		event.registerPipeline(LEGACY_ADDITIVE_SPRITE_THROUGH_WALL);
        event.registerPipeline(ENERGY_BEAM);
        event.registerPipeline(ADDITIVE_PARTICLE);
        event.registerPipeline(LEGACY_ADDITIVE_PARTICLE);
        event.registerPipeline(MONUMENT_GRADE);
		event.registerPipeline(LOCUS_POINTS);
		event.registerPipeline(AURA_LOCUS_LINES);
    }
}
