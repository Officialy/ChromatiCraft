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
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.PylonCastingRecipe;
import reika.chromaticraft.registry.CrystalElement;


public class WarpGateRecipe extends PylonCastingRecipe {

	public WarpGateRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		this.addAuxItem(Items.iron_ingot, -4, 0);
		this.addAuxItem(Items.iron_ingot, 4, 0);
		this.addAuxItem(Items.iron_ingot, 0, 4);
		this.addAuxItem(Items.iron_ingot, 0, -4);

		this.addAuxItem(Items.iron_ingot, -2, -2);
		this.addAuxItem(Items.iron_ingot, 2, -2);
		this.addAuxItem(Items.iron_ingot, -2, 2);
		this.addAuxItem(Items.iron_ingot, 2, 2);

		this.addAuxItem(ChromaStacks.spaceDust, -4, -4);
		this.addAuxItem(ChromaStacks.spaceDust, 4, -4);
		this.addAuxItem(ChromaStacks.spaceDust, -4, 4);
		this.addAuxItem(ChromaStacks.spaceDust, 4, 4);

		this.addAuxItem(ChromaStacks.enderDust, -2, 0);
		this.addAuxItem(ChromaStacks.enderDust, 2, 0);
		this.addAuxItem(ChromaStacks.enderDust, 0, 2);
		this.addAuxItem(ChromaStacks.enderDust, 0, -2);

		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), -2, -4);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), 2, -4);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), -2, 4);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), 2, 4);

		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), -4, -2);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), 4, -2);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), -4, 2);
		this.addAuxItem(ChromaStacks.getChargedShard(CrystalElement.LIME), 4, 2);

		this.addAuraRequirement(CrystalElement.LIME, 60000);
		this.addAuraRequirement(CrystalElement.BLACK, 20000);
		this.addAuraRequirement(CrystalElement.LIGHTBLUE, 10000);
		this.addAuraRequirement(CrystalElement.PURPLE, 10000);
	}

	@Override
	public int getNumberProduced() {
		return 2;
	}

}
