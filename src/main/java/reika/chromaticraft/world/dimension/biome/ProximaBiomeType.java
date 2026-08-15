package reika.chromaticraft.world.dimension.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import reika.chromaticraft.ChromatiCraft;

/**
 * V33a {@code ChromaDimensionManager.ChromaDimensionBiomeType}: the common face of Proxima's primary
 * biomes and their sub-biomes.
 *
 * <p>Upstream this returns a live {@code ChromaDimensionBiome} instance, because 1.7.10 biomes were
 * objects with numeric ids that the mod constructed itself. In 26.2 a biome is data, so the type
 * carries a {@link ResourceKey} instead and the {@link Biome} it names is registered through datagen.
 * That split is what lets the biome <em>distribution</em> — which only ever needs identities, weights
 * and height deltas — be ported before the biome definitions themselves.
 */
public interface ProximaBiomeType {

	/** The V33a display name, e.g. "Crystal Plains". */
	String biomeName();

	/** The registry key of the {@link Biome} this type resolves to. */
	ResourceKey<Biome> biomeKey();

	/** V33a isWaterBiome: whether terrain generation floods this biome. */
	boolean isWaterBiome();

	/** V33a isReasonablyFlat: whether the terrain shaper may treat it as gentle. */
	boolean isReasonablyFlat();

	/** V33a getBaseHeightDelta: the biome's offset from the dimension's base terrain height. */
	int getBaseHeightDelta();

	String name();

	int ordinal();

	static ResourceKey<Biome> key(String path) {
		return ResourceKey.create(Registries.BIOME,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}
}
