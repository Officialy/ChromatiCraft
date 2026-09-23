package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

import reika.chromaticraft.world.biome.ChromaBiomes;

/**
 * "Is this position in the Ender Forest / Rainbow Stream", answerable from a chunk-meshing thread.
 *
 * <p>A {@link BlockAndTintGetter} has no biome lookup, but it does have
 * {@link BlockAndTintGetter#getBlockTint}, which is thread-safe, cached per column, and blended across
 * the player's biome-blend radius. A {@link ColorResolver} that answers white inside the biome and black
 * outside therefore doubles as a presence probe whose value fades in over the border — which is what a
 * recolour layered over an already-blended base colour needs, or it shows a seam. This is the same
 * mechanism {@link LuminousCliffsColors#CLIFF_PRESENCE} uses, for the two other overworld biomes whose
 * V33a colours were positional.
 *
 * <p>A resolver is handed a bare {@link Biome} with no registry key, so each biome instance is looked
 * up once from the connected level's registry and compared by identity.
 */
public final class OverworldBiomePresence {

	private OverworldBiomePresence() {}

	private record Identities(Registry<Biome> registry, Biome enderForest, Biome rainbowStream) {}

	/** Written on the client thread, read from meshing threads; one volatile reference, never torn. */
	private static volatile Identities identities;

	public static final ColorResolver ENDER_FOREST =
			(biome, x, z) -> biome == identity(ChromaBiomes.ENDER_FOREST) ? 0xffffff : 0x000000;

	public static final ColorResolver RAINBOW_STREAM =
			(biome, x, z) -> biome == identity(ChromaBiomes.RAINBOW_STREAM) ? 0xffffff : 0x000000;

	private static Biome identity(ResourceKey<Biome> key) {
		Identities current = identities;
		if (current == null) {
			var level = Minecraft.getInstance().level;
			if (level == null)
				return null;
			Registry<Biome> registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
			current = new Identities(registry, registry.getValue(ChromaBiomes.ENDER_FOREST),
					registry.getValue(ChromaBiomes.RAINBOW_STREAM));
			identities = current;
		}
		return key == ChromaBiomes.ENDER_FOREST ? current.enderForest() : current.rainbowStream();
	}

	/** Drops the cached identities when the client leaves a level. */
	public static void onLevelChanged() {
		identities = null;
	}

	/** 0 outside, 1 fully inside, feathered across the blend radius at the border. */
	public static float strength(BlockAndTintGetter level, BlockPos pos, ColorResolver probe) {
		return (level.getBlockTint(pos, probe) & 255) / 255F;
	}
}
