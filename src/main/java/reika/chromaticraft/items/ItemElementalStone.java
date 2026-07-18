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

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;

import reika.chromaticraft.auxiliary.interfaces.TieredItem;
import reika.chromaticraft.base.ItemCrystalBasic;
import reika.chromaticraft.block.blockactivechroma.TileEntityChroma;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class ItemElementalStone extends ItemCrystalBasic implements TieredItem {

	public ItemElementalStone(int tex) {
		super(tex);
	}

	@Override
	public ProgressStage getDiscoveryTier(ItemStack is) {
		return ProgressStage.RUNEUSE;
	}

	@Override
	public boolean isTiered(ItemStack is) {
		return true;
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem ei) {
		int x = MathHelper.floor_double(ei.posX);
		int y = MathHelper.floor_double(ei.posY);
		int z = MathHelper.floor_double(ei.posZ);
		Block b = ei.worldObj.getBlock(x, y, z);
		if (b == ChromaBlocks.CHROMA.getBlockInstance()) {
			if (ei.worldObj.getBlockMetadata(x, y, z) == 0) {
				if (this.canCharge(ei)) {
					TileEntity te = ei.worldObj.getTileEntity(x, y, z);
					if (te instanceof TileEntityChroma) {
						TileEntityChroma tc = (TileEntityChroma)te;
						ItemStack is = ei.getEntityItem();
						boolean flag = tc.addElementalStone(CrystalElement.elements[is.getItemDamage()]);
						if (flag && !ei.worldObj.isRemote)
							is.stackSize--;
						if (is.stackSize <= 0)
							ei.setDead();
						else
							ei.setEntityItemStack(is);
					}
				}
			}
		}
		return false;
	}

	private boolean canCharge(EntityItem ei) {
		EntityPlayer ep = ReikaItemHelper.getDropper(ei);
		if (ep != null) {
			if (ProgressStage.SHARDCHARGE.playerHasPrerequisites(ep)) {
				return true;
			}
		}
		return false;
	}

}
