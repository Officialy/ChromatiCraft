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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.interfaces.TieredItem;
import reika.chromaticraft.base.ItemChromaMulti;
import reika.chromaticraft.block.dimension.blockdimensiondeco.DimDecoTypes;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class ItemDimGen extends ItemChromaMulti implements TieredItem {

	public ItemDimGen(int tex) {
		super(tex);
	}

	@Override
	public boolean onItemUse(ItemStack is, EntityPlayer ep, World world, int x, int y, int z, int s, float a, float b, float c) {
		if (ReikaItemHelper.matchStacks(is, ChromaStacks.miasma)) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[s];
			int dx = x+dir.offsetX;
			int dy = y+dir.offsetY;
			int dz = z+dir.offsetZ;
			if (world.getBlock(dx, dy, dz).isAir(world, dx, dy, dz)) {
				world.setBlock(dx, dy, dz, ChromaBlocks.DIMGEN.getBlockInstance(), DimDecoTypes.MIASMA.ordinal(), 3);
				if (!ep.capabilities.isCreativeMode)
					is.stackSize--;
				return true;
			}
		}
		return super.onItemUse(is, ep, world, x, y, z, s, a, b, c);
	}

	@Override
	public ProgressStage getDiscoveryTier(ItemStack is) {
		return ProgressStage.DIMENSION;
	}

	@Override
	public boolean isTiered(ItemStack is) {
		return true;
	}

}
