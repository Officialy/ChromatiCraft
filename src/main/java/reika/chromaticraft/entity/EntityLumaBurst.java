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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.base.ParticleEntity;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a "Luma Burst": a slow, colour-coded bolt fired by burst emitters/gravity-puzzle machinery that
 * bounces gently around its spawn height and paints a fading trail. Ported from the pristine
 * {@code entity/EntityLumaBurst.java} (1.7.10) onto the already-26.2-ported {@link ParticleEntity}
 * base (shared with the accepted {@code EntityPylonOverloadShock}).
 *
 * <p>Registration only; V33a's spawner is the gravity-puzzle structure
 * ({@code block/dimension/structure/gravity/BlockGravityTile.java}), which is still pristine
 * 1.7.10, so nothing currently creates one of these in the 26.2 world. The
 * {@code onEnterBlock}/{@code GravityTiles} pulse-absorption behaviour is marked
 * {@code CHROMA-PORT} below for the same reason.
 */
public class EntityLumaBurst extends ParticleEntity {

	private CrystalElement color = CrystalElement.WHITE;
	private boolean outOfSpawnZone;
	private boolean outOfSpawnZoneLast;

	public EntityLumaBurst(EntityType<? extends EntityLumaBurst> type, Level world) {
		super(type, world);
	}

	public EntityLumaBurst(EntityType<? extends EntityLumaBurst> type, Level world, BlockPos pos,
			CubeDirections dir, CrystalElement e) {
		super(type, world, pos);
		this.setColor(e);
		this.setDirection(dir, true);
	}

	public EntityLumaBurst(EntityType<? extends EntityLumaBurst> type, Level world, BlockPos pos,
			double ang, CrystalElement e) {
		super(type, world, pos);
		this.setColor(e);
		this.setAngle(ang);
	}

	public void copyFrom(EntityLumaBurst e) {
		this.setColor(e.color);
		this.setDeltaMovement(e.getDeltaMovement());
	}

	public void setColor(CrystalElement e) {
		color = e;
	}

	/** V33a's 8-way diagonal launch direction; distinct from the base class's 6-way
	 *  {@code setDirection(Direction, boolean)} overload used by other particle entities. */
	private void setDirection(CubeDirections dir, boolean setPos) {
		if (setPos) {
			this.snapTo(this.getBlockX() + 0.5, this.getBlockY() + 0.5, this.getBlockZ() + 0.5, 0, 0);
		}
		double d = 10; // V33a: a small random cone around the emitter's face angle.
		double a = ReikaRandomHelper.getRandomPlusMinus(dir.angle, d);
		this.setAngle(a);
	}

	private void setAngle(double a) {
		double[] vel = ReikaPhysicsHelper.polarToCartesian(this.getSpeed(), 0, -a);
		this.setDeltaMovement(vel[0], vel[1], vel[2]);
	}

	public void resetSpawnTimer() {
		if (!outOfSpawnZoneLast) {
			outOfSpawnZone = false;
		}
	}

	public boolean isOutOfSpawnZone() {
		return outOfSpawnZone;
	}

	@Override
	public double getHitboxSize() {
		return 0.0625;
	}

	@Override
	public boolean despawnOverTime() {
		return false;
	}

	@Override
	public boolean despawnOverDistance() {
		return true;
	}

	@Override
	protected double getDespawnDistance() {
		return 25;
	}

	@Override
	public boolean canInteractWithSpawnLocation() {
		return false;
	}

	@Override
	protected void onTick() {
		if (this.getSpawnLocation() != null) {
			double dy = this.getY() - this.getSpawnLocation().pos.getY() + 0.5;
			if (Math.abs(dy) >= 0.4 && Math.signum(this.getDeltaMovement().y) == Math.signum(dy)) {
				this.setDeltaMovement(this.getDeltaMovement().x, -this.getDeltaMovement().y,
						this.getDeltaMovement().z);
			}
		}
		if (this.level().isClientSide()) {
			this.doParticles();
		}
		if (outOfSpawnZone) {
			outOfSpawnZoneLast = true;
		}
		outOfSpawnZone = true;
	}

	private void doParticles() {
		int c = ReikaColorAPI.mixColors(color.getColor(), 0x000000, 0.5F);
		for (double d = 0; d <= 1; d += 0.25) {
			double px = this.getX() - (this.getX() - this.xo) * d;
			double py = this.getY() - (this.getY() - this.yo) * d;
			double pz = this.getZ() - (this.getZ() - this.zo) * d;
			ChromaParticle.spawnLumaBurstTrail(this.level(), px, py, pz, c, (int)Math.round(d));
		}
	}

	@Override
	public double getSpeed() {
		return ReikaRandomHelper.getRandomPlusMinus(0.15, 0.025);
	}

	@Override
	protected boolean onEnterBlock(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) {
			return false;
		}
		// CHROMA-PORT: V33a routes a Luma Burst that enters a Gravity Tile
		// (ChromaBlocks.GRAVITY / block.dimension.structure.gravity.BlockGravityTile, still pristine
		// 1.7.10) into `GravityTiles.list[meta].onPulse(world, x, y, z, this)`, which decides whether
		// the pulse is absorbed. Neither ChromaBlocks.GRAVITY nor a modern GravityTiles enum exists
		// yet, so every non-air block currently absorbs the burst (matches V33a's own fallback
		// `return true` for every other solid block).
		return true;
	}

	@Override
	public void applyEntityCollision(Entity e) {
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		super.writeSpawnData(data);

		data.writeVarInt(color.ordinal());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		super.readSpawnData(data);

		color = CrystalElement.elements[data.readVarInt()];
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);

		color = CrystalElement.elements[Math.floorMod(
				input.getIntOr("color", CrystalElement.WHITE.ordinal()), CrystalElement.elements.length)];
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		output.putInt("color", color.ordinal());
	}

	public CrystalElement getColor() {
		return color;
	}

	@Override
	public double getRenderRangeSquared() {
		return 4096D;
	}

	public void setRandomDirection(boolean fullFreedom) {
		if (fullFreedom) {
			double ang = this.random.nextDouble() * 360;
			double slope = ReikaRandomHelper.getRandomPlusMinus(0, 5);
			double[] vel = ReikaPhysicsHelper.polarToCartesian(this.getSpeed(), slope, ang);
			this.setDeltaMovement(vel[0], vel[1], vel[2]);
		}
		else {
			this.setDirection(CubeDirections.list[this.random.nextInt(CubeDirections.list.length)], false);
		}
	}

}
