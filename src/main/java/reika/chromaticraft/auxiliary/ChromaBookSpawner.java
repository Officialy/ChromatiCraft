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

import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaItems;
import reika.dragonapi.instantiable.ItemOnSpawnTracker;

public class ChromaBookSpawner extends ItemOnSpawnTracker {

	public ItemStack getItem() {
		return ChromaItems.LEXICON.get().getDefaultInstance();
	}

	@Override
	public String getID() {
		return "ChromatiCraft_Handbook";
	}

}
