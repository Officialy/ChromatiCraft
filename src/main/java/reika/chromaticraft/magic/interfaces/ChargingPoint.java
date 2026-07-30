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

	public CrystalElement getDeliveredColor(Player ep, Level world, int clickX, int clickY, int clickZ);

	public int getEnergy(CrystalElement e);

	public boolean allowCharging(Player ep, CrystalElement e);

	public float getChargeRateMultiplier(Player ep, CrystalElement e);

	public void onUsedBy(Player ep, CrystalElement e);

	public boolean drain(CrystalElement e, int amt);

	public Coordinate getChargeParticleOrigin(Player ep, CrystalElement e);

	public float getHeldToolChargingPower(Player ep, CrystalElement e, ItemStack is);

}
