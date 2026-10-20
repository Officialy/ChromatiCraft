package reika.chromaticraft.render;

import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.resources.Identifier;
import reika.chromaticraft.mixin.LocusItemModelAccessor;
import reika.chromaticraft.render.item.DimensionCoreItemRenderer;

/** A source-additive item atlas stores emitted RGB; its GUI blit must add that RGB without alpha darkening. */
public final class LocusGuiItems {
    private LocusGuiItems() {}
    public static final RenderPipeline ADDITIVE_BLIT=RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("chromaticraft","locus/gui_additive_blit"))
            .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE,BlendFactor.ZERO,BlendFactor.ONE))).build();
    public static RenderPipeline pipeline(RenderPipeline original,TrackingItemStackRenderState state) {
        if(state.getModelIdentity() instanceof Iterable<?> elements)for(var element:elements) {
            if(element instanceof SpecialModelWrapper<?> wrapper && ((LocusItemModelAccessor)wrapper).chromaticraft$locusRenderer() instanceof DimensionCoreItemRenderer)
                return ADDITIVE_BLIT;
        }
        return original;
    }
}
