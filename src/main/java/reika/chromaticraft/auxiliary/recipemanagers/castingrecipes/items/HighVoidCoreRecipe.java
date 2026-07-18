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

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

public class HighVoidCoreRecipe extends HighCoreRecipe {

	public HighVoidCoreRecipe(ItemStack out) {
		super(out, CrystalElement.BLACK, CrystalElement.WHITE, new Coordinate(3, -1, -2), new Coordinate(-3, -1, 2), ChromaStacks.voidDust);
	}

}
