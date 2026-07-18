/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.api.AdjacencyUpgradeAPI;

/** Supply an instance of this to the {@link AdjacencyUpgradeAPI} to specify custom range expansion behavior on your BlockEntity
 * if it has a range of effect (beyond one block) and which can have their range increased by an appropriate booster block.
 *
 *  This is for custom behavior;
 *	Use AdjacencyUpgradeAPI.addBasicRangeBoost if all you want to do is register simple behavior where the TE implements the RangeUpgradeable interface. */
public interface CustomRangeUpgrade extends CustomAdjacencyHandler {

	/** Called every tick; implementation is left up to you, whether it be setting a flag or increasing the range each tick to the maximum, or
	 * some other entirely different approach. Args: Your TE, Maximum increase factor. */
	public void upgradeRange(BlockEntity te, double r);

	public static interface RangeUpgradeable {

		public int getRange();

		public void upgradeRange(double r);

	}

}


