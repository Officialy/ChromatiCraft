package reika.chromaticraft.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.biome.ChromaBiomes;
import reika.dragonapi.instantiable.event.client.WaterColorEvent;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V33a {@code ChromaClientEventController.biomeWaterColor}: the overworld biomes' positional water.
 *
 * <p>Upstream's listener recoloured water in two biome families and deferred to the biome for the
 * colour itself. The Glowing Cliffs' water retains a softer teal tint at shallow and flowing edges and otherwise drifts
 * between teal and pink ({@link LuminousCliffsColors#waterColor}); the Rainbow Stream's mixes the
 * Rainbow Forest's cyan toward pure blue on simplex noise. The Rainbow Forest itself returned its flat
 * cyan, which is already the biome's static water colour here, so it needs nothing.
 *
 * <p>The Ender Forest was never in that listener: its water came from the biome's water multiplier,
 * which V33a's event default averaged over a 3x3. Its only non-default case is the {@code ENDERCOLORING}
 * pink, and since 26.3 cannot make a biome's static colour depend on a config option it is applied here
 * alongside the others.
 *
 * <p>V33a switched hard at the biome edge. Each recolour here is instead faded in by that biome's
 * presence strength, because the colour it replaces has already been blended across the border by
 * vanilla and a hard switch on top of a blended base leaves a visible seam.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ChromaWaterColors {

	/** V33a {@code RainbowRiver.waterColorMix}: wall-clock seeded, as upstream's was, at 1/8. */
	private static final NoiseGeneratorBase STREAM_MIX =
			new SimplexNoiseGenerator(System.currentTimeMillis() * 2387).setFrequency(1 / 8D);
	/** V33a {@code RainbowRiver}'s blue end of the mix. */
	private static final int STREAM_BLUE = 0x0000ff;
	/** Some LOD tint views expose biome color but reject all block and light reads. */
	private static final Set<Class<?>> TINT_ONLY_GETTERS = ConcurrentHashMap.newKeySet();

	private ChromaWaterColors() {}

	@SubscribeEvent
	public static void onWaterColor(WaterColorEvent event) {
		BlockAndTintGetter level = event.access;
		if (TINT_ONLY_GETTERS.contains(level.getClass()))
			return;
		BlockPos pos = event.pos;
		int color = event.color;

		try {
			float cliffs = LuminousCliffsColors.cliffStrength(level, pos);
			if (cliffs > 0)
				color = blend(LuminousCliffsColors.waterColor(level, pos), color, cliffs);

			float stream = OverworldBiomePresence.strength(level, pos, OverworldBiomePresence.RAINBOW_STREAM);
			if (stream > 0)
				color = blend(streamColor(pos), color, stream);

			event.color = EnderForestColors.applyWater(color, level, pos);
		}
		catch (UnsupportedOperationException ex) {
			if (ex.getMessage() == null || !ex.getMessage().contains("Object is for tinting only"))
				throw ex;
			// A tint-only view cannot answer V33a's depth, flow, light, or neighbour queries.
			// Keep the incoming biome color and avoid repeating exceptions for every LOD cell.
			TINT_ONLY_GETTERS.add(level.getClass());
		}
	}

	/**
	 * V33a {@code RainbowRiver.getWaterColor}: the Rainbow Forest's cyan mixed toward blue by noise
	 * normalised to 0.5-1, so the stream is never more than half blue.
	 */
	public static int streamColor(BlockPos pos) {
		float f = (float)ReikaMathLibrary.normalizeToBounds(STREAM_MIX.getValue(pos.getX(), pos.getZ()), 0.5, 1);
		return ReikaColorAPI.mixColors(ChromaBiomes.WATER_COLOR, STREAM_BLUE, f);
	}

	private static int blend(int recolour, int original, float strength) {
		int mixed = strength >= 1 ? recolour : ReikaColorAPI.mixColors(recolour, original, strength);
		return (original & 0xff000000) | (mixed & 0xffffff);
	}
}
