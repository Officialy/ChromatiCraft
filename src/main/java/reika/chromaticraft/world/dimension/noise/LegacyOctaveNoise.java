package reika.chromaticraft.world.dimension.noise;

import java.util.Arrays;
import java.util.Random;

/**
 * Minecraft 1.7.10's {@code NoiseGeneratorOctaves} and {@code NoiseGeneratorImproved}, transcribed
 * exactly so Proxima's terrain keeps the shape V33a generated.
 *
 * <p>{@code ChunkProviderChroma} is 1.7.10's own {@code ChunkProviderGenerate} pipeline with one
 * substitution (see {@link reika.chromaticraft.world.dimension.ProximaTerrainProfile}), so its terrain
 * is defined by this specific noise function fed specific scales. 26.2's density-function system is a
 * different construction entirely and cannot express it, so the original is preserved here rather than
 * approximated.
 *
 * <h2>Why this is a transcription and not a re-derivation</h2>
 *
 * 26.2's {@link net.minecraft.world.level.levelgen.synth.ImprovedNoise} is, on inspection, the same
 * function: the same {@code xo/yo/zo = nextDouble()*256} offsets, the same 256-entry permutation
 * shuffled by {@code nextInt(256-i)}, the same sixteen gradients in the same order, and the same
 * x-then-y-then-z lerp. What it no longer exposes is {@code populateNoiseArray}, the bulk lattice fill
 * that {@code generateNoiseOctaves} drives, and its permutation table is private — so the sampling
 * half is transcribed here instead of being reached through vanilla. The focused GameTest asserts the
 * two agree cell for cell, which is what makes that claim checkable rather than assumed.
 *
 * <h2>The one thing that is easy to get wrong</h2>
 *
 * {@code populateNoiseArray} <em>accumulates</em> into the array it is given; it never writes. That is
 * how the octaves sum. {@code generateNoiseOctaves} therefore zeroes the array once up front and then
 * calls it per octave with a halving amplitude, and the index order is x-major, then z, then y — not
 * the x/y/z order the parameter names suggest.
 */
public final class LegacyOctaveNoise {

	private final Layer[] octaves;

	/**
	 * 1.7.10 {@code NoiseGeneratorOctaves(Random, int)}: every octave draws from the same Random in
	 * sequence, so the order in which a caller constructs its generators is part of the seed.
	 */
	public LegacyOctaveNoise(Random rand, int octaveCount) {
		octaves = new Layer[octaveCount];
		for (int i = 0; i < octaveCount; i++)
			octaves[i] = new Layer(rand);
	}

	public int octaveCount() {
		return octaves.length;
	}

	/**
	 * 1.7.10 {@code generateNoiseOctaves}. The result is indexed
	 * {@code ((x * zSize) + z) * ySize + y}.
	 *
	 * @param out reused across calls; allocated when null, zeroed otherwise
	 */
	public double[] generateNoiseOctaves(double[] out, int xOffset, int yOffset, int zOffset,
			int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		if (out == null)
			out = new double[xSize * ySize * zSize];
		else
			Arrays.fill(out, 0);

		double amplitude = 1;
		for (Layer octave : octaves) {
			double x = xOffset * amplitude * xScale;
			double y = yOffset * amplitude * yScale;
			double z = zOffset * amplitude * zScale;
			// 1.7.10 folds the integer part of x and z modulo 2^24 before sampling, so that far-out
			// coordinates keep their fractional part exactly instead of losing it to double precision.
			long xFloor = floorLong(x);
			long zFloor = floorLong(z);
			x -= xFloor;
			z -= zFloor;
			xFloor %= 16777216L;
			zFloor %= 16777216L;
			x += xFloor;
			z += zFloor;
			octave.populateNoiseArray(out, x, y, z, xSize, ySize, zSize,
					xScale * amplitude, yScale * amplitude, zScale * amplitude, amplitude);
			amplitude /= 2;
		}
		return out;
	}

	/** 1.7.10's two-dimensional bouncer: {@code ySize} of one at a fixed y offset of ten. */
	public double[] generateNoiseOctaves(double[] out, int xOffset, int zOffset,
			int xSize, int zSize, double xScale, double yScale, double zScale) {
		return this.generateNoiseOctaves(out, xOffset, 10, zOffset, xSize, 1, zSize, xScale, 1, zScale);
	}

	/** 1.7.10 {@code MathHelper.floor_double_long}. */
	private static long floorLong(double d) {
		long l = (long)d;
		return d < l ? l - 1 : l;
	}

	/**
	 * 1.7.10 {@code NoiseGeneratorImproved}: Perlin's improved noise over a doubled 512-entry
	 * permutation table.
	 */
	public static final class Layer {

		private final int[] permutations = new int[512];
		public final double xCoord;
		public final double yCoord;
		public final double zCoord;

