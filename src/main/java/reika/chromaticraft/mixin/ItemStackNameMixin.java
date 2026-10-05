package reika.chromaticraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.chromaticraft.auxiliary.render.ChromaText;
import reika.chromaticraft.magic.progression.ChromaNameKnowledge;

/** Client-only: tooltips, held-item names and recipe-viewer names share the same progression gate. */
@Mixin(ItemStack.class)
public abstract class ItemStackNameMixin {
    @Inject(method = {"getItemName", "getHoverName"}, at = @At("RETURN"), cancellable = true)
    private void chromaticraft$discoveryName(CallbackInfoReturnable<Component> callback) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.isSameThread()) return;
        ItemStack stack = (ItemStack)(Object)this;
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getNamespace().equals("chromaticraft")) return;
        var disguise = ChromaNameKnowledge.disguise(stack, mc.player);
        if (disguise != null) callback.setReturnValue(disguise.getName());
        else if (ChromaNameKnowledge.hidden(stack, mc.player))
            callback.setReturnValue(ChromaText.scramble(callback.getReturnValue()));
    }
}
