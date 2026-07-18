/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.dragonapi.modinteract.itemhandlers.AppEngHandler;

public class MEDistributorRecipe extends MultiBlockCastingRecipe {

	public MEDistributorRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(AppEngHandler.getInstance().getQuartzProcessor(), 0, -4);
		this.addAuxItem(AppEngHandler.getInstance().getQuartzProcessor(), -2, -2);
		this.addAuxItem(AppEngHandler.getInstance().getQuartzProcessor(), 2, -2);

		this.addAuxItem(AppEngHandler.getInstance().getDiamondProcessor(), 0, -2);

		this.addAuxItem(AppEngHandler.getInstance().getGoldProcessor(), -2, 2);
		this.addAuxItem(AppEngHandler.getInstance().getGoldProcessor(), 2, 2);
		this.addAuxItem(AppEngHandler.getInstance().getGoldProcessor(), -2, 4);
		this.addAuxItem(AppEngHandler.getInstance().getGoldProcessor(), 2, 4);

		this.addAuxItem(Items.iron_ingot, -4, 0);
		this.addAuxItem(Items.iron_ingot, -4, 2);
		this.addAuxItem(Items.iron_ingot, -4, 4);
		this.addAuxItem(Items.iron_ingot, 4, 0);
		this.addAuxItem(Items.iron_ingot, 4, 2);
		this.addAuxItem(Items.iron_ingot, 4, 4);

		this.addAuxItem(ChromaStacks.auraIngot, -2, 0);
		this.addAuxItem(ChromaStacks.auraIngot, 2, 0);

		this.addAuxItem(Items.iron_ingot, -4, -2);
		this.addAuxItem(Items.iron_ingot, 4, -2);

		this.addAuxItem(ChromaStacks.conductiveIngot, -2, -4);
		this.addAuxItem(ChromaStacks.conductiveIngot, 2, -4);
	}

}
