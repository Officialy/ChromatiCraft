/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items;

import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

public class UpgradeRecipe extends MultiBlockCastingRecipe {

	public UpgradeRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.elementDust, -2, -2);
		this.addAuxItem(ChromaStacks.elementDust, 2, -2);
		this.addAuxItem(ChromaStacks.elementDust, -2, 2);
		this.addAuxItem(ChromaStacks.elementDust, 2, 2);

		this.addAuxItem(ChromaItems.ELEMENTAL.getStackOfMetadata(CrystalElement.PURPLE.ordinal()), -2, 0);
		this.addAuxItem(ChromaItems.ELEMENTAL.getStackOfMetadata(CrystalElement.PURPLE.ordinal()), 2, 0);
		this.addAuxItem(ChromaItems.ELEMENTAL.getStackOfMetadata(CrystalElement.BLACK.ordinal()), 0, -2);
		this.addAuxItem(ChromaStacks.bindingCrystal, 0, 2);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 16;
	}

}
