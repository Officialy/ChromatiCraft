package reika.chromaticraft.magic.lore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.network.ChromaNetwork;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/** Death-persistent V33a {@code loretowers} scan flags, independent of the later key-board puzzle. */
public final class LoreTowerProgress {

	private static final String TAG = "loretowers";

	private LoreTowerProgress() {}

	public static void trigger(ServerPlayer player, Towers tower) {
		setScanned(player, tower, true);
		ChromaNetwork.sendLoreNote(player, tower);
	}

	public static boolean hasScanned(Player player, Towers tower) {
		return tag(player).getBooleanOr(tower.name(), false);
	}

	public static boolean hasScannedAll(Player player) {
		for (Towers tower : Towers.towerList) {
			if (!hasScanned(player, tower)) return false;
		}
		return true;
	}

	public static void setScanned(Player player, Towers tower, boolean scanned) {
		tag(player).putBoolean(tower.name(), scanned);
		if (player instanceof ServerPlayer serverPlayer) {
			try {
				ReikaPlayerAPI.syncCustomData(serverPlayer);
			}
			catch (Exception ignored) {
				// Embedded GameTest players do not negotiate DragonAPI's custom-data channel.
			}
		}
	}

	private static CompoundTag tag(Player player) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag lore = NBTCompat.getCompound(root, TAG);
		root.put(TAG, lore);
		return lore;
	}
}
