/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.enchantment;

import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.base.ChromaticEnchantment;
import reika.chromaticraft.magic.progression.ProgressStage;


public class EnchantmentWeaponAOE extends ChromaticEnchantment {

	public EnchantmentWeaponAOE(int id) {
		super(id, EnumEnchantmentType.weapon);
	}

	@Override
	public int getMaxLevel() {
		return 5;
	}

	public static double getRadius(int level) {
		return 1.5D+level/2D;
	}

	public static double getDamageFactor(int level, double dist, double radius) {
		return Math.min(1, 1D-Math.pow(dist/radius-level/40D, level));
	}

	@Override
	public boolean canApply(ItemStack is) {
		return EnumEnchantmentType.weapon.canEnchantItem(is.getItem()) || EnumEnchantmentType.bow.canEnchantItem(is.getItem());
	}

	@Override
	public boolean isVisibleToPlayer(EntityPlayer ep, int level) {
		return ProgressStage.KILLMOB.isPlayerAtStage(ep);
	}

}
