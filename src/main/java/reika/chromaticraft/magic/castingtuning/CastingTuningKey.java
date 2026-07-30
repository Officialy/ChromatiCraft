package reika.chromaticraft.magic.castingtuning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.DragonAPI;

/** Immutable server-side form of the V33a player-specific casting tuning key. */
public final class CastingTuningKey {
	private static final int ICON_COUNT = 16;
	private final Map<BlockPos, CrystalElement> runes = new LinkedHashMap<>();
	private final UUID playerId;
	private final int iconIndex;

	CastingTuningKey(UUID playerId) {
		this.playerId = playerId;
		if (playerId.equals(DragonAPI.Reika_UUID)) iconIndex = 12;
		else {
			int value = Math.floorMod(playerId.hashCode(), ICON_COUNT - 1);
			iconIndex = value >= 12 ? value + 1 : value;
		}
	}

	void putRune(BlockPos offset, CrystalElement element) { runes.put(offset.immutable(), element); }
	public UUID playerId() { return playerId; }
	public int iconIndex() { return iconIndex; }
	public Map<BlockPos, CrystalElement> runes() { return Collections.unmodifiableMap(runes); }
	public boolean matches(Map<BlockPos, CrystalElement> actual) { return runes.equals(actual); }
}
