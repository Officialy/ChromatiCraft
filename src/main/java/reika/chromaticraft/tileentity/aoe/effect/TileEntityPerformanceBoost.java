/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe.effect;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.CrystalElement;


public class TileEntityPerformanceBoost extends TileEntityAdjacencyUpgrade {

	@Override
	protected EffectResult tickDirection(World world, int x, int y, int z, ForgeDirection dir, long startTime) {
		return EffectResult.STOP;
	}

	@Override
	public CrystalElement getColor() {
		return CrystalElement.PURPLE;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

}
