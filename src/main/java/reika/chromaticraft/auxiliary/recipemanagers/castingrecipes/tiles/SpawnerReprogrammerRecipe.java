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

import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class SpawnerReprogrammerRecipe extends PylonCastingRecipe {

	public SpawnerReprogrammerRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(ChromaStacks.chromaIngot, 0, 2);
		this.addAuxItem(ChromaStacks.chromaIngot, -2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 2, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, -4, 0);
		this.addAuxItem(ChromaStacks.chromaIngot, 4, 0);

		this.addAuraRequirement(CrystalElement.BLACK, 4000);
		this.addAuraRequirement(CrystalElement.PINK, 16000);
		this.addAuraRequirement(CrystalElement.GRAY, 8000);

		//want more
	}

}
