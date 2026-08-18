package reika.chromaticraft.world.dimension;


import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.world.dimension.biome.ProximaBiomeSource;

/**
 * Proxima's dimension type and level stem.
 *
 * <p>Every value here comes from {@code WorldProviderChroma}, which is where 1.7.10 expressed what
 * 26.2 expresses as {@link DimensionType}:
 *
 * <table>
 * <tr><td>{@code getHeight} 256</td><td>height, with a min y of 0</td></tr>
 * <tr><td>{@code isSurfaceWorld} true</td><td>{@code hasSkyLight}</td></tr>
 * <tr><td>{@code calculateCelestialAngle} pinned to 0.5, {@code isDaytime} false</td>
 *     <td>a fixed time, so the sky never moves</td></tr>
 * <tr><td>{@code canRespawnHere} false</td><td>beds and respawn anchors do not work</td></tr>
 * <tr><td>{@code canBlockFreeze}/{@code canSnowAt} false</td><td>no natural ice or snow</td></tr>
 * <tr><td>{@code getMovementFactor} 1</td><td>coordinate scale 1.0</td></tr>
 * </table>
 *
 * <p>Ultrawarm is off (V33a never evaporates water), natural is false so compasses and clocks spin the
 * way {@code shouldMapSpin} asked for, and piglins do not zombify because nothing in V33a says they
 * should. Ambient light is zero: V33a leaves {@code generateLightBrightnessTable} at the vanilla
 * default, having commented out its own brightening.
 *
 * <p>The infiniburn tag is the overworld's, matching V33a's silence on the subject; monster spawn
 * settings are vanilla's defaults, which is what its cleared spawn lists plus one Tunnel Nuker come to.
 */
public final class ProximaDimension {

	/** V33a getSpawnPoint: {@code new ChunkCoordinates(0, 1024, 0)}. */
	public static final int SPAWN_Y = 1024;

	private ProximaDimension() {}

	public static void bootstrapType(BootstrapContext<DimensionType> context) {
		HolderGetter<net.minecraft.world.level.block.Block> blocks = context.lookup(Registries.BLOCK);
		// V33a canRespawnHere() is false, which in 26.2 lives on the environment attributes rather
		// than the dimension record: beds refuse outright and respawn anchors do nothing.
		net.minecraft.world.attribute.EnvironmentAttributeMap attributes =
				net.minecraft.world.attribute.EnvironmentAttributeMap.builder()
						.set(net.minecraft.world.attribute.EnvironmentAttributes.BED_RULE,
								net.minecraft.world.attribute.BedRule.EXPLODES)
						.set(net.minecraft.world.attribute.EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
						.set(net.minecraft.world.attribute.EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS,
								false)
						.build();
		context.register(ChromaDimensions.PROXIMA_TYPE, new DimensionType(
				// V33a calculateCelestialAngle returns a constant and isDaytime is false: the sky is fixed.
				true,
				// isSurfaceWorld true, so sky light exists and the sun renders.
				true,
				false,
				false,
				// getMovementFactor 1.
				1.0,
				ProximaNoiseSettings.MIN_Y,
				ProximaNoiseSettings.HEIGHT,
				ProximaNoiseSettings.HEIGHT,
				blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
				// V33a leaves generateLightBrightnessTable at the vanilla default; its own brightening
				// is commented out.
				0.0F,
				new DimensionType.MonsterSettings(
						net.minecraft.util.valueproviders.UniformInt.of(0, 7), 0),
				// V33a draws Proxima's sky itself -- stars, nebulae and planets, and no sun or moon --
				// so vanilla must draw none of it. See ProximaSkyRenderer.
				DimensionType.Skybox.NONE,
				net.minecraft.world.level.CardinalLighting.Type.DEFAULT,
				attributes,
				// A fixed sky has no timeline to advance and no clock to read.
				net.minecraft.core.HolderSet.direct(),
				java.util.Optional.empty()));
	}

	public static void bootstrapStem(BootstrapContext<LevelStem> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<NoiseGeneratorSettings> noise = context.lookup(Registries.NOISE_SETTINGS);
		context.register(ChromaDimensions.PROXIMA_STEM, new LevelStem(
				context.lookup(Registries.DIMENSION_TYPE).getOrThrow(ChromaDimensions.PROXIMA_TYPE),
				new NoiseBasedChunkGenerator(new ProximaBiomeSource(biomes),
						noise.getOrThrow(ProximaNoiseSettings.PROXIMA))));
	}
}
