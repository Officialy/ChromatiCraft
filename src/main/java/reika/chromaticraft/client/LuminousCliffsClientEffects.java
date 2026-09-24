package reika.chromaticraft.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.instantiable.event.client.FluidModelEvent;
import reika.dragonapi.instantiable.event.client.SkyColorEvent;
import reika.dragonapi.instantiable.event.client.WeatherSkyStrengthEvent;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code ChromaClientEventController}'s Luminous Cliffs atmosphere.
 *
 * <p>{@code BiomeGlowingCliffs.renderFactor} fades from 0 to 1 over two seconds as the player walks into
 * the cliffs and back out over four. It drives the rest: the weather's darkening of the sky is taken away
 * by that much (the cliffs never rain), and the fog is tinted toward a pale violet, strongest at noon.
 * While the player stands in the cliffs, the sky's violet hue wanders with their position and thunder is
 * silent. Cave ambience in the cliffs is swapped for one of the biome's own four ambience tracks, and the
 * cliffs' clear shallows are drawn with their own water texture.
 *
 * <p>Not ported: the Winter-mod sky and fog colours ({@code WinterSkyColorsEvent}), for a mod with no
 * 26.2 release.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class LuminousCliffsClientEffects {

	/** V33a {@code BiomeGlowingCliffs.renderFactor}. */
	private static volatile float renderFactor;

	private static final Identifier CLEAR_STILL = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/fluid/glowcliffwater_still");
	private static final Identifier CLEAR_FLOW = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/fluid/glowcliffwater_flow");
	private static final Map<FluidModel, FluidModel> CLEAR_MODELS = new ConcurrentHashMap<>();

	private static final ChromaSounds[] AMBIENCE = {
			ChromaSounds.CLIFFSOUND, ChromaSounds.CLIFFSOUND2, ChromaSounds.CLIFFSOUND3, ChromaSounds.CLIFFSOUND4,
	};

	private LuminousCliffsClientEffects() {}

	public static float renderFactor() {
		return renderFactor;
	}

	/** Whether the column's surface biome is the cliffs, as 1.7.10's per-column biome was asked. */
	private static boolean isLuminousCliffs(double x, double z) {
		var level = Minecraft.getInstance().level;
		if (level == null)
			return false;
		int bx = (int)Math.floor(x);
		int bz = (int)Math.floor(z);
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
		return level.getBiome(new BlockPos(bx, y, bz)).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS);
	}

	/** V33a {@code updateRenderFactor}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void updateGlowCliffRendering(ClientTickEvent.Post event) {
		var player = Minecraft.getInstance().player;
		if (player == null || player.level() == null)
			renderFactor = Math.max(0, renderFactor - 0.1F);
		else if (isLuminousCliffs(player.getX(), player.getZ()))
			renderFactor = Math.min(1, renderFactor + 0.025F);
		else
			renderFactor = Math.max(0, renderFactor - 0.0125F);
	}

	/** V33a {@code preventCliffWeatherSky}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void preventCliffWeatherSky(WeatherSkyStrengthEvent event) {
		event.returnValue *= 1 - renderFactor;
	}

	/** V33a {@code preventCliffWeatherSky(FogColors)}: violet fog, strongest in full daylight. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void cliffFogColor(ViewportEvent.ComputeFogColor event) {
		var level = Minecraft.getInstance().level;
		if (level == null || Minecraft.getInstance().player == null)
			return;
		FogType fog = event.getCamera().getFluidInCamera();
		if (fog == FogType.LAVA || fog == FogType.WATER)
			return;
		float sun = ReikaWorldHelper.getSunIntensity(level, false, (float)event.getPartialTick());
		float f = renderFactor * (float)ReikaMathLibrary.normalizeToBounds(sun, 0, 1, 0.2, 0.8);
		if (f > 0) {
			event.setRed(event.getRed() * (1 - f) + f * 0.9F);
			event.setGreen(event.getGreen() * (1 - f) + f * 0.7F);
			event.setBlue(event.getBlue() * (1 - f) + f * 1F);
		}
	}

	/**
	 * V33a {@code BiomeGlowingCliffs.getSkyColorByTemp}: the violet sky's hue shifted by up to 15 degrees
	 * of simplex noise at the player's position. Upstream's sky was the biome's own; here it is blended in
	 * by the render factor so it arrives with the biome rather than switching.
	 */
	@SubscribeEvent
	public static void cliffSkyColor(SkyColorEvent event) {
		float f = renderFactor;
		var player = Minecraft.getInstance().player;
		if (f <= 0 || player == null)
			return;
		int shifted = LuminousCliffsColors.skyColor(player.getX(), player.getZ());
		int mixed = reika.dragonapi.libraries.rendering.ReikaColorAPI.mixColors(shifted, event.color, f);
		event.color = (event.color & 0xFF000000) | (mixed & 0xFFFFFF);
	}

	/** V33a {@code preventCliffThunder} and {@code preventCliffAmbience}. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void cliffSounds(PlaySoundEvent event) {
		SoundInstance sound = event.getSound();
		if (sound == null)
			return;
		String name = sound.getIdentifier().getPath();
		if (name.contains("lightning_bolt.thunder")) {
			var player = Minecraft.getInstance().player;
			if (player != null && isLuminousCliffs(player.getX(), player.getZ()))
				event.setSound(null);
		}
		else if (name.startsWith("ambient.cave") && isLuminousCliffs(sound.getX(), sound.getZ())) {
			var rand = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getRandom()
					: net.minecraft.util.RandomSource.create();
			ChromaSounds s = AMBIENCE[rand.nextInt(AMBIENCE.length)];
			event.setSound(new SimpleSoundInstance(s.getSoundEvent(), SoundSource.AMBIENT, 1, 1, rand,
					sound.getX(), sound.getY(), sound.getZ()));
		}
	}

	/**
	 * V33a {@code retextureGlowCliffWater}: the cliffs' clear water (see
	 * {@link LuminousCliffsColors#isClearWater}) is drawn with its own still and flowing textures.
	 */
	@SubscribeEvent
	public static void retextureGlowCliffWater(FluidModelEvent event) {
		if (!event.fluidState.is(FluidTags.WATER))
			return;
		if (LuminousCliffsColors.cliffStrength(event.access, event.pos) < 0.5F
				|| !LuminousCliffsColors.isClearWater(event.access, event.pos))
			return;
		event.model = CLEAR_MODELS.computeIfAbsent(event.model, LuminousCliffsClientEffects::clearModel);
	}

	private static FluidModel clearModel(FluidModel original) {
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite still = atlas.getSprite(CLEAR_STILL);
		TextureAtlasSprite flow = atlas.getSprite(CLEAR_FLOW);
		return new FluidModel(original.layer(), new Material.Baked(still, false), new Material.Baked(flow, false),
				original.overlayMaterial(), original.fluidTintSource(), original.customRenderer());
	}
}
