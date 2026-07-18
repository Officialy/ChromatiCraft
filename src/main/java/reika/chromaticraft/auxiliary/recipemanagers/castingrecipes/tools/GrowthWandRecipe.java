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
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class GrowthWandRecipe extends MultiBlockCastingRecipe {

	public GrowthWandRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		//for now
		this.addAuxItem("stickWood", -2, -2);
		this.addAuxItem("stickWood", 2, 2);
		this.addAuxItem("stickWood", -2, 2);
		this.addAuxItem("stickWood", 2, -2);

		this.addAuxItem(ChromaStacks.chromaIngot, -2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 0, 2);
		this.addAuxItem(ChromaStacks.chromaIngot, 0, -2);

		this.addAuxItem(Items.water_bucket, -4, 0);
		this.addAuxItem(Items.water_bucket, 4, 0);

		this.addAuxItem(ChromaStacks.auraDust, 0, -4);
		this.addAuxItem(ChromaStacks.auraDust, 0, 4);

		this.addAuxItem(ChromaStacks.greenShard, 4, -2);
		this.addAuxItem(ChromaStacks.greenShard, -4, 2);
		this.addAuxItem(ChromaStacks.greenShard, -2, -4);
		this.addAuxItem(ChromaStacks.greenShard, 2, 4);

		this.addAuxItem(ChromaStacks.livingEssence, -4, -2);
		this.addAuxItem(ChromaStacks.livingEssence, -2, 4);
		this.addAuxItem(ChromaStacks.livingEssence, 4, 2);
		this.addAuxItem(ChromaStacks.livingEssence, 2, -4);

		this.addAuxItem(ReikaItemHelper.bonemeal, -4, 4);
		this.addAuxItem(ReikaItemHelper.bonemeal, -4, -4);
		this.addAuxItem(ReikaItemHelper.bonemeal, 4, 4);
		this.addAuxItem(ReikaItemHelper.bonemeal, 4, -4);
	}

}
