/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tools;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class WideCollectorRecipe extends MultiBlockCastingRecipe {

	public WideCollectorRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(Items.gold_ingot, -2, -2);
		this.addAuxItem(Items.gold_ingot, 2, 2);

		this.addAuxItem(ChromaStacks.auraIngot, -2, 0);
		this.addAuxItem(ChromaStacks.auraIngot, 2, 0);
		this.addAuxItem(ChromaStacks.auraIngot, 0, 2);
		this.addAuxItem(ChromaStacks.auraIngot, 0, -2);

		this.addRune(CrystalElement.LIME, -3, -1, -4);
		this.addRune(CrystalElement.BLACK, 3, -1, -4);
	}

}
