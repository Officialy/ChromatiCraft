package reika.chromaticraft.world.dimension.structure.lightpanel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * Server-safe loader for V33a's seven authored Glowing Logic pattern sheets.
 *
 * <p>The PNGs are data, not decoration: every coloured pixel describes a switch-to-row connection.
 * Keeping them as the source of truth preserves Reika's vetted solvable layouts and avoids inventing
 * runtime puzzles. Loading is lazy so an unrelated data-generation invocation does not initialize AWT.
 */
public final class LightPanelPatternLibrary {

	private static final int[][] SIZES = {
			{3, 3}, {4, 4}, {4, 6}, {4, 8}, {5, 8}, {6, 10}, {8, 12}
	};
	private static volatile List<List<FixedLightPattern>> patterns;

	private LightPanelPatternLibrary() {}

	public static int tierCount() {
		return SIZES.length;
	}

	public static int switchCount(int tier) {
		return size(tier)[0];
	}

	public static int rowCount(int tier) {
		return size(tier)[1];
	}

	public static List<FixedLightPattern> patterns(int tier) {
		checkTier(tier);
		List<List<FixedLightPattern>> loaded = patterns;
		if (loaded == null) {
			synchronized (LightPanelPatternLibrary.class) {
				loaded = patterns;
				if (loaded == null)
					patterns = loaded = loadAll();
			}
		}
		return loaded.get(tier);
	}

	/** Draws without replacement from a caller-owned pool, matching V33a's per-structure selection. */
	public static FixedLightPattern takeRandom(List<FixedLightPattern> pool, Random random) {
		if (pool.isEmpty())
			throw new IllegalStateException("No Glowing Logic patterns remain in this tier");
		int index = random.nextInt(pool.size());
		FixedLightPattern pattern = pool.get(index);
		if (pool.size() > 1)
			pool.remove(index);
		return pattern;
	}

	public static boolean hasSolution(FixedLightPattern pattern) {
		int states = 1 << pattern.switchCount;
		for (int mask = 0; mask < states; mask++)
			if (isSolved(pattern, mask))
				return true;
		return false;
	}

	/** V33a completion rule: all green; every red must be cancelled by blue. */
	public static boolean isSolved(FixedLightPattern pattern, int activeSwitchMask) {
		for (int row = 0; row < pattern.rowCount; row++) {
			boolean target = false;
			boolean block = false;
			boolean cancel = false;
			for (int sw = 0; sw < pattern.switchCount; sw++) {
				if ((activeSwitchMask & 1 << sw) == 0)
					continue;
				LightGroup group = pattern.getConnections(sw);
				target |= group.containsLight(row, LightType.TARGET);
				block |= group.containsLight(row, LightType.BLOCK);
				cancel |= group.containsLight(row, LightType.CANCEL);
			}
			if (!target || block && !cancel)
				return false;
		}
		return true;
	}

	private static List<List<FixedLightPattern>> loadAll() {
		List<List<FixedLightPattern>> tiers = new ArrayList<>(SIZES.length);
		for (int tier = 0; tier < SIZES.length; tier++)
			tiers.add(Collections.unmodifiableList(loadTier(tier)));
		return Collections.unmodifiableList(tiers);
	}

	private static List<FixedLightPattern> loadTier(int tier) {
		int switches = switchCount(tier);
		int rows = rowCount(tier);
		String path = "/assets/" + ChromatiCraft.MODID + "/structures/lightpanel/tier" + tier + ".png";
		try (InputStream stream = ChromatiCraft.class.getResourceAsStream(path)) {
			if (stream == null)
				throw new IllegalStateException("Missing Glowing Logic pattern sheet " + path);
			BufferedImage image = ImageIO.read(stream);
			if (image == null)
				throw new IllegalStateException("Unreadable Glowing Logic pattern sheet " + path);
			List<FixedLightPattern> found = decode(tier, rows, switches, image);
			if (found.isEmpty())
				throw new IllegalStateException("Glowing Logic tier " + tier + " has no valid patterns");
			for (FixedLightPattern pattern : found)
				if (!hasSolution(pattern))
					throw new IllegalStateException("Glowing Logic tier " + tier
							+ " contains an unsolvable authored pattern: " + pattern);
			return found;
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not load Glowing Logic tier " + tier, e);
		}
	}

	private static List<FixedLightPattern> decode(int tier, int rows, int switches,
			BufferedImage image) {
		List<FixedLightPattern> found = new ArrayList<>();
		int strideX = LightType.list.length * switches + switches + 2;
		int baseX = 1;
		int baseY = 1;
		while (baseX < image.getWidth()) {
			FixedLightPattern pattern = new FixedLightPattern(tier, rows, switches);
			for (int sw = 0; sw < switches; sw++) {
				for (int row = 0; row < rows; row++) {
					for (int typeIndex = 0; typeIndex < LightType.list.length; typeIndex++) {
						LightType type = LightType.list[typeIndex];
						int x = baseX + 1 + typeIndex + sw * (LightType.list.length + 1);
						int y = baseY + 1 + row;
						if (x < image.getWidth() && y < image.getHeight()
								&& ReikaMathLibrary.clipLeadingHexBits(
										image.getRGB(x, y) & type.renderColor) == 0xff)
							pattern.connect(sw, row, type);
					}
				}
			}
			if (!pattern.isEmpty())
				found.add(pattern);
			baseX += strideX;
		}
		return found;
	}

	private static int[] size(int tier) {
		checkTier(tier);
		return SIZES[tier];
	}

	private static void checkTier(int tier) {
		if (tier < 0 || tier >= SIZES.length)
			throw new IllegalArgumentException("Glowing Logic tier " + tier);
	}
}