		/** 1.7.10's sixteen gradient vectors, split per axis exactly as the source stores them. */
		private static final double[] GRAD_X =
				{1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
		private static final double[] GRAD_Y =
				{1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1};
		private static final double[] GRAD_Z =
				{0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};

		public Layer(Random rand) {
			xCoord = rand.nextDouble() * 256;
			yCoord = rand.nextDouble() * 256;
			zCoord = rand.nextDouble() * 256;
			for (int i = 0; i < 256; i++)
				permutations[i] = i;
			for (int i = 0; i < 256; i++) {
				int swap = rand.nextInt(256 - i) + i;
				int held = permutations[i];
				permutations[i] = permutations[swap];
				permutations[swap] = held;
				permutations[i + 256] = permutations[i];
			}
		}

		private static double lerp(double t, double a, double b) {
			return a + t * (b - a);
		}

		/** 1.7.10 {@code grad}: the dot product of the hashed gradient with the offset. */
		private static double grad(int hash, double x, double y, double z) {
			int i = hash & 15;
			return GRAD_X[i] * x + GRAD_Y[i] * y + GRAD_Z[i] * z;
		}

		/** 1.7.10 {@code func_76309_a}: the same dot product with the y term dropped. */
		private static double grad2(int hash, double x, double z) {
			int i = hash & 15;
			return GRAD_X[i] * x + GRAD_Z[i] * z;
		}

		private static double fade(double t) {
			return t * t * t * (t * (t * 6 - 15) + 10);
		}

		/**
		 * 1.7.10 {@code populateNoiseArray}. Adds this octave's contribution, scaled by
		 * {@code 1/amplitudeDivisor}, into {@code out}; it never overwrites.
		 *
		 * <p>The {@code ySize == 1} branch is the source's flat fast path. It is not an optimisation
		 * that can be dropped: it walks the array in x-then-z order with no y stride at all, which is
		 * the layout the two-dimensional callers expect.
		 */
		public void populateNoiseArray(double[] out, double xOffset, double yOffset, double zOffset,
				int xSize, int ySize, int zSize, double xScale, double yScale, double zScale,
				double amplitudeDivisor) {
			double scale = 1 / amplitudeDivisor;
			if (ySize == 1) {
				int index = 0;
				for (int ix = 0; ix < xSize; ix++) {
					double x = xOffset + ix * xScale + xCoord;
					int xFloor = (int)x;
					if (x < xFloor)
						xFloor--;
					int xHash = xFloor & 255;
					x -= xFloor;
					double xFade = fade(x);
					for (int iz = 0; iz < zSize; iz++) {
						double z = zOffset + iz * zScale + zCoord;
						int zFloor = (int)z;
						if (z < zFloor)
							zFloor--;
						int zHash = zFloor & 255;
						z -= zFloor;
						double zFade = fade(z);
						int a = permutations[xHash];
						int aa = permutations[a] + zHash;
						int b = permutations[xHash + 1];
						int ba = permutations[b] + zHash;
						double lo = lerp(xFade, grad2(permutations[aa], x, z),
								grad(permutations[ba], x - 1, 0, z));
						double hi = lerp(xFade, grad(permutations[aa + 1], x, 0, z - 1),
								grad(permutations[ba + 1], x - 1, 0, z - 1));
						out[index++] += lerp(zFade, lo, hi) * scale;
					}
				}
				return;
			}

			int index = 0;
			int lastYHash = -1;
			double c00 = 0;
			double c01 = 0;
			double c10 = 0;
			double c11 = 0;
			for (int ix = 0; ix < xSize; ix++) {
				double x = xOffset + ix * xScale + xCoord;
				int xFloor = (int)x;
				if (x < xFloor)
					xFloor--;
				int xHash = xFloor & 255;
				x -= xFloor;
				double xFade = fade(x);
				for (int iz = 0; iz < zSize; iz++) {
					double z = zOffset + iz * zScale + zCoord;
					int zFloor = (int)z;
					if (z < zFloor)
						zFloor--;
					int zHash = zFloor & 255;
					z -= zFloor;
					double zFade = fade(z);
					for (int iy = 0; iy < ySize; iy++) {
						double y = yOffset + iy * yScale + yCoord;
						int yFloor = (int)y;
						if (y < yFloor)
							yFloor--;
						int yHash = yFloor & 255;
						y -= yFloor;
						double yFade = fade(y);
						// The four x-lerped corners only change when the y lattice cell does, which is
						// why the source caches them across the inner loop.
						if (iy == 0 || yHash != lastYHash) {
							lastYHash = yHash;
							int a = permutations[xHash] + yHash;
							int aa = permutations[a] + zHash;
							int ab = permutations[a + 1] + zHash;
							int b = permutations[xHash + 1] + yHash;
							int ba = permutations[b] + zHash;
							int bb = permutations[b + 1] + zHash;
							c00 = lerp(xFade, grad(permutations[aa], x, y, z),
									grad(permutations[ba], x - 1, y, z));
							c01 = lerp(xFade, grad(permutations[ab], x, y - 1, z),
									grad(permutations[bb], x - 1, y - 1, z));
							c10 = lerp(xFade, grad(permutations[aa + 1], x, y, z - 1),
									grad(permutations[ba + 1], x - 1, y, z - 1));
							c11 = lerp(xFade, grad(permutations[ab + 1], x, y - 1, z - 1),
									grad(permutations[bb + 1], x - 1, y - 1, z - 1));
						}
						double low = lerp(yFade, c00, c01);
						double high = lerp(yFade, c10, c11);
						out[index++] += lerp(zFade, low, high) * scale;
					}
				}
			}
		}

		/**
		 * Single-sample form of the three-dimensional path, for the equivalence check against
		 * vanilla's {@code ImprovedNoise}.
		 *
		 * <p>Deliberately asks for two y layers and reads the first. A size of one would take the
		 * {@code ySize == 1} branch, which is a genuinely different function — it drops the y term
		 * entirely — so sampling through it would compare the wrong thing.
		 */
		public double noise(double px, double py, double pz) {
			double[] column = new double[2];
			this.populateNoiseArray(column, px, py, pz, 1, 2, 1, 1, 1, 1, 1);
			return column[0];
		}
	}
}
