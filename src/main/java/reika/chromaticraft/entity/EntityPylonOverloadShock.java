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

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.chromaticraft.magic.interfaces.CrystalFuse;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.base.InertEntity;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * The travelling destructive pulse emitted by an unstable pylon. The server owns impact effects;
 * clients receive the complete interpolated route in the complex-spawn payload and render the pulse
 * at the same positions.
 */
public class EntityPylonOverloadShock extends InertEntity implements IEntityWithComplexSpawn {

	private CrystalElement color = CrystalElement.WHITE;
	private final ArrayList<Vec3> path = new ArrayList<>();
	private BlockPos target = BlockPos.ZERO;
	private int damageFactor = 1;

	public EntityPylonOverloadShock(EntityType<? extends EntityPylonOverloadShock> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	public EntityPylonOverloadShock(Level world, TileEntityCrystalPylon source, List<BlockPos> route,
			double speed, int damage) {
		this(ChromaEntityTypes.PYLON_OVERLOAD.get(), world);
		if (route.size() < 2)
			throw new IllegalArgumentException("A pylon overload route requires at least two nodes");
		color = source.getColor();
		damageFactor = Math.max(1, Math.min(8, damage));
		for (int i = 0; i < route.size() - 1; i++) {
			Vec3 from = Vec3.atCenterOf(route.get(i));
			Vec3 to = Vec3.atCenterOf(route.get(i + 1));
			for (int tick = 0; tick < speed; tick++)
				path.add(from.lerp(to, tick / speed));
		}
		target = route.get(route.size() - 1).immutable();
		this.snapTo(Vec3.atCenterOf(source.getBlockPos()));
	}

	public static double getRandomSpeed() {
		return 7 + Math.random() * 8;
	}

	@Override
	public void tick() {
		super.tick();
		if (!path.isEmpty())
			this.snapTo(path.get(Math.min(this.tickCount, path.size() - 1)));

		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
					this.getX(), this.getY(), this.getZ(),
					(this.random.nextDouble() - 0.5) * 0.25,
					(this.random.nextDouble() - 0.5) * 0.25,
					(this.random.nextDouble() - 0.5) * 0.25);
		}
		else if (this.tickCount >= path.size()) {
			this.finishImpact((ServerLevel)this.level());
		}
	}

	private void finishImpact(ServerLevel world) {
		this.discard();
		world.explode(this, this.getX(), this.getY(), this.getZ(), 5, true, Level.ExplosionInteraction.NONE);
		int chanceBound = Math.max(1, 8 / damageFactor);
		if (this.random.nextInt(chanceBound) != 0)
			return;
		var blockEntity = world.getBlockEntity(target);
		if (blockEntity instanceof TileEntityCrystalPylon pylon) {
			if (this.random.nextInt(chanceBound) == 0)
				pylon.destabilize();
		}
		else if (blockEntity instanceof CrystalFuse fuse) {
			fuse.overload(color);
		}
		else {
			ReikaWorldHelper.dropAndDestroyBlockAt(world, target, null, true, true);
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		data.writeVarInt(color.ordinal());
		data.writeVarInt(path.size());
		for (Vec3 point : path) {
			data.writeDouble(point.x());
			data.writeDouble(point.y());
			data.writeDouble(point.z());
		}
		data.writeBlockPos(target);
		data.writeVarInt(damageFactor);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		color = CrystalElement.elements[Math.floorMod(data.readVarInt(), CrystalElement.elements.length)];
		path.clear();
		int length = data.readVarInt();
		for (int i = 0; i < length; i++)
			path.add(new Vec3(data.readDouble(), data.readDouble(), data.readDouble()));
		target = data.readBlockPos();
		damageFactor = Math.max(1, Math.min(8, data.readVarInt()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		color = CrystalElement.elements[Math.floorMod(
				input.getIntOr("color", CrystalElement.WHITE.ordinal()), CrystalElement.elements.length)];
		path.clear();
		for (Vec3 point : input.listOrEmpty("path", Vec3.CODEC))
			path.add(point);
		target = input.read("target", BlockPos.CODEC).orElse(BlockPos.ZERO);
		damageFactor = Math.max(1, Math.min(8, input.getIntOr("damage_factor", 1)));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("color", color.ordinal());
		ValueOutput.TypedOutputList<Vec3> points = output.list("path", Vec3.CODEC);
		for (Vec3 point : path)
			points.add(point);
		output.store("target", BlockPos.CODEC, target);
		output.putInt("damage_factor", damageFactor);
	}

	public CrystalElement getColor() {
		return color;
	}

	public List<Vec3> getPath() {
		return List.copyOf(path);
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}
}
