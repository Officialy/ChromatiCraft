/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.ShardGroupingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaRecipeHelper;

public class CrystalClusterRecipe extends TempleCastingRecipe implements ShardGroupingRecipe {

	public CrystalClusterRecipe(ItemStack out, CrystalGroupRecipe r1, CrystalGroupRecipe r2) {
		super(out, getRecipe(out));

		this.addRunes(r1.getRunes());
		this.addRunes(r2.getRunes());
	}

	@Override
	public boolean canBeSimpleAutomated() {
		return true;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

	private static IRecipe getRecipe(ItemStack out) {
		int mod = (out.getItemDamage()-ChromaStacks.primaryCluster.getItemDamage())%2;
		ItemStack is1 = ChromaItems.CLUSTER.getStackOfMetadata(2*mod);
		ItemStack is2 = ChromaItems.CLUSTER.getStackOfMetadata(1+2*mod);

		ItemStack ctr = getShard(mod == 1 ? CrystalElement.BLACK : CrystalElement.WHITE);

		Object[] o = {" A ", "BFB", " A ", 'A', is1, 'B', is2, 'F', ctr};
		return ReikaRecipeHelper.getShapedRecipeFor(out, o);
	}

}
