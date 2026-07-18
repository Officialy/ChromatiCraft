/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.blocks;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.crafting.IRecipe;

import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaRecipeHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;


public class CrystalAltarRecipe extends TempleCastingRecipe {

	public CrystalAltarRecipe(CrystalElement e) {
		super(ChromaBlocks.COLORALTAR.getStackOfMetadata(e.ordinal()), getRecipe(e));

		this.addRuneRingRune(e);

		this.addRune(CrystalElement.BLUE, -1, -1, -3);
	}

	private static IRecipe getRecipe(CrystalElement e) {
		return ReikaRecipeHelper.getShapedRecipeFor(ChromaBlocks.COLORALTAR.getStackOfMetadata(e.ordinal()), "qgq", "cIc", "SOS", 'c', Blocks.cobblestone, 'O', Blocks.obsidian, 'I', Items.iron_ingot, 'S', ReikaItemHelper.stoneSlab, 'q', Items.quartz, 'g', ChromaBlocks.LAMP.getStackOfMetadata(e.ordinal()));
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 16;
	}

}
