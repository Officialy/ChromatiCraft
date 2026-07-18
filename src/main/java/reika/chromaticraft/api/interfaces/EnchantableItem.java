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

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

import reika.dragonapi.interfaces.item.CustomEnchantingCategory;

import cpw.mods.fml.common.eventhandler.Event.Result;


public interface EnchantableItem extends CustomEnchantingCategory {

	public Result getEnchantValidity(Enchantment e, ItemStack is);

}
