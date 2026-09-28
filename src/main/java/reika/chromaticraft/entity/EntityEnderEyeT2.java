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

import java.util.Random;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.chromaticraft.auxiliary.ElementEncodedNumber.EncodedPosition;
import reika.chromaticraft.magic.lore.LoreManager;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.Interpolation;

/**
 * V33a {@code EntityEnderEyeT2}, the thrown Enhanced Ender Eye.
 *
 * <p>It flies like a vanilla Eye of Ender, but sees three times as far ({@link #RANGE} 36 instead of 12),
 * moves under heavy damping, lives {@link #ADDITIONAL_LIFE} ticks longer, never falls as a vanilla eye,
 * and always returns as its own item, still bound to its owner. Beyond {@link #FUZZ} blocks from the
 * stronghold it also trails a spiral of colours that spell out the stronghold's coordinates in the
 * element code, and it may veer off toward the nearest lore tower the owner has not yet scanned, the more
 * likely the closer that tower is.
 *
 * <p>V33a extended the vanilla eye and reached into its target and despawn fields; those are private in
 * 26.2, so the vanilla eye's flight is reproduced here and V33a's changes applied on top of it.
 */
public final class EntityEnderEyeT2 extends Entity implements ItemSupplier, IEntityWithComplexSpawn {

	public static final int FUZZ = 440;
	public static final double DEVIATION_CHANCE_BASE = 0.08;
	public static final double DEVIATION_CHANCE_MAX = 0.4;
	public static final int ADDITIONAL_LIFE = 60;
	public static final double RANGE = 36; // vanilla is 12

	private static final Interpolation deviationCurve = new Interpolation(false);
	private static final Interpolation distanceColor = new Interpolation(true);

	static {
		distanceColor.addPoint(0, 0x606060);
		distanceColor.addPoint(FUZZ, 75);
		distanceColor.addPoint(6000, 180);
		distanceColor.addPoint(0, 0xffffff);

		deviationCurve.addPoint(0, DEVIATION_CHANCE_MAX);
		deviationCurve.addPoint(FUZZ, DEVIATION_CHANCE_MAX);
		double dm = DEVIATION_CHANCE_BASE + (DEVIATION_CHANCE_MAX - DEVIATION_CHANCE_BASE) / 2D;
		deviationCurve.addPoint(1024, dm);
		deviationCurve.addPoint(4096, DEVIATION_CHANCE_BASE);
	}

	private Vec3 target;
	/** Vanilla's despawn timer; V33a starts it at {@code -ADDITIONAL_LIFE}. */
	private int life;

	private UUID owner;
	private int deviateTime;
	private int deviationTick;

	private EncodedPosition colorData;
	private Interpolation spiralColor;

	private double finalX;
	private double finalZ;
	private double spawnX;
	private double spawnZ;
	private double totalDistance;

	public EntityEnderEyeT2(EntityType<? extends EntityEnderEyeT2> type, Level level) {
		super(type, level);
	}

	public EntityEnderEyeT2(Level level, double x, double y, double z) {
		this(ChromaEntityTypes.ENDER_EYE.get(), level);
		this.setPos(x, y, z);
	}

	public static EntityEnderEyeT2 create(Level level, double x, double y, double z, UUID owner) {
		EntityEnderEyeT2 eye = new EntityEnderEyeT2(level, x, y, z);
		eye.owner = owner;
		return eye;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(ChromaItems.ENDER_EYE.get());
	}

	/** V33a {@code moveTowards}. */
	public void moveTowards(double x, int y, double z) {
		finalX = x;
		finalZ = z;
		spawnX = this.getX();
		spawnZ = this.getZ();

		totalDistance = this.setTarget(x, y, z);
		life = -ADDITIONAL_LIFE;

		if (totalDistance > FUZZ) {
			int offset = new Random(System.identityHashCode(this.level()) + this.level().getGameTime() / 24000).nextInt(16);
			colorData = new EncodedPosition(offset, Mth.floor(finalX), Mth.floor(finalZ));
		}

		Vec3 c = this.getNearestTower();
		deviateTime = c != null && this.random.nextDouble() < deviationCurve.getValue(c.distanceTo(this.position()))
				? 10 + this.random.nextInt(80 + ADDITIONAL_LIFE - 40 - 10) : -1;
	}

