/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.enchantment;

import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.base.ChromaticEnchantment;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a Identity Retention: a few blocks that normally drop nothing of themselves (an uneaten cake) break into
 * their own item instead.
 */
public class EnchantmentDataKeeper extends ChromaticEnchantment {

	private static final HashMap<Block, BlockHandler> handlers = new HashMap();

	public EnchantmentDataKeeper() {
		super(Target.DIGGER);
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean isVisibleToPlayer(Player ep, int level) {
		return true;
	}

	/** True if the break was handled here, in which case the caller cancels it and clears the block. */
	public static boolean handleBreak(Level world, BlockPos pos, BlockState state, Player ep) {
		if (handlers.isEmpty())
			initHandlers();
		if (ep == null || ReikaPlayerAPI.isFake(ep))
			return false;
		ItemStack tool = ep.getMainHandItem();
		if (ChromaEnchants.DATAKEEP.getLevel(tool) <= 0)
			return false;
		BlockHandler h = handlers.get(state.getBlock());
		if (h == null)
			return false;
		ItemStack is = h.getItem(world, pos, state, ep);
		if (is == null && state.is(Tags.Blocks.ORES))
			is = new ItemStack(state.getBlock());
		if (is == null)
			return false;
		// V33a tested the dropped item, not the tool, for Inventory Insertion, so this always dropped in the world.
		if (ChromaEnchants.AUTOCOLLECT.getLevel(is) > 0)
			ReikaPlayerAPI.addOrDropItem(is, ep);
		else
			ReikaItemHelper.dropItem(world, pos.getX()+world.getRandom().nextDouble(), pos.getY()+world.getRandom().nextDouble(), pos.getZ()+world.getRandom().nextDouble(), is);
		return true;
	}

	private static void initHandlers() {
		// V33a keyed this BlockKey(cake, 0): only an uneaten cake.
		handlers.put(Blocks.CAKE, new DirectDrop(new ItemStack(Items.CAKE)) {
			@Override
			protected ItemStack getItem(Level world, BlockPos pos, BlockState state, Player ep) {
				return state.getValue(CakeBlock.BITES) == 0 ? super.getItem(world, pos, state, ep) : null;
			}
		});
		// CHROMA-PORT: V33a also kept an IC2 rubber log's resin spot (IC2LogDrop: a crop-state log dropped as the
		// log block with its resin-hole metadata facing EAST, and marked the log item as having subtypes so the
		// variants would not stack). IndustrialCraft 2 has no 26.2 port; add the handler with its block when one
		// exists.
	}

	private static class DirectDrop extends BlockHandler {

		private final ItemStack item;

		private DirectDrop(ItemStack is) {
			item = is.copy();
		}

		@Override
		protected ItemStack getItem(Level world, BlockPos pos, BlockState state, Player ep) {
			return item.copy();
		}

	}

	private abstract static class BlockHandler {

		protected abstract ItemStack getItem(Level world, BlockPos pos, BlockState state, Player ep);

	}

}
