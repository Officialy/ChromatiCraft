package reika.chromaticraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.chromaticraft.items.tools.powered.ItemSpawnerBypass;

/** Preserve the original activation check before either spawner countdown or spawning runs. */
@Mixin(BaseSpawner.class)
public abstract class SpawnerActivationMixin {
    @Shadow private int requiredPlayerRange;
    @Inject(method="isNearPlayer", at=@At("RETURN"), cancellable=true)
    private void chromaticraft$spawnerBypass(Level world, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(ItemSpawnerBypass.checkSpawnerActivation(world,pos,requiredPlayerRange,callback.getReturnValue()));
    }
}
