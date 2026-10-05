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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.dragonapi.base.ParticleEntity;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.Spline;

/**
 * V33a {@code EntitySplineProjectile}: a particle entity that steps along a precomputed spline, one sample per
 * tick, then stops at the last sample. Its velocity is a token 1/16 of each step so clients interpolate.
 */
public abstract class EntitySplineProjectile extends ParticleEntity {

	private int pathTick = 0;
	private boolean hitTarget;
	private final List<DecimalPosition> path;

	protected EntitySplineProjectile(EntityType<? extends EntitySplineProjectile> type, Level world, Spline s,
			int fineness) {
		super(type, world);
		path = new ArrayList<>(s.get(fineness, false));
		DecimalPosition pos = path.get(0);
		this.spawnAt(pos.getCoordinate());
		this.setPathPosition(pos);
	}

	protected EntitySplineProjectile(EntityType<? extends EntitySplineProjectile> type, Level world) {
		super(type, world);
		path = new ArrayList<>();
	}

	public @Nullable BlockPos getFinalPathBlock() {
		return path.isEmpty() ? null : path.getLast().getCoordinate();
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buf) {
		super.writeSpawnData(buf);
		buf.writeInt(path.size());
		for (DecimalPosition p : path)
			p.writeToBuf(buf);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buf) {
		super.readSpawnData(buf);
		path.clear();
		int n = buf.readInt();
		for (int i = 0; i < n; i++)
			path.add(DecimalPosition.readFromBuf(buf));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		path.clear();
		int n = input.getIntOr("pathSize", 0);
		for (int i = 0; i < n; i++)
			path.add(new DecimalPosition(input.getDoubleOr("px" + i, 0), input.getDoubleOr("py" + i, 0),
					input.getDoubleOr("pz" + i, 0)));
		pathTick = Math.min(input.getIntOr("pathTick", 0), Math.max(0, path.size() - 1));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("pathSize", path.size());
		for (int i = 0; i < path.size(); i++) {
			DecimalPosition p = path.get(i);
			output.putDouble("px" + i, p.xCoord());
			output.putDouble("py" + i, p.yCoord());
			output.putDouble("pz" + i, p.zCoord());
		}
		output.putInt("pathTick", pathTick);
	}

	@Override
	protected void onTick() {
		if (!path.isEmpty()) {
			if (hitTarget) {
				this.discard();
			}
			else {
				DecimalPosition p = path.get(pathTick);
				this.setPathPosition(p);
				pathTick = Math.min(pathTick + 1, path.size() - 1);
			}
		}
		else {
			this.discard();
		}
	}

	private void setPathPosition(DecimalPosition p) {
		double v = 0.0625;
		this.setDeltaMovement((p.xCoord() - this.getX()) * v, (p.yCoord() - this.getY()) * v,
				(p.zCoord() - this.getZ()) * v);
		DecimalPosition prev = pathTick == 0 ? path.get(0) : path.get(pathTick - 1);
		this.setPos(p.xCoord(), p.yCoord(), p.zCoord());
		this.xo = prev.xCoord();
		this.yo = prev.yCoord();
		this.zo = prev.zCoord();
		this.xOld = prev.xCoord();
		this.yOld = prev.yCoord();
		this.zOld = prev.zCoord();
	}

	@Override public double getRenderRangeSquared() { return 4096; }
	@Override public double getHitboxSize() { return 0.125; }
	@Override public boolean despawnOverTime() { return false; }
	@Override public boolean despawnOverDistance() { return false; }
	@Override public boolean canInteractWithSpawnLocation() { return false; }
	@Override public double getSpeed() { return 0; }
	@Override protected boolean dieOnNoVelocity() { return false; }
	@Override protected boolean onEnterBlock(Level world, BlockPos pos) { return false; }
	@Override public void applyEntityCollision(Entity e) {}
}
