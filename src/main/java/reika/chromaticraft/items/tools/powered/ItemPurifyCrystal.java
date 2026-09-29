/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.powered;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import reika.chromaticraft.base.ItemPoweredChromaTool;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemPurifyCrystal} ("Aura Purification Gem"): while charged and anywhere in the inventory it shields
 * its carrier from hostile worlds, so the Nether stops turning crystal effects against them and they accept only
 * good ones. It drains twelve times faster in a hostile world: 36 hours of charge in the Overworld, 3 in the
 * Nether. The carrier's orbiting motes are drawn by {@code PurifyCrystalClient}, since 26.x ticks inventory
 * items on the server only.
 */
public class ItemPurifyCrystal extends ItemPoweredChromaTool {

	private static final String TAG = "last_purify";
	public static final int CHARGE_STATES = 4;
	public static final int MAX_CHARGE = 216000*12; //3h in the Nether, 36h in the overworld

	public ItemPurifyCrystal(Properties properties) {
		super(properties);
	}

	@Override
	public boolean doTick(ItemStack is, Level world, Player e, boolean held) {
		e.getPersistentData().putLong(TAG, world.getGameTime());
		return true;
	}

	public static boolean isActive(Player ep) {
		return ep.level().getGameTime()-ep.getPersistentData().getLongOr(TAG, 0) < 20;
	}

	@Override
	public boolean isActivated(Player e, ItemStack is, boolean held) {
		return true;
	}

	@Override
	public CrystalElement getColor(ItemStack is) {
		return CrystalElement.WHITE;
	}

	@Override
	public int getMaxCharge() {
		return MAX_CHARGE;
	}

	@Override
	public int getChargeStates() {
		return CHARGE_STATES;
	}

	@Override
	public int getChargeConsumptionRate(Player e, Level world, ItemStack is) {
		return CrystalPotionController.instance.isWorldHostile(world) ? 12 : 1;
	}

}
