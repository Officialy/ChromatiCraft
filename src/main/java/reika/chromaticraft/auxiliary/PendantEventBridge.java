/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import reika.chromaticraft.items.tools.ItemBoostedPendant;
import reika.chromaticraft.items.tools.ItemPendant;
import reika.chromaticraft.items.tools.powered.ItemPurifyCrystal;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.interfaces.item.ActivatedInventoryItem;
import reika.dragonapi.libraries.ReikaEntityHelper;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Pendant hooks formerly hosted in V33a's very broad {@code ChromaticEventManager}, each a direct transcription
 * of the corresponding V33a method: {@code carryPotionEffect}, {@code extraXP}, and the particle suppression that
 * {@code ItemPendant.onUpdate} applied with {@code NOPARTICLES}.
 */
public final class PendantEventBridge {

	/** Killed in a hostile world: V33a zeroed their {@code experienceValue} (or cleared a player's experience). */
	private static final Set<LivingEntity> NO_EXPERIENCE = Collections.newSetFromMap(new WeakHashMap<>());

	private PendantEventBridge() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PendantEventBridge::carryPotionEffect);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, PendantEventBridge::extraXP);
		NeoForge.EVENT_BUS.addListener(PendantEventBridge::withholdExperience);
		NeoForge.EVENT_BUS.addListener(PendantEventBridge::hideEffectParticles);
	}

	/** V33a {@code carryPotionEffect}: every pendant in the attacker's main inventory lays its effect on the target. */
	private static void carryPotionEffect(AttackEntityEvent ev) {
		Player ep = ev.getEntity();
		Entity tg = ev.getTarget();
		if (tg instanceof LivingEntity elb && !(ItemPurifyCrystal.isActive(ep) && ReikaEntityHelper.isHostile(elb))) {
			parseInventoryForPendantCarry(elb, ep.getInventory().getNonEquipmentItems().toArray(new ItemStack[0]), null);
		}
	}

	private static void parseInventoryForPendantCarry(LivingEntity elb, ItemStack[] inv, ItemStack active) {
		for (int i = 0; i < inv.length; i++) {
			if (active == null || ((ActivatedInventoryItem)active.getItem()).isSlotActive(active, i)) {
				ItemStack is = inv[i];
				if (is != null && !is.isEmpty()) {
					if (is.getItem() instanceof ItemBoostedPendant pendant) {
						CrystalPotionController.instance.applyEffectFromColor(100, 1, elb, pendant.getColor(), true);
					}
					else if (is.getItem() instanceof ItemPendant pendant) {
						CrystalPotionController.instance.applyEffectFromColor(100, 0, elb, pendant.getColor(), true);
					}
					else if (is.getItem() instanceof ActivatedInventoryItem aii) {
						parseInventoryForPendantCarry(elb, aii.getInventory(is), is);
					}
				}
			}
		}
	}

	/**
	 * V33a {@code extraXP}: a player's kill in a hostile world drops no experience; elsewhere a Purple Pendant adds one
	 * more orb of the kill's worth (three with the Enhanced Pendant), paid straight to the player with
	 * {@code RANGEDBOOST}.
	 */
	private static void extraXP(LivingDropsEvent ev) {
		LivingEntity e = ev.getEntity();
		if (ev.getSource().getEntity() instanceof Player ep && e.level() instanceof ServerLevel world) {
			int val = e instanceof Player ? 25 : e instanceof Mob ? e.getExperienceReward(world, ep) : 5;
			if (val == 0)
				val = 5;
			if (e instanceof EnderDragon)
				val = 10000;
			if (CrystalPotionController.instance.shouldBeHostile(e, world)) {
				if (e instanceof Mob || e instanceof Player)
					NO_EXPERIENCE.add(e);
			}
			else {
				int orbs = 0;
				if (carries(ep, ChromaItems.BOOSTED_PENDANTS.get(CrystalElement.PURPLE).get())) {
					orbs = 3;
				}
				else if (carries(ep, ChromaItems.PENDANTS.get(CrystalElement.PURPLE).get())) {
					orbs = 1;
				}

				if (orbs > 0) {
					if (Chromabilities.RANGEDBOOST.enabledOn(ep)) {
						ep.giveExperiencePoints(val*orbs);
					}
					else {
						for (int i = 0; i < orbs; i++) {
							double px = e.getX()+(world.getRandom().nextDouble()*2-1)*0.125;
							double pz = e.getZ()+(world.getRandom().nextDouble()*2-1)*0.125;
							world.addFreshEntity(new ExperienceOrb(world, px, e.getY(), pz, val));
						}
					}
				}
			}
		}
	}

	private static void withholdExperience(LivingExperienceDropEvent ev) {
		if (NO_EXPERIENCE.remove(ev.getEntity()))
			ev.setDroppedExperience(0);
	}

	/** V33a {@code ReikaInventoryHelper.checkForItemStack(stack, ep.inventory, false)}: anywhere in the inventory. */
	private static boolean carries(Player ep, Item item) {
		return ep.getInventory().contains(is -> is.is(item));
	}

	/**
	 * V33a {@code ItemPendant.onUpdate} with {@code NOPARTICLES} called {@code setNoPotionParticles} every tick,
	 * zeroing the player's potion-colour watcher until the effects next changed. 26.x rebuilds that particle list
	 * only when effects change, and this event is where it does so.
	 */
	private static void hideEffectParticles(EffectParticleModificationEvent ev) {
		if (ChromaOptions.NOPARTICLES.getState() && ev.getEntity() instanceof Player ep && ItemPendant.hasActivePendant(ep))
			ev.setVisible(false);
	}

}
