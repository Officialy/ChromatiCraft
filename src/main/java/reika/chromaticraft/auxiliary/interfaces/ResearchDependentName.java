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

import java.util.Collection;

import net.minecraft.item.ItemStack;

import reika.chromaticraft.registry.ChromaResearch;

public interface ResearchDependentName {

	public Collection<ChromaResearch> getRequiredResearch(ItemStack is);

}
