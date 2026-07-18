/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2018
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


public class ExplosionShieldRecipe extends PylonCastingRecipe {

	public ExplosionShieldRecipe(ItemStack out, ItemStack main) {
		super(out, main);

		for (int i = -4; i <= 4; i += 2) {
			for (int k = -4; k <= 4; k += 2) {
				if (Math.abs(i) == 4 || Math.abs(k) == 4) {
					this.addAuxItem(Blocks.obsidian, i, k);
				}
				else if (i != 0 || k != 0) {
					this.addAuxItem(ChromaStacks.avolite, i, k);
				}
			}
		}

		this.addAuxItem(ChromaStacks.auraDust, 0, -4);
		this.addAuxItem(ChromaStacks.voidDust, -4, 0);
		this.addAuxItem(ChromaStacks.voidDust, 4, 0);


		this.addAuraRequirement(CrystalElement.RED, 100000);
		this.addAuraRequirement(CrystalElement.LIME, 20000);
		this.addAuraRequirement(CrystalElement.BLACK, 20000);
		this.addAuraRequirement(CrystalElement.YELLOW, 8000);
	}

}
