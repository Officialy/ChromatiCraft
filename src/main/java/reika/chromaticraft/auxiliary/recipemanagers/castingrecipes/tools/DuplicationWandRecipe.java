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
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class DuplicationWandRecipe extends PylonCastingRecipe {

	public DuplicationWandRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		//for now
		this.addAuxItem("stickWood", -2, -2);
		this.addAuxItem("stickWood", 2, 2);
		this.addAuxItem("stickWood", -2, 2);
		this.addAuxItem("stickWood", 2, -2);

		this.addAuxItem(ChromaStacks.auraIngot, -2, 0);
		this.addAuxItem(ChromaStacks.auraIngot, 2, 0);
		this.addAuxItem(ChromaStacks.auraIngot, 0, 2);
		this.addAuxItem(ChromaStacks.auraIngot, 0, -2);

		this.addAuxItem(Items.emerald, -4, 0);
		this.addAuxItem(Items.emerald, 4, 0);

		this.addAuxItem(ChromaStacks.spaceDust, 0, -4);
		this.addAuxItem(ChromaStacks.spaceDust, 0, 4);

		this.addAuxItem(ChromaStacks.lightBlueShard, -4, 2);
		this.addAuxItem(ChromaStacks.grayShard, -4, -2);
		this.addAuxItem(ChromaStacks.grayShard, 4, 2);
		this.addAuxItem(ChromaStacks.lightBlueShard, 4, -2);

		this.addAuxItem(ChromaStacks.grayShard, -2, 4);
		this.addAuxItem(ChromaStacks.lightBlueShard, -2, -4);
		this.addAuxItem(ChromaStacks.lightBlueShard, 2, 4);
		this.addAuxItem(ChromaStacks.grayShard, 2, -4);

		this.addAuxItem(Items.diamond, -4, 4);
		this.addAuxItem(Items.diamond, -4, -4);
		this.addAuxItem(Items.diamond, 4, 4);
		this.addAuxItem(Items.diamond, 4, -4);

		this.addAuraRequirement(CrystalElement.LIGHTBLUE, 20000);
		this.addAuraRequirement(CrystalElement.GRAY, 10000);
		this.addAuraRequirement(CrystalElement.BROWN, 15000);
	}

}
