package reika.chromaticraft.world.biome;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import terrablender.api.Region;
import terrablender.api.RegionType;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaOptions;

/** Dedicated overworld region for V33a's large, coherent Luminous Cliffs biome islands. */
public final class LuminousCliffsRegion extends Region {

	public LuminousCliffsRegion() {
		super(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "luminous_cliffs"),
				RegionType.OVERWORLD, ChromaOptions.getGlowingCliffsWeight());
	}

	@Override
	public void addBiomes(Registry<Biome> registry,
			Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
			// V33a inserts Luminous Cliffs as a complete warm/cool biome, not merely as a
			// replacement for the sparse Windswept climate points. Replacing every ordinary
			// surface-land target inside this low-weight TerraBlender region gives it that same
			// coherent footprint while leaving oceans, mushroom islands, and cave biomes intact.
			for (ResourceKey<Biome> biome : LAND_BIOMES)
				builder.replaceBiome(biome, ChromaBiomes.LUMINOUS_CLIFFS);
			for (ResourceKey<Biome> biome : EDGE_BIOMES)
				builder.replaceBiome(biome, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
		});
	}

	private static final List<ResourceKey<Biome>> LAND_BIOMES = List.of(
			Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES,
			Biomes.DESERT, Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.FOREST,
			Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.PALE_GARDEN,
			Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.TAIGA, Biomes.SNOWY_TAIGA,
			Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_HILLS,
			Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
			Biomes.WINDSWEPT_SAVANNA, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
			Biomes.BAMBOO_JUNGLE, Biomes.BADLANDS, Biomes.ERODED_BADLANDS,
			Biomes.WOODED_BADLANDS, Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.GROVE,
			Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS
	);

	private static final List<ResourceKey<Biome>> EDGE_BIOMES = List.of(
			Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.BEACH, Biomes.SNOWY_BEACH,
			Biomes.STONY_SHORE
	);
}
