/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * V33a {@code ItemPylonFinder}, the Pylon Compass: held, it shows the nearest known pylon of every colour on the HUD
 * ({@code PylonFinderOverlay}, reading the synced pylon cache). It also works from inside an Aura Pouch (V33a
 * registered it for the pouch's "Keeps functionality active" line): ticked from a pouch it stamps {@link #NBT_KEY}
 * with the world time every 8 ticks, which the overlay honours for 20 ticks.
 *
 * <p>26.x ticks items on the server only and entity data is not synced, so the client overlay also asks the pouches
 * directly ({@code ItemAuraPouch.containsActive}); V33a's client saw its own stamp, as 1.7.10 ticked both sides.
 */
public class ItemPylonFinder extends Item {

	public static final String NBT_KEY = "pylonoverlay";

	public ItemPylonFinder(Properties properties) {
		super(properties);
		ItemAuraPouch.setSpecialEffect(ItemAuraPouch.WORKS_IN_POUCH_EFFECT_DESC, this);
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, @Nullable EquipmentSlot slot) {
		if (e instanceof Player && ItemAuraPouch.isPouchTick() && world.getGameTime()%8 == 0)
			e.getPersistentData().putLong(NBT_KEY, world.getGameTime());
	}

}
