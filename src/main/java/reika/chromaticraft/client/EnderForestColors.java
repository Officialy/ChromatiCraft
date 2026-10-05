package reika.chromaticraft.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.Simplex3DGenerator;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.Random;
import java.util.Set;

/**
 * V33a {@code BiomeEnderForest}'s grass, foliage and water colouring.
 *
 * <p>Every grass and leaf block in the Ender Forest is a random blend of a temperate forest's colour
 * and an ice plain's, chosen per block-ish cell: the position is quantised to 6x4x6, jittered by three
 * simplex fields, hashed, and the hash seeds a {@link Random} whose third draw is the blend. The
 * result is a patchwork of greens drifting toward pale snowy teal, varying with height as well as
 * across the ground — which is why this cannot be a {@code ColorResolver}: those are never given a Y.
 * It wraps the vanilla tint sources instead, as {@link LuminousCliffsColors} does, and applies the
 * blend on top of whatever the wrapped source produced.
 *
 * <p>The {@code ENDERCOLORING} option replaces all of that with a flat pink palette — grass
 * {@code 255,200,255}, foliage {@code 255,150,255} and water {@code 195,0,105}.
 *
 * <p>The seeds, frequencies, quantisation and draw order are V33a's exactly, so the pattern at a given
 * position is the pattern V33a drew there. {@code java.util.Random} is used deliberately: the blend is
 * the third value of that specific generator's sequence, which {@code RandomSource} would not
 * reproduce. The source kept one shared static {@code Random} and reseeded it; a fresh instance per
 * call gives the same numbers and is safe on meshing threads, which the shared one would not be.
 */
public final class EnderForestColors {

	private static final long ROOT = 127645902378217L;
	private static final NoiseGeneratorBase EXTRA_X = new Simplex3DGenerator(3571 ^ ROOT).setFrequency(1 / 4D);
	private static final NoiseGeneratorBase EXTRA_Y = new Simplex3DGenerator(-8247 ^ ROOT).setFrequency(1 / 4D);
	private static final NoiseGeneratorBase EXTRA_Z = new Simplex3DGenerator(5648723 ^ ROOT).setFrequency(1 / 4D);

	/** V33a {@code ReikaColorAPI.RGBtoHex(255, 200, 255)}. */
	public static final int PINK_GRASS = 0xffc8ff;
	/** V33a {@code ReikaColorAPI.RGBtoHex(255, 150, 255)}. */
	public static final int PINK_FOLIAGE = 0xff96ff;
	/** V33a {@code ReikaColorAPI.RGBtoHex(195, 0, 105)}. */
	public static final int PINK_WATER = 0xc30069;

	/**
	 * The blocks vanilla tints with the biome grass colour. V33a's override reached anything that asked
	 * the biome for its grass colour, which in 1.7.10 included tall grass, ferns and reeds.
	 */
	private static final Set<Block> GRASS_TINTED = Set.of(
			Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.POTTED_FERN, Blocks.BUSH,
			Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.PINK_PETALS, Blocks.WILDFLOWERS,
			Blocks.SUGAR_CANE);

	/** The blocks vanilla tints with the biome foliage colour. */
	private static final Set<Block> FOLIAGE_TINTED = Set.of(
			Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
			Blocks.MANGROVE_LEAVES, Blocks.VINE);

	private EnderForestColors() {}

	/**
	 * V33a {@code getMix}. Integer division is Java's truncating one, as upstream's was, so the cells
	 * straddle zero rather than tiling it; that asymmetry is part of the pattern.
	 */
	public static float getMix(int x, int y, int z) {
		Random rand = new Random(new Coordinate(
				x / 6 + EXTRA_X.getValue(x, z) * 8,
				y / 4 + EXTRA_Y.getValue(x, z) * 3,
				z / 6 + EXTRA_Z.getValue(x, z) * 8).hashCode());
		rand.nextBoolean();
		rand.nextBoolean();
		return rand.nextFloat();
	}

	/** V33a {@code getBiomeGrassColor}: the ice plain's end of the blend is its climate's colormap entry. */
	public static int grassColor(int forest, BlockPos pos) {
		if (ChromaOptions.ENDERCOLORING.getState())
			return PINK_GRASS;
		int ice = GrassColor.get(0.0, 0.5);
		return ReikaColorAPI.mixColors(forest, ice, getMix(pos.getX(), pos.getY(), pos.getZ()));
	}

	/** V33a {@code getBiomeFoliageColor}. */
	public static int foliageColor(int forest, BlockPos pos) {
		if (ChromaOptions.ENDERCOLORING.getState())
			return PINK_FOLIAGE;
		int ice = FoliageColor.get(0.0, 0.5);
		return ReikaColorAPI.mixColors(forest, ice, getMix(pos.getX(), pos.getY(), pos.getZ()));
	}

	/**
	 * V33a {@code getWaterColorMultiplier}. Without the option the forest's own untinted water is the
	 * biome's static colour already, so only the pink case needs doing here.
	 */
	public static int applyWater(int original, BlockAndTintGetter level, BlockPos pos) {
		if (!ChromaOptions.ENDERCOLORING.getState())
			return original;
		float strength = OverworldBiomePresence.strength(level, pos, OverworldBiomePresence.ENDER_FOREST);
		if (strength <= 0)
			return original;
		return (original & 0xff000000) | (ReikaColorAPI.mixColors(PINK_WATER, original, strength) & 0xffffff);
	}

	/**
	 * Wraps a vanilla tint source so that inside the Ender Forest the V33a blend is applied, and outside
	 * it nothing changes. Blocks that are neither grass- nor foliage-tinted are returned untouched.
	 */
	public static BlockTintSource wrap(BlockTintSource delegate, Block block) {
		boolean grass = GRASS_TINTED.contains(block);
		if (!grass && !FOLIAGE_TINTED.contains(block))
			return delegate;
		return new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return delegate.color(state);
			}

			@Override
			public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				return apply(delegate.colorInWorld(state, level, pos), level, pos);
			}

			@Override
			public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				return apply(delegate.colorAsTerrainParticle(state, level, pos), level, pos);
			}

			/**
			 * -1 is an untinted layer (the petal layer of Pink Petals and Wildflowers is one), and must
			 * stay untinted: blending white toward snow-green would paint the flowers.
			 */
			private int apply(int base, BlockAndTintGetter level, BlockPos pos) {
				if (base == -1)
					return base;
				float strength = OverworldBiomePresence.strength(level, pos, OverworldBiomePresence.ENDER_FOREST);
				if (strength <= 0)
					return base;
				int ender = grass ? grassColor(base, pos) : foliageColor(base, pos);
				int blended = strength >= 1 ? ender : ReikaColorAPI.mixColors(ender, base, strength);
				return (base & 0xff000000) | (blended & 0xffffff);
			}

			@Override
			public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
				return delegate.relevantProperties();
			}
		};
	}
}
