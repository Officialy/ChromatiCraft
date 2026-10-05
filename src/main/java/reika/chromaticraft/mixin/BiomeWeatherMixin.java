package reika.chromaticraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.chromaticraft.world.biome.ChromaBiomeWeather;

@Mixin(Biome.class)
public abstract class BiomeWeatherMixin {
    @Inject(method="getHeightAdjustedTemperature", at=@At("RETURN"), cancellable=true)
    private void chromaticraft$heightCooling(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> callback) {
        if (ChromaBiomeWeather.hasRainbowCooling((Biome)(Object)this))
            callback.setReturnValue(ChromaBiomeWeather.rainbowTemperature(callback.getReturnValue(),pos.getY()));
    }
}
