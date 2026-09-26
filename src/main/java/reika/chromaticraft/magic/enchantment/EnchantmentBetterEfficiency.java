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

/** V33a Consistent Efficiency: pulls the mining speed of every block toward the same fifteen ticks. */
public class EnchantmentBetterEfficiency extends ChromaticEnchantment {

	public EnchantmentBetterEfficiency() {
		super(Target.DIGGER);
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return true;
	}

}
