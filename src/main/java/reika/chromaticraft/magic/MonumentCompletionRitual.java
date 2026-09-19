package reika.chromaticraft.magic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.MonumentRitualScore.TimedEvent;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.world.dimension.structure.MonumentMineralBlocks;
import reika.chromaticraft.world.dimension.structure.MonumentPiece;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * V33a {@code MonumentCompletionRitual}: the ceremony that turns Proxima's monument into an Aura Locus.
 *
 * <p>This is the server-side timeline only. Upstream is one class holding both the timeline and its
 * effects — sounds, particles, the vortex, camera manipulation, shader data — which in 26.2 would put
 * client-only descriptors on a class the dedicated server loads and verifies during mod construction.
 * That is the exact failure that made {@code ClientPayloadHandlers} necessary, so the effects live in a
 * client-only counterpart and the two are joined by packets.
 *
 * <h2>Why the clock is wall-clock</h2>
 *
 * <p>{@code System.currentTimeMillis}, deliberately, and not world time: every timing in
 * {@link MonumentRitualScore} is synchronised to six recorded audio tracks, which play at real speed
 * regardless of tick rate. What the tick does correct for is stalls — any step longer than a tick's
 * fifty milliseconds has its excess added to a running pause total and subtracted back out, so a
 * server hitch delays the ceremony instead of desynchronising it from its own music.
 *
 * <h2>The checks are the interesting part</h2>
 *
 * <p>{@link #doChecks} is what makes this a ritual rather than a button. All sixteen cores must be
 * present, each the colour its position calls for, and every core that records a placer must record the
 * <em>same</em> one — so a monument cannot be finished by a group each contributing a core, and a core
 * placed by a fake player disqualifies nothing but grants nothing either. Failing any of it ends the
 * ritual rather than pausing it.
 */
public class MonumentCompletionRitual {

	private final Level world;
	private final BlockPos pos;
	private final Player player;
	private final boolean inProxima;

	private final List<TimedEvent> events;

	private long startTime;
	private long lastTickTime;
	private long pauseTotal;
	private long runTime = -1;
	private long completionTime = -1;
	private Vec3 playerAnchor;

	private boolean running;
	private boolean complete;

	private static boolean runningRituals;

	public MonumentCompletionRitual(Level world, BlockPos pos, Player ep) {
		this.world = world;
		this.pos = pos;
		this.player = ep;
		this.inProxima = world.dimension() == ChromaDimensions.PROXIMA;
		this.events = new ArrayList<>(MonumentRitualScore.buildSchedule(inProxima));
	}

	/** Whether any ritual is running, which the dimension's audio handler mutes itself for. */
	public static boolean areRitualsRunning() {
		return runningRituals;
	}

	public static void clearRituals() {
		runningRituals = false;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isComplete() {
		return complete;
	}

	public List<TimedEvent> getSchedule() {
		return events;
	}

	public void start() {
		this.disableCores();
		playerAnchor = player.position();
		running = true;
		runningRituals = true;
		completionTime = -1;
		startTime = System.currentTimeMillis();
		lastTickTime = startTime;
		pauseTotal = 0;
		runTime = 0;
	}

	/** V33a disableCores: the cores are un-primed the moment the ceremony takes over from them. */
	private void disableCores() {
		for (CrystalElement e : CrystalElement.elements) {
			TileEntityDimensionCore core = this.getCore(e);
			if (core != null)
				core.prime(false);
		}
	}

	private TileEntityDimensionCore getCore(CrystalElement e) {
		Vec3i offset = TileEntityDimensionCore.getLocation(e);
		return world.getBlockEntity(pos.offset(offset)) instanceof TileEntityDimensionCore core
				? core : null;
	}

	/**
	 * V33a's tick. The pause correction is the load-bearing part: a step longer than one tick has its
	 * excess banked, so the elapsed time this reports stays true to the audio rather than to the clock.
	 */
	public void tick() {
		if (!running)
			return;
		long time = System.currentTimeMillis();
		long step = time - lastTickTime;
		if (step > 50)
			pauseTotal += step - 50;
		runTime = time - (startTime + pauseTotal);
		lastTickTime = time;
		this.holdPlayerAtActivationPoint();

		if (this.isReadyToComplete())
			this.completeRitual();
	}

	public long getRunTime() {
		return runTime;
	}

	/**
	 * A ceremony is deliberately not serialised. If it is interrupted (logout, server stop, failed
	 * check, or an explicit reset), the ring must therefore return to its pre-ritual primed state.
	 * Otherwise the persisted {@code prime=false} written by {@link #disableCores()} silences the
	 * dimension-core ensemble until one core happens to receive a neighbour update or is replaced.
	 */
	public void enableCores() {
		for (CrystalElement e : CrystalElement.elements) {
			TileEntityDimensionCore core = this.getCore(e);
			if (core != null)
				core.prime(true);
		}
	}

	/**
	 * V33a rewrote the client view entity to the scripted camera every tick, which prevented ordinary
	 * movement from carrying the activating player away. The modern camera is deliberately detached
	 * from the real player, so preserve that effective behavior authoritatively on the server. This
	 * also prevents queued movement packets or pre-existing flight momentum from escaping the ritual.
	 */
	private void holdPlayerAtActivationPoint() {
		if (playerAnchor == null)
			return;
		player.setDeltaMovement(Vec3.ZERO);
		player.setSprinting(false);
		player.fallDistance = 0;
		if (player.position().distanceToSqr(playerAnchor) > 1.0E-6)
			player.snapTo(playerAnchor.x, playerAnchor.y, playerAnchor.z);
	}

	private boolean isReadyToComplete() {
		return runTime >= MonumentRitualScore.completionTime(inProxima);
	}

	/**
	 * V33a completeRitual, in two stages. For the first three seconds the player is turned to face the
	 * monument and held there; after that the monument becomes an Aura Locus, the player is set down in
	 * front of it, and the progression step is granted.
	 */
	private void completeRitual() {
		if (completionTime < 0) {
			completionTime = runTime;
			// V33a MONUMENTCOMPLETE is separate from RESETMONUMENT: it begins the final
			// three-second shot and fires the completion seeds/core beams exactly once.
			if (world instanceof ServerLevel server)
				ChromaNetwork.sendMonumentRitualCompletion(server, pos);
		}
		if (runTime - completionTime < MonumentRitualScore.COMPLETION_EXTRA) {
			complete = true;
			this.facePlayerAtMonument();
			return;
		}
		world.setBlock(pos, ChromaBlocks.AURA_POINT.get().defaultBlockState(), 3);
		if (world.getBlockEntity(pos) instanceof TileEntityAuraPoint point)
			point.setPlacer(player);
		// V33a sets the player down four blocks below and four and a half out, looking slightly up at
		// what they have just made.
		player.teleportTo(pos.getX() + 0.5, pos.getY() - 4, pos.getZ() - 4.5);
		player.setYRot(0);
		player.setXRot(-25);
		player.setDeltaMovement(0, 0, 0);
		if (player instanceof ServerPlayer sp)
			ProgressStage.CTM.stepPlayerTo(sp);
		running = false;
		runningRituals = false;
		complete = true;
	}

	/** V33a: the camera is turned onto the monument for the completion, by setting the player's angles. */
	private void facePlayerAtMonument() {
		double dx = pos.getX() + 0.5 - player.getX();
		double dy = pos.getY() + 0.5 - player.getY();
		double dz = pos.getZ() + 0.5 - player.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		player.setYRot((float)(Math.toDegrees(Math.atan2(-dx, dz))));
		player.setXRot((float)(-Math.toDegrees(Math.atan2(dy, horizontal))));
		player.setYHeadRot(player.getYRot());
	}

	/**
	 * V33a doChecks: every core present, correctly coloured, and all placed by one real player.
	 *
	 * @return whether the ritual may start; a false answer has already ended it
	 */
	public boolean doChecks() {
		Player owner = null;
		for (CrystalElement e : CrystalElement.elements) {
			TileEntityDimensionCore core = this.getCore(e);
			if (core == null || core.getColor() != e) {
				BlockPos at = pos.offset(TileEntityDimensionCore.getLocation(e));
				ChromatiCraft.LOGGER.warn("Monument at {} cannot start: expected {} dimension core at {}, found {}",
						pos, e, at, core == null ? world.getBlockState(at) : core.getColor());
				this.endRitual();
				return false;
			}
			Player placer = core.getPlacer();
			if (placer == null || ReikaPlayerAPI.isFake(placer))
				continue;
			if (owner == null)
				owner = placer;
			else if (owner != placer) {
				// Two different players contributed cores; upstream refuses rather than picking one.
				ChromatiCraft.LOGGER.warn("Monument at {} cannot start: dimension cores belong to both {} and {}",
						pos, owner.getGameProfile().name(), placer.getGameProfile().name());
				this.endRitual();
				return false;
			}
		}
		if (owner == null) {
			ChromatiCraft.LOGGER.warn("Monument at {} cannot start: none of its dimension cores has a real player owner", pos);
			this.endRitual();
			return false;
		}
		if (!this.doMineralChecks()) {
			this.endRitual();
			return false;
		}
		return true;
	}

	/**
	 * V33a doMineralChecks: every cell of the monument's mineral inlay must be present and of the right
	 * material.
	 *
	 * <p>This is the ritual's second gate and it bites. Generation lays each cell only on a per-material
	 * chance — glowstone at thirty-five percent, redstone at forty — while registering every one of
	 * them as expected, and the centre chroma is registered without ever being laid. The active gold
	 * ring is a separate always-generated structural ring. So a freshly
	 * generated monument always fails this, and finishing the inlay by hand is the work the ritual is
	 * named for. See {@link MonumentMineralBlocks}.
	 *
	 * <p>Client-side it answers true unconditionally, as upstream does: the check is the server's to
	 * make, and a client that disagreed would only desynchronise the ceremony.
	 */
	private boolean doMineralChecks() {
		if (world.isClientSide())
			return true;
		// The inlay's coordinates are relative to the monument template's origin, which sits a
		// controller-offset away from this ritual's position.
		BlockPos origin = pos.subtract(MonumentPiece.CONTROLLER_OFFSET);
		for (MonumentMineralBlocks.Cell cell : MonumentMineralBlocks.expected()) {
			BlockPos at = origin.offset(cell.offset());
			boolean wrongBlock = !world.getBlockState(at).is(cell.mineral().block());
			boolean flowingChroma = cell.mineral() == MonumentMineralBlocks.Mineral.CHROMA
					&& !world.getFluidState(at).isSource();
			if (wrongBlock || flowingChroma) {
				ChromatiCraft.LOGGER.warn("Monument at {} cannot start: expected {} at inlay position {}, found {}",
						pos, cell.mineral(), at, world.getBlockState(at));
				return false;
			}
		}
		return true;
	}

	public void endRitual() {
		this.enableCores();
		running = false;
		runTime = -1;
		runningRituals = false;
		complete = false;
		ChromaSounds.ERROR.playSoundAtBlock(world, pos, 1, 0.75F);
	}
}
