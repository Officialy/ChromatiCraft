package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code BiomeGlowingCliffs} position- and altitude-sensitive colour resolution.
 *
 * <p>The 1.7.10 biome overrode {@code getBiomeGrassColor(x,y,z)},
 * {@code getBiomeFoliageColor(x,y,z)} and {@code getWaterColor(world,x,y,z,l)} directly. Minecraft
 * 26.2 resolves those through {@link net.minecraft.world.level.ColorResolver}, whose signature is
 * {@code (Biome, int x, int z)} — it never sees Y, so the altitude term cannot live there. The
 * equivalent hook that does receive the full position is
 * {@link BlockTintSource#colorInWorld(BlockState, BlockAndTintGetter, BlockPos)}, which runs during
 * chunk mesh baking. This class therefore wraps the vanilla tint sources: the wrapped source still
 * produces the biome-blended base colour, and the V33a hue/brightness shift is applied on top with
 * the real Y.
 *
 * <p>The noise generators keep V33a's seeding (wall-clock, so the palette differs per game session
 * exactly as it did in 1.7.10) and frequencies.
 */
public final class LuminousCliffsColors {

	/** V33a hue wander across the surface: simplex at 1/8, mapped to 0-50 degrees. */
	private static final NoiseGeneratorBase HUE_SHIFT =
			new SimplexNoiseGenerator(System.currentTimeMillis()).setFrequency(1 / 8D);
	/** V33a luminance wander: simplex at 1/6, mapped to 0.05-0.6 then scaled by altitude. */
	private static final NoiseGeneratorBase LUM_SHIFT =
			new SimplexNoiseGenerator(-System.currentTimeMillis()).setFrequency(1 / 6D);
	/** V33a teal/pink water mix: simplex at 1/10, mapped to 0-1. */
	private static final NoiseGeneratorBase WATER_COLOR_MIX =
			new SimplexNoiseGenerator(~System.currentTimeMillis()).setFrequency(1 / 10D);
	/** V33a sky hue wander: simplex at 1/20, mapped to +/-15 degrees off 0xd0a0ff. */
	private static final NoiseGeneratorBase SKY_COLOR_MIX =
			new SimplexNoiseGenerator(~System.currentTimeMillis() * 2).setFrequency(1 / 20D);

	/** V33a {@code getWaterColorMultiplier()} / the teal end of the surface mix. */
	public static final int WATER_TEAL = 0x22ffbb;
	/** V33a's pink end of the surface water mix. */
	public static final int WATER_PINK = 0xff50d0;
	/** V33a's shallow/edge/dark "clear" water. */
	public static final int WATER_CLEAR = 0xffffff;
	/** V33a {@code getSkyColorByTemp} base before the hue wander. */
	public static final int SKY_BASE = 0xd0a0ff;

	private LuminousCliffsColors() {}

	/**
	 * Biome-presence probe registered as a {@link ColorResolver}.
	 *
	 * <p>{@link BlockAndTintGetter} exposes no biome lookup, but it does expose
	 * {@link BlockAndTintGetter#getBlockTint}, which is thread-safe, per-column cached, and applies
	 * the player's biome-blend radius. Resolving "am I in the Luminous Cliffs" through a resolver
	 * therefore costs nothing per block and additionally feathers the effect across the biome
	 * border the same way the underlying grass colour is already feathered — V33a switched hard,
	 * which would leave a seam against a blended base colour.
	 */
	public static final ColorResolver CLIFF_PRESENCE =
			(biome, x, z) -> isLuminousCliffs(biome) ? 0xffffff : 0x000000;

