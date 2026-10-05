package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector26;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.Effector;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic.BeamColor;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic.Pulse;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseReceiver;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/** Persistent V33a effector settings, collision behavior and emitter/prism/target ticking. */
public final class TileEntityLaserEffector extends BlockEntity implements LaserPulseReceiver {

	private @Nullable BlockPos controllerPos;
	private BeamColor color = BeamColor.WHITE;
	private boolean rotateable;
	private boolean fixed;
	private int rotateableDifficulty;
	private int prismTimer = 2;
	private boolean fullBlock;
	private boolean silent;
	private double speedFactor = 1;
	private boolean firing;
	private boolean triggered;
	private int autoReset;
	private int resetTick;
	private BeamColor pending = BeamColor.BLACK;
	private int prismCountdown;

	public TileEntityLaserEffector(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LASER_EFFECTOR.get(), pos, state);
	}

	public void configure(Effector effector, BlockPos controller) {
		controllerPos = controller.immutable();
		color = new BeamColor(effector.red(), effector.green(), effector.blue());
		rotateable = effector.rotateable();
		fixed = effector.fixed();
		rotateableDifficulty = effector.rotateableDifficulty();
		prismTimer = Math.max(1, effector.prismTimer());
		autoReset = Math.max(0, effector.autoReset());
		fullBlock = effector.fullBlock();
		silent = effector.silent();
		speedFactor = effector.speedFactor();
		setChanged();
	}

	public EffectType effectType() {
		return EffectType.byLegacyMetadata(getBlockState().getValue(BlockLaserEffector26.TYPE));
	}

	public CubeDirections facing() {
		return CubeDirections.list[getBlockState().getValue(BlockLaserEffector26.DIRECTION)];
	}

	/**
	 * V33a {@code LaserEffectTile.isRotateable()} with {@code PARTIAL_ROTATEABILITY} false: targets,
	 * emitters and fixed devices never turn, and a device with a minimum difficulty only turns at or
	 * above it. Omnidirectional devices count as rotateable here, exactly as upstream; the click handler
	 * and the renderer each skip them separately.
	 */
	public static boolean isRotateable(EffectType type, boolean fixed, int rotateableDifficulty) {
		if (rotateableDifficulty > 0 && ChromaOptions.getStructureDifficulty() < rotateableDifficulty)
			return false;
		return !type.isTarget() && type != EffectType.EMITTER && !fixed;
	}

	public boolean isRotateable() {
		return isRotateable(effectType(), fixed, rotateableDifficulty);
	}

	/** V33a {@code onInteract} then {@code rotate}: refused while this room's lasers are in play. */
	public void rotate(boolean reverse) {
		if (level == null || effectType().isOmnidirectional() || !isRotateable()) return;
		if (controllerPos != null && level.getBlockEntity(controllerPos)
				instanceof TileEntityLaserController controller && controller.isFiring()) return;
		CubeDirections next = facing().getRotation(!reverse);
		level.setBlock(worldPosition, getBlockState().setValue(BlockLaserEffector26.DIRECTION,
				next.ordinal()), Block.UPDATE_ALL);
		level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS,
				0.4F, 0.5F);
	}

	/**
	 * The room switch. V33a fires each emitter exactly once per switch-on ({@code LaserEffectTile.fire()});
	 * {@code EmitterTile.keepFiring} is only ever read from a {@code firing} tag that no authored room
	 * carries, so no emitter repeats. {@code firing} records the room state for the rotation lock.
	 */
	public void setFiring(boolean active) {
		if (firing == active) return;
		firing = active;
		if (active) fire();
		setChanged();
	}

	public void resetTarget() {
		triggered = false;
		resetTick = 0;
		setLit(false);
		setChanged();
	}

	private void setLit(boolean lit) {
		if (level != null && getBlockState().getValue(BlockLaserEffector26.LIT) != lit)
			level.setBlock(worldPosition, getBlockState().setValue(BlockLaserEffector26.LIT, lit),
					Block.UPDATE_ALL);
	}

	public void fire() { spawn(new Pulse(facing(), color)); }

	private void spawn(Pulse pulse) {
		if (level != null && !level.isClientSide() && !pulse.color().isBlack())
			level.addFreshEntity(new EntityLaserPulse(level, worldPosition, pulse.direction(),
					pulse.color(), silent, speedFactor));
	}

	@Override public boolean receiveLaserPulse(EntityLaserPulse entity) {
		if (level == null || level.isClientSide()) return true;
		LaserPulseLogic.Interaction interaction = LaserPulseLogic.interact(effectType(), facing(),
				color, entity.pulse());
		if (interaction.targetMatched()) {
			// V33a TargetTile.trigger(true, true, true) runs on every matching hit: the cast sound,
			// the client burst, a refreshed auto-reset and the room's completion check.
			triggered = true;
			setLit(true);
			ChromaSounds.CAST.playSoundAtBlock(level, worldPosition);
			level.blockEvent(worldPosition, getBlockState().getBlock(), TARGET_HIT_EVENT, 0);
			if (controllerPos != null && level.getBlockEntity(controllerPos)
					instanceof TileEntityLaserController controller)
				controller.targetTriggered(worldPosition, true);
			if (autoReset > 0) resetTick = autoReset;
		}
		if (!interaction.prismContribution().isBlack()) {
			pending = pending.add(interaction.prismContribution());
			prismCountdown = prismTimer;
		}
		interaction.emittedPulses().forEach(this::spawn);
		if (interaction.continuingPulse() != null)
			entity.setPulse(interaction.continuingPulse());
		setChanged();
		return interaction.absorbed();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state,
			TileEntityLaserEffector effector) {
		if (effector.prismCountdown > 0 && --effector.prismCountdown == 0
				&& !effector.pending.isBlack()) {
			effector.spawn(new Pulse(effector.facing(), effector.pending));
			effector.pending = BeamColor.BLACK;
			effector.setChanged();
		}
		// V33a TargetTile.reset() is trigger(false, false, false): it darkens the target without
		// notifying the room, so an auto-reset target that was hit stays counted towards completion.
		if (effector.resetTick > 0 && --effector.resetTick == 0)
			effector.resetTarget();
	}

	private static final int TARGET_HIT_EVENT = 1;

	/** V33a TargetTile.doFXClient, delivered as a block event because the hit is resolved on the server. */
	@Override public boolean triggerEvent(int id, int value) {
		if (id == TARGET_HIT_EVENT) {
			if (level != null && level.isClientSide())
				reika.chromaticraft.render.particle.ChromaParticle.spawnLaserTargetHit(level,
						worldPosition, color.renderColor());
			return true;
		}
		return super.triggerEvent(id, value);
	}

	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (controllerPos != null) output.putLong("controller", controllerPos.asLong());
		output.putBoolean("red", color.red()); output.putBoolean("green", color.green());
		output.putBoolean("blue", color.blue()); output.putBoolean("free", rotateable);
		output.putBoolean("fixed", fixed); output.putInt("mindiff", rotateableDifficulty);
		output.putInt("timer", prismTimer); output.putBoolean("fullblock", fullBlock);
		output.putBoolean("silent", silent); output.putDouble("speed", speedFactor);
		output.putBoolean("firing", firing); output.putBoolean("triggered", triggered);
		output.putInt("reset", autoReset); output.putInt("resetTick", resetTick);
		output.putBoolean("pendingRed", pending.red()); output.putBoolean("pendingGreen", pending.green());
		output.putBoolean("pendingBlue", pending.blue()); output.putInt("prismCountdown", prismCountdown);
	}

	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		controllerPos = input.getLong("controller").map(BlockPos::of).orElse(null);
		color = new BeamColor(input.getBooleanOr("red", true), input.getBooleanOr("green", true),
				input.getBooleanOr("blue", true));
		rotateable = input.getBooleanOr("free", false); fixed = input.getBooleanOr("fixed", false);
		rotateableDifficulty = input.getIntOr("mindiff", 0);
		prismTimer = Math.max(1, input.getIntOr("timer", 2));
		fullBlock = input.getBooleanOr("fullblock", false);
		silent = input.getBooleanOr("silent", false);
		speedFactor = input.getDoubleOr("speed", 1);
		firing = input.getBooleanOr("firing", false);
		triggered = input.getBooleanOr("triggered", false);
		autoReset = input.getIntOr("reset", 0); resetTick = input.getIntOr("resetTick", 0);
		pending = new BeamColor(input.getBooleanOr("pendingRed", false),
				input.getBooleanOr("pendingGreen", false), input.getBooleanOr("pendingBlue", false));
		prismCountdown = input.getIntOr("prismCountdown", 0);
	}
}
