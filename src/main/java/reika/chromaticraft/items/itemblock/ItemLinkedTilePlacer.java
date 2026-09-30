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

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.auxiliary.interfaces.LinkedTile;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code ItemLinkedTilePlacer}, the World Rift item: placed into an open (soft or liquid) spot, or against a
 * face, it remembers where it went; the next one placed from the same stack links to that one and makes it the
 * primary end. The pending location is the stack's {@code loc} custom data, dimension included.
 */
public class ItemLinkedTilePlacer extends Item {

	public ItemLinkedTilePlacer(Properties properties) {
		super(properties);
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
		// Read before the decrement: a 26.x stack that reaches zero drops its components, where a 1.7.10 stack kept
		// its NBT at size 0, so the last item of a stack could never complete a pair.
		WorldLocation pending = getRiftLocation(is);
		if (ep == null || !ep.getAbilities().instabuild)
			is.shrink(1);
		world.setBlock(pos, this.getBlock().defaultBlockState(), 3);
		linkTile(world, pos, is, ep, pending);
		return InteractionResult.SUCCESS;
	}

	/** V33a: a soft block, or any water or lava. */
	private static boolean isOpen(Level world, BlockPos pos) {
		return ReikaWorldHelper.softBlocks(world, pos) || world.getFluidState(pos).is(FluidTags.WATER)
				|| world.getFluidState(pos).is(FluidTags.LAVA);
	}

	static void linkTile(Level world, BlockPos pos, ItemStack is, @Nullable Player ep, @Nullable WorldLocation loc) {
		// V33a ReikaSoundHelper.playPlaceSound(world, x, y, z, Blocks.wool): the cloth step sound, broadcast.
		SoundType wool = SoundType.WOOL;
		world.playSound(null, pos, wool.getStepSound(), SoundSource.BLOCKS, wool.getVolume(), wool.getPitch());
		LinkedTile te = (LinkedTile)world.getBlockEntity(pos);
		if (ep != null)
			te.setPlacer(ep);
		if (loc == null) {
			saveRiftLocation(world, pos, is);
		}
		else {
			if (!loc.equals(world, pos)) {
				Level other = loc.getWorld();
				// V33a loc.getTileEntity() loaded the first end's chunk on demand (1.7.10 never refused a provide
				// request), so a pair can be linked however far apart its ends are placed.
				BlockEntity tile2 = other != null ? other.getBlockEntity(loc.pos) : null;
				if (tile2 instanceof LinkedTile te2) {
					te.linkTo(loc);
					te2.setPrimary(true);
					is.remove(DataComponents.CUSTOM_DATA);
				}
				else {
					saveRiftLocation(world, pos, is);
				}
			}
			else {
				saveRiftLocation(world, pos, is);
			}
		}
	}

	protected Block getBlock() {
		return ChromaBlocks.RIFT.get();
	}

	static @Nullable WorldLocation getRiftLocation(ItemStack is) {
		CustomData data = is.get(DataComponents.CUSTOM_DATA);
		return data != null ? WorldLocation.load("loc", data.copyTag()) : null;
	}

	private static void saveRiftLocation(Level world, BlockPos pos, ItemStack is) {
		CompoundTag tag = new CompoundTag();
		new WorldLocation(world, pos).saveAdditional("loc", tag);
		is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		WorldLocation loc = getRiftLocation(is);
		if (loc != null)
			li.accept(Component.literal("Linking to "+loc.toString()));
	}

	protected boolean checkValidBounds(ItemStack is, @Nullable Player ep, Level world, BlockPos pos) {
		return true;
	}

}
