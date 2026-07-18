/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.tileentity.tileentitycrystalconsole.TileControl;


public class ItemCrystalCard extends ItemChromaTool {

	public ItemCrystalCard(int index) {
		super(index);
	}

	public static ArrayList<TileControl> getControllers(ItemStack is) {
		return null;
	}

}
