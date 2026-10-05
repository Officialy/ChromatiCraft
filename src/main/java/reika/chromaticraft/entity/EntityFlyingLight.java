/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEntityTypes;

import java.util.ArrayList;

/**
 * V33a {@code EntityFlyingLight}, the Light Wand's shot: a fireball without drag or burning that flies straight at
 * 1.5 blocks a tick for ten seconds or until it hits a block. On the client it carries a moving light: each tick it
 * puts an Ethereal Light in the air block it occupies and takes last tick's away. Those blocks exist only on the
 * client, so the server world is never touched.
 *
 * <p>V33a only cleared the client lights from its own {@code destroy()}, which the client never runs when the server
 * removes the entity, so the last light block stayed until the chunk was resent. It is cleared on client removal
 * here instead.
 */
public class EntityFlyingLight extends AbstractHurtingProjectile {

	private final ArrayList<BlockPos> lastTickLight = new ArrayList<>();
	private final ArrayList<BlockPos> thisTickLight = new ArrayList<>();

	public EntityFlyingLight(EntityType<? extends EntityFlyingLight> type, Level world) {
		super(type, world);
		this.accelerationPower = 0;
	}

	public EntityFlyingLight(Level world, Player ep) {
		this(ChromaEntityTypes.FLYING_LIGHT.get(), world);
		this.setOwner(ep);
		Vec3 vec = ep.getLookAngle();
		double v = 1.5;
		this.setDeltaMovement(vec.scale(v));
	}

	@Override
	public void tick() {
		// V33a restored its motion after EntityFireball's update, cancelling the fireball's drag.
		Vec3 motion = this.getDeltaMovement();
		super.tick();
		this.setDeltaMovement(motion);
		if (!this.level().isClientSide() && tickCount > 200) {
			this.destroy();
		}
		if (this.level().isClientSide()) {
			this.spawnLight();
		}
	}

	private void spawnLight() {
		BlockPos pos = this.blockPosition();
		lastTickLight.clear();
		for (BlockPos c : thisTickLight) {
			this.level().setBlock(c, Blocks.AIR.defaultBlockState(), 3);
			lastTickLight.add(c);
		}
		thisTickLight.clear();
		if (this.level().getBlockState(pos).isAir()) {
			this.level().setBlock(pos, ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState(), 3);
			thisTickLight.add(pos);
		}
	}

	private void destroy() {
		this.clearLight();
		this.discard();
	}

	@Override
	protected void onHit(HitResult mov) {
		if (!this.level().isClientSide()) {
			if (mov != null && mov.getType() == HitResult.Type.BLOCK) {
				this.destroy();
			}
		}
	}

	@Override
	public void onClientRemoval() {
		super.onClientRemoval();
		this.clearLight();
	}

	private void clearLight() {
		for (BlockPos c : lastTickLight) {
			if (this.level().getBlockState(c).is(ChromaBlocks.ETHEREAL_LIGHT.get()))
				this.level().setBlock(c, Blocks.AIR.defaultBlockState(), 3);
		}
		for (BlockPos c : thisTickLight) {
			if (this.level().getBlockState(c).is(ChromaBlocks.ETHEREAL_LIGHT.get()))
				this.level().setBlock(c, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

}
