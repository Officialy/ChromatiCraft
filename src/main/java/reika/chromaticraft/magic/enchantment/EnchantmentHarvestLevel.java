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

/** V33a Enhanced Utility: the tool harvests as if one or two tiers higher. */
public class EnchantmentHarvestLevel extends ChromaticEnchantment {

	public EnchantmentHarvestLevel() {
		super(Target.DIGGER);
	}

	@Override
	public int getMaxLevel() {
		return 2;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return true;
	}

}
