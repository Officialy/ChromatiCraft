package reika.chromaticraft.world.dimension;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.registry.ChromaDimensions;

/**
 * V33a {@code DimensionTuningManager}: how strongly Proxima responds to the player who just arrived.
 *
 * <p>Tuning is set once per trip, from the energy the Portal Rift had absorbed from Proximal Essence,
 * and stored on the player's persistent data so it survives the dimension change and a relog. Its two
 * consumers are the drop-rate curve and the {@link TuningThresholds} gates, which decide which parts
 * of the dimension are willing to appear at all.
 */
public final class DimensionTuningManager {

	private static final String NBT_TAG = "dimensionTuning";

	public static final DimensionTuningManager instance = new DimensionTuningManager();

	private DimensionTuningManager() {}

	public void tunePlayer(Player ep, int amt) {
		ep.getPersistentData().putInt(NBT_TAG, amt);
	}

	public int getPlayerTuning(Player ep) {
		return ep.getPersistentData().getIntOr(NBT_TAG, 0);
	}

	/**
	 * V33a getTunedDropRates. Untuned returns -1 (a sentinel meaning "no tuning at all"), up to 16 is
	 * a flat zero, 16-64 ramps linearly to 1x, 64-576 climbs to 3x, and past that it follows the
	 * source's {@code 2-23+24*(tune/576)^0.04} curve.
	 */
	public float getTunedDropRates(Player ep) {
		int tune = this.getPlayerTuning(ep);
		if (tune <= 0)
			return -1;
		if (tune <= 16)
			return 0;
		if (tune <= 64)
			return (tune - 16) / 48F;
		if (tune <= 576)
			return 1 + (tune - 64) / 256F;
		return 2 - 23 + 24 * (float)Math.pow(tune / 576F, 0.04);
	}

	public int getTunedDropCount(Player ep, int base, int min, int max) {
		if (ep.level().dimension() != ChromaDimensions.PROXIMA)
			return base;
		return Mth.clamp((int)(base * this.getTunedDropRates(ep)), min, max);
	}

	public enum TuningThresholds {
		STRUCTURES(192),
		STRUCTUREBIOMES(96),
		SKYRIVER(384, 256),
		FARREGIONS(512),
		DECOHARVEST(4),
		CHESTS(16),
		MONUMENT(224);

		public final int minimumTuning;
		public final int minimumEffect;

		public static final TuningThresholds[] list = values();

		TuningThresholds(int t) {
			this(t, 0);
		}

		TuningThresholds(int t, int e) {
			minimumTuning = t;
			minimumEffect = e;
		}

		public float getTuningFraction(Player ep) {
			int amt = instance.getPlayerTuning(ep);
			if (amt < minimumEffect)
				return 0;
			if (amt >= minimumTuning)
				return 1;
			return (amt - minimumEffect) / (float)(minimumTuning - minimumEffect);
		}

		/** Outside Proxima nothing is gated, exactly as V33a short-circuited on the dimension id. */
		public boolean isSufficientlyTuned(Player ep) {
			if (ep.level().dimension() != ChromaDimensions.PROXIMA)
				return true;
			return instance.getPlayerTuning(ep) >= minimumTuning;
		}
	}
}
