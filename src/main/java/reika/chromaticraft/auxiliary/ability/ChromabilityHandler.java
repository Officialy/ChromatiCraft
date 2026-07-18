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

import java.util.Collection;
import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.modinterface.MystPages;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickHandler;
import reika.dragonapi.auxiliary.trackers.tickregistry.TickType;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public class ChromabilityHandler implements TickHandler {

	public static final ChromabilityHandler instance = new ChromabilityHandler();

	private ChromabilityHandler() {

	}

	@Override
	public void tick(TickType type, Object... tickData) {
		EntityPlayer ep = (EntityPlayer) tickData[0];
		Collection<Ability> li = Chromabilities.getAbilitiesForTick((Phase)tickData[1]);
		for (Ability c : li) {
			if ((!ep.worldObj.isRemote || c.actOnClient()) && Chromabilities.playerHasAbility(ep, c) && Chromabilities.enabledOn(ep, c)) {
				if (!c.isFunctioningOn(ep))
					continue;
				if (Chromabilities.canPlayerExecuteAt(ep, c))
					c.apply(ep);
				if (ReikaRandomHelper.doWithChance(0.002)) { //was 0.0002
					ElementTagCompound tag = Chromabilities.getTickCost(c, ep);
					if (tag != null) {
						if (PlayerElementBuffer.instance.playerHas(ep, tag))
							PlayerElementBuffer.instance.removeFromPlayer(ep, tag);
						else {
							Chromabilities.setToPlayer(ep, false, c);
						}
					}
				}
			}
		}
		if (ep.ticksExisted%40 == 0) {
			for (Ability a : Chromabilities.getAbilities()) {
				if (Chromabilities.playerHasAbility(ep, a)) {
					if (a.isAvailableToPlayer(ep)) {

					}
					else {
						Chromabilities.removeFromPlayer(ep, a);
					}
				}
			}
		}
		if (ep.worldObj != null && ModList.MYSTCRAFT.isLoaded() && MystPages.Pages.BUFFERDRAIN.existsInWorld(ep.worldObj)) {
			PlayerElementBuffer.instance.removeFromPlayer(ep, CrystalElement.randomElement(), 1);
		}
		//ep.noClip = Chromabilities.ORECLIP.enabledOn(ep);
	}

	@Override
	public EnumSet<TickType> getType() {
		return EnumSet.of(TickType.PLAYER);
	}

	@Override
	public boolean canFire(Phase p) {
		return true;
	}

	@Override
	public String getLabel() {
		return "Chromabilities";
	}

}
