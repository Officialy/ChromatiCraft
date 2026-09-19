package reika.chromaticraft.world.dimension;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.world.dimension.SkyRiverGenerator.RiverPoint;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * V33a {@code SkyRiverManager}: catches a player who drifts into a sky river and carries them along it.
 *
 * <h2>How a rider is moved</h2>
 *
 * <p>Each tick the nearest river point within sixteen blocks is found, and if the player is inside the
 * tunnel of one of its two segments they are pulled. The pull is a blend, and the blend is the whole
 * feel of it: sixty percent along the river's own direction and forty percent towards the next node, so
 * a rider is carried forward while being steered back to the centre line rather than flung out of a
 * bend. Speed is a flat seven blocks a tick, which is what makes crossing fifteen thousand blocks
 * reasonable.
 *
 * <p>Upstream's forward-first ordering is kept and matters: the segment towards <em>next</em> is tested
 * before the one from <em>previous</em>, so a player standing where both apply is carried onward rather
 * than backwards.
 *
 * <h2>The tuning gate</h2>
 *
 * <p>Sky rivers are not free transport. {@code TuningThresholds.SKYRIVER} decides what a player gets:
 * untuned, the river throws them out and cuts their flight; partially tuned, it carries them only so
 * far from the origin before ejecting them, with the limit growing as they tune; fully tuned, it carries
 * them the whole way. The partial limit is deliberately jittered from the player's own id and the world
 * time so it is not a hard visible ring.
 *
 * <p>Flight is revoked on ejection because a rider is hundreds of blocks up; upstream does the same, and
 * the fall is the point.
 *
 * <h2>One deliberate deviation from V33a: creative</h2>
 *
 * <p>A creative or spectator player is exempt from the tuning gate and is never stripped of flight.
 * Upstream is not — {@code ejectPlayer} clears {@code capabilities.allowFlying} for anyone, which in
 * 1.7.10 and here alike leaves a creative player unable to fly until their game mode is set again. That
 * is a bug rather than a design, and the gate itself is a progression gate, which creative bypasses
 * everywhere else in the mod. A survival player is gated exactly as upstream gates them.
 */
public final class SkyRiverManager {

	/**
	 * How long the river keeps hold of a rider who has slipped outside it. Two seconds is chosen to
	 * cover a terrain-generation hitch, which is what actually knocks people out of a river.
	 */
	private static final int RECAPTURE_GRACE = 40;

	/** V33a's flat river speed, in blocks per tick. */
	private static final double SPEED = 7;
	/** How far from a player a river point is looked for. */
	private static final double SEARCH_RANGE = 16;
	/** Ticks a player is locked out of a river after being ejected from one. */
	private static final int EJECT_COOLDOWN = 60;

	/**
	 * Chunks generated ahead of a rider, so the server never has to generate them under one.
	 *
	 * <p>A server profile of a river crossing showed the tick thread parked inside
	 * {@code ServerChunkCache.getChunk} -> {@code managedBlock} -> {@code waitForTasks}, reached from the
	 * player's own movement packets — {@code Entity.setPosRaw} and {@code checkFallDamage} both ask for
	 * the chunk the player is entering, and if it does not exist yet the main thread <b>blocks</b> while
	 * it is generated. At seven blocks a tick a rider outruns generation continuously, so the server
	 * stalls, no tick applies any motion, the client drifts out of the tube on its own, and the first
	 * tick that does run drops them.
	 *
	 * <p>A loading ticket placed some way down the path asks for that generation <em>asynchronously and
	 * early</em>, which is the difference between the work happening off-thread before the rider arrives
	 * and on-thread once they have. The ticket expires by itself, so nothing has to be cleaned up when a
	 * ride ends however it ends.
	 */
	/** Long enough to cover the approach, short enough that the trail behind a rider lets go quickly. */
	private static final long PRELOAD_TIMEOUT = 200L;
	/**
	 * A narrow corridor, not broad radius tickets. At V33a speed the rider crosses almost nine chunks
	 * a second; requesting 25 chunks at three distances every tick overwhelmed generation rather than
	 * staying ahead of it. These probes cover the actual line of travel and are issued once per rider
	 * chunk.
	 */
	private static final int PRELOAD_STEP = 32;
	private static final int PRELOAD_DISTANCE = 384;
	/** Never enter terrain unless this much of the immediate route is already a full LevelChunk. */
	private static final int READY_DISTANCE = 64;

