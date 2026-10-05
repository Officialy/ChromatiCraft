package reika.chromaticraft.world.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.densityfunction.*;

/**
 * V33a's radial terrain profile, expressed as a 26.3 {@link DensityFunction} so the ordinary
 * {@code NoiseBasedChunkGenerator} pipeline can build Proxima.
 *
 * <p>1.7.10 substituted two values into its copy of {@code ChunkProviderGenerate} — {@code f3}, the
 * base height, and {@code f4}, the height variation — which vanilla otherwise read from the biome.
 * Everything else in that method was stock. The density router expresses exactly the same two concepts as the
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
public record ProximaTerrainDensityFunction(Mode mode) implements DensityFunction {

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

    @Override
    public DensitySampler compileSampler(DensityFunction.CompileContext context) {
        return new Sampler(this);
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        return mode == Mode.OFFSET ? Interval.of(-0.25F, -0.0625F) : Interval.of(0, 10);
    }

    @Override
    public @DensityFunction.Axes int domainAxes() {
        return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
    }

    @Override
    public MapCodec<ProximaTerrainDensityFunction> codec() {
        return CODEC;
    }

    /** Evaluate the unchanged V33a radial profile at a block column. */
    public float compute(int blockX, int blockZ) {
        // The source profile samples one horizontal cell for every four blocks.
        double roughness = this.roughness(blockX >> 2, blockZ >> 2);
        if (mode == Mode.OFFSET)
            return (ProximaTerrainProfile.baseHeightFor(roughness) * 4 - 1) / 8;
        // A large factor holds the flat centre close to its base height; the outer terrain varies more.
        return (float)(1 / (0.9 * ProximaTerrainProfile.heightVariationFor(roughness) + 0.1));
    }

    private record Sampler(ProximaTerrainDensityFunction function) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer output, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, output, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return function.compute(blockX, blockZ);
        }
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

}
