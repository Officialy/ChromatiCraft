/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import reika.chromaticraft.base.ItemChromaTool;
import reika.dragonapi.ModList;
import reika.dragonapi.asm.apistripper.Strippable;
import reika.dragonapi.asm.dependentmethodstripper.ModDependent;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Strippable("appeng.api.implementations.ICraftingPatternItem")
public class ItemPatternCrystal extends ItemChromaTool implements ICraftingPatternItem {

	public ItemPatternCrystal(int tex) {
		super(tex);
	}

	@Override
	@ModDependent(ModList.APPENG)
	public ICraftingPatternDetails getPatternForItem(ItemStack is, World w) {
		return null;
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack is, EntityPlayer ep, List li, boolean vb) {

	}

}
