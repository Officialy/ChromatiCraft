/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.registry.Chromabilities;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;

/**
 * V33a ability ownership and enabled-state persistence. An absent key means unowned; a present false
 * key means owned but inactive. This retains the original {@code chromabilities/<ability id>} layout
 * so the behavior controller can use the same data without migration.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaAbilityData {

	private static final String ABILITY_TAG = "chromabilities";
	private static final String PYLON_ID = "pylon";
	private static final String DOUBLECRAFT_ID = "doublecraft";

	private ChromaAbilityData() {}

	public static boolean hasAbility(Player player, Ability ability) {
		return getAbilities(player).contains(ability.getID());
	}

	public static boolean enabledOn(Player player, Ability ability) {
		return getAbilities(player).getBooleanOr(ability.getID(), false);
	}

	public static void give(Player player, Ability ability) {
		setToPlayer(player, false, ability, true);
	}

	public static void setToPlayer(Player player, boolean enabled, Ability ability) {
		setToPlayer(player, enabled, ability, false);
	}

	private static void setToPlayer(Player player, boolean enabled, Ability ability, boolean force) {
		CompoundTag abilities = getAbilities(player);
		if (force || enabled || abilities.contains(ability.getID())) {
			abilities.putBoolean(ability.getID(), enabled);
			player.getPersistentData().put(ABILITY_TAG, abilities);
			sync(player);
		}
	}

	public static void removeFromPlayer(Player player, Ability ability) {
		CompoundTag abilities = getAbilities(player);
		abilities.remove(ability.getID());
		player.getPersistentData().put(ABILITY_TAG, abilities);
		ability.onRemoveFromPlayer(player);
		sync(player);
	}

	public static java.util.List<Ability> getAvailableFrom(Player player) {
		java.util.List<Ability> available = new java.util.ArrayList<>();
		for (String id : getAbilities(player).keySet()) {
			Ability ability = Chromabilities.getAbility(id);
			if (ability != null) available.add(ability);
		}
		return available;
	}

	public static java.util.Map<Ability, Boolean> getAbilitiesOn(Player player) {
		java.util.Map<Ability, Boolean> states = new java.util.HashMap<>();
		CompoundTag abilities = getAbilities(player);
		for (Ability ability : getAvailableFrom(player))
			states.put(ability, abilities.getBooleanOr(ability.getID(), false));
		return states;
	}

	public static void copyAbilities(Player from, Player to) {
		to.getPersistentData().put(ABILITY_TAG, getAbilities(from).copy());
		sync(to);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath()) copyAbilities(event.getOriginal(), event.getEntity());
	}

	private static void sync(Player player) {
		if (player instanceof ServerPlayer serverPlayer)
			ReikaPlayerAPI.syncCustomData(serverPlayer);
	}

	public static boolean hasPylonImmunity(Player player) {
		return getAbilities(player).getBooleanOr(PYLON_ID, false);
	}

	public static void setPylonImmunity(Player player, boolean enabled) {
		CompoundTag abilities = getAbilities(player);
		abilities.putBoolean(PYLON_ID, enabled);
		player.getPersistentData().put(ABILITY_TAG, abilities);
	}

	/** V33a {@code Chromabilities.DOUBLECRAFT.enabledOn}: doubles Aura Infuser output. */
	public static boolean hasDoubleCraft(Player player) {
		return getAbilities(player).getBooleanOr(DOUBLECRAFT_ID, false);
	}

	public static void setDoubleCraft(Player player, boolean enabled) {
		CompoundTag abilities = getAbilities(player);
		abilities.putBoolean(DOUBLECRAFT_ID, enabled);
		player.getPersistentData().put(ABILITY_TAG, abilities);
	}

	private static CompoundTag getAbilities(Player player) {
		return NBTCompat.getCompound(player.getPersistentData(), ABILITY_TAG);
	}
}
