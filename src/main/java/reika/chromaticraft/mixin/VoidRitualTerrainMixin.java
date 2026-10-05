package reika.chromaticraft.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.chromaticraft.modinterface.voidritual.VoidRitualShaders;

@Mixin(targets={"net.minecraft.client.renderer.chunk.ChunkSectionsToRender$DrawSeparate","net.minecraft.client.renderer.chunk.ChunkSectionsToRender$DrawIndirect"})
public abstract class VoidRitualTerrainMixin {
    @ModifyArg(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;"),index=0)
    private RenderPipeline chroma$ritualTerrain(RenderPipeline pipeline) { return net.neoforged.fml.ModList.get().isLoaded("voidmonster")?VoidRitualShaders.terrainPipeline(pipeline):pipeline; }
    @Inject(method="render",at=@At("HEAD"))
    private void chroma$bindRitual(ChunkSectionLayer layer,RenderPass pass,GpuBuffer indices,IndexType indexType,RenderPipeline override,RenderPipeline multiOverride,CallbackInfo callback) {
        if (net.neoforged.fml.ModList.get().isLoaded("voidmonster")) VoidRitualShaders.bindTerrain(pass);
    }
}
