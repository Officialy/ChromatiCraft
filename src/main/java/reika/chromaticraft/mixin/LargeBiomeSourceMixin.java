package reika.chromaticraft.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.chromaticraft.world.biome.LargeBiomePlacement;
import reika.chromaticraft.world.biome.LargeBiomeSource;
import terrablender.worldgen.IExtendedParameterList;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class LargeBiomeSourceMixin implements LargeBiomeSource {
    @Shadow public abstract Climate.ParameterList<Holder<Biome>> parameters();
    @Unique private LargeBiomePlacement chroma$largeBiomes;

    @Override
    public void chroma$initializeLargeBiomes(RegistryAccess registries, long seed, NoiseGeneratorSettings settings) {
        // TerraBlender clones this source for parallel chunks; the immutable seed binding and bounded,
        // synchronized region cache are deliberately shared by those shallow clones.
        if (chroma$largeBiomes == null && ((IExtendedParameterList<?>)(Object)parameters()).isInitialized())
            chroma$largeBiomes = new LargeBiomePlacement(registries, seed, settings);
    }

    @Inject(method = "createResolver", at = @At("RETURN"), cancellable = true)
    private void chroma$globalRegions(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> callback) {
        chroma$wrap(sampler, callback);
    }

    @Inject(method = "createResolverForChunk", at = @At("RETURN"), cancellable = true)
    private void chroma$chunkRegions(Climate.Sampler sampler, int x, int y, int z, int width, int height, int depth,
            CallbackInfoReturnable<BiomeResolver> callback) {
        chroma$wrap(sampler, callback);
    }

    @Unique
    private void chroma$wrap(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> callback) {
        if (chroma$largeBiomes != null)
            callback.setReturnValue(chroma$largeBiomes.wrap(callback.getReturnValue(), sampler,
                    parameters()));
    }
}
