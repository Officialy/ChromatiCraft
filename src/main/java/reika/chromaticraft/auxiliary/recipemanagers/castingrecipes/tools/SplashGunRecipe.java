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
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

public class SplashGunRecipe extends MultiBlockCastingRecipe {

	public SplashGunRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.auraDust, -2, -2);
		this.addAuxItem(ChromaStacks.auraDust, -2, -4);
		this.addAuxItem(ChromaStacks.auraDust, 2, -2);
		this.addAuxItem(ChromaStacks.auraDust, 2, -4);

		this.addAuxItem(ChromaItems.LENS.getStackOf(CrystalElement.BLACK), 0, -4);

		this.addAuxItem(Items.diamond, 0, -2);

		this.addAuxItem(ChromaStacks.conductiveIngot, -2, 0);
		this.addAuxItem(ChromaStacks.conductiveIngot, 2, 0);
		this.addAuxItem(ChromaStacks.conductiveIngot, 0, 2);

		this.addAuxItem("stickWood", 0, 4);
	}

}