	public static final DeferredRegister<net.minecraft.server.level.TicketType> TICKET_TYPES =
			DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
					reika.chromaticraft.ChromatiCraft.MODID);

	public static final DeferredHolder<net.minecraft.server.level.TicketType,
			net.minecraft.server.level.TicketType> SKY_RIVER_TICKET =
			TICKET_TYPES.register("sky_river",
					() -> new net.minecraft.server.level.TicketType(PRELOAD_TIMEOUT,
							net.minecraft.server.level.TicketType.FLAG_LOADING));

	private static final Map<UUID, RiderState> riders = new HashMap<>();
	private static final Random random = new Random();

	/**
	 * Which of the ways a rider can fail to be carried has already been reported. Being ejected for
	 * want of tuning and never being caught at all feel identical from inside the game, so each says so
	 * once.
	 */
	private static final java.util.Set<String> reported = new java.util.HashSet<>();

	private SkyRiverManager() {}

	private static void reportOnce(String cause, String detail) {
		if (reported.add(cause))
			reika.chromaticraft.ChromatiCraft.LOGGER.info("Sky river transport: {} ({})", cause, detail);
	}

	/** Creative and spectator bypass the tuning gate, as they bypass every other gate in the mod. */
	private static boolean bypassesTuning(Player player) {
		return player.getAbilities().instabuild || player.isSpectator();
	}

	public static void clear() {
		riders.clear();
	}

	/** Called once a tick for the Proxima level. */
	public static void tick(ServerLevel level) {
		SkyRiverGenerator rivers = SkyRiverGenerator.getActive();
		if (rivers == null) {
			reportOnce("no rivers", "the server-side generator has not run");
			return;
		}
		reportOnce("ticking", "the Proxima level tick reaches the sky river manager");
		for (Player player : level.players()) {
			if (player.isRemoved())
				continue;
			RiderState state = riders.computeIfAbsent(player.getUUID(), id -> new RiderState());
			state.tick();
			boolean carried = false;
			if (state.canRide()) {
				RiverPoint closest = rivers.getClosestPoint(player, SEARCH_RANGE);
				if (SkyRiverGenerator.isWithinRiver(player, closest))
					carried = move(player, state, closest);
			}
			if (carried) {
				state.graceTicks = RECAPTURE_GRACE;
			}
			else if (state.graceTicks > 0 && state.canRide()) {
				// A rider crosses about a hundred and forty blocks a second, which makes the server
				// generate terrain hard enough to hitch. While it is hitching no tick runs, so no motion
				// is applied, and the client drifts down out of the tube on its own; the next tick that
				// does run then sees a player outside the river and drops them hundreds of blocks up.
				//
				// So leaving the tube does not end the ride immediately. For a short window the river
				// still has hold: gravity stays off, the fall counter stays at zero, and the player is
				// drawn back toward the nearest point instead of released. Only when that window closes
				// without a recapture is the ride really over.
				state.graceTicks--;
				RiverPoint closest = rivers.getClosestPoint(player, SEARCH_RANGE);
				if (closest != null) {
					player.setNoGravity(true);
					player.fallDistance = 0;
					Vec3 back = toVec(closest.position()).subtract(player.position());
					if (back.lengthSqr() > 1.0E-4) {
						player.setDeltaMovement(back.normalize().scale(Math.min(SPEED, back.length())));
						player.hurtMarked = true;
					}
					continue;
				}
				state.graceTicks = 0;
			}
			if (!carried && state.graceTicks <= 0) {
				state.riverTicks = 0;
				// Gravity is restored the moment the river lets go, however that happened.
				if (player.isNoGravity())
					player.setNoGravity(false);
			}
		}
		riders.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
	}

	/** @return whether the player was actually carried this tick */
	private static boolean move(Player player, RiderState state, RiverPoint point) {
		Vec3 position = player.position();
		Vec3 along;
		Vec3 towardsNode;
		// Forward first: a player where both segments apply is carried onward, not back.
		if (distanceToSegment(point.position(), point.next(), position) < SkyRiverGenerator.RIVER_TUNNEL_RADIUS) {
			along = toVec(point.next()).subtract(toVec(point.position()));
			towardsNode = toVec(point.next()).subtract(position);
		}
		else if (distanceToSegment(point.previous(), point.position(), position)
				< SkyRiverGenerator.RIVER_TUNNEL_RADIUS) {
			along = toVec(point.position()).subtract(toVec(point.previous()));
			towardsNode = toVec(point.position()).subtract(position);
		}
		else {
			return false;
		}

		Vec3 move = along.normalize().scale(0.6).add(towardsNode.normalize().scale(0.4));

		float tuning = bypassesTuning(player) ? 1
				: DimensionTuningManager.TuningThresholds.SKYRIVER.getTuningFraction(player);
		if (tuning <= 0) {
			// Untuned: thrown back the way the river came rather than carried along it. Upstream does
			// not simply drop the player -- it overwrites the move vector with the segment direction and
			// applies it at a multiplier of -1, so an untuned player is spat back out of the mouth they
			// drifted into. The port previously just cut their flight and left them, which read as the
			// river doing nothing at all.
			reportOnce("untuned", "SKYRIVER needs " + DimensionTuningManager.TuningThresholds.SKYRIVER
					.minimumEffect + " tuning before it carries anyone; "
					+ DimensionTuningManager.instance.getPlayerTuning(player) + " held");
			eject(player, state);
			Vec3 back = toVec(point.next()).subtract(toVec(point.position())).normalize().scale(-1);
			player.setDeltaMovement(back);
			player.hurtMarked = true;
			return false;
		}
		if (tuning < 1) {
			// Partially tuned: carried only so far out, with the limit jittered off the player's id and
			// the world clock so the boundary is not a visible ring in the sky.
			random.setSeed(player.getUUID().hashCode() ^ player.level().getGameTime());
			random.nextBoolean();
			random.nextBoolean();
			double reach = 150 + 50 * random.nextDouble() + tuning * (200 + random.nextInt(100));
			double dx = player.getX();
			double dz = player.getZ();
			if (dx * dx + dz * dz > reach * reach) {
				eject(player, state);
				return false;
			}
		}

		state.riverTicks++;
		reportOnce("carrying", "a player is being moved along a river");
		preload(player, state, along);
		// Nothing about being carried should ever accumulate a fall: a rider is hundreds of blocks up
		// and any hitch that briefly interrupts the ride would otherwise land as damage.
		player.fallDistance = 0;
		if (!isRouteReady(player, move)) {
			// Do not let either the authoritative entity or its client prediction cross into an unloaded
			// chunk. Keeping the rider caught, weightless and stationary lets queued generation finish
			// without Entity#setPosRaw forcing the server thread to wait for it synchronously.
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			player.setNoGravity(true);
			return true;
		}
		player.setDeltaMovement(move.scale(SPEED));
		player.hurtMarked = true;
		// A rider hangs hundreds of blocks up moving faster than the server's anti-flight check
		// tolerates, and would be kicked for floating. V33a zeroes the private counter directly;
		// 26.2 exempts an entity whose gravity is effectively zero, which is both reachable without
		// touching private state and true of a rider in any case — their motion is overwritten every
		// tick, so gravity has no effect on them while the river has hold.
		player.setNoGravity(true);
		return true;
	}

	/**
	 * Asks for the terrain a rider is about to cross, before they cross it. See {@link #SKY_RIVER_TICKET}
	 * for why this is the fix rather than a nicety.
	 */
	private static void preload(Player player, RiderState state, Vec3 direction) {
		if (!(player.level() instanceof ServerLevel server) || direction.lengthSqr() < 1.0E-6)
			return;
		ChunkPos current = ChunkPos.containing(player.blockPosition());
		long currentKey = current.pack();
		if (state.lastPreloadChunk == currentKey)
			return;
		state.lastPreloadChunk = currentKey;
		Vec3 heading = direction.normalize();
		Vec3 position = player.position();
		long previous = Long.MIN_VALUE;
		for (int blocks = PRELOAD_STEP; blocks <= PRELOAD_DISTANCE; blocks += PRELOAD_STEP) {
			Vec3 ahead = position.add(heading.scale(blocks));
			ChunkPos chunk = new ChunkPos(Mth.floor(ahead.x) >> 4, Mth.floor(ahead.z) >> 4);
			if (chunk.pack() == previous)
				continue;
			previous = chunk.pack();
			// addTicketWithRadius rather than addTicketAndLoadWithRadius: the latter refuses a ticket
			// type that can expire, and returns a future nothing here would wait on anyway. This queues
			// the work and moves on, which is the whole point.
			server.getChunkSource().addTicketWithRadius(SKY_RIVER_TICKET.get(), chunk, 0);
		}
	}

	/** Read-only readiness check: {@code getChunkNow} never generates or waits. */
	private static boolean isRouteReady(Player player, Vec3 direction) {
		if (!(player.level() instanceof ServerLevel server) || direction.lengthSqr() < 1.0E-6)
			return true;
		Vec3 ahead = player.position().add(direction.normalize().scale(READY_DISTANCE));
		return server.getChunkSource().getChunkNow(Mth.floor(ahead.x) >> 4,
				Mth.floor(ahead.z) >> 4) != null;
	}

	private static void eject(Player player, RiderState state) {
		player.setNoGravity(false);
		// A creative or spectator player keeps their flight. Upstream clears it for everyone, and the
		// abilities are only rebuilt when the game mode is next set, so a creative player who brushed a
		// river was left unable to fly for the rest of the session.
		if (!bypassesTuning(player)) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		}
		state.ejectCooldown = EJECT_COOLDOWN;
		state.riverTicks = 0;
		// A deliberate ejection is not a hitch: clear the slack, or the recapture window would simply
		// pull the player straight back into the river they were just refused by.
		state.graceTicks = 0;
	}

	private static double distanceToSegment(reika.dragonapi.instantiable.data.immutable.DecimalPosition from,
			reika.dragonapi.instantiable.data.immutable.DecimalPosition to, Vec3 point) {
		return ReikaVectorHelper.getDistFromPointToLine(from.xCoord, from.yCoord, from.zCoord,
				to.xCoord, to.yCoord, to.zCoord, point.x, point.y, point.z);
	}

	private static Vec3 toVec(reika.dragonapi.instantiable.data.immutable.DecimalPosition position) {
		return new Vec3(position.xCoord, position.yCoord, position.zCoord);
	}

	/** Per-player river state: how long they have been riding, and any lockout after an ejection. */
	private static final class RiderState {

		private int riverTicks;
		private int ejectCooldown;
		/** Ticks of slack left before a rider who has slipped out of the tube is actually dropped. */
		private int graceTicks;
		/** Chunk in which the current forward corridor was last requested. */
		private long lastPreloadChunk = Long.MIN_VALUE;

		private void tick() {
			if (ejectCooldown > 0)
				ejectCooldown--;
		}

		private boolean canRide() {
			return ejectCooldown <= 0;
		}
	}
}
