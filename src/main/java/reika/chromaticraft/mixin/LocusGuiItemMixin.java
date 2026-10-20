package reika.chromaticraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reika.chromaticraft.render.LocusGuiItems;

@Mixin(GuiRenderer.class)
public abstract class LocusGuiItemMixin {
    @ModifyExpressionValue(method="submitBlitFromItemAtlas",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/RenderPipelines;GUI_TEXTURED_PREMULTIPLIED_ALPHA:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline chromaticraft$locusAtlas(RenderPipeline original,@Local(argsOnly=true) GuiItemRenderState state) {
        return LocusGuiItems.pipeline(original,state.itemStackRenderState());
    }
}
