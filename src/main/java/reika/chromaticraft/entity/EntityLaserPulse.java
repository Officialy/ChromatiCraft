package reika.chromaticraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.interfaces.LaserPulseEffect;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEntityTypes;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic.BeamColor;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic.Pulse;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseReceiver;
import reika.dragonapi.base.ParticleEntity;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/** V33a's persistent travelling Chromatic Beams pulse on the 26.2 entity pipeline. */
public final class EntityLaserPulse extends ParticleEntity {

	private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(
			EntityLaserPulse.class, EntityDataSerializers.BYTE);

	private BeamColor color = BeamColor.WHITE;
	private CubeDirections direction = CubeDirections.NORTH;
	private boolean silentImpact;
	private double moveSpeed = 0.1875;
	private boolean impactDeath;

	public EntityLaserPulse(EntityType<? extends EntityLaserPulse> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	public EntityLaserPulse(Level world, BlockPos pos, CubeDirections direction, BeamColor color,
			boolean silentImpact, double speedFactor) {
		super(ChromaEntityTypes.LASER_PULSE.get(), world, pos);
		this.noPhysics = true;
		this.direction = direction;
		this.color = color;
		this.silentImpact = silentImpact;
		this.moveSpeed = Math.min(0.2, 0.1875 * speedFactor);
		this.entityData.set(COLOR, packColor(color));
		this.setDirection(direction, true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOR, packColor(BeamColor.WHITE));
	}

	@Override protected double getBlockThreshold() { return 0.125; }
	@Override protected double getDespawnDistance() { return 40; }

	@Override
	protected void onTick() {
		if (this.level().isClientSide()) {
			color = unpackColor(this.entityData.get(COLOR));
			ChromaParticle.spawnLaserPulseTrail(this.level(), this.getX(), this.getY(), this.getZ(),
					color.renderColor(), this.random);
		}
	}

	@Override
	protected boolean onEnterBlock(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir() || state.is(ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get()))
			return false;

		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof LaserPulseReceiver receiver) {
			playTonalSound(ChromaSounds.USE, 0.5F, 2);
			boolean absorbed = receiver.receiveLaserPulse(this);
			impactDeath = absorbed;
			return absorbed;
		}
		if (state.getBlock() instanceof LaserPulseEffect effect) {
			boolean absorbed = effect.onImpact(world, pos, this);
			impactDeath = absorbed;
			return absorbed;
		}

		impactDeath = true;
		if (!silentImpact) playTonalSound(ChromaSounds.POWERDOWN, 0.5F, 2);
		return true;
	}

	@Override
	protected void onDeath() {
		if (impactDeath && this.level().isClientSide() && !silentImpact)
			ChromaParticle.spawnLaserPulseImpact(this.level(), this.getX(), this.getY(), this.getZ(),
					color.renderColor(), this.random);
	}

	private void playTonalSound(ChromaSounds sound, float volume, float basePitch) {
		if (color.red()) sound.playSound(this.level(), this.getX(), this.getY(), this.getZ(),
				volume, basePitch * 0.5F);
		if (color.green()) sound.playSound(this.level(), this.getX(), this.getY(), this.getZ(),
				volume, basePitch * 0.75F);
		if (color.blue()) sound.playSound(this.level(), this.getX(), this.getY(), this.getZ(),
				volume, basePitch);
	}

	public void setPulse(Pulse pulse) {
		this.setColor(pulse.color());
		this.setDirection(pulse.direction(), true);
	}

	public void setColor(BeamColor color) {
		this.color = color;
		this.entityData.set(COLOR, packColor(color));
	}

	public void setDirection(CubeDirections direction, boolean setPosition) {
		this.direction = direction;
		if (setPosition)
			this.snapTo(this.getBlockX() + 0.5, this.getBlockY() + 0.5,
					this.getBlockZ() + 0.5, 0, 0);
		this.setDeltaMovement(direction.directionX * moveSpeed, 0, direction.directionZ * moveSpeed);
		this.hurtMarked = true;
	}

	public Pulse pulse() { return new Pulse(direction, color); }
	public BeamColor color() { return color; }
	public CubeDirections direction() { return direction; }
	public boolean silentImpact() { return silentImpact; }
	public double speedFactor() { return moveSpeed / 0.1875; }

	@Override public void applyEntityCollision(Entity entity) {}
	@Override public boolean despawnOverTime() { return false; }
	@Override public boolean despawnOverDistance() { return true; }
	@Override public boolean canInteractWithSpawnLocation() { return false; }
	@Override public double getSpeed() { return moveSpeed; }
	@Override public double getHitboxSize() { return 0.05; }
	@Override public double getRenderRangeSquared() { return Double.POSITIVE_INFINITY; }

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		super.writeSpawnData(data);
		data.writeByte(packColor(color));
		data.writeVarInt(direction.ordinal());
		data.writeBoolean(silentImpact);
		data.writeDouble(moveSpeed);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		super.readSpawnData(data);
		this.setColor(unpackColor(data.readByte()));
		direction = CubeDirections.list[Math.floorMod(data.readVarInt(), CubeDirections.list.length)];
		silentImpact = data.readBoolean();
		moveSpeed = data.readDouble();
		this.setDirection(direction, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setColor(new BeamColor(input.getBooleanOr("red", true),
				input.getBooleanOr("green", true), input.getBooleanOr("blue", true)));
		direction = CubeDirections.list[Math.floorMod(input.getIntOr("direction", 0),
				CubeDirections.list.length)];
		silentImpact = input.getBooleanOr("silent", false);
		moveSpeed = input.getDoubleOr("speed", 0.1875);
		this.setDirection(direction, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("red", color.red());
		output.putBoolean("green", color.green());
		output.putBoolean("blue", color.blue());
		output.putInt("direction", direction.ordinal());
		output.putBoolean("silent", silentImpact);
		output.putDouble("speed", moveSpeed);
	}

	private static byte packColor(BeamColor color) {
		return (byte)((color.red() ? 1 : 0) | (color.green() ? 2 : 0) | (color.blue() ? 4 : 0));
	}

	private static BeamColor unpackColor(byte packed) {
		return new BeamColor((packed & 1) != 0, (packed & 2) != 0, (packed & 4) != 0);
	}
}
