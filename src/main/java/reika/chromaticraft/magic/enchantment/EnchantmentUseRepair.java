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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.base.ChromaticEnchantment;

/** V33a Durability Leech: each hit repairs the weapon a little, at the cost of a little of the damage. */
public class EnchantmentUseRepair extends ChromaticEnchantment {

	public EnchantmentUseRepair() {
		super(Target.WEAPON_OR_BOW);
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	public static int getRepairedDurability(ItemStack item, int level, float damage/*, UseType type*/) {
		float frac = (float)item.getDamageValue()/item.getMaxDamage();
		return (int)Math.min(item.getDamageValue(), 1+level*level*frac*damage/2D);
	}
	/*
	public static enum UseType {
		MINE(),
		MELEE(),
		RANGED();
	}
	 */

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return level < 3 || ProgressStage.MAKECHROMA.isPlayerAtStage(ep);
	}

}
