/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.recipemanagers.castingrecipes.special;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.MathHelper;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipe;
import reika.chromaticraft.registry.ExtraChromaIDs;
import reika.dragonapi.libraries.ReikaRecipeHelper;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import reika.dragonapi.modinteract.itemhandlers.tinkertoolhandler.TinkerPart;


public class TinkerToolPartRecipe extends CastingRecipe {

	private final TinkerPart part;

	public TinkerToolPartRecipe(TinkerPart p) {
		super(p.getItem(ExtraChromaIDs.CHROMAMATID.getValue()), getRecipe(p));
		part = p;
	}

	private static IRecipe getRecipe(TinkerPart p) {
		ItemStack out = p.getItem(ExtraChromaIDs.CHROMAMATID.getValue());
		ItemStack[] in = ReikaArrayHelper.getArrayOf(ChromaStacks.complexIngot, MathHelper.ceiling_float_int(p.getIngotCost()));
		in = Arrays.copyOf(in, in.length+1);
		in[in.length-1] = p.getPattern();
		return ReikaRecipeHelper.getShapelessRecipeFor(out, in);
	}

	@Override
	public int getNumberProduced() {
		return part.getIngotCost() >= 1 ? 1 : 2;
	}

}
