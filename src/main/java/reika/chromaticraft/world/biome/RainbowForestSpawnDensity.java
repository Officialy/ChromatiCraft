package reika.chromaticraft.world.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;

import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import reika.chromaticraft.registry.ChromaBiomeModifiers;
import reika.chromaticraft.registry.ChromaOptions;

/**
 * V33a {@code BiomeRainbowForest.getSpawningChance}: {@code 0.1 * max(1, ANIMALSPAWN)}, the chance that
 * chunk generation keeps adding passive animals to a chunk. The option defaults to 6, so a Rainbow
 * Forest is far livelier than an ordinary forest (0.6 against vanilla's 0.1).
 *
 * <p>That is a live config option, while a 26.2 biome is datapack data, so the biome carries the
 * default-config value and this modifier replaces it with the configured one when the server loads its
 * biomes. The value is capped just below 1, the most the biome codec allows, which upstream's
 * {@code nextFloat() < chance} loop reached at 10 anyway.
 */
public record RainbowForestSpawnDensity(HolderSet<Biome> biomes) implements BiomeModifier {

	public static final MapCodec<RainbowForestSpawnDensity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryCodecs.holderSet(Registries.BIOME).fieldOf("biomes").forGetter(RainbowForestSpawnDensity::biomes)
	).apply(i, RainbowForestSpawnDensity::new));

	/** V33a {@code getSpawningChance} for a given option value. */
	public static float spawningChance(int option) {
		return Math.min(0.9999999F, 0.1F * Math.max(1, option));
	}

	@Override
	public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase == Phase.MODIFY && biomes.contains(biome))
			builder.getAttributes().set(EnvironmentAttributes.CREATURE_WORLD_GEN_SPAWN_PROBABILITY,
					spawningChance(ChromaOptions.ANIMALSPAWN.getValue()));
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return ChromaBiomeModifiers.RAINBOW_FOREST_SPAWN_DENSITY.get();
	}
}
