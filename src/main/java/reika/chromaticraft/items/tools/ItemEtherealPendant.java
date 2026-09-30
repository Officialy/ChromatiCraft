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

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * V33a {@code ItemEtherealPendant}, the Luma Capsule (tool sprite 72, always glinting): carried, it counts as every
 * level of Void Gaze at once (see {@code PotionVoidGaze.VoidGazeLevels.isActiveOnPlayer}). Its inventory tick stamps
 * the carrier with the world time; the stamp holds for 20 ticks. V33a's commented-out variant also applied the Luma
 * effects every 10 ticks; that stays unapplied, as in V33a.
 *
 * <p>26.x ticks items on the server only and entity data is not synced, so {@link #isActive} also looks for a capsule
 * in the player's inventory or an active Aura Pouch slot -- where V33a's client saw its own stamp.
 */
public class ItemEtherealPendant extends Item {

	private static final String TAG = "last_lumapendant";

	public ItemEtherealPendant(Item.Properties properties) {
		super(properties.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, @Nullable EquipmentSlot slot) {
		e.getPersistentData().putLong(TAG, world.getGameTime());/*
		if (e instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer) e;
			if (world.getTotalWorldTime()%10 == 0) {
				BlockEtherealLuma.addPotions(ep);
			}
		}*/
	}

	public static boolean isActive(Player ep) {
		if (ep.level().getGameTime()-ep.getPersistentData().getLongOr(TAG, Long.MIN_VALUE/2) < 20)
			return true;
		for (ItemStack is : ep.getInventory().getNonEquipmentItems()) {
			if (is.getItem() instanceof ItemEtherealPendant)
				return true;
		}
		return ItemAuraPouch.containsActive(ep, is -> is.getItem() instanceof ItemEtherealPendant);
	}

}
