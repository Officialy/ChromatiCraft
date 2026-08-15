package reika.chromaticraft.world.dimension.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.world.biome.ChromaBiomes;

/**
 * Datapack definitions for Proxima's thirteen biomes.
 *
 * <p>V33a's biome classes are almost entirely shared: every one of them extends
 * {@code ChromaDimensionBiome}, which disables rain, empties all four spawn lists, and takes both its
 * water colour and its grass colour from the overworld Rainbow Forest. Only four facts differ across
 * the whole family, and all four are reproduced here:
 *
 * <ul>
 * <li>Aura Ocean re-enables rain ({@code BiomeGenChromaOcean} sets {@code enableRain = true}). It is
 *     the only Proxima biome where it rains, which is what upstream's commented-out "always raining,
 *     but most biomes do not render it" note in {@code WorldProviderChroma} refers to.</li>
 * <li>Luminescent Sanctuary is the only biome with a spawn: the Tunnel Nuker, one at a time
 *     ({@code BiomeGenCentral} adds it to the cave-creature list at weight 1, group 1-1).</li>
 * <li>Structure Field brightens its grass and uses white water ({@code StructureBiome}).</li>
 * <li>Monument Field replaces the grass base outright with {@code 0x77ccff} ({@code MonumentBiome}).</li>
 * </ul>
 *
 * <h2>What is a static colour here and dynamic upstream</h2>
 *
 * V33a computes grass colour per position: the base is modulated by a simplex field into the green
 * channel between 1.0x and 1.5x, and Structure/Monument Fields additionally scan four blocks in each
 * cardinal direction and switch to a highlight colour at their own edge, blending toward it by a
 * second noise field. A 26.2 {@code BiomeSpecialEffects} grass override is a single constant, so the
 * base colour lives here and the per-position modulation belongs to a client colour resolver
 * alongside the existing {@code ChromaBlockColors}. That is a real remaining piece of work, not a
 * substitution: the constants below are exactly upstream's bases, not invented approximations.
 *
 * <h2>Deliberately empty for now</h2>
 *
 * Generation settings carry no features or carvers. Proxima's decoration does not come from biome
 * features at all — it comes from {@code DimensionGenerators}/{@code DecoratorChroma}, which run off
 * the chunk generator and land with it. Attaching vanilla features here would be inventing content.
 * The sky is likewise left at its default because upstream draws it with a custom
 * {@code ChromaSkyRenderer} rather than a per-biome colour.
 */
public final class ProximaBiomeDefinitions {

	/** V33a StructureBiome: the Rainbow Forest grass with its green channel raised, which saturates. */
	private static final int STRUCTURE_GRASS = 0x79FF5A;
	/** V33a StructureBiome.getWaterColorMultiplier. */
	private static final int STRUCTURE_WATER = 0xFFFFFF;
	/** V33a MonumentBiome.getBaseColor. */
	private static final int MONUMENT_GRASS = 0x77CCFF;

	/** V33a BiomeGenBase defaults; the Proxima biomes never set either. */
	private static final float TEMPERATURE = 0.5F;
	private static final float DOWNFALL = 0.5F;

	private ProximaBiomeDefinitions() {}

	public static void bootstrap(BootstrapContext<Biome> context) {
		HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

		for (ProximaBiomes b : ProximaBiomes.biomeList)
			context.register(b.biomeKey(), define(b, features, carvers));
		for (ProximaSubBiomes b : ProximaSubBiomes.biomeList)
			context.register(b.biomeKey(), define(b, features, carvers));
	}

	private static Biome define(ProximaBiomeType type, HolderGetter<PlacedFeature> features,
			HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		if (type == ProximaBiomes.CENTER) {
			// V33a BiomeGenCentral: the only spawn anywhere in Proxima, and only ever one at a time.
			mobs.addSpawn(ChromaEntityTypes.TUNNEL_NUKER.get().getCategory(), 1,
					new MobSpawnSettings.SpawnerData(ChromaEntityTypes.TUNNEL_NUKER.get(), 1, 1));
		}

		BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
				.waterColor(waterColor(type))
				.grassColorOverride(grassColor(type))
				.foliageColorOverride(grassColor(type));

		return new Biome.BiomeBuilder()
				// V33a ChromaDimensionBiome calls setDisableRain(); only BiomeGenChromaOcean undoes it.
				.hasPrecipitation(type == ProximaSubBiomes.DEEPOCEAN)
				.temperature(TEMPERATURE)
				.downfall(DOWNFALL)
				.specialEffects(effects.build())
				.mobSpawnSettings(mobs.build())
				.generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
				.build();
	}

	private static int grassColor(ProximaBiomeType type) {
		if (type == ProximaBiomes.MONUMENT)
			return MONUMENT_GRASS;
		if (type == ProximaBiomes.STRUCTURE)
			return STRUCTURE_GRASS;
		return ChromaBiomes.FOREST_GRASS;
	}

	private static int waterColor(ProximaBiomeType type) {
		if (type == ProximaBiomes.MONUMENT || type == ProximaBiomes.STRUCTURE)
			return STRUCTURE_WATER;
		return ChromaBiomes.WATER_COLOR;
	}
}