	private double setTarget(double x, double y, double z) {
		double dx = x - this.getX();
		double dz = z - this.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d > RANGE)
			target = new Vec3(this.getX() + dx / d * RANGE, this.getY() + 8.0D, this.getZ() + dz / d * RANGE);
		else
			target = new Vec3(x, y, z);
		return d;
	}

	private Vec3 getNearestTower() {
		if (!(this.level() instanceof ServerLevel world))
			return null;
		Towers t = LoreManager.instance.getNearestActiveTower(world, this.getX(), this.getZ(), this.getOwner());
		if (t == null)
			return null;
		var c = t.getGeneratedLocation();
		if (c != null)
			return new Vec3(c.getX(), c.getY() + 18, c.getZ());
		return new Vec3(t.getRootPosition().x(), 96, t.getRootPosition().z());
	}

	/** V33a {@code deviate}, server side: bend toward the tower, then hand over to a fresh eye aimed at it. */
	private void deviate() {
		Vec3 c = this.getNearestTower();
		if (c == null)
			return;
		if (deviationTick > 30) {
			EntityEnderEyeT2 repl = create(this.level(), this.getX(), this.getY(), this.getZ(), owner);
			repl.moveTowards(c.x, (int)c.y, c.z);
			repl.deviateTime = -1;
			this.level().addFreshEntity(repl);
			this.discard();
		}
		else {
			deviationTick++;
			double dx = c.x - this.getX();
			double dz = c.z - this.getZ();
			double f = deviationTick / 30D;
			double tx = f * dx + (1 - f) * (finalX - this.getX());
			double tz = f * dz + (1 - f) * (finalZ - this.getZ());
			this.setTarget(tx, target != null ? target.y : this.getY(), tz);
		}
	}

	@Override
	public void tick() {
		if (this.level().isClientSide())
			this.doParticles();
		else if (this.tickCount % 8 == 0 && this.level() instanceof ServerLevel server)
			ChromaNetwork.sendEnderEyeSync(server, this);

		boolean flag = deviateTime > 0 && this.tickCount >= deviateTime;
		this.vanillaTick();
		if (this.isRemoved())
			return;
		if (flag) {
			if (!this.level().isClientSide())
				this.deviate();
			this.setDeltaMovement(this.getDeltaMovement().scale(0.85));
		}
		else {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
		}
		this.syncVelocity = true;
	}

	/** 26.2 {@code EyeOfEnder.tick}, with V33a's lifetime and its never-drop-a-vanilla-eye rule. */
	private void vanillaTick() {
		super.tick();
		Vec3 newPosition = this.position().add(this.getDeltaMovement());
		if (!this.level().isClientSide() && target != null)
			this.setDeltaMovement(updateDeltaMovement(this.getDeltaMovement(), newPosition, target));
		if (this.level().isClientSide()) {
			Vec3 origin = newPosition.subtract(this.getDeltaMovement().scale(0.25));
			Vec3 movement = this.getDeltaMovement();
			if (this.isInWater()) {
				for (int i = 0; i < 4; i++)
					this.level().addParticle(ParticleTypes.BUBBLE, origin.x, origin.y, origin.z, movement.x, movement.y, movement.z);
			}
			else {
				this.level().addParticle(ParticleTypes.PORTAL, origin.x + this.random.nextDouble() * 0.6 - 0.3,
						origin.y - 0.5, origin.z + this.random.nextDouble() * 0.6 - 0.3, movement.x, movement.y, movement.z);
			}
		}
		this.setPos(newPosition);
		if (!this.level().isClientSide()) {
			life++;
			if (life > 80) {
				// 1.7.10's eye only shattered here; 26.2's death sound did not exist.
				this.level().levelEvent(2003, this.blockPosition(), 0);
				// V33a: the eye always comes back as its own item, still bound to its owner.
				ItemStack is = new ItemStack(ChromaItems.ENDER_EYE.get());
				if (owner != null) {
					CompoundTag tag = new CompoundTag();
					tag.putString("owner", owner.toString());
					is.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
				}
				this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), is));
				this.discard();
			}
		}
	}

	private static Vec3 updateDeltaMovement(Vec3 oldMovement, Vec3 position, Vec3 target) {
		Vec3 horizontalDelta = new Vec3(target.x - position.x, 0.0, target.z - position.z);
		double horizontalLength = horizontalDelta.length();
		double wantedSpeed = Mth.lerp(0.0025, oldMovement.horizontalDistance(), horizontalLength);
		double movementY = oldMovement.y;
		if (horizontalLength < 1.0) {
			wantedSpeed *= 0.8;
			movementY *= 0.8;
		}
		double wantedMovementY = position.y - oldMovement.y < target.y ? 1.0 : -1.0;
		return horizontalDelta.scale(wantedSpeed / horizontalLength).add(0.0, movementY + (wantedMovementY - movementY) * 0.015, 0.0);
	}

	/** V33a {@code doParticles}: the distance-coloured trail, and past {@link #FUZZ} the coordinate spiral. */
	private void doParticles() {
		if (this.tickCount >= 6) {
			int n = 2 + this.random.nextInt(5);
			double r = 0.125;
			for (int i = 0; i < n; i++) {
				double dx = this.getX() + (this.random.nextDouble() * 2 - 1) * r;
				double dy = this.getY() + (this.random.nextDouble() * 2 - 1) * r;
				double dz = this.getZ() + (this.random.nextDouble() * 2 - 1) * r;
				int l = Math.max(1, 80 - this.tickCount + this.random.nextInt(40));
				float s = (float)(1.5 + this.random.nextDouble() * 1.5);
				int c = reika.dragonapi.libraries.rendering.ReikaColorAPI.getModifiedHue(0xff0000, 75 + this.random.nextInt(106));
				if (this.random.nextInt(6) > 0)
					c = (int)distanceColor.getValue(totalDistance);
				reika.chromaticraft.render.particle.ChromaParticle.spawnIconFade(this.level(), dx, dy, dz, 0, 0, 0,
						c, l, s, "fade_gentle", true, true);
			}
		}

		if (colorData != null && spiralColor != null && (deviateTime < 0 || deviateTime > this.tickCount)) {
			int t = this.tickCount;
			int c = (int)spiralColor.getValue(t);
			double lf = 80 + ADDITIONAL_LIFE;
			double ang = t * 360D / lf * 2;
			Vec3 vec = reika.dragonapi.libraries.mathsci.ReikaVectorHelper.getPointAroundVector(this.getDeltaMovement(), 3, ang);
			reika.chromaticraft.render.particle.ChromaParticle.spawnIconFade(this.level(), this.getX() + vec.x,
					this.getY() + vec.y, this.getZ() + vec.z, 0, 0, 0, c, 180, 7.5F, "fade_basic", false, true);
		}
	}

	public boolean isOwner(Player ep) {
		return ep.getUUID().equals(owner);
	}

	public Player getOwner() {
		return owner != null ? this.level().getPlayerByUUID(owner) : null;
	}

	/** V33a ENDEREYESYNC: the client's age drives the spiral's colour timing. */
	public void doSync(int t) {
		this.tickCount = t;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		if (this.tickCount < 2 && distance < 12.25)
			return false;
		double size = this.getBoundingBox().getSize() * 4.0;
		if (Double.isNaN(size))
			size = 4.0;
		size *= 64.0;
		return distance < size * size;
	}

	@Override
	public float getLightLevelDependentMagicValue() {
		return 1.0F;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (owner != null)
			output.putString("owner", owner.toString());
		output.putInt("life", life);
		if (target != null) {
			output.putDouble("targetX", target.x);
			output.putDouble("targetY", target.y);
			output.putDouble("targetZ", target.z);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		String o = input.getStringOr("owner", "");
		owner = o.isEmpty() ? null : UUID.fromString(o);
		life = input.getIntOr("life", 0);
		if (!Double.isNaN(input.getDoubleOr("targetX", Double.NaN)))
			target = new Vec3(input.getDoubleOr("targetX", 0), input.getDoubleOr("targetY", 0), input.getDoubleOr("targetZ", 0));
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buf) {
		buf.writeDouble(finalX);
		buf.writeDouble(finalZ);
		buf.writeDouble(spawnX);
		buf.writeDouble(spawnZ);
		buf.writeInt(deviateTime);
		if (colorData != null) {
			buf.writeInt(colorData.totalLength());
			colorData.writeData(buf);
		}
		else {
			buf.writeInt(-1);
		}
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buf) {
		finalX = buf.readDouble();
		finalZ = buf.readDouble();
		spawnX = buf.readDouble();
		spawnZ = buf.readDouble();
		deviateTime = buf.readInt();
		totalDistance = Math.hypot(finalX - spawnX, finalZ - spawnZ);
		int amt = buf.readInt();
		if (amt != -1) {
			colorData = EncodedPosition.readData(buf);
			spiralColor = new Interpolation(true);
			int lf = 80 + ADDITIONAL_LIFE;
			for (int i = 0; i < colorData.totalLength(); i++) {
				CrystalElement e = colorData.getColor(i);
				int t = lf * i / amt;
				int t2 = lf * (i + 1) / amt;
				int cr = 3;
				int c = colorData.isVariableChange(i) ? 0x22aaff : colorData.isPartOfNegative(i) ? 0x000000 : 0xffffff;
				spiralColor.addPoint(t, c);
				spiralColor.addPoint(t + cr, e.getColor());
				spiralColor.addPoint(t2 - cr, e.getColor());
				spiralColor.addPoint(t2, c);
			}
		}
	}
}
