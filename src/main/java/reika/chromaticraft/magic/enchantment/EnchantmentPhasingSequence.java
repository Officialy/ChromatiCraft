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
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a Armor Breach: part of each hit is also dealt as absolute pylon energy that armour and protection cannot stop,
 * with the remainder re-dealt as a normal blow. The original hit is not cancelled, so the hurt cooldown decides how
 * much of it still lands on top (roughly the full hit plus the piercing part).
 */
public class EnchantmentPhasingSequence extends ChromaticEnchantment {

	public EnchantmentPhasingSequence() {
		super(Target.WEAPON_OR_BOW);
	}

	@Override
	public int getMaxLevel() {
		return 4;
	}

	private static final float[] REDUCTION_FACTORS = {1, 0.9375F, 0.875F, 0.75F, 0.667F};

	private static float getActualDamage(float baseDmg, int level) {
		return (float)ReikaMathLibrary.roundDownToFraction(baseDmg*REDUCTION_FACTORS[level], 0.125);
	}

	public static float getPenetratingDamage(float baseDmg, int level) {
		return (float)ReikaMathLibrary.roundUpToFraction(0.1F*level*getActualDamage(baseDmg, level), 0.25);
	}

	public static float getSpilloverDamage(float baseDmg, int level) {
		return getActualDamage(baseDmg, level)-getPenetratingDamage(baseDmg, level);
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		switch(level) {
			case 1:
			case 2:
			default:
				return ProgressStage.KILLMOB.isPlayerAtStage(ep);
			case 3:
				return ProgressStage.ENERGYIDEA.isPlayerAtStage(ep);
			case 4:
				return ProgressStage.LINK.isPlayerAtStage(ep);
		}
	}

}
