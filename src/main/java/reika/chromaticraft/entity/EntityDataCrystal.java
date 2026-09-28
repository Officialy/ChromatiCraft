package reika.chromaticraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.ChromaEntityTypes;

/** V33a immortal, non-despawning dropped Memory Crystal. */
public final class EntityDataCrystal extends ItemEntity {

	/**
	 * V33a {@code getEntityData().getBoolean("artealloy")}: set when the crystal joins the Artifact Distribution
	 * Map alloy, and never cleared, so the crystal stays orange even if that recipe is abandoned. Synced so the
	 * renderer can read it, and saved as V33a's entity data was.
	 */
	private static final EntityDataAccessor<Boolean> DATA_ARTEALLOY =
			SynchedEntityData.defineId(EntityDataCrystal.class, EntityDataSerializers.BOOLEAN);

	public EntityDataCrystal(EntityType<? extends ItemEntity> type, Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setUnlimitedLifetime();
	}

	public EntityDataCrystal(Level level, double x, double y, double z, ItemStack stack) {
		super(ChromaEntityTypes.DATA_CRYSTAL.get(), level);
		this.setPos(x, y, z);
		this.setItem(stack);
		this.setPermanentlyInvulnerable(true);
		this.setUnlimitedLifetime();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ARTEALLOY, false);
	}

	public boolean isArtealloy() {
		return this.entityData.get(DATA_ARTEALLOY);
	}

	public void setArtealloy(boolean artealloy) {
		this.entityData.set(DATA_ARTEALLOY, artealloy);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("artealloy", this.isArtealloy());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setArtealloy(input.getBooleanOr("artealloy", false));
	}

	@Override
	public void tick() {
		super.tick();
		this.setUnlimitedLifetime();
		this.setYRot(0);
		Vec3 motion = this.getDeltaMovement();
		if (this.getY() < this.level().getMinY()) {
			this.setPos(this.getX(), this.level().getMinY(), this.getZ());
			motion = new Vec3(motion.x, Math.max(0, motion.y), motion.z);
		}
		if (motion.y < -0.125) motion = new Vec3(motion.x, -0.125, motion.z);
		if (this.isInWater()) motion = motion.add(0, 0.05, 0);
		this.setDeltaMovement(motion);
		if (!this.level().isClientSide()) this.syncVelocity = true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true;
	}
}
