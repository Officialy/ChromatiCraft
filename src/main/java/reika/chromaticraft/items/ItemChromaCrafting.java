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

import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;

import reika.chromaticraft.ChromaNames;
import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.ResearchDependentName;
import reika.chromaticraft.base.ItemChromaMulti;
import reika.chromaticraft.block.blockactivechroma.TileEntityChroma;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaResearch;
import reika.dragonapi.interfaces.item.AnimatedSpritesheet;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class ItemChromaCrafting extends ItemChromaMulti implements ResearchDependentName, AnimatedSpritesheet {

	public ItemChromaCrafting(int tex) {
		super(tex);
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem ei) {
		int x = MathHelper.floor_double(ei.posX);
		int y = MathHelper.floor_double(ei.posY);
		int z = MathHelper.floor_double(ei.posZ);
		ItemStack is = ei.getEntityItem();
		if (ReikaItemHelper.matchStacks(is, ChromaStacks.etherBerries)) {
			Block b = ei.worldObj.getBlock(x, y, z);
			if (b == ChromaBlocks.CHROMA.getBlockInstance()) {
				if (ei.worldObj.getBlockMetadata(x, y, z) == 0) {
					if (this.canCharge(ei)) {
						TileEntity te = ei.worldObj.getTileEntity(x, y, z);
						if (te instanceof TileEntityChroma) {
							TileEntityChroma tc = (TileEntityChroma)te;
							int df = is.stackSize;
							//ReikaJavaLibrary.pConsole("pre "+is.stackSize, Side.SERVER);
							int amt = tc.etherize(is.stackSize);
							//ReikaJavaLibrary.pConsole(amt+" from "+is.stackSize, Side.SERVER);
							if (!ei.worldObj.isRemote)
								is.stackSize -= amt;
							//ReikaJavaLibrary.pConsole(ei.age+":"+amt+", "+df+">"+is.stackSize, Side.SERVER);
							if (is.stackSize <= 0)
								ei.setDead();
							else
								ei.setEntityItemStack(is);
						}
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
			if (ProgressStage.ALLOY.playerHasPrerequisites(ep)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getNumberTypes() {
		return ChromaNames.craftingNames.length;
	}

	@Override
	public Collection<ChromaResearch> getRequiredResearch(ItemStack is) {
		ChromaResearch r = ChromaResearch.getPageFor(is);
		if (r == null)
			return null;
		switch(r) {
			case CORES:
				return ReikaJavaLibrary.makeListFrom(ChromaResearch.CORES);
			case ALLOYS:
				return ReikaJavaLibrary.makeListFrom(ChromaResearch.ALLOYS);
			default:
				return null;
		}
	}

	@Override
	public boolean useAnimatedRender(ItemStack is) {
		return ReikaItemHelper.matchStacks(ChromaStacks.complexIngot, is);
	}

	@Override
	public int getFrameCount(ItemStack is) {
		return 16;
	}

	@Override
	public int getBaseRow(ItemStack is) {
		return 0;
	}

	@Override
	public int getColumn(ItemStack is) {
		return 0;
	}

	@Override
	public int getFrameOffset(ItemStack is) {
		return 0;
	}

	@Override
	public int getFrameSpeed(ItemStack is) {
		return 4;
	}

	@Override
	public String getTexture(ItemStack is) {
		return this.useAnimatedRender(is) ? "/Reika/ChromatiCraft/Textures/Items/miscanim.png" : super.getTexture(is);
	}

	@Override
	public boolean verticalFrames() {
		return false;
	}

}
