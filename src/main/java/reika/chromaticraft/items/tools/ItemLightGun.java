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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.api.interfaces.ProjectileFiringTool;
import reika.chromaticraft.entity.EntityLightShot;

/**
 * V33a {@code ItemLightGun}, the Illumination Gun, with its {@code ItemProjectileFiringTool} base folded in: each use
 * fires an {@link EntityLightShot} from just ahead of the player's eyes, with the fizz V33a gave it. It works from a
 * dispenser ({@code ProjectileToolDispenseBehavior}).
 */
public class ItemLightGun extends Item implements ProjectileFiringTool {

	public ItemLightGun(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		if (!world.isClientSide()) {
			this.fire(is, world, ep, false);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public final void fire(ItemStack is, Level world, Player ep, boolean randomVec) {
		Entity e = this.createProjectile(is, world, ep, randomVec);
		Vec3 vec = ep.getLookAngle();
		e.snapTo(ep.getX()+vec.x, ep.getY()+vec.y+1.5, ep.getZ()+vec.z, 0, 0);
		world.addFreshEntity(e);
		if (world instanceof ServerLevel)
			world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2, 0.7F);
	}

	@Override
	public int getAutofireRate() {
		return 10;
	}

	protected Entity createProjectile(ItemStack is, Level world, Player ep, boolean randomVec) {
		return new EntityLightShot(world, ep, randomVec);
	}

}
