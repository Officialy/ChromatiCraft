package reika.chromaticraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import reika.chromaticraft.ChromatiCraft;

/**
 * Registry keys for V33a's own dimension, "Proxima" — the world the Portal Rift opens onto.
 *
 * <p>1.7.10 addressed it by the numeric {@code ExtraChromaIDs.DIMID}; 26.2 addresses it by resource
 * key, and the level itself is data-driven through {@code data/chromaticraft/dimension/} and
 * {@code dimension_type/}, both emitted by {@link reika.chromaticraft.data.ChromaWorldGenProvider}.
 */
public final class ChromaDimensions {

	public static final Identifier PROXIMA_ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "proxima");

	public static final ResourceKey<Level> PROXIMA = ResourceKey.create(Registries.DIMENSION, PROXIMA_ID);
	public static final ResourceKey<LevelStem> PROXIMA_STEM = ResourceKey.create(Registries.LEVEL_STEM, PROXIMA_ID);
	public static final ResourceKey<DimensionType> PROXIMA_TYPE =
			ResourceKey.create(Registries.DIMENSION_TYPE, PROXIMA_ID);

	public static ResourceKey<Biome> biome(String path) {
		return ResourceKey.create(Registries.BIOME,
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}

	private ChromaDimensions() {}
}
