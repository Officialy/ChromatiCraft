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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.registry.ChromaItems;

public class ItemPylonFinder extends ItemChromaTool {

	public static final String NBT_KEY = "pylonoverlay";

	public ItemPylonFinder(int index) {
		super(index);
		ItemAuraPouch.setSpecialEffect(ItemAuraPouch.WORKS_IN_POUCH_EFFECT_DESC, this);
	}

	@Override
	public void onUpdate(ItemStack is, World world, Entity e, int slot, boolean selected) {
		if (e instanceof EntityPlayer && ChromaItems.AURAPOUCH.matchWith(((EntityPlayer)e).inventory.mainInventory[slot]) && world.getTotalWorldTime()%8 == 0)
			e.getEntityData().setLong(NBT_KEY, world.getTotalWorldTime());
	}

}
