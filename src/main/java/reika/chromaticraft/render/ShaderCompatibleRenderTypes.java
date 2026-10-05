package reika.chromaticraft.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.render.IrisCompatibility;

import java.util.function.Function;

/** Standard world vertex contracts and public Iris program assignments for glows and mist. */
public final class ShaderCompatibleRenderTypes {
    private ShaderCompatibleRenderTypes() {}

    private static RenderPipeline emissive(String name, CompareOp depth, boolean opaque) {
        return RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                .withLocation(id("pipeline/"+name))
                .withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                .withShaderDefine("ALPHA_CUTOUT", 0.5F/255F)
                .withColorTargetState(opaque ? ColorTargetState.DEFAULT : new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(depth, opaque)).withCull(false).build();
    }

    public static final RenderPipeline PYLON = emissive("shader_pylon", CompareOp.GREATER_THAN_OR_EQUAL, false);
    public static final RenderPipeline PYLON_XRAY = emissive("shader_pylon_xray", CompareOp.ALWAYS_PASS, false);
    public static final RenderPipeline ENERGY_BEAM = emissive("shader_energy_beam", CompareOp.GREATER_THAN_OR_EQUAL, true);
    public static final RenderPipeline MIST = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(id("pipeline/shader_cliffs_mist"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build();

    private static Function<Identifier, RenderType> emissiveTypes(String name, RenderPipeline pipeline) {
        return Util.memoize(texture -> RenderType.create(name, RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
    }
    private static final Function<Identifier, RenderType> PYLON_TYPES = emissiveTypes("chromaticraft_shader_pylon", PYLON);
    private static final Function<Identifier, RenderType> PYLON_XRAY_TYPES = emissiveTypes("chromaticraft_shader_pylon_xray", PYLON_XRAY);
    private static final Function<Identifier, RenderType> BEAM_TYPES = emissiveTypes("chromaticraft_shader_energy_beam", ENERGY_BEAM);
    private static final Function<Identifier, RenderType> MIST_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_shader_cliffs_mist", RenderSetup.builder(MIST)
                    .withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                    .useLightmap().sortOnUpload().createRenderSetup()));
    private static final Function<Identifier, RenderType> VANILLA_MIST_TYPES = Util.memoize(texture ->
            RenderType.create("chromaticraft_cliffs_mist", RenderSetup.builder(ChromaRenderPipelines.TRANSLUCENT_SPRITE)
                    .withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                    .sortOnUpload().createRenderSetup()));

    public static RenderType pylon(Identifier texture, boolean xray) { return (xray ? PYLON_XRAY_TYPES : PYLON_TYPES).apply(texture); }
    public static RenderType beam(Identifier texture) { return BEAM_TYPES.apply(texture); }
    public static RenderType mist(Identifier texture) {
        return (IrisCompatibility.shadersEnabled() ? MIST_TYPES : VANILLA_MIST_TYPES).apply(texture);
    }

    public static void register(IEventBus bus) { bus.addListener(ShaderCompatibleRenderTypes::registerPipelines); }
    private static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PYLON);
        event.registerPipeline(PYLON_XRAY);
        event.registerPipeline(ENERGY_BEAM);
        event.registerPipeline(MIST);
        IrisCompatibility.assign(PYLON, "EMISSIVE_ENTITIES");
        IrisCompatibility.assign(PYLON_XRAY, "EMISSIVE_ENTITIES");
        IrisCompatibility.assign(ENERGY_BEAM, "EMISSIVE_ENTITIES");
        IrisCompatibility.assign(MIST, "PARTICLES_TRANSLUCENT");
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name); }
}
