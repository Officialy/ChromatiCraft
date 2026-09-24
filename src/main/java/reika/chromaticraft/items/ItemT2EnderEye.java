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

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.entity.EntityEnderEyeT2;
import reika.chromaticraft.registry.ChromaSounds;

/**
 * V33a {@code ItemT2EnderEye}, the Enhanced Ender Eye, cast three at a time and bound to its caster.
 *
 * <p>Thrown, it becomes an {@link EntityEnderEyeT2} toward the nearest stronghold and always returns.
 * Used on an End Portal frame it fills it like a vanilla eye; and if that completes the portal, the stack
 * comes back as twelve. Dropped, it never despawns, cannot be destroyed, floats up out of water, homes in
 * on its owner within 128 blocks, and only its owner can pick it up.
 */
public class ItemT2EnderEye extends Item implements OwnableItem {

	public ItemT2EnderEye(Properties properties) {
		super(properties);
	}

	static UUID owner(ItemStack is) {
		CustomData data = is.get(DataComponents.CUSTOM_DATA);
		if (data == null)
			return null;
		String s = data.copyTag().getStringOr("owner", "");
		if (s.isEmpty())
			return null;
		try {
			return UUID.fromString(s);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult ret = Items.ENDER_EYE.useOn(context);
		if (ret.consumesAction()) {
			Level world = context.getLevel();
			BlockPos pos = context.getClickedPos();
			boolean portal = false;
			for (Direction d : Direction.values())
				portal |= world.getBlockState(pos.relative(d)).is(Blocks.END_PORTAL);
			if (portal) {
				ItemStack is = context.getItemInHand();
				is.setCount(12);
				Player ep = context.getPlayer();
				if (ep != null)
					world.playSound(null, ep.getX(), ep.getY(), ep.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1, 1.35F);
			}
		}
		return ret;
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		BlockHitResult mov = getPlayerPOVHitResult(world, ep, ClipContext.Fluid.NONE);
		if (mov.getType() == HitResult.Type.BLOCK && world.getBlockState(mov.getBlockPos()).is(Blocks.END_PORTAL_FRAME))
			return InteractionResult.PASS;
		if (world instanceof ServerLevel server) {
			BlockPos pos = server.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, ep.blockPosition(), 100, false);
			if (pos != null) {
				UUID owner = owner(is);
				if (owner == null) {
					CompoundTag tag = new CompoundTag();
					tag.putString("owner", ep.getUUID().toString());
					is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
					owner = ep.getUUID();
				}
				EntityEnderEyeT2 eye = EntityEnderEyeT2.create(world, ep.getX(), ep.getEyeY(), ep.getZ(), owner);
				eye.moveTowards(pos.getX(), pos.getY(), pos.getZ());
				world.addFreshEntity(eye);
				// V33a: "random.bow" plus aux event 1002, as 1.7.10's vanilla eye did.
				world.playSound(null, ep.getX(), ep.getY(), ep.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL,
						0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
				world.levelEvent(null, 1002, ep.blockPosition(), 0);
				is.consume(1, ep);
			}
			else {
				ChromaSounds.ERROR.playSound(ep);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public int getEntityLifespan(ItemStack is, Level world) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity ei) {
		ei.setInvulnerable(true);
		if (ei.level().getBlockState(ei.blockPosition()).is(Blocks.WATER)) {
			Vec3 v = ei.getDeltaMovement();
			ei.setDeltaMovement(v.x * 0.85, v.y + 0.07, v.z * 0.85);
			ei.hurtMarked = true;
		}
		UUID uid = owner(stack);
		if (uid != null) {
			Player ep = ei.level().getPlayerByUUID(uid);
			if (ep != null && !ei.hasPickUpDelay()) {
				double x = ep.getX();
				double y = ep.getY() + 1.5;
				double z = ep.getZ();
				double dx = x + 0.5 - ei.getX();
				double dy = y + 0.5 - ei.getY();
				double dz = z + 0.5 - ei.getZ();
				double ddt = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (ddt < 128) {
					if (Math.hypot(dx, dz) < 1) {
						ei.playerTouch(ep);
					}
					else {
						Vec3 v = ei.getDeltaMovement();
						double mx = Mth.clamp(v.x + dx / ddt / ddt, -0.75, 0.75);
						double my = Mth.clamp(v.y + dy / ddt / ddt / 2, -0.75, 0.75);
						double mz = Mth.clamp(v.z + dz / ddt / ddt, -0.75, 0.75);
						if (ei.getY() < y)
							my += 0.125;
						if (ei.getY() < 0)
							my = Math.max(1, my);
						ei.setDeltaMovement(mx, my, mz);
						if (!ei.level().isClientSide())
							ei.hurtMarked = true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean isUsableBy(ItemStack is, Player ep) {
		UUID owner = owner(is);
		return owner == null || owner.equals(ep.getUUID());
	}

	@Override
	public boolean isCollectableBy(ItemEntity ei, Player ep) {
		return this.isUsableBy(ei.getItem(), ep);
	}
}
