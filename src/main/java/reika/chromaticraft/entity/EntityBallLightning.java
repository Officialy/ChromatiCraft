package reika.chromaticraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.SphericalVector;
import reika.dragonapi.interfaces.entity.DestroyOnUnload;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.List;

public final class EntityBallLightning extends Mob implements DestroyOnUnload {

	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
			EntityBallLightning.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RENDER_COLOR = SynchedEntityData.defineId(
			EntityBallLightning.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BOLT_TARGET = SynchedEntityData.defineId(
			EntityBallLightning.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BOLT_SEQUENCE = SynchedEntityData.defineId(
			EntityBallLightning.class, EntityDataSerializers.INT);

	private final SphericalVector velocity;
	private double targetTheta;
	private double targetPhi;
	private double targetVelocity;
	private CrystalElement color;
	private CrystalElement targetColor;
	private float colorTransitionFraction;
	private boolean pylonSpawn;
	private boolean drops = true;
	private int lastBoltSequence;

	public EntityBallLightning(EntityType<? extends EntityBallLightning> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		color = CrystalElement.elements[this.random.nextInt(CrystalElement.elements.length)];
		velocity = new SphericalVector(0.15, this.random.nextInt(360), this.random.nextInt(360));
		targetTheta = this.random.nextInt(360);
		targetPhi = this.random.nextInt(360);
		targetVelocity = ReikaRandomHelper.getRandomPlusMinus(0.1, 0.1);
		this.entityData.set(COLOR, color.ordinal());
		this.entityData.set(RENDER_COLOR, color.color());
	}

	public EntityBallLightning(Level level, CrystalElement color, double x, double y, double z) {
		this(ChromaEntityTypes.BALL_LIGHTNING.get(), level);
		this.setColor(color);
		this.snapTo(x, y, z);
	}

	public EntityBallLightning setNoDrops() { drops = false; return this; }
	public EntityBallLightning setPylon() { pylonSpawn = true; return this; }
	public CrystalElement getElement() { return CrystalElement.elements[this.entityData.get(COLOR)]; }
	public int getRenderColor() { return this.entityData.get(RENDER_COLOR); }
	@Override public Component getName() {
		return this.hasCustomName() ? super.getName()
				: Component.literal(this.getElement().displayName() + " Ball Lightning");
	}

	private void setColor(CrystalElement element) {
		color = element;
		this.entityData.set(COLOR, element.ordinal());
		this.entityData.set(RENDER_COLOR, element.color());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COLOR, CrystalElement.WHITE.ordinal());
		builder.define(RENDER_COLOR, CrystalElement.WHITE.color());
		builder.define(BOLT_TARGET, 0);
		builder.define(BOLT_SEQUENCE, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20);
	}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ChromaEntityTypes.BALL_LIGHTNING.get(), createAttributes().build());
	}

	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		event.register(ChromaEntityTypes.BALL_LIGHTNING.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityBallLightning::checkSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.OR);
	}

	private static boolean checkSpawnRules(EntityType<EntityBallLightning> type,
			LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return ChromaOptions.BALLLIGHTNING.getState() && random.nextInt(5) == 0
				&& level.canSeeSky(pos.above()) && canSpawnHere(level, pos);
	}

	public static boolean canSpawnHere(LevelAccessor level, BlockPos pos) {
		int nearby = level.getEntitiesOfClass(EntityBallLightning.class,
				new AABB(pos).inflate(24, 1024, 24)).size();
		return level.getRandom().nextInt(1 + nearby / 16) == 0;
	}

	@Override protected void registerGoals() {}
	@Override public int getMaxSpawnClusterSize() { return 4; }
	@Override public boolean isPushable() { return false; }
	@Override public boolean removeWhenFarAway(double distanceSquared) { return distanceSquared >= 65536; }
	@Override public boolean isPushedByFluid() { return false; }
	@Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
	@Override public boolean displayFireAnimation() { return false; }
	@Override public boolean causeFallDamage(double distance, float modifier, DamageSource source) { return false; }
	@Override protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.NONE; }
	@Override protected SoundEvent getAmbientSound() { return ChromaSounds.BALLLIGHTNING.getSoundEvent(); }
	@Override public int getAmbientSoundInterval() { return 20; }
	@Override public float getVoicePitch() { return CrystalMusicManager.instance.getRandomScaledDing(this.getElement()); }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return null; }
	@Override protected SoundEvent getDeathSound() { return null; }
	@Override protected void dropExperience(ServerLevel level, Entity killer) {}

	@Override
	public void aiStep() {
		super.aiStep();
		this.fallDistance = 0;
		if (this.level().isClientSide()) {
			ChromaParticle.spawnBallLightningAmbient(this.level(), this.getX(), this.getY(), this.getZ(),
					this.getRenderColor(), this.random);
			int targetId = this.entityData.get(BOLT_TARGET);
			int sequence = this.entityData.get(BOLT_SEQUENCE);
			if (targetId > 0 && sequence != lastBoltSequence) {
				Entity target = this.level().getEntity(targetId);
				if (target instanceof EntityBallLightning other)
					ChromaParticle.spawnBallLightningBolt(this.level(), this.position(), other.position(),
							this.getRenderColor(), other.getRenderColor(), this.random);
			}
			lastBoltSequence = sequence;
			return;
		}
		if (!ChromaOptions.BALLLIGHTNING.getState()) { this.discard(); return; }
		this.tickMovement();
		this.tickColor();
		this.touchEntities();
		if (this.isRemoved()) return;
		this.tryBolt();
		this.tickServerBehavior();
	}

	private void tickMovement() {
		if (ReikaMathLibrary.approxr(velocity.inclination, targetTheta, 2))
			targetTheta = this.random.nextInt(360);
		else
			velocity.inclination += targetTheta > velocity.inclination ? 1 : -1;
		if (ReikaMathLibrary.approxr(velocity.rotation, targetPhi, 2))
			targetPhi = this.random.nextInt(360);
		else
			velocity.rotation += targetPhi > velocity.rotation ? 1 : -1;
		if (ReikaMathLibrary.approxr(velocity.magnitude, targetVelocity, 0.05))
			targetVelocity = ReikaRandomHelper.getRandomPlusMinus(0.1, 0.1);
		else
			velocity.magnitude += targetVelocity > velocity.magnitude ? 0.01 : -0.01;
		double[] movement = velocity.getCartesian();
		this.setDeltaMovement(movement[0], movement[1], movement[2]);
	}

	private void tickColor() {
		if (targetColor != null) {
			colorTransitionFraction += 0.1F;
			if (colorTransitionFraction >= 1) {
				this.setColor(targetColor);
				targetColor = null;
				colorTransitionFraction = 0;
			}
		}
		this.entityData.set(RENDER_COLOR, targetColor == null ? color.color()
				: ReikaColorAPI.mixColors(targetColor.color(), color.color(), colorTransitionFraction));
	}

	private void touchEntities() {
		List<LivingEntity> touching = this.level().getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox(), other -> other != this && !(other instanceof EntityBallLightning));
		for (LivingEntity other : touching) {
			if (other instanceof Player player && player.isCreative()) continue;
			if (ChromaOptions.HOSTILEFOREST.getState()) other.hurt(this.damageSources().generic(), 5);
			if (other instanceof Player player) ProgressStage.BALLLIGHTNING.stepPlayerTo(player);
			this.die();
			return;
		}
	}

	private void tryBolt() {
		if (colorTransitionFraction != 0 || this.random.nextInt(400) != 0) return;
		List<EntityBallLightning> nearby = this.level().getEntitiesOfClass(EntityBallLightning.class,
				this.getBoundingBox().inflate(24), other -> other != this && other.colorTransitionFraction == 0);
		if (nearby.isEmpty()) return;
		EntityBallLightning other = nearby.stream().min((first, second) ->
				Double.compare(this.distanceToSqr(first), this.distanceToSqr(second))).orElseThrow();
		targetColor = other.color;
		other.targetColor = color;
		this.entityData.set(BOLT_TARGET, other.getId());
		this.entityData.set(BOLT_SEQUENCE, this.entityData.get(BOLT_SEQUENCE) + 1);
		if (ChromaOptions.HOSTILEFOREST.getState()) {
			Vec3 direction = other.position().subtract(this.position());
			double length = direction.length();
			for (double step = 0; step < length; step += 0.0625) {
				Vec3 point = this.position().add(direction.scale(step / length));
				for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
						new AABB(point, point).inflate(0.5)))
					victim.hurt(this.damageSources().generic(), pylonSpawn ? 4 : 1);
			}
		}
	}

	private void tickServerBehavior() {
		if (this.random.nextInt(1600) == 0
				&& !CrystalNetworker.instance.getNearbyPylons(this.level(), this.getX(), this.getY(),
						this.getZ(), color, 24, false).isEmpty())
			this.heal(this.getMaxHealth());
		Player player = this.level().getNearestPlayer(this, -1);
		if (this.getY() >= 128 || player == null || this.tickCount >= 12000
				|| this.random.nextInt(12000 - this.tickCount) == 0
				|| this.isInWater() || this.distanceToSqr(player) >= 65536
				|| this.distanceToSqr(player) >= 1024 && this.random.nextInt(200) == 0
				|| this.level().isRaining() && this.random.nextInt(80) == 0) {
			this.die();
			return;
		}
		if (this.random.nextInt(20) == 0) {
			int count = this.level().getEntitiesOfClass(EntityBallLightning.class,
					this.getBoundingBox().inflate(24, 1024, 24)).size();
			if (this.random.nextInt(1 + count / 16) > 0) this.die();
		}
	}

	private void die() { this.discard(); }

	@Override
	public void onClientRemoval() {
		ChromaParticle.spawnBallLightningDeath(this.level(), this.getX(), this.getY(), this.getZ(),
				this.getRenderColor(), this.random);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof Player player) || ReikaPlayerAPI.isFake(player)) return false;
		boolean damaged = super.hurtServer(level, source, amount);
		if (damaged && this.getHealth() <= 0) {
			ChromaSounds.DISCHARGE.playSound(this, 1, 2);
			if (drops) this.dropLoot(player);
			ProgressStage.BALLLIGHTNING.stepPlayerTo(player);
			this.die();
		}
		return damaged;
	}

	private void dropLoot(Player player) {
		int looting = EnchantmentHelper.getItemEnchantmentLevel(
				this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
						.getOrThrow(Enchantments.LOOTING), player.getMainHandItem());
		if (ChromaTieredPlants.RADIANCE_BUSH.stage().isPlayerAtStage(player)) {
			int count = this.random.nextInt(1 + looting * 2);
			if (count > 0) this.spawnAtLocation((ServerLevel)this.level(),
					new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.BEACON_DUST).get(), count));
		}
		if (looting > 1 && color.isPrimary()) {
			ChromaTieredItems dust = ChromaTieredPlants.ROCK_FLOWER.stage().isPlayerAtStage(player)
					? ChromaTieredItems.PURITY_DUST
					: ChromaTieredPlants.AURA_BLOOM.stage().isPlayerAtStage(player)
							? ChromaTieredItems.AURA_DUST : null;
			if (dust != null) this.spawnAtLocation((ServerLevel)this.level(),
					new ItemStack(ChromaItems.TIERED.get(dust).get()));
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int index = input.getIntOr("color", -1);
		this.setColor(index >= 0 && index < CrystalElement.elements.length
				? CrystalElement.elements[index] : color);
		pylonSpawn = input.getBooleanOr("pylon", false);
		drops = input.getBooleanOr("dodrops", true);
		if (input.getBooleanOr("isdead", false)) this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("color", color.ordinal());
		output.putBoolean("pylon", pylonSpawn);
		output.putBoolean("dodrops", drops);
		Entity.RemovalReason removal = this.getRemovalReason();
		output.putBoolean("isdead", removal != null && removal.shouldDestroy());
	}

	@Override public void destroy() { this.discard(); }
}
