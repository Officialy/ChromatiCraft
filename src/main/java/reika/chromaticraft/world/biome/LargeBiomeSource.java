package reika.chromaticraft.world.biome;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Seed and registry binding supplied when TerraBlender initializes the Overworld biome source. */
public interface LargeBiomeSource {
    void chroma$initializeLargeBiomes(RegistryAccess registries, long seed, NoiseGeneratorSettings settings);
}
