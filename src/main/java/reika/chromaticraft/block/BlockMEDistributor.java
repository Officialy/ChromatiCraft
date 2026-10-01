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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a MEDISTRIBUTOR on {@code TILEMODELLED2}: the ME Item Distributor. It is placed against an inventory and keeps
 * the clicked side as its facing ({@code SidePlacedTile.placeOnSide}: metadata = side): the ME cable connects on that
 * outer face, the inventory is behind it. Drawn entirely by its renderer; right-click opens its GUI.
 */
public final class BlockMEDistributor extends BlockChromaticTile {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public BlockMEDistributor(Properties properties) {
		super(properties, ChromaTiles.MEDISTRIBUTOR);
		this.registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	/** V33a {@code BlockChromaTile.onBlockActivated}: the tile's GUI. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityMEDistributor te))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(te, pos);
		return InteractionResult.SUCCESS;
	}

}
