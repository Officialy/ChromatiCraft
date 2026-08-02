package reika.chromaticraft.magic.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.network.ChromaNetwork;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/** Death-persistent V33a research-tier storage and migration. */
public final class ResearchProgress {

	public static final String ROOT_TAG = "Chroma_Research";
	public static final String LEVEL_TAG = "research_level";

	public static ResearchLevel getLevel(Player player) {
		CompoundTag tag = getTag(player);
		String name = tag.getStringOr(LEVEL_TAG, "");
		if (!name.isEmpty()) {
			try {
				return ResearchLevel.valueOf(name);
			}
			catch (IllegalArgumentException e) {
				ChromatiCraft.LOGGER.warn("Invalid ChromatiCraft research level '{}' for {}; resetting to ENTRY",
						name, player.getName().getString());
			}
		}

		// V33a migration: older worlds stored an ordinal and one historic level was removed after
		// ENERGY, so ordinals above ENERGY are shifted down once before being rewritten as a name.
		int ordinal = NBTCompat.getInt(tag, LEVEL_TAG, 0);
		if (ordinal > ResearchLevel.ENERGY.ordinal())
			ordinal--;
		ordinal = Math.clamp(ordinal, 0, ResearchLevel.levelList.length - 1);
		ResearchLevel level = ResearchLevel.levelList[ordinal];
		tag.putString(LEVEL_TAG, level.name());
		return level;
	}

	public static boolean stepTo(Player player, ResearchLevel level, boolean notify) {
		return getLevel(player).ordinal() == level.ordinal() - 1 && setLevel(player, level, notify);
	}

	public static boolean setLevel(Player player, ResearchLevel level, boolean notify) {
		if (player == null || player.level().isClientSide() || ReikaPlayerAPI.isFake(player)
				|| getLevel(player) == level)
			return false;
		getTag(player).putString(LEVEL_TAG, level.name());
		if (player instanceof ServerPlayer serverPlayer) {
			try {
				ReikaPlayerAPI.syncCustomData(serverPlayer);
			}
			catch (Exception e) {
				ChromatiCraft.LOGGER.debug("Could not sync research level to client {}: {}",
						serverPlayer.getName().getString(), e.toString());
			}
			if (notify)
				ChromaNetwork.sendProgressionNote(serverPlayer, level.ordinal());
		}
		return true;
	}

	private static CompoundTag getTag(Player player) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag tag = NBTCompat.getCompound(root, ROOT_TAG);
		root.put(ROOT_TAG, tag);
		return tag;
	}

	private ResearchProgress() {}
}
