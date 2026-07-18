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

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.EnergyLinkingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.MultiBlockCastingRecipe;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

public class RelaySourceRecipe extends MultiBlockCastingRecipe implements EnergyLinkingRecipe {

	public RelaySourceRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(Items.iron_ingot, -4, -4);
		this.addAuxItem(ChromaStacks.elementDust, -2, -4);
		this.addAuxItem(ChromaStacks.focusDust, 0, -4);
		this.addAuxItem(ChromaStacks.elementDust, 2, -4);
		this.addAuxItem(Items.iron_ingot, 4, -4);

		this.addAuxItem(ChromaStacks.focusDust, 0, -2);
		this.addAuxItem(ChromaStacks.focusDust, 0, -4);
		this.addAuxItem(ChromaStacks.focusDust, 0, 2);

		this.addAuxItem(ChromaItems.LENS.getAnyMetaStack(), -4, 0);
		this.addAuxItem(ChromaItems.LENS.getAnyMetaStack(), 4, 0);

		this.addAuxItem(Blocks.glowstone, -2, 0);
		this.addAuxItem(Blocks.glowstone, 2, 0);

		this.addAuxItem(Blocks.obsidian, -4, -2);
		this.addAuxItem(Blocks.obsidian, -2, -2);
		this.addAuxItem(Blocks.obsidian, 2, -2);
		this.addAuxItem(Blocks.obsidian, 4, -2);

		this.addAuxItem(Blocks.obsidian, -4, 2);
		this.addAuxItem(Blocks.obsidian, -2, 2);
		this.addAuxItem(Blocks.obsidian, 2, 2);
		this.addAuxItem(Blocks.obsidian, 4, 2);

		this.addAuxItem(Items.iron_ingot, -4, 4);
		this.addAuxItem(Items.iron_ingot, -2, 4);
		this.addAuxItem(Items.iron_ingot, 0, 4);
		this.addAuxItem(Items.iron_ingot, 2, 4);
		this.addAuxItem(Items.iron_ingot, 4, 4);

		this.addRune(CrystalElement.BLACK, -4, 0, -5);
		this.addRune(CrystalElement.BLUE, 5, 0, -4);
		this.addRune(CrystalElement.WHITE, 4, 0, 5);
		this.addRune(CrystalElement.YELLOW, -5, 0, 4);
	}

}
