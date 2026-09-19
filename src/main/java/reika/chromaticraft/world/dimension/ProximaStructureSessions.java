package reika.chromaticraft.world.dimension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase;

/**
 * The active-player half of V33a {@code ChromaDimensionManager}. A structure session is deliberately
 * transient: Dimension Cores rescan their entry after a relog and restore it, while completion itself
 * lives permanently in {@code ProgressionManager}.
 */
public final class ProximaStructureSessions {

	/** 64 + V33a's 48-block terrain offset + two blocks of clearance. */
	public static final double EXIT_HEIGHT = 114;

	private static final Map<UUID, StructureGeneratorBase> ACTIVE = new HashMap<>();

	private ProximaStructureSessions() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ProximaStructureSessions::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(ProximaStructureSessions::onLogout);
		NeoForge.EVENT_BUS.addListener(ProximaStructureSessions::onDimensionChange);
	}

	/** V33a addPlayerToStructure: tuning is the only admission gate. */
	public static boolean addPlayerToStructure(ServerPlayer player, StructureGeneratorBase structure) {
		if (structure == null || !DimensionTuningManager.TuningThresholds.STRUCTURES
				.isSufficientlyTuned(player))
			return false;
		ACTIVE.put(player.getUUID(), structure);
		ChromaNetwork.sendStructureEntry(player, structure.getType().ordinal());
		return true;
	}

	public static StructureGeneratorBase getStructurePlayerIsIn(Player player) {
		return player == null ? null : ACTIVE.get(player.getUUID());
	}

	public static boolean isPlayerInStructure(Player player, StructureGeneratorBase structure) {
		return structure != null && getStructurePlayerIsIn(player) == structure;
	}

	public static void removePlayerFromStructure(Player player) {
		if (player != null)
			ACTIVE.remove(player.getUUID());
	}

	private static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		StructureGeneratorBase structure = ACTIVE.get(player.getUUID());
		if (structure == null)
			return;
		if (player.level().dimension() != ChromaDimensions.PROXIMA || player.getY() >= EXIT_HEIGHT) {
			removePlayerFromStructure(player);
			return;
		}
		structure.tickPlayer(player);
		CheatingPreventionSystem.instance.tick(player);
	}

	private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		removePlayerFromStructure(event.getEntity());
	}

	private static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getTo() != ChromaDimensions.PROXIMA)
			removePlayerFromStructure(event.getEntity());
	}

	/** Test/reset seam; does not touch permanent completion data. */
	public static void clear() {
		ACTIVE.clear();
	}
}
