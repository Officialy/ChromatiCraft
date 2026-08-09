package reika.chromaticraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.ChromaEntityTypes;

/** V33a immortal, non-despawning dropped Memory Crystal. */
public final class EntityDataCrystal extends ItemEntity {

	public EntityDataCrystal(EntityType<? extends ItemEntity> type, Level level) {
		super(type, level);
		this.setInvulnerable(true);
		this.setUnlimitedLifetime();
	}

	public EntityDataCrystal(Level level, double x, double y, double z, ItemStack stack) {
		super(ChromaEntityTypes.DATA_CRYSTAL.get(), level);
		this.setPos(x, y, z);
		this.setItem(stack);
		this.setInvulnerable(true);
		this.setUnlimitedLifetime();
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
		if (!this.level().isClientSide()) this.hurtMarked = true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true;
	}
}
