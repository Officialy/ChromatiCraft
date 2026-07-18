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

import java.util.Arrays;
import java.util.Collection;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipeList;

import reika.chromaticraft.magic.artefact.UATrades;
import reika.dragonapi.auxiliary.VillageTradeHandler;
import reika.dragonapi.auxiliary.villagetradehandler.SimpleTradeHandler;
import reika.dragonapi.auxiliary.villagetradehandler.TradeToAdd;


public class CCTradeHandler extends SimpleTradeHandler {

	public static final CCTradeHandler instance = new CCTradeHandler();

	private CCTradeHandler() {

	}

	@Override
	public void manipulateTradesForVillager(EntityVillager ev, MerchantRecipeList li, VillageTradeHandler h) {
		super.manipulateTradesForVillager(ev, li, h);
		UATrades.instance.addTradesToVillager(ev, li, h);
	}

	@Override
	protected Collection<TradeToAdd> getTradesToAdd() {
		return Arrays.asList(new TradeToAdd(FocusCrystalTrade.class, 0.4, "FocusTrade").setVillagerType(1, 2, 3), new TradeToAdd(FragmentTrade.class, 0.4, "FragmentTrade").setVillagerType(2));
	}

}
