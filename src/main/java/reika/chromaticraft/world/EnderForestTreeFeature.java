package reika.chromaticraft.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.Simplex3DGenerator;

/**
 * V33a {@code BiomeEnderForest}'s tree selector.
 *
 * <p>The biome does not carry a tree list. It carries a {@code WorldGenAbstractTree} that, on every
 * attempt, samples a {@link Simplex3DGenerator} at frequency 1/30 seeded from the world seed, then
 * recomputes every candidate's weight as {@code max(0, base + coefficient * noise)} and draws from the
 * result. Because the coefficients have opposite signs, the mix drifts across the biome: noise-high
 * regions favour the small ender oak (+20) and ordinary oak (+10), noise-low regions favour the large
 * ender oak (-5) and vanilla big oak (-2), and a "nothing" entry with a +6 coefficient thins the
 * canopy wherever the noise runs high.
 *
 * <p>That last entry is why this cannot be a vanilla weighted tree list: the empty outcome is a
 * competitor in the same draw, so tree density and tree species vary together with one noise field. A
 * static {@code WeightedList} would reproduce the average and lose the structure entirely, which is
 * the whole character of the biome.
 */
public final class EnderForestTreeFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a: {@code new Simplex3DGenerator(seed).setFrequency(1/30D)}. */
	private static final double FREQUENCY = 1 / 30D;

	/** A candidate and its V33a base weight / noise coefficient. A null variant is "no tree". */
	private record Candidate(EnderOakFeature.Variant variant, boolean vanillaBig, double base,
			double coefficient) {

		double weight(double noise) {
			return Math.max(0, base + coefficient * noise);
		}
	}

	// V33a treeTypes, in source order: vanilla oak, vanilla big oak, then the three ender oaks, then
	// the empty entry that competes alongside them.
	private static final List<Candidate> CANDIDATES = List.of(
			new Candidate(null, false, 25, 10),
			new Candidate(null, true, 5, -2),
			new Candidate(EnderOakFeature.Variant.SMALL, false, 50, 20),
			new Candidate(EnderOakFeature.Variant.LARGE, false, 10, -5),
			new Candidate(EnderOakFeature.Variant.NARROW, false, 6, -1),
			new Candidate(null, false, 0, 6));

	private NoiseGeneratorBase noise;
	private long noiseSeed;

	public EnderForestTreeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		// V33a derives the noise seed from the world seed with this exact shuffle, so the field is
		// stable for a world but unrelated to the ordinary worldgen random.
		long seed = (world.getSeed() << 17) + (world.getSeed() >> 43);
		if (noise == null || noiseSeed != seed) {
			noise = new Simplex3DGenerator(seed).setFrequency(FREQUENCY);
			noiseSeed = seed;
		}
		double value = noise.getValue(origin.getX(), origin.getY(), origin.getZ());
		Candidate chosen = this.draw(random, value);
		if (chosen == null)
			return false;
		if (chosen.variant() != null)
			return new EnderOakFeature(chosen.variant()).place(context);
		// V33a's other two entries are the biome's own worldGeneratorTrees and worldGeneratorBigTree,
		// i.e. plain vanilla oaks competing in the same draw. Their modern equivalents are configured
		// features rather than callable generators, so they are resolved from the registry and placed
		// here. Dropping them would quietly thin the forest and skew the mix toward ender oaks.
		ResourceKey<ConfiguredFeature<?, ?>> key = chosen.vanillaBig() ? TreeFeatures.FANCY_OAK : TreeFeatures.OAK;
		return world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key)
				.value().place(world, context.chunkGenerator(), random, origin);
	}

	/** V33a WeightedRandom over the recomputed dynamic weights. */
	private Candidate draw(RandomSource random, double noiseValue) {
		double total = 0;
		for (Candidate candidate : CANDIDATES)
			total += candidate.weight(noiseValue);
		if (total <= 0)
			return null;
		double roll = random.nextDouble() * total;
		for (Candidate candidate : CANDIDATES) {
			roll -= candidate.weight(noiseValue);
			if (roll <= 0)
				return candidate;
		}
		return CANDIDATES.get(CANDIDATES.size() - 1);
	}
}
