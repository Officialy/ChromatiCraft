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

import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * Custom regeneration effect (was PotionCustomRegen extends ChromaPotion/Potion). The 1.7.10
 * icon/renderInventoryEffect surface is dropped — 26.2 effect icons are a texture at
 * assets/chromaticraft/textures/mob_effect/&lt;name&gt;.png (added when the effect registers).
 */
public class PotionCustomRegen extends MobEffect {

	public PotionCustomRegen(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int dura, int level) {
		int d = level > 0 ? 1 + (120 >> (6 * level)) : 50;
		return dura % d == 0 && (level > 0 || ReikaRandomHelper.doWithChance(25));
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity e, int amplification) {
		if (e.getHealth() < e.getMaxHealth())
			e.heal(1.0F);
		return true;
	}
}
