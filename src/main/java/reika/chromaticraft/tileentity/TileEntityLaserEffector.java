package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
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

	public void rotate(boolean reverse) {
		if (level == null || fixed || effectType().isOmnidirectional()
				|| effectType().isTarget() || effectType() == EffectType.EMITTER
				|| rotateableDifficulty > ChromaOptions.getStructureDifficulty()) return;
		if (controllerPos != null && level.getBlockEntity(controllerPos)
				instanceof TileEntityLaserController controller && controller.isFiring()) return;
		CubeDirections next = facing().getRotation(!reverse);
		level.setBlock(worldPosition, getBlockState().setValue(BlockLaserEffector26.DIRECTION,
				next.ordinal()), Block.UPDATE_ALL);
		ChromaSounds.USE.playSoundAtBlock(level, worldPosition, 0.4F, 0.5F);
	}

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
		if (interaction.targetMatched() && !triggered) {
			triggered = true;
			setLit(true);
			ChromaSounds.CAST.playSoundAtBlock(level, worldPosition);
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
		if (effector.effectType() == EffectType.EMITTER && effector.firing
				&& level.getRandom().nextInt(4) == 0) effector.fire();
		if (effector.prismCountdown > 0 && --effector.prismCountdown == 0
				&& !effector.pending.isBlack()) {
			effector.spawn(new Pulse(effector.facing(), effector.pending));
			effector.pending = BeamColor.BLACK;
			effector.setChanged();
		}
		if (effector.resetTick > 0 && --effector.resetTick == 0) {
			effector.resetTarget();
			if (effector.controllerPos != null && level.getBlockEntity(effector.controllerPos)
					instanceof TileEntityLaserController controller)
				controller.targetTriggered(pos, false);
		}
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
