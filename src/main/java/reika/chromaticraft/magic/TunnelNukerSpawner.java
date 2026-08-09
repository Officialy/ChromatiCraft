package reika.chromaticraft.magic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import reika.chromaticraft.entity.EntityTunnelNuker;
import reika.chromaticraft.magic.lore.LoreTowerProgress;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.registry.ChromaEntityTypes;

/** V33a world-tick Lumafly guide spawner for players who still have an unscanned lore tower. */
public final class TunnelNukerSpawner {

	private TunnelNukerSpawner() {}

	public static void tick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()
				|| level.getRandom().nextInt(120) != 0)
			return;
		ServerPlayer player = level.players().get(level.getRandom().nextInt(level.players().size()));
		List<Towers> remaining = new ArrayList<>();
		for (Towers tower : Towers.towerList)
			if (!LoreTowerProgress.hasScanned(player, tower)) remaining.add(tower);
		if (remaining.isEmpty()) return;
		Towers tower = remaining.get(level.getRandom().nextInt(remaining.size()));
		if (!Towers.initialized(level)) Towers.loadPositions(level, 64 * 16 * 2);

		double x = player.getX() + level.getRandom().nextIntBetweenInclusive(-32, 32) + level.getRandom().nextDouble();
		double z = player.getZ() + level.getRandom().nextIntBetweenInclusive(-32, 32) + level.getRandom().nextDouble();
		double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int)Math.floor(x), (int)Math.floor(z)) + 7.5;
		var destination = tower.getGeneratedLocation();
		double tx = destination != null ? destination.getX() : tower.getRootPosition().x() + 8;
		double tz = destination != null ? destination.getZ() : tower.getRootPosition().z() + 8;
		float yaw = (float)(Math.toDegrees(Math.atan2(-(tx - x), tz - z)));

		EntityTunnelNuker nuker = ChromaEntityTypes.TUNNEL_NUKER.get().create(level, EntitySpawnReason.EVENT);
		if (nuker != null) {
			nuker.snapTo(x, y, z, yaw, 0);
			if (nuker.isValidSpawnPosition()) level.addFreshEntity(nuker);
		}
	}
}
