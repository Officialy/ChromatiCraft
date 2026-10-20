package reika.chromaticraft.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.extras.shader.IrisCompat;

import java.util.function.Function;

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

    /** Untextured V33a ADDITIVEDARK geometry: RGB screen blending on composed scene color. */
    /** V33a prepareGeoDraw(false): untextured, unlit, opaque model geometry with depth writes. */
    public static final RenderPipeline SOURCE_EDGES = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,"pipeline/source_edges"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_color").withFragmentShader("core/position_color")
            .withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(true).build();
    public static final RenderType SOURCE_EDGES_TYPE=RenderType.create("chromaticraft_source_edges",
            RenderSetup.builder(SOURCE_EDGES).createRenderSetup());

    public static final RenderPipeline ADDITIVE_COLOR = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,"pipeline/additive_color"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_color").withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)).withCull(false).build();
    public static final RenderType ADDITIVE_COLOR_TYPE=RenderType.create("chromaticraft_additive_color",
            RenderSetup.builder(ADDITIVE_COLOR).createRenderSetup());
    public static final RenderPipeline ADDITIVE_LINES = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,"pipeline/additive_lines"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/rendertype_lines").withFragmentShader("core/rendertype_lines")
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0,DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH).withPrimitiveTopology(PrimitiveTopology.LINES)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)).withCull(false).build();
    public static final RenderType ADDITIVE_LINES_TYPE=RenderType.create("chromaticraft_additive_lines",
            RenderSetup.builder(ADDITIVE_LINES).createRenderSetup());

    /** The GUI-textured pipeline with V33a's ADDITIVEDARK blend, for HUD flares (the Pylon Compass turbo halo). */
    public static final RenderPipeline GUI_ADDITIVE_DARK = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/gui_additive_dark"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    public static final RenderPipeline ADDITIVE_SPRITE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_sprite"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /**
     * {@link #ADDITIVE_SPRITE} for the Proxima sky. Iris holds one program per pipeline, and the sky pass needs
     * the pack's sky program rather than the emissive one the in-world glows use.
     */
    public static final RenderPipeline ADDITIVE_SKY_SPRITE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_sky_sprite"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

	/** As {@link #ADDITIVE_SPRITE} with no depth test: V33a ADDITIVEDARK drawn with GL_DEPTH_TEST off (the probe's error cubes). */
	public static final RenderPipeline ADDITIVE_SPRITE_THROUGH_WALL = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_sprite_through_wall"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.withCull(false)
			.build();

	/** Dense centre of the original pylon flare, alpha derived from its RGB rather than the atlas tile. */
	public static final RenderPipeline PYLON_CORE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/pylon_core"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "core/pylon_core"))
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	/** As {@link #PYLON_CORE} with no depth test: V33a's pylon flare under Void Gaze's PYLONXRAY. */
	public static final RenderPipeline PYLON_CORE_THROUGH_WALL = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/pylon_core_through_wall"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "core/pylon_core"))
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.withCull(false)
			.build();

	/** Plain alpha-blended world sprites with no depth write: V33a's BlendMode.DEFAULT billboards. */
	public static final RenderPipeline TRANSLUCENT_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/translucent_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();
	/**
	 * V33a {@code BlendMode.MULTIPLY} (GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA) world sprites without depth write: the
	 * darkening X the Biome Fragment hangs over its shaft for a player who may not pass yet.
	 */
	public static final RenderPipeline MULTIPLY_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/multiply_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE_MINUS_SRC_ALPHA)))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	/**
	 * V33a {@code BlendMode.INVERTEDADD} (GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR): the in-world structure
	 * controller flare. The result is src(1-dst) + dst(1-src): black source leaves the frame unchanged, so the
	 * sprite's black field drops out as under ADDITIVEDARK, while bright texels glow on dark surroundings and
	 * invert bright ones.
	 */
	public static final RenderPipeline INVERTED_ADD_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/inverted_add_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.ONE_MINUS_SRC_COLOR)))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	public static final RenderPipeline TRANSLUCENT_SPRITE_THROUGH_WALL = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/translucent_sprite_through_wall"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader("core/position_tex_color")
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.withCull(false)
			.build();

	public static final RenderPipeline ADDITIVE_ALPHA_SPRITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_alpha_sprite"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
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
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
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
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
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
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            // 26.2 defaulted a pipeline with no colour target to this; 26.3 gives it none.
            .withColorTargetState(ColorTargetState.DEFAULT)
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
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
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

    /** V33a {@code EntityFX.setNoDepthTest()}: the same additive particle, drawn through terrain. */
    public static final RenderPipeline LEGACY_ADDITIVE_PARTICLE_NO_DEPTH = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "pipeline/legacy_additive_particle_no_depth"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/particle")
            .withFragmentShader(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "core/legacy_additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withColorTargetState(new ColorTargetState(
                    LEGACY_ADDITIVE_DARK_PARTICLE_TARGET))
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    /**
     * V33a's alpha-blended ("basic blend") particles. DragonAPI's ParticleEngine drew every layer with
     * {@code glDepthMask(false)} and an alpha clip of 1/255; vanilla's translucent particle pipeline writes depth
     * and discards below 0.1, so overlapping soft sprites (the Enhanced Ender Eye's spiral) cut hard-edged holes
     * in whatever lay behind them.
     */
    public static final RenderPipeline LEGACY_TRANSLUCENT_PARTICLE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "pipeline/legacy_translucent_particle"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withVertexShader("core/particle")
            .withFragmentShader(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "core/legacy_translucent_particle"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(PARTICLE_DEPTH_NO_WRITE)
            .withCull(false)
            .build();

    public static final RenderPipeline LEGACY_ADDITIVE_PARTICLE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                    ChromatiCraft.MODID, "pipeline/legacy_additive_particle"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
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
            .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder()
                    .withUniform("InSampler", com.mojang.renderpearl.api.pipeline.UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("MonumentCores",
                            com.mojang.renderpearl.api.pipeline.UniformType.UNIFORM_BUFFER)
                    .build())
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build();

	/** V33a DIMCORE and AURALOC global-no-GUI shaders, resolved from live locus-point emitters. */
	/** V33a UAZONE: the artefact-ring screen shader; its per-frame values arrive as a UBO. */
	public static final RenderPipeline UA_ZONE = RenderPipeline.builder(
				net.minecraft.client.renderer.RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/ua_zone"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "post/ua_zone"))
			.withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder()
					.withUniform("InSampler", com.mojang.renderpearl.api.pipeline.UniformType.COMBINED_IMAGE_SAMPLER)
					.withUniform("UaZone", com.mojang.renderpearl.api.pipeline.UniformType.UNIFORM_BUFFER)
					.build())
			.withColorTargetState(ColorTargetState.DEFAULT)
			.build();

	/** V33a ARTEALLOY$SHOCK and ARTEALLOY$GLOW in one pass; see {@code ArtealloyEffects}. */
	public static final RenderPipeline ARTEALLOY = RenderPipeline.builder(
				net.minecraft.client.renderer.RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/artealloy"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "post/artealloy"))
			.withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder()
					.withUniform("InSampler", com.mojang.renderpearl.api.pipeline.UniformType.COMBINED_IMAGE_SAMPLER)
					.withUniform("Artealloy", com.mojang.renderpearl.api.pipeline.UniformType.UNIFORM_BUFFER)
					.build())
			.withColorTargetState(ColorTargetState.DEFAULT)
			.build();

	public static final RenderPipeline LOCUS_POINTS = RenderPipeline.builder(
				net.minecraft.client.renderer.RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/locus_points"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"post/locus_points"))
			.withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder()
					.withUniform("InSampler", com.mojang.renderpearl.api.pipeline.UniformType.COMBINED_IMAGE_SAMPLER)
					.withUniform("LocusPoints",
							com.mojang.renderpearl.api.pipeline.UniformType.UNIFORM_BUFFER)
					.build())
			.withColorTargetState(ColorTargetState.DEFAULT)
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
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withVertexShader("core/rendertype_lines")
			.withFragmentShader("core/rendertype_lines")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build();
    public static final RenderPipeline TRANSLUCENT_COLOR_LINES = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"pipeline/translucent_color_lines"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withVertexShader("core/rendertype_lines")
			.withFragmentShader("core/rendertype_lines")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build();

	/** V33a sonar: additive coloured lines, through terrain, with no depth writes. */
    public static final RenderPipeline MOB_SONAR_LINES = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"pipeline/mob_sonar_lines"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withVertexShader("core/rendertype_lines")
			.withFragmentShader("core/rendertype_lines")
			.withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.build();

	/**
	 * V33a untextured ADDITIVEDARK geometry (DragonAPI {@code TruncatedCube}'s faces and corners): position-colour
	 * triangles, depth-tested without writing depth, unculled.
	 */
	public static final RenderPipeline ADDITIVE_COLOR_TRIANGLES = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/additive_color_triangles"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withColorTargetState(new ColorTargetState(ADDITIVE_DARK))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();
    public static final RenderPipeline TRANSLUCENT_COLOR_TRIANGLES = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pipeline/translucent_color_triangles"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build();

	private static final RenderType ADDITIVE_COLOR_TRIANGLE_TYPE = RenderType.create(
			"chromaticraft_additive_color_triangles",
			RenderSetup.builder(ADDITIVE_COLOR_TRIANGLES).sortOnUpload().createRenderSetup());

	private static final RenderType AURA_LOCUS_LINE_TYPE = RenderType.create(
			"chromaticraft_aura_locus_lines",
			RenderSetup.builder(AURA_LOCUS_LINES).createRenderSetup());

    private static final Function<Identifier, RenderType> ADDITIVE_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_additive_sprite", RenderSetup.builder(ADDITIVE_SPRITE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> TRANSLUCENT_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_translucent_sprite", RenderSetup.builder(TRANSLUCENT_SPRITE)
					.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> MULTIPLY_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_multiply_sprite", RenderSetup.builder(MULTIPLY_SPRITE)
					.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> INVERTED_ADD_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_inverted_add_sprite", RenderSetup.builder(INVERTED_ADD_SPRITE)
					.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> PYLON_CORE_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_pylon_core", RenderSetup.builder(PYLON_CORE)
					.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
    private static final Function<Identifier, RenderType> ENERGY_BEAM_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_energy_beam", RenderSetup.builder(ENERGY_BEAM)
                    .withTexture("Sampler0", texture).createRenderSetup()));
	private static final Function<Identifier, RenderType> ADDITIVE_ALPHA_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_additive_alpha_sprite",
					RenderSetup.builder(ADDITIVE_ALPHA_SPRITE)
							.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> ADDITIVE_THROUGH_WALL_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_additive_sprite_through_wall",
					RenderSetup.builder(ADDITIVE_SPRITE_THROUGH_WALL)
							.withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
	private static final Function<Identifier, RenderType> PYLON_CORE_THROUGH_WALL_TYPES = Util.memoize(texture ->
			RenderType.create("chromaticraft_pylon_core_through_wall", RenderSetup.builder(PYLON_CORE_THROUGH_WALL)
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

    private static final Function<Identifier,RenderType> TRANSLUCENT_THROUGH_WALL_TYPES=Util.memoize(texture ->
        RenderType.create("chromaticraft_translucent_through_wall",RenderSetup.builder(TRANSLUCENT_SPRITE_THROUGH_WALL)
            .withTexture("Sampler0",texture).createRenderSetup()));
    public static RenderType translucentSpriteThroughWall(Identifier texture) { return TRANSLUCENT_THROUGH_WALL_TYPES.apply(texture); }
    private static final RenderType ALPHA_TRIANGLES=RenderType.create("chromaticraft_alpha_triangles",RenderSetup.builder(TRANSLUCENT_COLOR_TRIANGLES).createRenderSetup());
    private static final RenderType ALPHA_LINES=RenderType.create("chromaticraft_alpha_lines",RenderSetup.builder(TRANSLUCENT_COLOR_LINES).createRenderSetup());
    public static RenderType translucentColorTriangles() { return ALPHA_TRIANGLES; }
    public static RenderType translucentColorLines() { return ALPHA_LINES; }
    private static final RenderType SONAR_LINES = RenderType.create("chromaticraft_mob_sonar_lines", RenderSetup.builder(MOB_SONAR_LINES).createRenderSetup());
    public static RenderType mobSonarLines() { return SONAR_LINES; }
    public static RenderType additiveSprite(Identifier texture) {
        return ADDITIVE_TYPES.apply(texture);
    }

	/** V33a BlendMode.DEFAULT billboards without depth write (TRANSLUCENT_SPRITE), as a render type. */
	public static RenderType translucentSprite(Identifier texture) {
		return TRANSLUCENT_TYPES.apply(texture);
	}

	/** V33a BlendMode.MULTIPLY sprites without depth write (MULTIPLY_SPRITE), as a render type. */
	public static RenderType multiplySprite(Identifier texture) {
		return MULTIPLY_TYPES.apply(texture);
	}

	/** V33a BlendMode.INVERTEDADD sprites without depth write (INVERTED_ADD_SPRITE), as a render type. */
	public static RenderType invertedAddSprite(Identifier texture) {
		return INVERTED_ADD_TYPES.apply(texture);
	}

	public static RenderType pylonCore(Identifier texture) {
		return PYLON_CORE_TYPES.apply(texture);
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

	/** {@link #additiveSprite} without the depth test. */
	public static RenderType additiveSpriteThroughWall(Identifier texture) {
		return ADDITIVE_THROUGH_WALL_TYPES.apply(texture);
	}

	/** {@link #pylonCore} without the depth test. */
	public static RenderType pylonCoreThroughWall(Identifier texture) {
		return PYLON_CORE_THROUGH_WALL_TYPES.apply(texture);
	}

	public static RenderType legacyAdditiveSpriteThroughWall(Identifier texture) {
		return LEGACY_ADDITIVE_THROUGH_WALL_TYPES.apply(texture);
	}

	public static RenderType additiveColorTriangles() {
		return ADDITIVE_COLOR_TRIANGLE_TYPE;
	}

	public static RenderType auraLocusLines() {
		return AURA_LOCUS_LINE_TYPE;
	}

    public static void register(IEventBus bus) {
        bus.addListener(ChromaRenderPipelines::registerPipelines);
    }

    private static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE_SPRITE);
        event.registerPipeline(SOURCE_EDGES);
        IrisCompat.assign(SOURCE_EDGES,"ENTITIES");
        IrisCompat.assignShadow(SOURCE_EDGES,"SHADOW_ENTITIES");
        event.registerPipeline(ADDITIVE_COLOR);
        event.registerPipeline(ADDITIVE_LINES);
        IrisCompat.assignWithoutShadow(ADDITIVE_COLOR,"BASIC");
        IrisCompat.assignWithoutShadow(ADDITIVE_LINES,"LINES");
        event.registerPipeline(ADDITIVE_COLOR_TRIANGLES);
        event.registerPipeline(TRANSLUCENT_COLOR_TRIANGLES);
        event.registerPipeline(TRANSLUCENT_COLOR_LINES);
        event.registerPipeline(MOB_SONAR_LINES);
        event.registerPipeline(ADDITIVE_SPRITE_THROUGH_WALL);
        event.registerPipeline(GUI_ADDITIVE_DARK);
        event.registerPipeline(TRANSLUCENT_SPRITE);
        event.registerPipeline(TRANSLUCENT_SPRITE_THROUGH_WALL);
        event.registerPipeline(MULTIPLY_SPRITE);
        event.registerPipeline(INVERTED_ADD_SPRITE);
		event.registerPipeline(PYLON_CORE);
		event.registerPipeline(PYLON_CORE_THROUGH_WALL);
		event.registerPipeline(ADDITIVE_ALPHA_SPRITE);
		event.registerPipeline(LEGACY_ADDITIVE_SPRITE);
		event.registerPipeline(LEGACY_ADDITIVE_SPRITE_THROUGH_WALL);
        event.registerPipeline(ENERGY_BEAM);
        event.registerPipeline(ADDITIVE_PARTICLE);
        event.registerPipeline(LEGACY_ADDITIVE_PARTICLE);
        event.registerPipeline(LEGACY_TRANSLUCENT_PARTICLE);
        event.registerPipeline(LEGACY_ADDITIVE_PARTICLE_NO_DEPTH);
        event.registerPipeline(MONUMENT_GRADE);
		event.registerPipeline(LOCUS_POINTS);
		event.registerPipeline(UA_ZONE);
		event.registerPipeline(ARTEALLOY);
		event.registerPipeline(AURA_LOCUS_LINES);
        event.registerPipeline(ADDITIVE_SKY_SPRITE);
        assignIrisPrograms();
    }

    /**
     * Every pipeline drawn into the world gets an Iris program, picked so Iris finds a variant built for its vertex
     * format. Iris also applies a pack's blend.<program> override, so additive glows (about half their sprites use
     * black as transparency) go to EMISSIVE_ENTITIES: packs that override gbuffers_spidereyes keep it additive (Bliss
     * ONE ONE, SoftVoxels SRC_ALPHA ONE). Alpha-textured ones go to the textured, particle, basic or line program.
     * Glows, lines and overlays stay out of the shadow map. The GUI pipeline and the full-screen passes are never
     * drawn while Iris renders the world, so Iris does not override them.
     */
    private static void assignIrisPrograms() {
        for (RenderPipeline glow : new RenderPipeline[] {ADDITIVE_SPRITE, ADDITIVE_SPRITE_THROUGH_WALL, ADDITIVE_ALPHA_SPRITE, INVERTED_ADD_SPRITE,
                LEGACY_ADDITIVE_SPRITE, LEGACY_ADDITIVE_SPRITE_THROUGH_WALL, PYLON_CORE, PYLON_CORE_THROUGH_WALL, ENERGY_BEAM})
            IrisCompat.assignWithoutShadow(glow, "EMISSIVE_ENTITIES");
        IrisCompat.assignWithoutShadow(TRANSLUCENT_SPRITE, "TEXTURED");
        IrisCompat.assignWithoutShadow(TRANSLUCENT_SPRITE_THROUGH_WALL, "TEXTURED");
        // A darkening layer, not a glow: the emissive program's pack blend would turn it additive.
        IrisCompat.assignWithoutShadow(MULTIPLY_SPRITE, "TEXTURED");
        IrisCompat.assignWithoutShadow(ADDITIVE_COLOR_TRIANGLES, "BASIC");
        IrisCompat.assignWithoutShadow(TRANSLUCENT_COLOR_TRIANGLES, "BASIC");
        IrisCompat.assignWithoutShadow(AURA_LOCUS_LINES, "LINES");
        IrisCompat.assignWithoutShadow(TRANSLUCENT_COLOR_LINES, "LINES");
        IrisCompat.assignWithoutShadow(MOB_SONAR_LINES, "LINES");
        // Particles are drawn by the particle engine, which the shadow pass does not run. The additive ones share
        // vanilla's PARTICLE layout, whose first three attributes are exactly the emissive program's.
        IrisCompat.assign(ADDITIVE_PARTICLE, "EMISSIVE_ENTITIES");
        IrisCompat.assign(LEGACY_ADDITIVE_PARTICLE, "EMISSIVE_ENTITIES");
        IrisCompat.assign(LEGACY_ADDITIVE_PARTICLE_NO_DEPTH, "EMISSIVE_ENTITIES");
        IrisCompat.assign(LEGACY_TRANSLUCENT_PARTICLE, "PARTICLES_TRANSLUCENT");
        IrisCompat.assign(ADDITIVE_SKY_SPRITE, "SKY_TEXTURED");
        // Real models (focus crystals, rifts) keep casting shadows.
    }
}
