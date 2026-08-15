package reika.chromaticraft.world.dimension.biome;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import reika.chromaticraft.world.dimension.BiomeDistributor;

/**
 * The {@link BiomeSource} that reads Proxima's painted biome map.
 *
 * <p>Proxima does not use climate parameters at all. V33a answers every biome query from
 * {@code BiomeDistributor.getBiome(x, z)}, which consults the structure and monument regions, then
 * the central region, then a global painted map that tiles over the world — so this source ignores
 * the {@link Climate.Sampler} entirely and simply converts quart coordinates back to blocks.
 *
 * <p>The map is built once by {@link reika.chromaticraft.world.dimension.ProximaGenerators}. Until
 * that finishes, this answers with the Luminescent Sanctuary — which is what the world origin resolves
 * to anyway, and is unreachable in practice because the Portal Rift refuses to carry anyone while the
 * generator gate is closed. It is a defined answer for a race rather than a stand-in for the map.
 */
public class ProximaBiomeSource extends BiomeSource {

	public static final MapCodec<ProximaBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(RegistryOps.retrieveGetter(Registries.BIOME))
					.apply(i, i.stable(ProximaBiomeSource::new)));

	private final HolderGetter<Biome> biomes;

	public ProximaBiomeSource(HolderGetter<Biome> biomes) {
		this.biomes = biomes;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Stream.concat(
						Stream.of(ProximaBiomes.biomeList).map(ProximaBiomeType.class::cast),
						Stream.of(ProximaSubBiomes.biomeList).map(ProximaBiomeType.class::cast))
				.map(type -> biomes.getOrThrow(type.biomeKey()));
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		ProximaBiomeType type = BiomeDistributor.getBiome(
				QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
		return biomes.getOrThrow((type == null ? ProximaBiomes.CENTER : type).biomeKey());
	}
}
