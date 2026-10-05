/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.Coordinate;


public interface ChargingPoint extends CrystalNetworkTile, LumenTile {

	CrystalElement getDeliveredColor(Player ep, Level world, int clickX, int clickY, int clickZ);

	int getEnergy(CrystalElement e);

	boolean allowCharging(Player ep, CrystalElement e);

	float getChargeRateMultiplier(Player ep, CrystalElement e);

	void onUsedBy(Player ep, CrystalElement e);

	boolean drain(CrystalElement e, int amt);

	Coordinate getChargeParticleOrigin(Player ep, CrystalElement e);

	float getHeldToolChargingPower(Player ep, CrystalElement e, ItemStack is);

}
