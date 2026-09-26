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

/** V33a Reposession: a kill drops the mob's equipment, and often its rare drop and head. */
public class EnchantmentRareLoot extends ChromaticEnchantment {

	public EnchantmentRareLoot() {
		super(Target.WEAPON_OR_BOW);
	}

	@Override
	public int getMaxLevel() {
		return 4;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		switch(level) {
			case 1:
			case 2:
			default:
				return ProgressStage.KILLMOB.isPlayerAtStage(ep);
			case 3:
				return ProgressStage.CHROMA.isPlayerAtStage(ep);
			case 4:
				return ProgressStage.ALLOY.isPlayerAtStage(ep);
		}
	}

}
