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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import reika.chromaticraft.base.ChromaticEnchantment;
import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * V33a Capital Strike: against a mob of enough maximum health each hit takes a fixed fraction of it, capped; anything
 * weaker is not hurt at all, and the weapon takes wear for trying.
 */
public class EnchantmentBossKill extends ChromaticEnchantment {

	public EnchantmentBossKill() {
		super(Target.WEAPON_OR_BOW);
	}

	@Override
	public int getMaxLevel() {
		return damageFraction.length;
	}

	private static final DamageProfile[] damageFraction = {
			new DamageProfile(16, 50, 500),
			new DamageProfile(4, 80, 300),
			new DamageProfile(2, 100, 200),
			new DamageProfile(2, 250, 100),
	};

	public static float getDamageDealt(LivingEntity e, int lvl) {
		float max = e.getMaxHealth();
		DamageProfile dp = damageFraction[lvl-1];
		if (max >= dp.damageThreshold) {
			return Math.min(max*dp.damageFraction, dp.damageCap);
		}
		else {
			return 0;
		}
	}

    private record DamageProfile(float damageFraction, int damageCap, int damageThreshold) {

        private DamageProfile(int damageFraction, int damageCap, int damageThreshold) {
            this(1F / damageFraction, damageCap, damageThreshold);
        }

    }

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return ProgressStage.KILLDRAGON.isPlayerAtStage(ep) || ProgressStage.KILLWITHER.isPlayerAtStage(ep);
	}

}
