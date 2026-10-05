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
import reika.chromaticraft.magic.progression.ProgressStage;

/** V33a Area Strike: a hit also lands, falling off with distance, on nearby mobs of the same kind. */
public class EnchantmentWeaponAOE extends ChromaticEnchantment {

	public EnchantmentWeaponAOE() {
		super(Target.WEAPON_OR_BOW);
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
	public boolean isVisibleToPlayer(Player ep, int level) {
		return ProgressStage.KILLMOB.isPlayerAtStage(ep);
	}

}
