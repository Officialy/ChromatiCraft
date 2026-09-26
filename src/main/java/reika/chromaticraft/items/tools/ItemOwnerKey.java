/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaSounds;

/**
 * V33a {@code ItemOwnerKey}, the Ownership Expansion Key: bound to the first player to hold it (or, from the casting
 * table, to the table's owner who cast it), it lets whoever uses it on one of its owner's ChromatiCraft tiles become an
 * owner of that tile too, and is spent doing so.
 *
 * <p>As in V33a the tile's placer must be online (V33a's {@code getPlacer()} resolved only a present player), and the
 * tooltip names the owner by raw UUID.
 */
public class ItemOwnerKey extends Item {

	public static final String OWNER_TAG = "owner";

	public ItemOwnerKey(Properties properties) {
		super(properties);
	}

	private static String owner(ItemStack is) {
		CustomData data = is.get(DataComponents.CUSTOM_DATA);
		return data != null ? data.copyTag().getStringOr(OWNER_TAG, "") : "";
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, EquipmentSlot slot) {
		if (!(e instanceof Player ep))
			return;
		if (!owner(is).isEmpty())
			return;
		CustomData.update(DataComponents.CUSTOM_DATA, is, tag -> tag.putString(OWNER_TAG, ep.getUUID().toString()));
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		String owner = owner(is);
		if (!owner.isEmpty())
			li.accept(Component.literal(owner+"'s Lumen Key"));
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		ItemStack is = ctx.getItemInHand();
		String owner = owner(is);
		if (owner.isEmpty())
			return InteractionResult.PASS;
		Level world = ctx.getLevel();
		if (world.isClientSide())
			return InteractionResult.SUCCESS;
		Player ep = ctx.getPlayer();
		BlockEntity te = world.getBlockEntity(ctx.getClickedPos());
		if (ep != null && te instanceof TileEntityChromaticBase tc) {
			UUID id;
			try {
				id = UUID.fromString(owner);
			}
			catch (IllegalArgumentException ex) {
				return InteractionResult.PASS;
			}
			Player plc = tc.getPlacer();
			if (plc != null && id.equals(plc.getUUID())) {
				tc.addOwner(ep);
				tc.syncAllData(true);
				ep.setItemInHand(ctx.getHand(), ItemStack.EMPTY);
				ChromaSounds.CAST.playSound(world, ctx.getClickedPos(), 1, 0.75F);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

}
