package reika.chromaticraft.world.dimension.biome;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import net.minecraft.world.level.LevelReader;

import reika.chromaticraft.world.dimension.BiomeDistributor;
import reika.chromaticraft.world.dimension.ProximaGenerators;

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

	/**
	 * Answers {@code /locate biome} for Proxima, which vanilla's own search cannot.
	 *
	 * <p>Two things defeat it. The command asks for a radius of 6400 sampled every 32 blocks, and
	 * Proxima's Sanctuary alone can reach fifteen thousand blocks — so every other biome begins outside
	 * the search before it starts. And the Monument and Structure Fields are small islands painted
	 * around placements that could be anywhere on a ring of radius 5000 +/- 3000 about an offset origin,
	 * so even a wider spiral would be luck.
	 *
	 * <p>Neither needs searching. The layout already holds the monument's position and every structure's
	 * entry, so those two are answered exactly and instantly. Everything else falls back to vanilla's
	 * spiral, but with the radius widened to clear the central region — sampling is an array lookup
	 * here, not a noise evaluation, so a wide sweep is cheap in a way it is not for the Overworld.
	 *
	 * <p>With no layout yet, this defers to vanilla entirely rather than inventing an answer: the map
	 * genuinely is not decided, and a confident wrong coordinate would be worse than none.
	 */
	@Override
	public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(BlockPos origin, int searchRadius,
			int sampleResolutionHorizontal, int sampleResolutionVertical, Predicate<Holder<Biome>> allowed,
			Climate.Sampler sampler, LevelReader level) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return super.findClosestBiome3d(origin, searchRadius, sampleResolutionHorizontal,
					sampleResolutionVertical, allowed, sampler, level);

		Holder<Biome> monument = biomes.getOrThrow(ProximaBiomes.MONUMENT.biomeKey());
		if (allowed.test(monument)) {
			BlockPos at = layout.structures().getMonumentPosition();
			return Pair.of(new BlockPos(at.getX(), origin.getY(), at.getZ()), monument);
		}

		Holder<Biome> structure = biomes.getOrThrow(ProximaBiomes.STRUCTURE.biomeKey());
		if (allowed.test(structure)) {
			BlockPos nearest = null;
			long best = Long.MAX_VALUE;
			for (var placement : layout.structures().getPlacements()) {
				long dx = placement.getEntryPosX() - (long)origin.getX();
				long dz = placement.getEntryPosZ() - (long)origin.getZ();
				long distance = dx * dx + dz * dz;
				if (distance < best) {
					best = distance;
					nearest = new BlockPos(placement.getEntryPosX(), origin.getY(),
							placement.getEntryPosZ());
				}
			}
			if (nearest != null)
				return Pair.of(nearest, structure);
		}

		// Everything else is painted, not placed, so it still wants a search -- just one that starts
		// beyond the Sanctuary rather than ending well inside it.
		int reach = (int)(layout.structures().getMaximumDistanceFromOrigin()
				+ reika.chromaticraft.world.dimension.RegionMapper.MAX_BUFFER) * 2;
		return super.findClosestBiome3d(origin, Math.max(searchRadius, reach),
				sampleResolutionHorizontal, sampleResolutionVertical, allowed, sampler, level);
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		ProximaBiomeType type = BiomeDistributor.getBiome(
				QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
		return biomes.getOrThrow((type == null ? ProximaBiomes.CENTER : type).biomeKey());
	}
}
