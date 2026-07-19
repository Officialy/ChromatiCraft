/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;


public interface CrystalPotionAPI {

	/** Whether a world is "evil" like the Nether, thus corrupting the effect of the crystals. */
	public boolean isWorldHostile(Level world);

	/** Whether crystals can even apply this potion effect to this entity. */
	public boolean isPotionAllowed(MobEffectInstance eff, LivingEntity e);

	/** Registers a potion to the ignore list for T1 white pendants. */
	public void addBadPotionForIgnore(Holder<MobEffect> p);

	/** Fetches the ignore list for T1 white pendants. */
	public Set<Holder<MobEffect>> ignoredBadPotionsForLevelZero();

	/** Constructs a potion effect for the given color, with a specified duration and amplifier, optionally corrupting it. */
	public MobEffectInstance getEffectFromColor(CrystalElementProxy color, int dura, int level, boolean evil);

}
