package reika.chromaticraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import reika.chromaticraft.world.biome.ChromaBiomeWeather;

@Mixin(ServerLevel.class)
public abstract class EnderForestThunderMixin {
    @Redirect(method="tickThunder", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerLevel;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean chromaticraft$rainlessThunder(ServerLevel level, BlockPos pos) {
        return ChromaBiomeWeather.canSpawnLightning(level,pos);
    }
}
