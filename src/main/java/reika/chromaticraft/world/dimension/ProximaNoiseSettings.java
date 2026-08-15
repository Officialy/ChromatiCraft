package reika.chromaticraft.world.dimension;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.dimension.ProximaTerrainDensityFunction.Mode;

/**
 * Proxima's terrain, built out of vanilla 26.2's noise pipeline.
 *
 * <p>{@code ChunkProviderChroma} is 1.7.10's own {@code ChunkProviderGenerate} with two values
 * substituted, so the modern equivalent is vanilla's {@code NoiseBasedChunkGenerator} with the same
 * two values supplied through {@link ProximaTerrainDensityFunction}. Nothing else needs porting:
 * vanilla owns the noise, the cell interpolation, the surface pass and the bedrock.
 *
 * <h2>The numbers that carry over exactly</h2>
 *
 * <ul>
 * <li><b>The cell lattice.</b> V33a samples 5x33x5 noise per 16x256x16 chunk, i.e. four horizontal
 *     cells of four blocks and thirty-two vertical cells of eight. {@link NoiseSettings} spells that
 *     as {@code noiseSizeHorizontal = 1} (4 blocks) and {@code noiseSizeVertical = 2} (8 blocks), so
 *     the modern generator interpolates over the identical grid.</li>
 * <li><b>The noise scales.</b> V33a drives its two main octave generators at 684.412 and its selector
 *     at 8.555150/4.277575. Vanilla's {@code BlendedNoise} computes those internally as
 *     {@code 684.412 * xzScale} and {@code 684.412 * xzScale / xzFactor}, so
 *     {@code blendedNoise(1, 1, 80, 160, 8)} is upstream's configuration exactly —
 *     {@code 684.412/80 = 8.55515} and {@code 684.412/160 = 4.277575}.</li>
 * <li><b>Sea level 63</b>, from {@code generateColumnData}'s {@code byte b0 = 63}.</li>
 * <li><b>Height 0-256</b>, from {@code WorldProviderChroma.getHeight}.</li>
 * </ul>
 *
 * <p>One scale does not carry over: V33a raises its selector generator to 96 octaves where vanilla's
 * is fixed at 8. That count is baked into {@code BlendedNoise} and is not exposed, and 96 octaves of
 * a 684.412-scale selector are almost entirely below the sampling resolution anyway. It is recorded
 * rather than worked around, because working around it would mean replacing vanilla's noise.
 *
 * <p>Aquifers and ore veins are off: V33a has neither, and both would carve water tables and copper
 * ribbons through a dimension that is supposed to be crystal.
 */
public final class ProximaNoiseSettings {

	public static final ResourceKey<NoiseGeneratorSettings> PROXIMA = ResourceKey.create(
			Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "proxima"));

	/** V33a WorldProviderChroma.getHeight. */
	public static final int MIN_Y = 0;
	public static final int HEIGHT = 256;
	/** V33a generateColumnData: {@code byte b0 = 63}. */
	public static final int SEA_LEVEL = 63;

	private ProximaNoiseSettings() {}

	public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
		context.register(PROXIMA, new NoiseGeneratorSettings(
				new NoiseSettings(MIN_Y, HEIGHT, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router(),
				surfaceRules(),
				List.<Climate.ParameterPoint>of(),
				SEA_LEVEL,
				false,
				false,
				false,
				false));
	}

	/**
	 * V33a's column formula, in vanilla's terms: a vertical gradient offset by the base height, scaled
	 * by the factor, with the blended noise added on top.
	 *
	 * <p>This is the same composition vanilla builds for the overworld — {@code depth} from
	 * {@code yClampedGradient + offset}, {@code initialDensity} from {@code 4 * (depth * factor)}
	 * quarter-negatived, then the 3D noise — with Proxima's offset and factor in place of the
	 * biome-spline ones. Proxima has no continents, erosion or ridges, so those inputs are zero.
	 */
	private static NoiseRouter router() {
		DensityFunction offset = new ProximaTerrainDensityFunction(Mode.OFFSET);
		DensityFunction factor = new ProximaTerrainDensityFunction(Mode.FACTOR);
		DensityFunction depth = DensityFunctions.add(
				DensityFunctions.yClampedGradient(MIN_Y, MIN_Y + HEIGHT, 1.5, -1.5), offset);
		DensityFunction initialDensity = DensityFunctions.mul(DensityFunctions.constant(4),
				DensityFunctions.mul(depth, factor).quarterNegative());
		DensityFunction terrain = DensityFunctions.add(initialDensity,
				BlendedNoise.createUnseeded(1, 1, 80, 160, 8));
		DensityFunction finalDensity = DensityFunctions.interpolated(
				DensityFunctions.blendDensity(terrain));

		DensityFunction zero = DensityFunctions.zero();
		return new NoiseRouter(zero, zero, zero, zero, zero, zero, zero, zero, depth, zero, zero,
				finalDensity, zero, zero, zero);
	}

	/**
	 * V33a {@code replaceBlocksForBiome}: bedrock at the bottom, then grass over dirt on the surface,
	 * with sand where the surface meets the sea. Vanilla's {@link SurfaceRules} express all three, and
	 * the biome-specific variants land with the per-biome terrain shapers.
	 */
	private static SurfaceRules.RuleSource surfaceRules() {
		SurfaceRules.RuleSource grass = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
		SurfaceRules.RuleSource dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
		SurfaceRules.RuleSource sand = SurfaceRules.state(Blocks.SAND.defaultBlockState());

		// V33a generateSandBeaches: sand where the column surfaces within a couple of blocks of sea
		// level, rather than grass.
		SurfaceRules.RuleSource beachOrGrass = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), grass),
				sand);

		return SurfaceRules.sequence(
				// V33a generateBedrockLayer: the floor of the world.
				SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchorAt(MIN_Y),
						VerticalAnchorAt(MIN_Y + 5)), SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())),
				SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, beachOrGrass),
								SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt))));
	}

	private static net.minecraft.world.level.levelgen.VerticalAnchor VerticalAnchorAt(int y) {
		return net.minecraft.world.level.levelgen.VerticalAnchor.absolute(y);
	}
}
