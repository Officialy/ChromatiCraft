package reika.chromaticraft.mixin;

import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpecialModelWrapper.class)
public interface LocusItemModelAccessor {
    @Accessor("specialRenderer") SpecialModelRenderer<?> chromaticraft$locusRenderer();
}
