/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.tab;

import net.minecraft.item.ItemStack;

import reika.dragonapi.instantiable.gui.RegistryEnumCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TabChromatiCraft extends RegistryEnumCreativeTab {

	private ItemStack icon;

	public TabChromatiCraft(String tabID) {
		super(tabID);
		this.setNoTitle();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ItemStack getIconItemStack() {
		return icon;
	}

	public void setIcon(ItemStack is) {
		icon = is;
	}
}
