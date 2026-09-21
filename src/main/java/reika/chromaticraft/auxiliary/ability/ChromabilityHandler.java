/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.ability;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.api.abilityapi.Ability.Phase;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.registry.Chromabilities;

/** Drives V33a ambient Chromabilities on the matching half of every player tick. */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromabilityHandler {

	private static final double ENERGY_USE_CHANCE = 0.002;

	private ChromabilityHandler() {}

	@SubscribeEvent
	public static void playerTickPre(PlayerTickEvent.Pre event) {
		tick(event.getEntity(), Phase.START);
	}

	@SubscribeEvent
	public static void playerTickPost(PlayerTickEvent.Post event) {
		tick(event.getEntity(), Phase.END);
	}

	private static void tick(Player player, Phase phase) {
		for (Ability ability : Chromabilities.getAbilities()) {
			if (!ability.isTickBased() || ability.getTickPhase() != phase)
				continue;
			if (!ChromaAbilityData.hasAbility(player, ability) || !ChromaAbilityData.enabledOn(player, ability))
				continue;
			if (player.level().isClientSide() && !ability.actOnClient())
				continue;
			if (!ability.isFunctioningOn(player))
				continue;
			if (ability.canPlayerExecuteAt(player))
				ability.apply(player);

			if (!player.level().isClientSide() && player.getRandom().nextDouble() < ENERGY_USE_CHANCE)
				chargeTickCost(player, ability);
		}

		if (!player.level().isClientSide() && phase == Phase.END && player.tickCount % 40 == 0)
			removeAbilitiesWhoseRequirementsWereLost(player);

		// V33a additionally drains one random element in Mystcraft worlds carrying the BUFFERDRAIN
		// page. Mystcraft has no 26.2 port, so the integration remains intentionally dormant.
	}

	private static void chargeTickCost(Player player, Ability ability) {
		ElementTagCompound cost = ability instanceof Chromabilities nativeAbility
				? nativeAbility.getTickCost(player)
				: AbilityHelper.instance.getTickCost(ability, player);
		if (cost == null || cost.isEmpty())
			return;
		if (PlayerElementBuffer.instance.playerHas(player, cost)) {
			PlayerElementBuffer.instance.removeFromPlayer(player, cost);
		}
		else {
			ChromaAbilityData.setToPlayer(player, false, ability);
			ability.onRemoveFromPlayer(player);
		}
	}

	private static void removeAbilitiesWhoseRequirementsWereLost(Player player) {
		for (Ability ability : Chromabilities.getAbilities()) {
			if (ChromaAbilityData.hasAbility(player, ability) && !ability.isAvailableToPlayer(player))
				ChromaAbilityData.removeFromPlayer(player, ability);
		}
	}
}
