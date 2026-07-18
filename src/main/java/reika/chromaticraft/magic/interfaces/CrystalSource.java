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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.registry.CrystalElement;

public interface CrystalSource extends CrystalTransmitter, LumenTile {

	//public int getTransmissionStrength();

	public boolean drain(CrystalElement e, int amt);

	/** Higher number = higher priority */
	//public int getSourcePriority();

	public boolean canSupply(CrystalReceiver te, CrystalElement e);

	public void onUsedBy(EntityPlayer ep, CrystalElement e);

	public boolean playerCanUse(EntityPlayer ep);

	public double getMaximumBeamRadius();

	public float getDroppedItemChargeRate(ItemStack is);

}
