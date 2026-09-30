/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.transport.TileEntityRift;

/**
 * V33a {@code BlockRift}: unbreakable (resistance 600000), cloth-sounding, walked through but selectable as a full
 * block, drawn only by its TESR. Any other block changing beside it passes through to the far rift, and it emits the
 * redstone its far rift receives. It is removed only by sneak-clicking it with the Manipulator, which drops the World
 * Rift item for both ends.
 */
public class BlockRift extends BlockChromaticTile {

	public BlockRift(Properties props) {
		super(props, ChromaTiles.RIFT);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block id, @Nullable Orientation orientation,
			boolean movedByPiston) {
		super.neighborChanged(state, world, pos, id, orientation, movedByPiston);
		if (id != this && !id.defaultBlockState().isAir() && world.getBlockEntity(pos) instanceof TileEntityRift te)
			te.passThrough();
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter iba, BlockPos pos, Direction s) {
		return iba.getBlockEntity(pos) instanceof TileEntityRift te ? te.getRedstoneLevel(s) : 0;
	}

}
