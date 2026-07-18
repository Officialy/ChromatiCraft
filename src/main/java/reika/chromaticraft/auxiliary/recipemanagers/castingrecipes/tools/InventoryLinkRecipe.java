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

import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class InventoryLinkRecipe extends MultiBlockCastingRecipe {

	public InventoryLinkRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.resonanceDust, -2, 0);
		this.addAuxItem(ChromaStacks.resonanceDust, 2, 0);
		this.addAuxItem(ChromaStacks.resonanceDust, 0, 2);
		this.addAuxItem(ChromaStacks.resonanceDust, 0, -2);

		this.addAuxItem(ReikaItemHelper.lapisDye, -4, 0);
		this.addAuxItem(ReikaItemHelper.lapisDye, 4, 0);
		this.addAuxItem(ReikaItemHelper.lapisDye, 0, 4);
		this.addAuxItem(ReikaItemHelper.lapisDye, 0, -4);

		//this.addAuraRequirement(CrystalElement.LIME, 5000);
		//this.addAuraRequirement(CrystalElement.BLACK, 1000);

		this.addRune(CrystalElement.LIME, -3, -1, -4);
		this.addRune(CrystalElement.BLACK, 3, -1, -4);
	}

}
