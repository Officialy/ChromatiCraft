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

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.block.BlockAdjacencyUpgrade;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityAccelerator;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code ItemAdjacencyPlacer}, one per implemented colour (V33a's damage value). It goes into an open (soft or
 * liquid) spot or against a face, never where a living thing stands (the block has no collision), and the placed tile
 * takes the placer and the stack's tier and data.
 */
public class ItemAdjacencyPlacer extends BlockItem {

	public ItemAdjacencyPlacer(BlockAdjacencyUpgrade block, Properties props) {
		super(block, props);
	}

	public AdjacencyUpgrades getUpgrade() {
		return ((BlockAdjacencyUpgrade)this.getBlock()).upgrade;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		ItemStack is = context.getItemInHand();
		Player ep = context.getPlayer();
		if (!isOpen(world, pos)) {
			pos = pos.relative(context.getClickedFace());
			if (!isOpen(world, pos))
				return InteractionResult.PASS;
		}
		if (!this.checkValidBounds(is, ep, world, pos))
			return InteractionResult.PASS;
		if (!world.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty())
			return InteractionResult.PASS;
		if (ep != null && !ep.mayUseItemAt(pos, Direction.DOWN, is))
			return InteractionResult.PASS;
		if (world.isClientSide())
			return InteractionResult.SUCCESS;
		// Copied before the decrement: a 26.x stack that reaches zero drops its components, where a 1.7.10 stack
		// kept its NBT at size 0, so the last upgrade of a stack placed as tier 0.
		ItemStack placed = is.copyWithCount(1);
		if (ep == null || !ep.getAbilities().instabuild)
			is.shrink(1);
		world.setBlock(pos, this.getBlock().defaultBlockState(), 3);

		// V33a ReikaSoundHelper.playPlaceSound(world, x, y, z, ChromaBlocks.ADJACENCY): the block's place sound at
		// volume and pitch 1, broadcast from the server.
		SoundType sound = this.getBlock().defaultBlockState().getSoundType();
		world.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 1, 1);

		if (world.getBlockEntity(pos) instanceof TileEntityAdjacencyUpgrade te) {
			if (ep != null)
				te.setPlacer(ep);
			te.setDataFromItemStackTag(placed);
		}
		return InteractionResult.SUCCESS;
	}

	/** V33a: a soft block, or any water or lava. */
	private static boolean isOpen(Level world, BlockPos pos) {
		return ReikaWorldHelper.softBlocks(world, pos) || world.getFluidState(pos).is(FluidTags.WATER)
				|| world.getFluidState(pos).is(FluidTags.LAVA);
	}

	/** V33a: {@code y > 0 && y < height-1}, one block inside each end of the world. */
	protected boolean checkValidBounds(ItemStack is, @Nullable Player ep, Level world, BlockPos pos) {
		return pos.getY() > world.getMinY() && pos.getY() < world.getMaxY();
	}

	@Override
	public Component getName(ItemStack is) {
		return this.getUpgrade().getName();
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		int tier = TileEntityAdjacencyUpgrade.getTier(is);
		AdjacencyUpgrades a = this.getUpgrade();
		li.accept(Component.literal(a.getDesc(tier)));
		if (a.getColor() == CrystalElement.LIGHTBLUE) {
			long max = TileEntityAccelerator.getMaxLag()/1000000;
			li.accept(Component.literal("Admin Note:").withStyle(ChatFormatting.GOLD)
					.append(Component.literal(" Will not cause more than "+max+"ms lag.").withStyle(ChatFormatting.WHITE)));
		}
	}

}
