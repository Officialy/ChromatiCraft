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

import reika.chromaticraft.base.ChromaticEnchantment;

/** V33a Inventory Insertion: a mined block's drops go straight into the miner's inventory. */
public class EnchantmentAutoCollect extends ChromaticEnchantment {

	public EnchantmentAutoCollect() {
		super(Target.DIGGER);
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return true;
	}

}