	public static boolean isLuminousCliffs(Holder<Biome> biome) {
		return biome.is(ChromaBiomes.LUMINOUS_CLIFFS) || biome.is(ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
	}

	/**
	 * A {@link ColorResolver} is handed a bare {@link Biome}, which carries no registry key, so the
	 * two biome instances are resolved once from the connected level's registry and compared by
	 * identity. The cache is keyed on the registry itself so a world change re-resolves.
	 */
	private record BiomeIdentity(Registry<Biome> registry, Biome cliffs, Biome shores) {}

	/**
	 * Written on the client thread on level load, read from chunk-mesh worker threads. It must be
	 * a single volatile reference rather than three fields: a torn read across a world switch would
	 * be latched into {@link net.minecraft.client.color.block.BlockTintCache}, which only
	 * invalidates per chunk, so a wrong answer would survive until the chunk reloaded.
	 */
	private static volatile BiomeIdentity biomeIdentity;

	private static boolean isLuminousCliffs(Biome biome) {
		BiomeIdentity identity = biomeIdentity;
		if (identity == null) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null)
				return false;
			identity = resolve(mc.level.registryAccess().lookupOrThrow(Registries.BIOME));
		}
		return biome == identity.cliffs() || biome == identity.shores();
	}

	private static BiomeIdentity resolve(Registry<Biome> registry) {
		BiomeIdentity identity = new BiomeIdentity(registry,
				registry.getValue(ChromaBiomes.LUMINOUS_CLIFFS),
				registry.getValue(ChromaBiomes.LUMINOUS_CLIFFS_SHORES));
		biomeIdentity = identity;
		return identity;
	}

	/** Re-resolves the biome identities when the client joins or leaves a level. */
	public static void onLevelChanged(net.minecraft.client.multiplayer.ClientLevel level) {
		biomeIdentity = level == null ? null
				: resolve(level.registryAccess().lookupOrThrow(Registries.BIOME));
	}

	/** 0 outside the biome, 1 fully inside, feathered across the blend radius at the border. */
	public static float cliffStrength(BlockAndTintGetter level, BlockPos pos) {
		return (level.getBlockTint(pos, CLIFF_PRESENCE) & 255) / 255F;
	}

	public static boolean isLuminousCliffs(BlockAndTintGetter level, BlockPos pos) {
		return cliffStrength(level, pos) > 0;
	}

	/** V33a {@code shiftHue}: rotate the base hue by 0-50 degrees of simplex noise. */
	public static int shiftHue(int base, int x, int z) {
		float h = (float)ReikaMathLibrary.normalizeToBounds(HUE_SHIFT.getValue(x, z), 0, 50);
		return ReikaColorAPI.getShiftedHue(base, h);
	}

	/**
	 * V33a {@code shiftBrightness}: wash the colour towards white by a noise factor that only takes
	 * effect above y=64 and saturates at y=189, so the high plateaus glow and the shore does not.
	 */
	public static int shiftBrightness(int base, int x, int y, int z) {
		float f = (float)ReikaMathLibrary.normalizeToBounds(LUM_SHIFT.getValue(x, z), 0.05F, 0.6F);
		f *= Mth.clamp((y - 64F) / 125F, 0, 1);
		return ReikaColorAPI.mixColors(base, 0xffffff, 1 - f);
	}

	/** The full V33a grass/foliage resolution: hue wander in XZ, then the altitude wash. */
	public static int shiftTerrainColor(int base, BlockPos pos) {
		int c = shiftHue(base, pos.getX(), pos.getZ());
		return shiftBrightness(c, pos.getX(), pos.getY(), pos.getZ());
	}

	/** V33a {@code getSkyColorByTemp}: the pastel sky hue wanders with the viewer's position. */
	public static int skyColor(double x, double z) {
		return ReikaColorAPI.getShiftedHue(SKY_BASE, (float)(15 * SKY_COLOR_MIX.getValue(x, z)));
	}

	/** V33a {@code getWaterColor}: clear at the edges, otherwise a teal-to-pink simplex mix. */
	public static int waterColor(BlockAndTintGetter level, BlockPos pos) {
		if (isClearWater(level, pos))
			return WATER_CLEAR;
		float f = (float)ReikaMathLibrary.normalizeToBounds(
				WATER_COLOR_MIX.getValue(pos.getX(), pos.getZ()), 0, 1);
		return ReikaColorAPI.mixColors(WATER_TEAL, WATER_PINK, f);
	}

	/**
	 * V33a {@code isClearWater}: shallow high water, flowing water, unlit water, or water with two
	 * or more air neighbours reads as clear rather than coloured.
	 */
	public static boolean isClearWater(BlockAndTintGetter level, BlockPos pos) {
		if (pos.getY() > 65 && waterDepth(level, pos) <= 3)
			return true;
		FluidState fluid = level.getBlockState(pos).getFluidState();
		if (!fluid.isEmpty() && !fluid.isSource())
			return true;
		if (level.getBrightness(LightLayer.SKY, pos) < 2)
			return true;
		return countAdjacentAir(level, pos) >= 2;
	}

	/** Blocks of water above this position, matching V33a's {@code ReikaWorldHelper.getWaterDepth}. */
	private static int waterDepth(BlockAndTintGetter level, BlockPos pos) {
		BlockPos.MutableBlockPos cursor = pos.mutable();
		int depth = 0;
		while (depth <= 4) {
			FluidState fluid = level.getBlockState(cursor).getFluidState();
			if (fluid.isEmpty() || !fluid.getType().isSame(Fluids.WATER))
				break;
			depth++;
			cursor.move(Direction.UP);
		}
		return depth;
	}

	private static int countAdjacentAir(BlockAndTintGetter level, BlockPos pos) {
		int count = 0;
		for (Direction dir : Direction.values()) {
			if (level.getBlockState(pos.relative(dir)).isAir())
				count++;
		}
		return count;
	}

	/** Applies the V33a shift, faded in over the biome border by {@link #cliffStrength}. */
	public static int applyCliffShift(int base, BlockAndTintGetter level, BlockPos pos) {
		float strength = cliffStrength(level, pos);
		if (strength <= 0)
			return base;
		int shifted = shiftTerrainColor(base, pos);
		return strength >= 1 ? shifted : ReikaColorAPI.mixColors(shifted, base, strength);
	}

	/**
	 * Wraps an existing tint source so that inside the Luminous Cliffs the V33a hue/altitude shift
	 * is applied to whatever colour the wrapped source produced, and outside it nothing changes.
	 */
	public static BlockTintSource wrapTerrain(BlockTintSource delegate) {
		return new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return delegate.color(state);
			}

			@Override
			public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				return applyCliffShift(delegate.colorInWorld(state, level, pos), level, pos);
			}

			@Override
			public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				int base = delegate.colorAsTerrainParticle(state, level, pos);
				return base == -1 ? base : applyCliffShift(base, level, pos);
			}

			@Override
			public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
				return delegate.relevantProperties();
			}
		};
	}
}
