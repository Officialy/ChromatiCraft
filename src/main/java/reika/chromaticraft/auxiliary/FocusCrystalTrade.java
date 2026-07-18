/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.tileentity.auxiliary.tileentityfocuscrystal.CrystalTier;
import reika.dragonapi.interfaces.PlayerSpecificTrade;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;


public class FocusCrystalTrade extends MerchantRecipe implements PlayerSpecificTrade {

	public FocusCrystalTrade() {
		super(new ItemStack(Items.emerald, 1, 0), CrystalTier.FLAWED.getCraftedItem());
	}

	@Override
	public void incrementToolUses() {
		//No-op to prevent expiry
		ReikaJavaLibrary.pConsole("Player purchased focus crystals");
	}

	@Override
	public boolean isValid(EntityPlayer ep) {
		return ProgressStage.CRYSTALS.isPlayerAtStage(ep);
	}

}
