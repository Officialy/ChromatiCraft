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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.block.decoration.BlockEtherealLight;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

import java.util.HashSet;

/**
 * V33a {@code EntityLightShot}, the Illumination Gun's bolt: a fast, unaccelerated fireball (3.5 blocks a tick, slowed
 * only by drag) that now and then leaves a short-lived ethereal light behind it, and plants a lasting one against the
 * first solid block it meets. It lives 50 ticks, or dies at once without the player who fired it.
 *
 * <p>V33a's unused {@code spawnTraceParticle} blur trail was never called and is not carried over.
 */
public class EntityLightShot extends AbstractHurtingProjectile {

	private final HashSet<BlockPos> trailCoords = new HashSet();

	public EntityLightShot(EntityType<? extends EntityLightShot> type, Level world) {
		super(type, world);
		this.accelerationPower = 0;
	}

	public EntityLightShot(Level world, Player ep, boolean randomVec) {
		this(ChromaEntityTypes.LIGHT_SHOT.get(), world);
		this.setOwner(ep);

		Vec3 vec = ep.getLookAngle();
		if (randomVec) {
			vec = new Vec3(ReikaRandomHelper.getRandomPlusMinus(0, 1D), ReikaRandomHelper.getRandomPlusMinus(0, 1D),
					ReikaRandomHelper.getRandomPlusMinus(0, 1D)).normalize();
		}
		double v = 3.5;
		this.setDeltaMovement(vec.scale(v));
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.level().isClientSide() && random.nextInt(30) == 0) {
			BlockPos c = this.blockPosition();
			if (this.level().isEmptyBlock(c)) {
				this.level().setBlock(c, ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState()
						.setValue(BlockEtherealLight.SLOW_DECAY, true), 3);
				trailCoords.add(c);
			}
		}

		if (!this.level().isClientSide() && (!(this.getOwner() instanceof Player) || tickCount > 50)) {
			this.destroy(false);
		}

		if (this.level().isClientSide() && tickCount > 10) {
			this.level().addParticle(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void onHit(HitResult mov) {
		if (!(this.getOwner() instanceof Player) && !this.level().isClientSide()) {
			this.destroy(true);
			return;
		}
		if (!this.level().isClientSide()) {
			if (mov instanceof BlockHitResult hit && mov.getType() == HitResult.Type.BLOCK) {
				BlockPos c = hit.getBlockPos();
				BlockState state = this.level().getBlockState(c);
				if (!state.getCollisionShape(this.level(), c).isEmpty()) {
					c = c.relative(hit.getDirection());
					if (this.level().isEmptyBlock(c)) {
						this.level().setBlock(c, ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState()
								.setValue(BlockEtherealLight.PARTICLES, true).setValue(BlockEtherealLight.MINEABLE, true)
								.setValue(BlockEtherealLight.SLOW_DECAY, true), 3);
						this.destroy(true);
					}
				}
			}
		}
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	private void destroy(boolean impact) {
		this.discard();
		for (BlockPos c : trailCoords) {
			if (this.level().getBlockState(c).is(ChromaBlocks.ETHEREAL_LIGHT.get()))
				this.level().scheduleTick(c, ChromaBlocks.ETHEREAL_LIGHT.get(), random.nextInt(40));
		}
		ChromaSounds.BOUNCE.playSound(this, 0.5F, 0.5F);
		if (this.getOwner() instanceof Player ep)
			ChromaSounds.BOUNCE.playSound(ep, 0.25F, 0.5F);
	}
}
