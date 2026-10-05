package reika.chromaticraft.magic.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

import java.util.Locale;

/**
 * V33a's player-owned {@code castingprog} record, separated from the table's own recipe history.
 * Research tiers ask whether a player has personally completed each physical casting tier, so this
 * data must survive table replacement, logout, and death with the rest of ChromatiCraft progression.
 */
public final class CastingProgression {

	public static final String NBT_TAG = "castingprog";

	public static boolean hasCrafted(Player player, CastingTableRecipe.Tier tier) {
		return NBTCompat.getBoolean(getTag(player), key(tier), false);
	}

	/** @return true only when this is the player's first completion at the supplied tier. */
	public static boolean markCrafted(Player player, CastingTableRecipe.Tier tier) {
		if (player == null || player.level().isClientSide() || ReikaPlayerAPI.isFake(player))
			return false;
		CompoundTag tag = getTag(player);
		String key = key(tier);
		if (NBTCompat.getBoolean(tag, key, false)) {
			// V33a setPlayerHasCrafted re-checks the research tier on every completion.
			PlayerResearch.checkForUpgrade(player);
			return false;
		}
		tag.putBoolean(key, true);
		if (player instanceof ServerPlayer serverPlayer) {
			try {
				ReikaPlayerAPI.syncCustomData(serverPlayer);
			}
			catch (Exception e) {
				ChromatiCraft.LOGGER.debug("Could not sync casting progression to client {}: {}",
						serverPlayer.getName().getString(), e.toString());
			}
		}
		// V33a setPlayerHasCrafted: a first completion at a tier is what opens the next research tier.
		PlayerResearch.checkForUpgrade(player);
		return true;
	}

	private static CompoundTag getTag(Player player) {
		CompoundTag root = ReikaPlayerAPI.getDeathPersistentNBT(player);
		CompoundTag tag = NBTCompat.getCompound(root, NBT_TAG);
		root.put(NBT_TAG, tag);
		return tag;
	}

	private static String key(CastingTableRecipe.Tier tier) {
		return tier.name().toLowerCase(Locale.ROOT);
	}

	private CastingProgression() {}
}
