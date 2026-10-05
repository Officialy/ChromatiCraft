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
import reika.chromaticraft.registry.Chromabilities;

/**
 * V33a Aggro Mask: I keeps a struck zombified piglin from calling the others onto you, II keeps a struck monster from
 * turning on you at all, III keeps a hit from breaking the Communicate ability's truce.
 */
public class EnchantmentAggroMask extends ChromaticEnchantment {

	public EnchantmentAggroMask() {
		super(Target.WEAPON_OR_BOW);
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	public static boolean hidePigmanSpreadDamage(int level) {
		return level >= 1;
	}

	public static boolean hideDirectDamage(int level) {
		return level >= 2;
	}

	public static boolean preservePeace(int level) {
		return level >= 3;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return level == 3 ? ProgressStage.DIMENSION.playerHasPrerequisites(ep) && Chromabilities.COMMUNICATE.isAvailableToPlayer(ep) : ProgressStage.KILLMOB.isPlayerAtStage(ep);
	}

}
