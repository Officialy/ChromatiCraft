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
import net.minecraft.item.ItemStack;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;

public class TelePumpRecipe extends PylonCastingRecipe {

	public TelePumpRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(Blocks.glass, -2, 2);
		this.addAuxItem(Blocks.glass, 0, 2);
		this.addAuxItem(Blocks.glass, 2, 2);

		this.addAuxItem(Blocks.glass, -2, -2);
		this.addAuxItem(Blocks.glass, 0, -2);
		this.addAuxItem(Blocks.glass, 2, -2);

		this.addAuxItem(Blocks.cobblestone, -2, 4);
		this.addAuxItem(Blocks.cobblestone, 0, 4);
		this.addAuxItem(Blocks.cobblestone, 2, 4);

		this.addAuxItem(Blocks.cobblestone, -2, -4);
		this.addAuxItem(Blocks.cobblestone, 0, -4);
		this.addAuxItem(Blocks.cobblestone, 2, -4);

		this.addAuxItem(ChromaStacks.waterIngot, -4, 4);
		this.addAuxItem(ChromaStacks.waterIngot, -4, -4);
		this.addAuxItem(ChromaStacks.waterIngot, 4, 4);
		this.addAuxItem(ChromaStacks.waterIngot, 4, -4);

		this.addAuxItem(ChromaStacks.enderIngot, -4, 2);
		this.addAuxItem(ChromaStacks.enderIngot, -4, 0);
		this.addAuxItem(ChromaStacks.enderIngot, -4, -2);

		this.addAuxItem(ChromaStacks.enderIngot, 4, 2);
		this.addAuxItem(ChromaStacks.enderIngot, 4, 0);
		this.addAuxItem(ChromaStacks.enderIngot, 4, -2);

		this.addAuxItem(ChromaStacks.beaconDust, 2, 0);
		this.addAuxItem(ChromaStacks.beaconDust, -2, 0);

		this.addAuraRequirement(CrystalElement.BLACK, 8000);
		this.addAuraRequirement(CrystalElement.CYAN, 16000);
		this.addAuraRequirement(CrystalElement.LIME, 32000);
	}

}
