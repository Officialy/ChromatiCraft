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

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipe.TempleCastingRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.tiles.HeatLilyRecipe;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class HeatLampRecipe extends TempleCastingRecipe {

	public HeatLampRecipe(Object ingot, int n, HeatLilyRecipe r, boolean isColdLamp, boolean isT2) {
		super(calcOutput(n, isColdLamp), getRecipe(ingot, n, isColdLamp, isT2));

		this.addRunes(r.getRunes());
		if (isColdLamp) {
			this.addRune(CrystalElement.WHITE, 0, -1, -4);
			this.addRuneRingRune(CrystalElement.LIGHTGRAY);
		}
	}

	private static IRecipe getRecipe(Object ingot, int n, boolean isColdLamp, boolean isT2) {
		return new ShapedOreRecipe(calcOutput(n, isColdLamp), "fgf", "gag", "fgf", 'g', isColdLamp ? new ItemStack(Items.snowball) : (isT2 ? ChromaStacks.thermiticCrystal : ChromaStacks.firaxite), 'f', isColdLamp ? ChromaStacks.icyDust : ChromaStacks.firaxite, 'a', ingot);
	}

	private static ItemStack calcOutput(int n, boolean isColdLamp) {
		return ReikaItemHelper.getSizedItemStack(ChromaBlocks.HEATLAMP.getStackOfMetadata(isColdLamp ? 8 : 0), n);
	}

	@Override
	public int getTypicalCraftedAmount() {
		return 8;
	}

	@Override
	public boolean canGiveDoubleOutput() {
		return true;
	}

}
