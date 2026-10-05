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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaTiles;

import java.util.List;

/**
 * V33a {@code BlockAdjacencyUpgrade}, one identity per implemented colour (V33a's metadata): no collision, a
 * 0.375-block selection box in the middle, drawn only by its renderer (its icon was the transparent one). Breaking it,
 * picking it, or sneak-popping it gives its colour's item with the tile's tier, stored lumens and owners.
 */
public class BlockAdjacencyUpgrade extends BlockChromaticTile {

	private static final double R = 0.3125;
	private static final VoxelShape SELECTION = Shapes.box(R, R, R, 1-R, 1-R, 1-R);

	public final AdjacencyUpgrades upgrade;

	public BlockAdjacencyUpgrade(Properties props, AdjacencyUpgrades upgrade) {
		super(props, ChromaTiles.ADJACENCY);
		this.upgrade = upgrade;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return upgrade.createTileEntity(pos, state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SELECTION;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	/** V33a harvested the colour's item with {@code getTagsToWriteToStack}, the tier among them. */
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TileEntityAdjacencyUpgrade te) {
			for (ItemStack is : drops) {
				if (is.is(this.asItem()))
					te.writeToStack(is);
			}
		}
		return drops;
	}

	/** V33a {@code getPickBlock}: the colour's item carrying the tile's data. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack is = upgrade.getStackOfTier(0);
		if (level.getBlockEntity(pos) instanceof TileEntityAdjacencyUpgrade te)
			te.writeToStack(is);
		return is;
	}

}
