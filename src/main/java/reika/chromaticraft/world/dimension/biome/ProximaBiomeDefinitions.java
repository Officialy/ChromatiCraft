package reika.chromaticraft.world.dimension.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import reika.chromaticraft.ChromatiCraft;
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
				.generationSettings(generation(type, features, carvers))
				.build();
	}

	/**
	 * V33a {@code DimensionGenerators.generateIn}, which decides per biome which decorators run. It is
	 * a chain of tests rather than a table, so it is transcribed here in the same order:
	 *
	 * <ul>
	 * <li>The Central biome takes everything that is not tied to one specific biome — upstream's
	 *     {@code isDedicatedBiomeOnly}. Floatstone and the geode qualify; the crystal shrub, the crystal
	 *     tree and the glass cliffs do not.</li>
	 * <li>A {@code SKYFEATURE} theme means Skylands, which is where Floatstone belongs.</li>
	 * <li>The rest name their biome outright: the geode is the Crystal Plains', and both crystal plants
	 *     are the Crystal Forest sub-biome's alone.</li>
	 * </ul>
	 *
	 * <p>One branch is deliberately not reproduced. Upstream lets a Structure or Monument field run any
	 * non-dedicated generator at a flat 25% chance, decided per attempt. A biome's feature list is static
	 * data with no such gate, so expressing it would mean a second copy of every placed feature at four
	 * times the rarity. It is recorded rather than approximated, since a wrong rate would be harder to
	 * notice than an absent one.
	 *
	 * <p>Glass Cliffs is absent from every list here because it is a structure, not a feature: its
	 * biomes are declared on the structure itself.
	 */
	private static BiomeGenerationSettings generation(ProximaBiomeType type,
			HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(features, carvers);
		// Upstream's shape, kept deliberately: the Luminescent Sanctuary is not a biome with a list of
		// its own, it is "everything that is not dedicated to somewhere else". Reproducing that as a
		// term in each rule rather than as a hand-kept list is what stops the Sanctuary quietly falling
		// behind every time a generator is ported -- which is exactly how it ended up bare.
		boolean central = type == ProximaBiomes.CENTER;

		// Floatstone: a SKYFEATURE, so Skylands, plus the Sanctuary.
		if (central || type == ProximaBiomes.SKYLANDS)
			add(builder, features, "floatstone");
		// The aurorae are a SKYFEATURE too, but also dedicated-biome-only, so Skylands and nowhere else.
		if (type == ProximaBiomes.SKYLANDS)
			add(builder, features, "aurorae");
		// The geode: the Crystal Plains proper, plus the Sanctuary.
		if (central || type == ProximaBiomes.PLAINS)
			add(builder, features, "crystal_pit");
		// The glowing trees, V33a's TREES: the Glowing Forest, Crystal Plains, Iridescent Archipelago
		// proper, Sparkling Sands and Radiant Fissures -- plus the Sanctuary, which is what upstream's
		// CENTER branch grants before the TREES rule is ever reached.
		if (central || type == ProximaBiomes.FOREST || type == ProximaBiomes.PLAINS
				|| type == ProximaBiomes.ISLANDS || type == ProximaBiomes.SPARKLE
				|| type == ProximaBiomes.GLOWCRACKS)
			add(builder, features, "glow_tree");
		// V33a JETS: `case JETS: return true` -- every biome, the Sanctuary among them. Its own site
		// check is what actually decides where a jet appears.
		add(builder, features, "fire_jet");
		// V33a's FORESTS: the Glowing Forest alone among the named biomes, plus the Sanctuary, which
		// takes it from the CENTER branch as it takes everything not dedicated elsewhere.
		if (central || type == ProximaBiomes.FOREST)
			add(builder, features, "tree_cluster");
		// V33a RIFT: `b == Biomes.PLAINS.getBiome() || b.biomeType == Biomes.GLOWCRACKS` -- plus the
		// Sanctuary, which takes it from the CENTER branch as it takes everything not dedicated
		// elsewhere. The fissure is a TERRAIN generator upstream, run before the features; here it is a
		// vegetal-step feature like the rest, which puts it after the surface is laid -- the same
		// relative order, since upstream's decorator ran its whole ordered list after terrain too.
		if (central || type == ProximaBiomes.PLAINS || type == ProximaBiomes.GLOWCRACKS)
			add(builder, features, "fissure");
		// V33a CRACKS: `isReasonablyFlat() && !isWaterBiome()` -- plus the Sanctuary, which takes it
		// from the CENTER branch since the cracks are not isDedicatedBiomeOnly.
		if (central || (type.isReasonablyFlat() && !type.isWaterBiome()))
			add(builder, features, "glowing_cracks");
		// V33a ALTAR: `b.getExactType().isReasonablyFlat()` -- and the Sanctuary, which takes it from
		// the CENTER branch since the altar is not isDedicatedBiomeOnly. Its own site check wants seven
		// by seven of flat open grass, so the biome test only decides where it may try.
		if (central || type.isReasonablyFlat())
			add(builder, features, "mini_altar");
		// Both crystal plants belong to the Crystal Forest sub-biome and nowhere else.
		if (type == ProximaSubBiomes.CRYSFOREST) {
			add(builder, features, "crystal_tree");
			add(builder, features, "crystal_shrub");
		}
		return builder.build();
	}

	/**
	 * Proxima's decoration all runs in the vegetal step. V33a has no notion of steps — its decorator
	 * walks one ordered list per chunk — so the choice is ours, and vegetal is the step whose ordering
	 * against vanilla's own passes matches what upstream did: after the surface is laid, before the top
	 * layer.
	 */
	private static void add(BiomeGenerationSettings.Builder builder,
			HolderGetter<PlacedFeature> features, String name) {
		builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
				features.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,
						Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name))));
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
