/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code ItemChromaTool}: a single-stacking ChromatiCraft tool (in the Tools tab, by package), with an optional
 * owner and a per-player use permission -- a player refused with PUNISH loses the tool from their hand, takes magic
 * damage and is thrown. V33a's {@code setNoRepair} has no counterpart: an item without durability cannot be repaired.
 */
public abstract class ItemChromaTool extends Item {

	public ItemChromaTool(Properties properties) {
		super(properties.stacksTo(1));
	}

	public UseResult canPlayerUse(Player ep) {
		return UseResult.ALLOW;
	}

	/** True when the player may not use it (and was punished if the result says so). */
	public final boolean handleUseAllowance(Player ep) {
		UseResult res = this.canPlayerUse(ep);
		if (res == UseResult.ALLOW)
			return false;
		if (res == UseResult.PUNISH || res == UseResult.PUNISHSEVERE) {
			this.harmDisallowedPlayer(ep, res == UseResult.PUNISHSEVERE);
		}
		return true;
	}

	protected void harmDisallowedPlayer(Player ep, boolean severe) {
		if (!ep.level().isClientSide())
			ReikaItemHelper.dropItem(ep, ep.getMainHandItem());
		ep.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		ep.hurt(ep.damageSources().magic(), severe ? 8 : 4);
		if (!ep.level().isClientSide()) {
			double phi = ep.getRandom().nextDouble()*360;
			double[] xyz = ReikaPhysicsHelper.polarToCartesian(5, 0, phi);
			ep.teleportTo(ep.getX(), ep.getY()+0.5, ep.getZ());
			ep.setDeltaMovement(new Vec3(xyz[0], 1, xyz[2]));
			ep.syncVelocity = true;
			if (severe)
				ep.fallDistance += 8;
			ChromaSounds.DISCHARGE.playSound(ep);
		}
	}

	public final void setOwner(ItemStack is, Player ep) {
		ReikaItemHelper.updateStackTag(is, tag -> tag.putString("ownerid", ep.getUUID().toString()));
	}

	public final boolean isOwner(ItemStack is, Player ep) {
		return ep.getUUID().equals(this.getOwner(is));
	}

	public final UUID getOwner(ItemStack is) {
		CompoundTag tag = ReikaItemHelper.getStackTag(is);
		if (tag == null || !tag.contains("ownerid"))
			return null;
		return UUID.fromString(tag.getStringOr("ownerid", ""));
	}

	public static enum UseResult {
		ALLOW(),
		NOTHING(),
		PUNISH(),
		PUNISHSEVERE();
	}

}
