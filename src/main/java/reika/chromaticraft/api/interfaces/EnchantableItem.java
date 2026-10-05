/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.core.Holder;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import reika.dragonapi.interfaces.item.CustomEnchantingCategory;

/**
 * V33a API {@code EnchantableItem}: an item that decides for itself which enchantments the Auto Enchanter may put on it
 * -- TRUE allows, FALSE denies, DEFAULT falls through to the usual checks (1.7.10's {@code Event.Result}).
 */
public interface EnchantableItem extends CustomEnchantingCategory {

	TriState getEnchantValidity(Holder<Enchantment> e, ItemStack is);

}
