package reika.chromaticraft.mixin;

import net.minecraft.client.renderer.item.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.chromaticraft.render.item.DimensionCoreItemRenderer;

/** Wall-clock special-model animation must invalidate native cached GUI atlas entries. */
@Mixin(SpecialModelWrapper.class)
public abstract class LocusItemAnimationMixin {
    @Inject(method="update",at=@At("RETURN"))
    private void chromaticraft$locusAnimation(ItemStackRenderState output,ItemStack item,ItemModelResolver resolver,
            ItemDisplayContext context,ClientLevel level,ItemOwner owner,int seed,CallbackInfo callback) {
        if(((LocusItemModelAccessor)this).chromaticraft$locusRenderer() instanceof DimensionCoreItemRenderer)output.setAnimated();
    }
}
