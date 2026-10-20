package reika.chromaticraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reika.chromaticraft.render.LocusGuiItems;

@Mixin(PictureInPictureRenderer.class)
public abstract class LocusGuiPictureInPictureMixin {
    @ModifyExpressionValue(method="blitTexture",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/RenderPipelines;GUI_TEXTURED_PREMULTIPLIED_ALPHA:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline chromaticraft$locusOversized(RenderPipeline original,@Local(argsOnly=true) PictureInPictureRenderState state) {
        return state instanceof OversizedItemRenderState item?LocusGuiItems.pipeline(original,item.guiItemRenderState().itemStackRenderState()):original;
    }
}
