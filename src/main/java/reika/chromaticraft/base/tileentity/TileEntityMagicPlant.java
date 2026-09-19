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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.EffectPlant;
import reika.chromaticraft.tileentity.plants.TileEntityAccelerationPlant;

/** Shared V33a behavior for the functional decorative plants. */
public abstract class TileEntityMagicPlant extends TileEntityChromaticBase implements EffectPlant {

	protected TileEntityMagicPlant(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract Direction getGrowthDirection();

	protected final int getAccelerationPlants() {
		Direction growth = this.getGrowthDirection();
		if (growth == null || level == null) return 0;
		Direction direction = growth.getOpposite();
		BlockPos cursor = worldPosition.relative(direction);
		int count = 0;
		while (level.hasChunkAt(cursor)) {
			BlockEntity blockEntity = level.getBlockEntity(cursor);
			if (!(blockEntity instanceof TileEntityAccelerationPlant plant) || !plant.isActive()) break;
			count++;
			cursor = cursor.relative(direction);
		}
		return count;
	}

	public abstract boolean isPlantable(Level world, BlockPos pos);
}
