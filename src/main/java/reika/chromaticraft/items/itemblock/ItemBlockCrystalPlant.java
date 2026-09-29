/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.itemblock;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockCrystalPlant;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;

/**
 * V33a {@code ItemBlockCrystalPlant}: the bloom itself as an item. It places only where
 * {@code BlockCrystalPlant.canPlaceBlockAt} allowed (dirt-type ground; unlike seeds, not Chroma Mud) and grows the
 * new plant twice, so it arrives ripe.
 */
public class ItemBlockCrystalPlant extends BlockItem {

	public ItemBlockCrystalPlant(Block b, Properties properties) {
		super(b, properties);
	}

	@Override
	protected boolean canPlace(BlockPlaceContext context, BlockState state) {
		return BlockCrystalPlant.isDirtType(context.getLevel().getBlockState(context.getClickedPos().below()))
				&& super.canPlace(context, state);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		InteractionResult result = super.place(context);
		if (result.consumesAction() && !context.getLevel().isClientSide()
				&& context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileEntityCrystalPlant te) {
			te.grow();
			te.grow();
		}
		return result;
	}

}
