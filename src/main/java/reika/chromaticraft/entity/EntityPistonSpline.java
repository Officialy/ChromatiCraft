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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTarget26;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.RGBColorData;
import reika.dragonapi.instantiable.math.Spline;

/**
 * V33a {@code EntityPistonSpline}: Filter Cycle's read result, carried along a spline from a read head to the
 * door target of the step being played, in the colour the tape filtered the beam to. It is drawn only by its
 * trail (the V33a entity renderer returned before drawing anything) and hums every two seconds.
 */
public final class EntityPistonSpline extends EntitySplineProjectile {

	private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(EntityPistonSpline.class,
			EntityDataSerializers.BYTE);

	private RGBColorData color = RGBColorData.white();

	public EntityPistonSpline(Level world, Spline s, RGBColorData clr) {
		super(ChromaEntityTypes.PISTON_SPLINE.get(), world, s, 20);
		color = clr.copy();
		this.entityData.set(COLOR, pack(color));
	}

	public EntityPistonSpline(EntityType<? extends EntityPistonSpline> type, Level world) {
		super(type, world);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOR, pack(RGBColorData.white()));
	}

	private static byte pack(RGBColorData c) {
		return (byte)((c.red ? 4 : 0) | (c.green ? 2 : 0) | (c.blue ? 1 : 0));
	}

	private static RGBColorData unpack(byte b) {
		return new RGBColorData((b & 4) != 0, (b & 2) != 0, (b & 1) != 0);
	}

	public RGBColorData color() {
		return color.copy();
	}

	@Override
	protected void onTick() {
		super.onTick();
		if (!this.level().isClientSide()) {
			this.entityData.set(COLOR, pack(color));
		}
		else {
			color = unpack(this.entityData.get(COLOR));
			ChromaParticle.spawnPistonSplineTrail(this.level(), this.getX(), this.getY(), this.getZ(),
					color.getRenderColor(), this.random);
			if (this.tickCount % 40 == 0)
				ChromaSounds.FIRE.playSound(this.level(), this.getX(), this.getY(), this.getZ(), 0.3F, 0.5F);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		color = unpack((byte)input.getIntOr("rgb", 7));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("rgb", pack(color));
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		super.writeSpawnData(data);
		data.writeByte(pack(color));
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		super.readSpawnData(data);
		color = unpack(data.readByte());
	}

	/** V33a: absorbed only by the door target at the very end of its path. */
	@Override
	protected boolean onEnterBlock(Level world, BlockPos pos) {
		if (world.getBlockState(pos).is(ChromaBlocks.PISTON_TARGET.get())) {
			BlockPos end = this.getFinalPathBlock();
			if (end != null && end.equals(pos)) {
				BlockPistonTarget26.receiveSplineParticle(world, pos, this);
				return true;
			}
		}
		return false;
	}
}
