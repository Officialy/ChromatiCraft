package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.chromaticraft.world.dimension.biome.ProximaBiomes;
import reika.chromaticraft.world.dimension.biome.ProximaSubBiomes;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a's per-position grass and foliage colour for Proxima.
 *
 * <p>Every Proxima biome extends {@code ChromaDimensionBiome}, which overrides
 * {@code getBiomeGrassColor(x,y,z)} to take the overworld Rainbow Forest's grass and push its green
 * channel between 1.0x and 1.5x by a simplex field sampled at one eighth scale. Structure Field and
 * Monument Field replace that with an edge-aware rule of their own. A 26.2 {@code BiomeSpecialEffects}
 * grass override is a single constant, so the biome definitions carry the base colours and this class
 * carries the modulation — the same split, and the same mechanism, as
 * {@link LuminousCliffsColors}.
 *
 * <h2>The edge rule, feathered rather than stepped</h2>
 *
 * {@code StructureBiome.getBiomeGrassColor} scans four blocks in each cardinal direction and, if any
 * of them is a different biome, returns the highlight colour outright; only deeper inside does it
 * blend base and highlight by a second noise field. That hard switch worked in 1.7.10 because the
 * underlying grass colour was itself unblended. In 26.2 the base arrives already feathered across the
 * biome border by the client's blend radius, so a hard switch would leave a visible seam against it.
 * The biome-presence {@link ColorResolver} below is blended by the same radius, which gives a
 * strength that is 0 outside, 1 well inside and feathered exactly across the border region V33a was
 * scanning for — so the highlight is mixed in by that strength instead. Same two end states, no seam.
 *
 * <h2>Why the structure colours are computed from a constant</h2>
 *
 * V33a's {@code getBaseColor} is explicitly the Rainbow Forest's grass, not the biome's own colour, so
 * these read {@link ChromaBiomes#FOREST_GRASS} directly rather than the delegate's output. The static
 * overrides in the biome definitions are representative values for anything that cannot run this
 * resolver; re-applying the brightening on top of them would double-count it.
 */
public final class ProximaBiomeColors {

	/** V33a ChromaDimensionBiome.grassColor: wall-clock seeded, so the palette differs per session. */
	private static final NoiseGeneratorBase GRASS_SHIFT =
			new SimplexNoiseGenerator(System.currentTimeMillis());
	/** V33a MonumentBiome.colorBlend: simplex at 1/32 driving the monument's two-tone wash. */
	private static final NoiseGeneratorBase MONUMENT_BLEND =
			new SimplexNoiseGenerator(System.currentTimeMillis()).setFrequency(1 / 32D);

	/** V33a MonumentBiome.getBaseColor / getHighlightColor. */
	private static final int MONUMENT_BASE = 0x77CCFF;
	private static final int MONUMENT_HIGHLIGHT = 0xFF77CC;

	private ProximaBiomeColors() {}

	/**
	 * Biome-presence probe, one bit per family packed into a colour channel so all three feather
	 * independently across their borders: red is "any Proxima biome", green is Structure Field, blue
	 * is Monument Field. Read through {@link BlockAndTintGetter#getBlockTint}, which is thread-safe,
	 * per-column cached and already applies the player's biome-blend radius.
	 */
	public static final ColorResolver PROXIMA_PRESENCE = (biome, x, z) -> {
		Identities ids = identities();
		if (ids == null)
			return 0;
		int color = 0;
		if (ids.isProxima(biome))
			color |= 0xFF0000;
		if (biome == ids.structure())
			color |= 0x00FF00;
		if (biome == ids.monument())
			color |= 0x0000FF;
		return color;
	};

	/**
	 * A {@link ColorResolver} receives a bare {@link Biome} with no registry key, so the Proxima
	 * biomes are resolved once from the connected level's registry and compared by identity.
	 *
	 * <p>Written on the client thread on level load, read from chunk-mesh worker threads, so it is a
	 * single volatile reference: a torn read across a world switch would be latched into the block
	 * tint cache, which only invalidates per chunk, and the wrong answer would survive until that
	 * chunk reloaded.
	 */
	private record Identities(Registry<Biome> registry, java.util.Set<Biome> all, Biome structure,
			Biome monument) {
		boolean isProxima(Biome biome) {
			return all.contains(biome);
		}
	}

	private static volatile Identities identities;

	private static Identities identities() {
		Identities current = identities;
		if (current != null)
			return current;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return null;
		return resolve(mc.level.registryAccess().lookupOrThrow(Registries.BIOME));
	}

	private static Identities resolve(Registry<Biome> registry) {
		java.util.Set<Biome> all = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (ProximaBiomes b : ProximaBiomes.biomeList)
			all.add(registry.getValue(b.biomeKey()));
		for (ProximaSubBiomes b : ProximaSubBiomes.biomeList)
			all.add(registry.getValue(b.biomeKey()));
		all.remove(null);
		Identities result = new Identities(registry, all,
				registry.getValue(ProximaBiomes.STRUCTURE.biomeKey()),
				registry.getValue(ProximaBiomes.MONUMENT.biomeKey()));
		identities = result;
		return result;
	}

	/** Re-resolves the biome identities when the client joins or leaves a level. */
	public static void onLevelChanged(net.minecraft.client.multiplayer.ClientLevel level) {
		identities = level == null ? null
				: resolve(level.registryAccess().lookupOrThrow(Registries.BIOME));
	}

	/** V33a: the green channel wanders between 1.0x and 1.5x on a one-eighth-scale simplex field. */
	private static float grassFactor(int x, int z) {
		return (float)ReikaMathLibrary.normalizeToBounds(GRASS_SHIFT.getValue(x / 8D, z / 8D), 1, 1.5);
	}

	/** V33a MonumentBiome.getBlendFactor: a 1/32 simplex normalised to 0-1 and raised to the 1.5. */
	private static float monumentBlend(int x, int z) {
		return (float)Math.pow(ReikaMathLibrary.normalizeToBounds(
				MONUMENT_BLEND.getValue(x, z), 0, 1), 1.5);
	}

	/** V33a StructureBiome.getHighlightColor: the base with green 2.5x and blue 1.5x. */
	private static int structureHighlight(int base) {
		return ReikaColorAPI.multiplyChannels(base, 1, 2.5F, 1.5F);
	}

	/**
	 * The full V33a resolution for one position, given how strongly it belongs to each family.
	 *
	 * @param delegate the biome-blended colour the wrapped tint source produced
	 */
	private static int resolve(int delegate, BlockAndTintGetter level, BlockPos pos) {
		int presence = level.getBlockTint(pos, PROXIMA_PRESENCE);
		float proxima = ((presence >> 16) & 255) / 255F;
		if (proxima <= 0)
			return delegate;
		float structure = ((presence >> 8) & 255) / 255F;
		float monument = (presence & 255) / 255F;

		int x = pos.getX();
		int z = pos.getZ();
		int shifted;
		if (structure > 0 || monument > 0) {
			float blend = monumentBlend(x, z);
			int base = ChromaBiomes.FOREST_GRASS;
			int highlight = structureHighlight(base);
			if (monument > 0) {
				// V33a MonumentBiome mixes its own two tones over the Structure Field's by the same
				// noise, and the monument strength additionally fades that where the two regions meet.
				base = ReikaColorAPI.mixColors(
						ReikaColorAPI.mixColors(MONUMENT_BASE, base, blend), base, monument);
				highlight = ReikaColorAPI.mixColors(
						ReikaColorAPI.mixColors(MONUMENT_HIGHLIGHT, highlight, blend), highlight, monument);
			}
			// Deep inside, V33a blends the brightened base toward the highlight by a quarter-scale
			// noise field; at the border it uses the highlight outright. The presence strength is what
			// feathers between those two, in place of upstream's four-block scan.
			float interiorMix = (float)ReikaMathLibrary.normalizeToBounds(
					GRASS_SHIFT.getValue(x / 4D, z / 4D), 0, 1);
			int interior = ReikaColorAPI.mixColors(highlight,
					ReikaColorAPI.multiplyChannels(base, 1, 1.5F, 1), interiorMix);
			float edge = Math.max(structure, monument);
			shifted = ReikaColorAPI.mixColors(interior, highlight, edge);
		}
		else {
			shifted = ReikaColorAPI.multiplyChannels(delegate, 1, grassFactor(x, z), 1);
		}
		return proxima >= 1 ? shifted : ReikaColorAPI.mixColors(shifted, delegate, proxima);
	}

	/**
	 * Wraps an existing tint source so that inside Proxima the V33a modulation is applied to whatever
	 * colour the wrapped source produced, and outside it nothing changes.
	 */
	public static BlockTintSource wrapTerrain(BlockTintSource delegate) {
		return new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return delegate.color(state);
			}

			@Override
			public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				return resolve(delegate.colorInWorld(state, level, pos), level, pos);
			}

			@Override
			public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				int base = delegate.colorAsTerrainParticle(state, level, pos);
				return base == -1 ? base : resolve(base, level, pos);
			}

			@Override
			public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
				return delegate.relevantProperties();
			}
		};
	}
}
