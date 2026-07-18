/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import reika.chromaticraft.auxiliary.interfaces.ResearchDependentName;
import reika.chromaticraft.base.ItemChromaMulti;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaResearch;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
public class ItemCluster extends ItemChromaMulti implements ResearchDependentName {

	private final IIcon[] icons = new IIcon[this.getNumberTypes()];

	public ItemCluster(int tex) {
		super(tex);
		hasSubtypes = true;
	}

	@Override
	public int getNumberTypes() {
		return ChromaItems.CLUSTER.getNumberMetadatas();
	}

	@Override
	public Collection<ChromaResearch> getRequiredResearch(ItemStack is) {
		return ReikaJavaLibrary.makeListFrom(ChromaResearch.GROUPS);
	}
}
