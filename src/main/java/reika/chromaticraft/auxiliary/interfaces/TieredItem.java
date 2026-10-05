/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;

public interface TieredItem {

	ProgressStage getDiscoveryTier(ItemStack is);
	boolean isTiered(ItemStack is);

}
