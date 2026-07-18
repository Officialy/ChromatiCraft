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

public class HighEnergyCoreRecipe extends HighCoreRecipe {

	public HighEnergyCoreRecipe(ItemStack out) {
		super(out, CrystalElement.YELLOW, CrystalElement.WHITE, new Coordinate(-3, 0, -2), new Coordinate(3, 0, 2), ChromaStacks.energyPowder);
	}

}
