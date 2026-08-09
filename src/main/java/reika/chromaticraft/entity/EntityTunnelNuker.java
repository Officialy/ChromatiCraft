package reika.chromaticraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.interfaces.entity.DestroyOnUnload;

/** V33a Lumafly/Tunnel Nuker: a slow circling, terrain-following immortal lore-tower guide. */
public final class EntityTunnelNuker extends Mob implements DestroyOnUnload {

	public EntityTunnelNuker(EntityType<? extends EntityTunnelNuker> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.noPhysics = false;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.FLYING_SPEED, 0.075);
	}

	@Override
	protected void registerGoals() {
		// Motion is source-driven; V33a had no pathfinder tasks.
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.setXRot(0);
		this.setYRot(this.getYRot() + Math.signum(System.identityHashCode(this)) / 8F);
		this.yRotO = this.getYRot();
		this.setOnGround(false);
		this.fallDistance = 0;

		int x = (int)Math.floor(this.getX());
		int z = (int)Math.floor(this.getZ());
		int top = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		double yaw = Math.toRadians(this.getYRot());
		double speed = 0.075;
		double vy = Math.clamp((top - this.getY() + 8) / 32D, -0.08, 0.125);
		this.setDeltaMovement(-Math.sin(yaw) * speed, vy, Math.cos(yaw) * speed);
		this.hurtMarked = true;

		if (this.level().isClientSide()) {
			if (this.tickCount % 16 == 0)
				ChromaParticle.spawnTunnelNuker(this.level(), this.position(), this.random);
		}
		else {
			if (this.tickCount % 8 == 0)
				ChromaSounds.TUNNELNUKERAMBIENT.playSound(this, 0.2F + this.random.nextFloat() * 0.2F, 1);
			if (this.random.nextInt(160) == 0)
				ChromaSounds.TUNNELNUKERCALL.playSound(this, 0.25F, 0.75F + this.random.nextFloat() * 0.75F);
		}
	}

	public boolean isValidSpawnPosition() {
		BlockPos pos = this.blockPosition();
		AABB nearby = this.getBoundingBox().inflate(32);
		return this.level().canSeeSky(pos.above())
				&& this.level().getEntitiesOfClass(EntityTunnelNuker.class, nearby, e -> e != this).isEmpty();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		if (input.getBooleanOr("isdead", false)) this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		Entity.RemovalReason reason = this.getRemovalReason();
		output.putBoolean("isdead", reason != null && reason.shouldDestroy());
	}

	@Override
	public void destroy() {
		this.discard();
	}
}
