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

import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public interface CustomAdjacencyHandler {

	/** A basic description of the effect. */
    String getDescription();

	/** A list of the item/held versions of the blocks on which this effect applies. Used for lexicon "show relevant items" display. */
    Collection<ItemStack> getItems();

}
