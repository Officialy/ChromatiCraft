package reika.chromaticraft.world.luminous;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import reika.chromaticraft.world.GlowingCliffsColumnShaper.BiomeKind;
import reika.chromaticraft.world.GlowingCliffsColumnShaper.BiomeMask;
import reika.chromaticraft.world.biome.ChromaBiomes;

import java.util.HashMap;
import java.util.Map;

/**
 * The shaper's view of the biome layout: what kind of biome each column is, read once per 4x4 quart
 * and cached.
 *
 * <p>1.7.10 biomes were a flat per-column map. 26.2 biomes are 3D, so every reader samples at the same
 * height (sea level). The terrain pass and the island placer must see the same layout or an island's
 * predicted ground would not be the ground it gets, so both build their mask through here, one from the
 * level and one from the structure context's biome source.
 */
public final class LuminousCliffsBiomeMask implements BiomeMask {

	@FunctionalInterface
	public interface QuartSampler {
		Holder<Biome> biome(int quartX, int quartY, int quartZ);
	}

	private final QuartSampler sampler;
	private final int quartY;
	private final Map<Long, BiomeKind> cache = new HashMap<>();

	public LuminousCliffsBiomeMask(QuartSampler sampler, int seaLevel) {
		this.sampler = sampler;
		this.quartY = QuartPos.fromBlock(seaLevel);
	}

	@Override
	public BiomeKind at(int x, int z) {
		int qx = QuartPos.fromBlock(x);
		int qz = QuartPos.fromBlock(z);
		long key = ((long)qx << 32) ^ (qz & 0xFFFFFFFFL);
		BiomeKind kind = cache.get(key);
		if (kind == null) {
			kind = kindOf(sampler.biome(qx, quartY, qz));
			cache.put(key, kind);
		}
		return kind;
	}

	public static BiomeKind kindOf(Holder<Biome> biome) {
		if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS))
			return BiomeKind.MAIN;
		if (biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
			return BiomeKind.EDGE;
		if (biome.is(BiomeTags.IS_OCEAN))
			return BiomeKind.OCEAN;
		return BiomeKind.OTHER;
	}
}
