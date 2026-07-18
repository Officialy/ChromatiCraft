/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.EnergyLinkingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;

public class CompoundRelayRecipe extends MultiBlockCastingRecipe implements EnergyLinkingRecipe {

	public CompoundRelayRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.elementDust, -2, -2);
		this.addAuxItem(ChromaStacks.elementDust, 2, -2);
		this.addAuxItem(ChromaStacks.elementDust, -2, 2);
		this.addAuxItem(ChromaStacks.elementDust, 2, 2);

		this.addAuxItem(ChromaStacks.beaconDust, -2, 0);
		this.addAuxItem(ChromaStacks.beaconDust, 2, 0);
		this.addAuxItem(ChromaStacks.beaconDust, 0, -2);

		this.addAuxItem(new ItemStack(ChromaBlocks.RELAY.getBlockInstance(), 1, OreDictionary.WILDCARD_VALUE), 0, 2);
	}

}
