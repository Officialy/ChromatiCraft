package reika.chromaticraft.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.chromaticraft.world.biome.LargeBiomeSource;
import terrablender.util.LevelUtils;
import terrablender.api.RegionType;

@Mixin(value = LevelUtils.class, remap = false)
public abstract class LargeBiomeInitializationMixin {
    @Inject(method = "initializeBiomes", at = @At("TAIL"))
    private static void chroma$bindOverworld(RegistryAccess registries, Holder<DimensionType> dimension,
            ResourceKey<LevelStem> stem, ChunkGenerator generator, long seed, CallbackInfo callback) {
        if (LevelUtils.getRegionTypeForDimension(dimension) == RegionType.OVERWORLD
                && generator instanceof NoiseBasedChunkGenerator noise
                && generator.getBiomeSource() instanceof MultiNoiseBiomeSource source)
            ((LargeBiomeSource)source).chroma$initializeLargeBiomes(registries, seed, noise.generatorSettings().value());
    }
}
