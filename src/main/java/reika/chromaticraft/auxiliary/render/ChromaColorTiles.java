package reika.chromaticraft.auxiliary.render;

import java.util.Arrays;

/**
 * V33a {@code ChromaFX.getChromaColorTiles}: the chroma colour ramp (its colours, then the same colours back again), which
 * the pylon compass's power-crystal diamonds and other shimmering effects cycle through. Split out so ported code can
 * use it while the rest of {@code ChromaFX} is still unported; that class's copy should delegate here once ported.
 */
public final class ChromaColorTiles {

	private static final int[] TILES = {
			0x94DE5C, 0xC5D471, 0xE6C192, 0xF9A9B0, 0xFE8CC8, 0xFC71D5, 0xF061D8, 0xD563D1,
			0xAC76C0, 0x7290AA, 0x28A792, 0x00B781, 0x00C47E, 0x18CD8B, 0x58CF9F, 0x96C8B2,
			0xBEBCC0, 0xD9AEC8, 0xE99EC8, 0xF090C1, 0xED87B5, 0xE186A8, 0xCE8B9C, 0xB49596,
			0x97A298, 0x78ADA0, 0x60B6AA, 0x56BDB5, 0x60C0BC, 0x77BFBE, 0x8FB8BB, 0x98ACBA,
	};

	private ChromaColorTiles() {}

	public static int[] getChromaColorTiles() {
		int[] ret = Arrays.copyOf(TILES, TILES.length*2);
		for (int i = 0; i < TILES.length; i++)
			ret[TILES.length+i] = TILES[TILES.length-1-i];
		return ret;
	}

}
