/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.CrystalPotionAPI;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Maps crystal colours to potion (26.2 {@link MobEffect}) effects and applies them. Port note: the
 * 1.7.10 {@code Potion}/{@code PotionEffect} + int-id API became {@code Holder<MobEffect>} /
 * {@code MobEffectInstance}. DEFERRED integrations (reference unported content, so cannot function
 * yet — re-add when those port): the ItemPurifyCrystal / ItemPendant player-item modifiers, and the
 * ExtraUtils/Thaumcraft/Mystcraft "hostile world" checks (only the vanilla Nether remains). The
 * ReikaPotionHelper bad-effect helpers are inlined via {@link MobEffectCategory#HARMFUL}.
 */
public class CrystalPotionController implements CrystalPotionAPI {

	public static final CrystalPotionController instance = new CrystalPotionController();

	private final EnumMap<CrystalElement, Holder<MobEffect>> potions = new EnumMap<>(CrystalElement.class);
	private final EnumMap<CrystalElement, Holder<MobEffect>> potionsNether = new EnumMap<>(CrystalElement.class);

	private final HashSet<Holder<MobEffect>> ignoredPotions = new HashSet<>();

	private final Random rand = new Random();

	private CrystalPotionController() {
		this.addColorPotion(CrystalElement.BLUE, MobEffects.NIGHT_VISION);
		this.addColorPotion(CrystalElement.CYAN, MobEffects.WATER_BREATHING);
		this.addColorPotion(CrystalElement.GRAY, MobEffects.SLOWNESS);
		this.addColorPotion(CrystalElement.GREEN, MobEffects.POISON);
		this.addColorPotion(CrystalElement.LIGHTBLUE, MobEffects.SPEED);
		this.addColorPotion(CrystalElement.LIGHTGRAY, MobEffects.WEAKNESS);
		this.addColorPotion(CrystalElement.LIME, MobEffects.JUMP_BOOST);
		this.addColorPotion(CrystalElement.MAGENTA, ChromatiCraft.betterRegen);
		this.addColorPotion(CrystalElement.RED, MobEffects.RESISTANCE);
		this.addColorPotion(CrystalElement.ORANGE, MobEffects.FIRE_RESISTANCE);
		this.addColorPotion(CrystalElement.PINK, MobEffects.STRENGTH);
		this.addColorPotion(CrystalElement.YELLOW, MobEffects.HASTE);
		this.addColorPotion(CrystalElement.BROWN, ChromatiCraft.betterSat);

		this.addNetherPotion(CrystalElement.BLACK, MobEffects.WITHER);
		this.addNetherPotion(CrystalElement.CYAN, MobEffects.HUNGER);
		this.addNetherPotion(CrystalElement.GRAY, MobEffects.BLINDNESS);
		this.addNetherPotion(CrystalElement.GREEN, MobEffects.POISON);
		this.addNetherPotion(CrystalElement.LIGHTBLUE, MobEffects.SLOWNESS);
		this.addNetherPotion(CrystalElement.LIGHTGRAY, MobEffects.WEAKNESS);
		this.addNetherPotion(CrystalElement.YELLOW, MobEffects.MINING_FATIGUE);
		this.addNetherPotion(CrystalElement.WHITE, MobEffects.INVISIBILITY);
		this.addNetherPotion(CrystalElement.BROWN, MobEffects.NAUSEA);
		this.addNetherPotion(CrystalElement.BLUE, MobEffects.NIGHT_VISION);
		this.addNetherPotion(CrystalElement.PINK, MobEffects.STRENGTH);
		this.addNetherPotion(CrystalElement.MAGENTA, ChromatiCraft.betterRegen);
	}

	private static boolean isBadEffect(Holder<MobEffect> pot) {
		return pot.value().getCategory() == MobEffectCategory.HARMFUL;
	}

	private void addColorPotion(CrystalElement color, Holder<MobEffect> pot) {
		potions.put(color, pot);
	}

	private void addNetherPotion(CrystalElement color, Holder<MobEffect> pot) {
		potionsNether.put(color, pot);
	}

	public boolean shouldBeHostile(LivingEntity e, Level world) {
		// ItemPurifyCrystal player check deferred (item unported).
		return this.isWorldHostile(world);
	}

	@Override
	public boolean isWorldHostile(Level world) {
		// ExtraUtils dark / Thaumcraft / Mystcraft hostile-page checks deferred (mods unported).
		return world.dimension() == Level.NETHER;
	}

	@Override
	public boolean isPotionAllowed(MobEffectInstance eff, LivingEntity e) {
		if (eff == null)
			return false;
		Holder<MobEffect> pot = eff.getEffect();
		MobEffectInstance has = e.getEffect(pot);
		if (has != null) {
			if (has.getAmplifier() > eff.getAmplifier())
				return false;
			if (has.getDuration() > eff.getDuration())
				return false;
		}
		// ItemPurifyCrystal "only good effects" override deferred (item unported).
		if (!(e instanceof Player)) {
			boolean flag = e.isInvertedHealAndHarm(); // undead
			return this.shouldBeHostile(e, e.level()) ? isBadEffect(pot) == flag : true;
		}
		if (this.shouldBeHostile(e, e.level()))
			return pot == MobEffects.NIGHT_VISION || isBadEffect(pot);
		if (e.level().dimension() == Level.END)
			return true;
		return !isBadEffect(pot);
	}

	public boolean isBadPotion(CrystalElement e) {
		Holder<MobEffect> pot = potions.get(e);
		return pot != null && isBadEffect(pot);
	}

	public MobEffectInstance getEffectFromColor(CrystalElement color, int dura, int level, boolean evil) {
		EnumMap<CrystalElement, Holder<MobEffect>> map = evil ? potionsNether : potions;
		Holder<MobEffect> pot = map.get(color);
		if (pot == null)
			return null;
		return new MobEffectInstance(pot, dura, level, true, true);
	}

	public String getPotionName(CrystalElement color) {
		if (color == CrystalElement.BLACK)
			return "corrupting";
		if (color == CrystalElement.PURPLE)
			return "enhancing";
		Holder<MobEffect> pot = potions.get(color);
		if (pot == null && color == CrystalElement.BROWN)
			return "lengthening";
		if (pot == null)
			return "[None]";
		return pot.value().getDisplayName().getString();
	}

	public boolean requiresCustomPotion(CrystalElement color) {
		switch (color) {
			case CYAN:
			case YELLOW:
			case LIME:
			case RED:
			case BROWN:
				return true;
			default:
				return false;
		}
	}

	public boolean isCorruptedPotion(CrystalElement color) {
		return color == CrystalElement.GRAY || color == CrystalElement.LIGHTGRAY || color == CrystalElement.WHITE;
	}

	public boolean isPotionModifier(CrystalElement color) {
		return color == CrystalElement.BLACK || color == CrystalElement.PURPLE;
	}

	public String getEffectName(CrystalElement color, boolean boost) {
		if (color == CrystalElement.BLACK)
			return "Confuses Mobs";
		if (color == CrystalElement.PURPLE)
			return "Gives XP";
		if (color == CrystalElement.BROWN && !boost)
			return "Prevents Starvation";
		if (color == CrystalElement.ORANGE && boost)
			return "Protects from Extreme Heat";
		if (color == CrystalElement.WHITE)
			return boost ? "Clears All Negative Effects" : "Clears Most Negative Effects";
		return potions.get(color).value().getDisplayName().getString();
	}

	public String getNetherEffectName(CrystalElement color) {
		if (color == CrystalElement.BROWN)
			return "Nausea";
		if (color == CrystalElement.PURPLE)
			return "Takes XP";
		if (color == CrystalElement.ORANGE)
			return "Fire Damage";
		if (color == CrystalElement.LIME)
			return "Jump Disability";
		if (color == CrystalElement.RED)
			return "Direct Damage";
		return potionsNether.get(color).value().getDisplayName().getString();
	}

	@Override
	public Set<Holder<MobEffect>> ignoredBadPotionsForLevelZero() {
		return Collections.unmodifiableSet(ignoredPotions);
	}

	@Override
	public void addBadPotionForIgnore(Holder<MobEffect> p) {
		ignoredPotions.add(p);
	}

	public void applyEffectFromColor(int dura, int level, LivingEntity e, CrystalElement color, boolean doFX) {
		this.applyEffectFromColor(dura, level, e, color, doFX, false, false);
	}

	public void applyEffectFromColor(int dura, int level, LivingEntity e, CrystalElement color, boolean doFX, boolean forceGood, boolean forceBad) {
		if (forceBad || (!forceGood && this.shouldBeHostile(e, e.level()))) {
			switch (color) {
				case ORANGE:
					e.igniteForSeconds(2);
					break;
				case RED:
					if (e.level() instanceof ServerLevel sl)
						e.hurtServer(sl, sl.damageSources().magic(), 1);
					break;
				case PURPLE:
					if (!e.level().isClientSide()) {
						if (e instanceof Player ep) {
							if (rand.nextInt(5) == 0) {
								if (ep.experienceLevel > 0) {
									ep.giveExperienceLevels(-1);
								}
								else {
									ep.experienceLevel = 0;
									ep.totalExperience = 0;
									ep.experienceProgress = 0;
								}
							}
						}
						// Legacy: hostile mobs gained +1 xpReward — no clean 26.2 API, dropped.
					}
					break;
				case BROWN:
					if (!e.hasEffect(MobEffects.NAUSEA))
						addPotionEffect(e, new MobEffectInstance(MobEffects.NAUSEA, Math.max(100, (int) (dura * 1.8)), level, true, true));
					break;
				case LIME:
					addPotionEffect(e, new MobEffectInstance(MobEffects.JUMP_BOOST, dura, -5, true, true));
					break;
				default:
					MobEffectInstance eff = this.getEffectFromColor(color, dura, level, true);
					if (forceBad || this.isPotionAllowed(eff, e))
						addPotionEffect(e, eff);
			}
		}
		else {
			switch (color) {
				case BLACK:
					if (e instanceof Monster m) { //clear AI
						m.setTarget(null);
						m.getNavigation().stop();
					}
					break;
				case WHITE:
					clearBadPotions(e, level > 0 ? Collections.emptySet() : this.ignoredBadPotionsForLevelZero());
					break;
				case PURPLE:
					if (e instanceof Player ep && !e.level().isClientSide() && (level > 0 || rand.nextInt(2) == 0)) {
						if (doFX)
							ep.level().playSound(null, ep.getX(), ep.getY(), ep.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, rand.nextFloat() * 2);
						// ItemPendant PURPLE-boost multiplier deferred (item unported).
						ep.giveExperiencePoints(1);
					}
					break;
				default:
					MobEffectInstance eff = this.getEffectFromColor(color, dura, level, false);
					if (eff != null) {
						if (forceGood || this.isPotionAllowed(eff, e)) {
							addPotionEffect(e, eff);
						}
					}
			}
		}
	}

	private static void clearBadPotions(LivingEntity e, Set<Holder<MobEffect>> ignore) {
		List<Holder<MobEffect>> toRemove = new ArrayList<>();
		for (MobEffectInstance mei : e.getActiveEffects()) {
			Holder<MobEffect> h = mei.getEffect();
			if (isBadEffect(h) && !ignore.contains(h))
				toRemove.add(h);
		}
		for (Holder<MobEffect> h : toRemove)
			e.removeEffect(h);
	}

	private static void addPotionEffect(LivingEntity e, MobEffectInstance eff) {
		MobEffectInstance cur = e.getEffect(eff.getEffect());
		if (e instanceof Player || cur == null || cur.getAmplifier() < eff.getAmplifier() || cur.getDuration() < 20 || eff.getDuration() < 80)
			e.addEffect(eff);
	}

	@Override
	public MobEffectInstance getEffectFromColor(CrystalElementProxy color, int dura, int level, boolean evil) {
		return this.getEffectFromColor(CrystalElement.elements[color.ordinal()], dura, level, evil);
	}
}
