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

import java.awt.Color;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.SphericalVector;
import reika.dragonapi.interfaces.entity.DestroyOnUnload;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a "Luma Fog" (registry id {@code glow_cloud}): a wandering, non-hostile-until-provoked cloud
 * that lights its own path, retaliates when attacked, and spreads anger to nearby clouds. Ported
 * from the pristine {@code entity/EntityGlowCloud.java} (753 lines, 1.7.10).
 *
 * <p>Movement is fully self-driven (a {@link SphericalVector} recomputed every tick), so this
 * extends {@link Mob} directly (not {@link net.minecraft.world.entity.PathfinderMob}) with an empty
 * goal selector; {@link #aiStep()} never calls {@code super.aiStep()} and instead reimplements the
 * merged V33a {@code onUpdate()}/{@code onLivingUpdate()} body end to end.
 *
 * <p>Several V33a subsystems are still pristine 1.7.10 and are not yet importable from here; each is
 * marked {@code CHROMA-PORT} at its call site with the original logic preserved rather than deleted:
 * the ChromatiCraft pocket dimension ({@code ExtraChromaIDs.DIMID}), the ambient RF/IC2 tile-charging
 * and Crystal Tank top-off ({@code TileEntityCrystalTank}/{@code CrystalTankAuxTile}), the dynamic
 * ethereal light block ({@code ChromaBlocks.LIGHT}), the pylon self-damage after an attack
 * ({@code ChromatiCraft.pylonDamage}), and the ranged-pickup ability drop redirection
 * ({@code Chromabilities.RANGEDBOOST} / {@code ItemInventoryLinker}).
 */
public class EntityGlowCloud extends Mob implements DestroyOnUnload {

	private SphericalVector velocity;
	private double targetTheta;
	private double targetPhi;
	private double targetVelocity;

	private int color;
	private int targetColor;
	private int colorTransitionTick = 0;

	private boolean isNaturalSpawn = true;

	private static final int COLOR_TRANSITION_LENGTH = 120;
	private static final int SOLID_COLOR_LENGTH = 80;

	private boolean isAngry;
	private int attackCooldown = 20;

	/** V33a EntityGlowCloud.attack()'s client-visible burst; not a vanilla LivingEntity event id. */
	private static final byte ATTACK_EVENT_ID = 70;

	public EntityGlowCloud(EntityType<? extends EntityGlowCloud> type, Level world) {
		super(type, world);
		this.setNoGravity(true);
		color = this.generateRandomColor();
		targetColor = this.generateRandomColor();
		velocity = new SphericalVector(0.15, this.random.nextInt(360), this.random.nextInt(360));
		targetTheta = this.random.nextInt(360);
		targetPhi = this.random.nextInt(360);
		targetVelocity = ReikaRandomHelper.getRandomPlusMinus(0.1, 0.1);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0D);
	}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ChromaEntityTypes.GLOW_CLOUD.get(), createAttributes().build());
	}

	public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		event.register(ChromaEntityTypes.GLOW_CLOUD.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityGlowCloud::checkGlowCloudSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.OR);
	}

	/** V33a getCanSpawnHere(): 1-in-5 (1-in-3 in the ChromatiCraft dimension, y<=6 only) and no other
	 *  natural-spawn cloud within range. */
	private static boolean checkGlowCloudSpawnRules(EntityType<EntityGlowCloud> type, LevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		// CHROMA-PORT: ExtraChromaIDs.DIMID (the ChromatiCraft pocket dimension) is not yet
		// registered in 26.2, so V33a's tighter in-dimension rule (posY > 6 rejected, n = 3,
		// clustering radius 20 instead of 32) is dormant; every spawn attempt currently uses the
		// overworld/Luminous Cliffs branch below.
		int n = 5;
		int radius = 32;
		return random.nextInt(n) == 0
				&& level.getEntitiesOfClass(EntityGlowCloud.class, new AABB(pos).inflate(radius),
						e -> e.isNaturalSpawn).isEmpty();
	}

	private int generateRandomColor() {
		int hue = ReikaRandomHelper.getRandomBetween(120, 300);
		int c = Color.HSBtoRGB(hue / 360F, 1, 1);
		return ReikaColorAPI.mixColors(c, 0xffffff, this.random.nextFloat());
	}

	@Override
	protected void registerGoals() {
		// V33a drives every tick of motion directly from a SphericalVector in aiStep(); no goal
		// selector or navigation is used.
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE; // V33a func_146067_o/func_145780_a: no step sound/events.
	}

	@Override
	public boolean isPushedByFluid() {
		return false; // V33a handleWaterMovement() == false.
	}

	@Override
	public boolean isPushable() {
		return false; // V33a getCollisionBox(Entity) == null.
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false; // V33a fall(float) is a no-op.
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null; // V33a playLivingSound() is a no-op.
	}

	@Override
	public int getAmbientSoundInterval() {
		return 20; // V33a getTalkInterval().
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BAT_TAKEOFF; // V33a getHurtSound() == "mob.bat.takeoff".
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BAT_AMBIENT; // V33a getDeathSound() == "mob.bat.loop".
	}

	@Override
	protected void dropExperience(ServerLevel level, Entity killer) {
		// V33a dropFewItems() drops nothing on recent-hit death; real drops happen in doDrops().
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason spawnReason, SpawnGroupData groupData) {
		if (spawnReason != EntitySpawnReason.NATURAL) {
			isNaturalSpawn = false; // V33a onSpawnWithEgg().
		}
		return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
	}

	@Override
	public void aiStep() {
		// Merged V33a onUpdate() + onLivingUpdate(); intentionally never calls super.aiStep() so that
		// vanilla gravity/travel/goal-selector logic never fights the hand-driven SphericalVector.
		if (!this.level().isClientSide()) {
			this.tickMovement();
			this.doAmbientEffects();
		}

		colorTransitionTick++;
		if (colorTransitionTick >= COLOR_TRANSITION_LENGTH) {
			color = targetColor;
			targetColor = this.generateRandomColor();
			colorTransitionTick = -SOLID_COLOR_LENGTH;
		}

		if (this.level().isClientSide()) {
			this.lifeParticles();
		}

		this.fallDistance = 0;

		if (!this.level().isClientSide()) {
			this.tickServerBehavior();
		}
	}

	private void tickMovement() {
		if (ReikaMathLibrary.approxr(velocity.inclination, targetTheta, 2)) {
			targetTheta = this.random.nextInt(360);
		}
		else {
			velocity.inclination += targetTheta > velocity.inclination ? 1 : -1;
		}

		if (ReikaMathLibrary.approxr(velocity.rotation, targetPhi, 2)) {
			targetPhi = this.random.nextInt(360);
		}
		else {
			velocity.rotation += targetPhi > velocity.rotation ? 1 : -1;
		}

		if (ReikaMathLibrary.approxr(velocity.magnitude, targetVelocity, 0.05)) {
			targetVelocity = ReikaRandomHelper.getRandomPlusMinus(0.1, 0.1);
		}
		else {
			velocity.magnitude += targetVelocity > velocity.magnitude ? 0.01D : -0.01D;
		}

		if (this.onGround() || this.getY() <= -18) {
			velocity.inclination = 90;
			velocity.magnitude *= 2;
			this.setPos(this.getX(), this.getY() + 1, this.getZ());
		}

		double[] v = velocity.getCartesian();
		this.setDeltaMovement(v[0], v[1], v[2]);
		this.move(MoverType.SELF, this.getDeltaMovement());
	}

	/** CHROMA-PORT: V33a tops off a Crystal Tank's Luma fluid and donates 40 (120 if angry) RF/IC2
	 *  energy to a cached or randomly probed neighbour tile every tick, via
	 *  {@code reika.chromaticraft.tileentity.storage.TileEntityCrystalTank}/
	 *  {@code block.blockcrystaltank.CrystalTankAuxTile} and IC2's {@code IEnergySink}. Neither
	 *  {@code ChromaBlocks.TANK} nor an RF energy-receiver capability integration exists in the 26.2
	 *  port yet, so this is a dormant no-op until they land. See pristine
	 *  {@code EntityGlowCloud.doAmbientEffects()} for the exact original body. */
	private void doAmbientEffects() {
	}

	private void tickServerBehavior() {
		if (this.tickCount % 80 == 0) {
			ChromaSounds.GLOWCLOUD.playSound(this, 0.5F, 1.5F);
		}
		if (this.random.nextInt(40) == 0) {
			ChromaSounds.BUFFERWARNING_LOW.playSound(this, 1F, 0.5F);
		}

		if (!this.hasCustomName()) {
			Player ep = this.level().getNearestPlayer(this, -1);
			if (ep == null || this.level().players().isEmpty()) {
				this.die();
			}
			else if (this.tickCount >= 80000 || this.random.nextInt(Math.max(1, 80000 - this.tickCount)) == 0) {
				this.die();
			}
			else if (this.isInWater()) {
				this.die();
			}
			else if (this.distanceToSqr(ep) >= 65536) {
				this.die();
			}
			else if (this.distanceToSqr(ep) >= 16384 && this.random.nextInt(200) == 0) {
				this.die();
			}
			if (this.isRemoved()) {
				return;
			}
		}

		if (isAngry) {
			Player ep = this.level().getNearestPlayer(this, -1);
			if (ep != null) {
				if (attackCooldown > 0) {
					attackCooldown--;
				}
				else if (this.distanceToSqr(ep) <= 64) {
					if (this.random.nextInt(40) == 0) {
						this.attack();
					}
				}
				if (velocity != null) {
					velocity.aimFrom(this.getX(), this.getY(), this.getZ(), ep.getX(), ep.getY() + 1.62, ep.getZ());
					velocity.magnitude = 0.375;
				}
			}
		}
		else {
			// CHROMA-PORT: ExtraChromaIDs.DIMID (ChromatiCraft pocket dimension) not yet registered in
			// 26.2; V33a's line-of-sight homing-in-the-dimension behaviour (using RayTracer LOS below)
			// is dormant until that dimension exists. RayTracer itself is fully ported (see
			// RayTracer.getVisualLOSForRenderCulling()/isClearLineOfSight), ready for when it lands:
			//
			// LOS.setOrigins(ep.getX(), ep.getY()+1.62, ep.getZ(), getX(), getY(), getZ());
			// if (LOS.isClearLineOfSight(level())) { velocity.aimFrom(...); velocity.magnitude = 0.125; }
		}
	}

	private void attack() {
		this.doAttack();
		((ServerLevel)this.level()).broadcastEntityEvent(this, ATTACK_EVENT_ID);
	}

	private void doAttack() {
		attackCooldown = 15;
		AABB box = ReikaAABBHelper.getEntityCenteredAABB(this, 8);
		List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity e : nearby) {
			if (!(e instanceof EntityGlowCloud)) {
				e.hurt(this.damageSources().magic(), 4);
				if (e instanceof Player && e.getHealth() <= 0) {
					isAngry = false;
				}
			}
		}
		// CHROMA-PORT: ChromatiCraft.pylonDamage[0] (BLACK-element PylonDamage) self-damages this
		// cloud for 2 after every attack in V33a. PylonDamage (auxiliary/PylonDamage.java) is still
		// pristine 1.7.10, so `this.hurt(ChromatiCraft.pylonDamage[0], 2)` can't be wired up yet.
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == ATTACK_EVENT_ID) {
			this.doAttackFX();
		}
		else {
			super.handleEntityEvent(id);
		}
	}

	public void doAttackFX() {
		// CHROMA-PORT: V33a scales the pitch by
		// 2 * CrystalMusicManager.instance.getRandomScaledDing(CrystalElement.BLACK). Crystal
		// MusicManager is still pristine 1.7.10 (its reikamusichelper.* imports predate the modern
		// ReikaMusicHelper nested-enum layout), so the pitch is left un-scaled below.
		ReikaSoundHelper.playClientSound(ChromaSounds.FLAREATTACK, this, 2F, 2F, true);
		ChromaParticle.spawnGlowCloudAttack(this.level(), this.getX(), this.getY(), this.getZ(),
				this.getRenderColor(), this.random);
	}

	private void lifeParticles() {
		ChromaParticle.spawnGlowCloudAmbient(this.level(), this.getX(), this.getY(), this.getZ(),
				this.getRenderColor(), this.random);
	}

	private void die() {
		this.discard();
	}

	@Override
	public void onClientRemoval() {
		// V33a die()/setDead() sent a CLOUDDIE packet so nearby clients could render the death burst
		// after the entity was already gone; the vanilla client-removal hook supersedes that packet.
		this.doDeathParticles();
	}

	public void doDeathParticles() {
		ChromaParticle.spawnGlowCloudDeath(this.level(), this.getX(), this.getY(), this.getZ(),
				this.getRenderColor(), this.random);
	}

	public int getRenderColor() {
		float f = this.getColorFraction();
		return f > 0 ? ReikaColorAPI.mixColors(targetColor, color, f) : color;
	}

	private float getColorFraction() {
		return colorTransitionTick > 0 ? (float)colorTransitionTick / COLOR_TRANSITION_LENGTH : 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource src, float dmg) {
		// CHROMA-PORT: ExtraChromaIDs.DIMID (ChromatiCraft pocket dimension) not registered in 26.2
		// yet; V33a made this entity fully invulnerable there (`return false` unconditionally). That
		// early-return is dormant until the dimension exists.
		Entity attacker = src.getEntity();
		if (attacker instanceof Player player) {
			boolean flag = super.hurtServer(level, src, dmg);
			if (flag && this.getHealth() <= 0) {
				this.die();
				this.doDrops(player);
			}
			isAngry = true;
			int n = this.random.nextInt(8);
			int r = this.random.nextInt(64);
			AABB box = ReikaAABBHelper.getEntityCenteredAABB(attacker, r);
			List<EntityGlowCloud> nearby = level.getEntitiesOfClass(EntityGlowCloud.class, box, e -> e != this);
			for (int i = 0; i < Math.min(n, nearby.size()); i++) {
				nearby.get(i).isAngry = true;
			}
			return flag;
		}
		// CHROMA-PORT: `src instanceof PylonDamage -> super.hurtServer + die()` can't be wired up
		// (auxiliary/PylonDamage.java is still pristine 1.7.10 and off the 26.2 classpath). Every
		// other damage source (fire, explosions, etc.) matches V33a's blanket `return false` below.
		return false;
	}

	private void doDrops(Player ep) {
		this.drop(ep, new ItemStack(Items.GLOWSTONE_DUST, 1 + this.random.nextInt(16)));
		if (this.random.nextInt(10) == 0) {
			this.drop(ep, new ItemStack(Items.GHAST_TEAR));
		}
		// CHROMA-PORT: ChromaStacks.energyPowder (ChromaItems.CRAFTING meta 28, "Energy Powder") is a
		// 1-in-3 drop in V33a. ChromaItems.java is owned by the parallel naming-audit wave this cycle
		// and can't be touched here; wire this drop back in once that item lands.
	}

	private void drop(Player ep, ItemStack is) {
		// CHROMA-PORT: Chromabilities.RANGEDBOOST + ItemInventoryLinker.tryLinkItem (both still
		// pristine 1.7.10) redirect drops straight into the ability-holder's inventory instead of a
		// physical item entity. Until they're ported, drops always fall normally, matching the
		// non-ability-holder branch of V33a's drop() exactly.
		ReikaItemHelper.dropItem(this, is);
	}

	public void aimAwayFrom(double x, double y, double z, double speed) {
		if (velocity != null) {
			double dx = -(x - this.getX());
			double dy = -(y - this.getY());
			double dz = -(z - this.getZ());
			velocity.aimFrom(this.getX(), this.getY(), this.getZ(), dx, dy, dz);
			velocity.magnitude = speed;
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);

		if (input.getBooleanOr("isdead", false)) {
			this.discard();
		}
		isAngry = input.getBooleanOr("angry", false);
		isNaturalSpawn = input.getBooleanOr("natural", false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		output.putBoolean("isdead", this.isRemoved());
		output.putBoolean("angry", isAngry);
		output.putBoolean("natural", isNaturalSpawn);
	}

	@Override
	public void destroy() {
		// CHROMA-PORT: ExtraChromaIDs.DIMID (ChromatiCraft pocket dimension) not registered in 26.2
		// yet; V33a only self-destroys on dimension unload there. DestroyOnUnload has no confirmed
		// caller yet in the ported DragonAPI/ChromatiCraft either, so this is presently inert.
	}

}
