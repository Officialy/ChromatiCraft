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
import net.minecraft.world.entity.player.Player;

import reika.dragonapi.libraries.io.NBTCompat;

/**
 * Modern persistence seam for the original {@code Chromabilities} boolean map. It intentionally
 * retains the V33a {@code chromabilities/<ability id>} layout so the complete ability controller can
 * adopt the data without migration. Only gameplay systems whose ability semantics are already known
 * should use this leaf while the much larger ability cluster remains outside the compile allowlist.
 */
public final class ChromaAbilityData {

	private static final String ABILITY_TAG = "chromabilities";
	private static final String PYLON_ID = "pylon";

	private ChromaAbilityData() {}

	public static boolean hasPylonImmunity(Player player) {
		return getAbilities(player).getBooleanOr(PYLON_ID, false);
	}

	public static void setPylonImmunity(Player player, boolean enabled) {
		CompoundTag abilities = getAbilities(player);
		abilities.putBoolean(PYLON_ID, enabled);
		player.getPersistentData().put(ABILITY_TAG, abilities);
	}

	private static CompoundTag getAbilities(Player player) {
		return NBTCompat.getCompound(player.getPersistentData(), ABILITY_TAG);
	}
}
