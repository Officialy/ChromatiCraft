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

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.base.ChromaticEnchantment;

/** V33a Ender Lock: endermen no longer teleport out of the way of the holder's arrows. */
public class EnchantmentEnderLock extends ChromaticEnchantment {

	public EnchantmentEnderLock() {
		super(Target.BOW);
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return ProgressStage.END.isPlayerAtStage(ep);
	}

}
