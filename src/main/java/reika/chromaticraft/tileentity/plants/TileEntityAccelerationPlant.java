/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.block.BlockDecoPlant;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

/** V33a Enrichment Vine. Every consecutive active vine adds one operation per tick to its plant. */
public final class TileEntityAccelerationPlant extends TileEntityMagicPlant {

	public TileEntityAccelerationPlant(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.PLANT_ACCELERATOR.get(), pos, state);
	}

	@Override public Direction getGrowthDirection() { return null; }

	@Override public ChromaTiles getTile() { return ChromaTiles.PLANTACCEL; }

	public boolean isActive() { return true; }

	@Override public void updateEntity(Level world, BlockPos pos) {
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override public boolean isPlantable(Level world, BlockPos pos) {
		return BlockDecoPlant.canAccelerationPlantSurvive(world, pos);
	}
}
