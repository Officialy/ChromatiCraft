package reika.chromaticraft.client;

import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Texture lookup for the individual rune glyphs V33a exposed through {@code CrystalElement} icons.
 *
 * <p>The similarly named {@code runes/template/<colour>.png} files are 16-by-1024 animated block
 * texture strips. They must never be sampled as GUI glyphs; doing so produces a stack of dark
 * horizontal frames. GUI and HUD rune displays use these sixteen static outline sprites instead.
 */
public final class CrystalRuneTextures {

	private CrystalRuneTextures() {}

	public static Identifier outline(CrystalElement element) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"textures/block/runes/outline/tile" + element.ordinal() + "_0.png");
	}

	public static Identifier glow(CrystalElement element) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"textures/block/runes/glow/tile" + element.ordinal() + "_0.png");
	}
}
