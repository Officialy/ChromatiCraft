package reika.chromaticraft.magic.castingtuning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.ReikaDirectionHelper.FanDirections;

/** Deterministic V33a casting-key generator and per-player runtime cache. */
public final class CastingTuningRegistry {

	public static final CastingTuningRegistry instance = new CastingTuningRegistry();
	private final Map<FanDirections, BlockPos> tuningPositions = new EnumMap<>(FanDirections.class);
	private final Map<UUID, CastingTuningKey> keys = new ConcurrentHashMap<>();

	private CastingTuningRegistry() {
		for (FanDirections direction : FanDirections.list) {
			if (direction.isCardinal()) continue;
			int scale = direction.isOctagonal() ? 6 : 3;
			tuningPositions.put(direction, new BlockPos(direction.directionX * scale, 0, direction.directionZ * scale));
		}
	}

	public CastingTuningKey getTuningKey(Level level, UUID playerId) {
		return keys.computeIfAbsent(playerId, id -> this.calculate(level, id));
	}

	private CastingTuningKey calculate(Level level, UUID playerId) {
		CastingTuningKey key = new CastingTuningKey(playerId);
		if (playerId.equals(DragonAPI.Reika_UUID)) {
			put(key, FanDirections.WNW, CrystalElement.RED); put(key, FanDirections.NW, CrystalElement.BLACK);
			put(key, FanDirections.NNW, CrystalElement.BLUE); put(key, FanDirections.NNE, CrystalElement.BLACK);
			put(key, FanDirections.NE, CrystalElement.LIME); put(key, FanDirections.ENE, CrystalElement.YELLOW);
			put(key, FanDirections.ESE, CrystalElement.WHITE); put(key, FanDirections.SE, CrystalElement.LIGHTBLUE);
			put(key, FanDirections.SSE, CrystalElement.BLACK); put(key, FanDirections.SSW, CrystalElement.MAGENTA);
			put(key, FanDirections.SW, CrystalElement.PURPLE); put(key, FanDirections.WSW, CrystalElement.RED);
			return key;
		}

		// Preserve V33a's effective seed calculation, including its LSB xor with itself.
		long seed = playerId.getLeastSignificantBits() ^ playerId.getLeastSignificantBits();
		if (level instanceof ServerLevel serverLevel)
			seed += serverLevel.getServer().getDefaultGameType().getId() * 237617L;
		Random random = new Random(seed);
		random.nextBoolean(); random.nextBoolean();
		List<BlockPos> positions = new ArrayList<>(tuningPositions.values());
		Collections.sort(positions);
		Collections.shuffle(positions, random);
		for (BlockPos position : positions) key.putRune(position, CrystalElement.elements[random.nextInt(16)]);
		return key;
	}

	private void put(CastingTuningKey key, FanDirections direction, CrystalElement element) {
		key.putRune(tuningPositions.get(direction), element);
	}

	public List<BlockPos> locations() {
		List<BlockPos> result = new ArrayList<>(tuningPositions.values());
		Collections.sort(result);
		return List.copyOf(result);
	}

	public Map<FanDirections, BlockPos> compassLocations() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(tuningPositions));
	}
}
