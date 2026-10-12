package reika.chromaticraft.world.biome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import reika.chromaticraft.registry.ChromaOptions;

/** World-owned placement shared by global lookups and TerraBlender's per-chunk biome-source clones. */
public final class LargeBiomePlacement {
    private final LargeBiomeLayout layout;
    private final Holder<Biome> cliffs, shores, rainbow, stream;
    private final Climate.Sampler surfaceSampler;
    private final int seaQuart;
    private final Map<Long, Optional<LargeBiomeLayout.Mask>> masks = new LinkedHashMap<>(256, .75F, true);

    public LargeBiomePlacement(RegistryAccess registries, long seed, NoiseGeneratorSettings settings) {
        layout = new LargeBiomeLayout(seed, ChromaOptions.getGlowingCliffsWeight(), ChromaOptions.getRainbowForestWeight());
        var biomes = registries.lookupOrThrow(Registries.BIOME);
        cliffs = biomes.getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS);
        shores = biomes.getOrThrow(ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        rainbow = biomes.getOrThrow(ChromaBiomes.RAINBOW_FOREST);
        stream = biomes.getOrThrow(ChromaBiomes.RAINBOW_STREAM);
        // Region admission reads beyond any single chunk. A private unbounded sampler avoids the
        // caches/buffers in the chunk generator's supplied sampler and is safe across chunk workers.
        surfaceSampler = RandomState.create(registries.lookupOrThrow(Registries.NOISE), seed, settings)
                .createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        seaQuart = QuartPos.fromBlock(settings.seaLevel());
    }

    public BiomeResolver wrap(BiomeResolver original, Climate.Sampler sampler,
            Climate.ParameterList<Holder<Biome>> parameters) {
        // Non-positional findValue preserves the world's datapack climate table. Never sample the wrapped resolver
        // to validate a region: the chunk resolver's buffered climate covers only that one chunk.
        BiomeResolver vanilla = (x, y, z) -> parameters.findValue(sampler.sample(x, y, z));
        return (x, y, z) -> {
            Holder<Biome> biome = original.getNoiseBiome(x, y, z);
            if (isLargeBiome(biome))
                biome = vanilla.getNoiseBiome(x, y, z);
            if (isCave(biome))
                return biome;
            int cellX = Math.floorDiv(x, LargeBiomeLayout.CELL_QUARTS);
            int cellZ = Math.floorDiv(z, LargeBiomeLayout.CELL_QUARTS);
            var candidate = layout.candidate(cellX, cellZ);
            if (candidate == null || !candidate.contains(x, z))
                return biome;
            var mask = mask(cellX, cellZ, candidate, parameters);
            int surface = mask == null ? 0 : mask.surfaceAt(x, z);
            if (surface == 0)
                return biome;
            return candidate.kind() == LargeBiomeLayout.Kind.CLIFFS
                    ? (surface == 2 ? shores : cliffs) : (surface == 2 ? stream : rainbow);
        };
    }

    private synchronized LargeBiomeLayout.Mask mask(int cellX, int cellZ, LargeBiomeLayout.Region candidate,
            Climate.ParameterList<Holder<Biome>> parameters) {
        long key = ((long)cellX << 32) ^ (cellZ & 0xffffffffL);
        var result = masks.get(key);
        if (result == null) {
            result = Optional.ofNullable(candidate.fit((x, z) -> {
                var target = surfaceSampler.sample(x, seaQuart, z);
                // A surface region follows the two-dimensional land/climate footprint. Depth zero
                // excludes underground cave entries from coastal admission, while the actual
                // three-dimensional resolver still preserves those cave biomes below the region.
                return surface(parameters.findValue(new Climate.TargetPoint(target.temperature(), target.humidity(),
                        target.continentalness(), target.erosion(), 0, target.weirdness())), candidate.kind());
            }));
            masks.put(key, result);
            if (masks.size() > 256)
                masks.remove(masks.keySet().iterator().next());
        }
        return result.orElse(null);
    }

    private static int surface(Holder<Biome> biome, LargeBiomeLayout.Kind kind) {
        var key = biome.unwrapKey().orElse(null);
        if (LuminousCliffsRegion.EDGE_BIOMES.contains(key)) {
            // Rainbow Stream is a river child; keep ordinary beaches on the forest's coastline.
            if (kind == LargeBiomeLayout.Kind.RAINBOW && !biome.is(Biomes.RIVER) && !biome.is(Biomes.FROZEN_RIVER))
                return 0;
            return 2;
        }
        return LuminousCliffsRegion.LAND_BIOMES.contains(key) ? 1 : 0;
    }

    private static boolean isCave(Holder<Biome> biome) {
        return biome.is(Biomes.LUSH_CAVES) || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.DEEP_DARK);
    }

    private static boolean isLargeBiome(Holder<Biome> biome) {
        return biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES)
                || biome.is(ChromaBiomes.RAINBOW_FOREST) || biome.is(ChromaBiomes.RAINBOW_STREAM);
    }
}
