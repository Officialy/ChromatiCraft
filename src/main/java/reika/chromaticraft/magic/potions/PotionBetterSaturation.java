/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.potions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Custom saturation effect (was PotionBetterSaturation extends ChromaPotion/Potion). Icon surface
 * dropped (26.2 texture-based). The HungerOverhaul max-food integration is deferred (unported mod) —
 * uses the vanilla threshold of 17.
 */
public class PotionBetterSaturation extends MobEffect {

	public PotionBetterSaturation(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int dura, int level) {
		return level > 0 || dura == 5;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity e, int amplification) {
		if (e instanceof Player ep) {
			if (amplification > 0 || ep.getFoodData().getFoodLevel() < 17)
				ep.getFoodData().eat(amplification + 1, 1.0F);
		}
		return true;
	}
}
