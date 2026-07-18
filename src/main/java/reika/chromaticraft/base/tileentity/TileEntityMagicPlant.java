/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.auxiliary.interfaces.EffectPlant;
import reika.chromaticraft.tileentity.plants.TileEntityAccelerationPlant;


public abstract class TileEntityMagicPlant extends TileEntityChromaticBase implements EffectPlant {

	public abstract ForgeDirection getGrowthDirection();

	protected final int getAccelerationPlants() {
		int n = 0;
		ForgeDirection dir = this.getGrowthDirection().getOpposite();
		TileEntity te = this.getAdjacentTileEntity(dir);
		while (te instanceof TileEntityAccelerationPlant) {
			TileEntityAccelerationPlant tile = (TileEntityAccelerationPlant)te;
			if (tile.isActive()) {
				n++;
				te = tile.getAdjacentTileEntity(dir);
			}
			else {
				break;
			}
		}
		return n;
	}

	public abstract boolean isPlantable(World world, int x, int y, int z);

}
