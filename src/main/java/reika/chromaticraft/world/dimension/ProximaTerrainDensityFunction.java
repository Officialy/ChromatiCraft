package reika.chromaticraft.world.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * V33a's radial terrain profile, expressed as a 26.2 {@link DensityFunction} so the ordinary
 * {@code NoiseBasedChunkGenerator} pipeline can build Proxima.
 *
 * <p>1.7.10 substituted two values into its copy of {@code ChunkProviderGenerate} — {@code f3}, the
 * base height, and {@code f4}, the height variation — which vanilla otherwise read from the biome.
 * Everything else in that method was stock. 26.2 expresses exactly the same two concepts as the
 * <em>offset</em> and <em>factor</em> inputs of the noise router, so the port supplies only those and
 * lets vanilla own the noise, the interpolation, the cell lattice and the surface pass.
 *
 * <p>That is the whole reason this is a density function rather than a transcription of upstream's
 * noise code: the only part of V33a's generator that is actually V33a's is this profile. Reproducing
 * 1.7.10's Perlin implementation to get vanilla's own formula back would be copying the parts that
 * were never modified.
 *
 * <p>{@link Mode#OFFSET} and {@link Mode#FACTOR} are the two outputs. The mapping is direct:
 *
 * <pre>
 *   f0     = sqrt((x*x + z*z) / (65536*32)) * 0.03125, damped to 0 within 8 chunks of a structure
 *   offset = max(-0.25, 0.125 - f0*0.125)    // V33a f3
 *   factor = 0.5 * f0                        // V33a f4
 * </pre>
 *
 * <p>So the centre of Proxima is a high flat plain, the land falls towards a floor and grows steadily
 * more mountainous outward, and the ground under every puzzle structure is levelled — see
 * {@link ProximaTerrainProfile}, which owns the arithmetic and is shared with anything else that needs
 * to know the shape of the dimension.
 *
 * <h2>Why it reads global state</h2>
 *
 * The structure damping depends on where the puzzle ring landed, so this cannot be a pure function of
 * position alone — upstream has the same dependency. It reads the finished
 * {@link ProximaGenerators#getLayout()}, which is written once before any chunk is built and never
 * mutated afterwards, and which the Portal Rift's generator gate already prevents a player from
 * outrunning. With no layout yet it falls back to the undamped profile rather than failing a chunk
 * build.
 */
public record ProximaTerrainDensityFunction(Mode mode) implements DensityFunction.SimpleFunction {

	public enum Mode implements StringRepresentable {
		/** V33a {@code f3}: the base height the column is built around. */
		OFFSET("offset"),
		/** V33a {@code f4}: how far the terrain may wander from it. */
		FACTOR("factor");

		public static final com.mojang.serialization.Codec<Mode> CODEC =
				StringRepresentable.fromEnum(Mode::values);

		private final String name;

		Mode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final MapCodec<ProximaTerrainDensityFunction> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Mode.CODEC.fieldOf("mode").forGetter(ProximaTerrainDensityFunction::mode))
					.apply(i, ProximaTerrainDensityFunction::new));

	public static final KeyDispatchDataCodec<ProximaTerrainDensityFunction> DISPATCH_CODEC =
			KeyDispatchDataCodec.of(CODEC);

	@Override
	public double compute(FunctionContext context) {
		// The router works in block coordinates; V33a's profile is written against the quart lattice
		// its noise layer sampled, which is one cell per four blocks.
		double roughness = this.roughness(context.blockX() >> 2, context.blockZ() >> 2);
		if (mode == Mode.OFFSET) {
			// V33a f1 = (f3*4 - 1)/8, its own conversion of the base height into the term the column
			// formula adds. Range [-0.25, -0.0625]: highest at the centre, lowest far out.
			return (ProximaTerrainProfile.baseHeightFor(roughness) * 4 - 1) / 8;
		}
		// V33a divides its vertical slope by d14 = 0.9*f4 + 0.1; 26.2 multiplies depth by the factor.
		// So the factor is the reciprocal, and getting this the right way round is the difference
		// between a flat centre with mountainous edges and exactly the opposite: f4 is 0 at the origin,
		// giving the maximum factor 10 and terrain that hugs its base height, and grows without bound
		// outward, driving the factor towards 0 and letting the land range freely.
		return 1 / (0.9 * ProximaTerrainProfile.heightVariationFor(roughness) + 0.1);
	}

	private double roughness(int quartX, int quartZ) {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null) {
			// No structure ring yet, so nothing to level the ground around. The radial term is
			// unaffected, and the gate stops a player reaching a chunk built in this state.
			double f0 = Math.sqrt((quartX * (double)quartX + quartZ * (double)quartZ) / (65536D * 32D))
					* 0.03125;
			return f0;
		}
		return new ProximaTerrainProfile(layout.structures()).roughness(quartX, quartZ);
	}

	@Override
	public double minValue() {
		// The offset floor is V33a's own -0.25 base height run through f1; the factor tends to zero as
		// the height variation grows, but never reaches it.
		return mode == Mode.OFFSET ? -0.25 : 0;
	}

	@Override
	public double maxValue() {
		// f3 caps at 0.125, so the offset caps at (0.125*4-1)/8; f4 bottoms out at 0 at the world
		// origin, so the factor caps at 1/0.1.
		return mode == Mode.OFFSET ? -0.0625 : 10;
	}

	@Override
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return DISPATCH_CODEC;
	}
}
