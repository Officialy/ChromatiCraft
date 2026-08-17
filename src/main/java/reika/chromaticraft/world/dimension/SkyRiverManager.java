package reika.chromaticraft.world.dimension;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
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
 */
public final class SkyRiverManager {

	/** V33a's flat river speed, in blocks per tick. */
	private static final double SPEED = 7;
	/** How far from a player a river point is looked for. */
	private static final double SEARCH_RANGE = 16;
	/** Ticks a player is locked out of a river after being ejected from one. */
	private static final int EJECT_COOLDOWN = 60;

	private static final Map<UUID, RiderState> riders = new HashMap<>();
	private static final Random random = new Random();

	private SkyRiverManager() {}

	public static void clear() {
		riders.clear();
	}

	/** Called once a tick for the Proxima level. */
	public static void tick(ServerLevel level) {
		SkyRiverGenerator rivers = SkyRiverGenerator.getActive();
		if (rivers == null)
			return;
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
			if (!carried) {
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

		float tuning = DimensionTuningManager.TuningThresholds.SKYRIVER.getTuningFraction(player);
		if (tuning <= 0) {
			// Untuned: thrown clear rather than carried.
			eject(player, state);
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

	private static void eject(Player player, RiderState state) {
		player.setNoGravity(false);
		player.getAbilities().mayfly = false;
		player.getAbilities().flying = false;
		player.onUpdateAbilities();
		state.ejectCooldown = EJECT_COOLDOWN;
		state.riverTicks = 0;
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

		private void tick() {
			if (ejectCooldown > 0)
				ejectCooldown--;
		}

		private boolean canRide() {
			return ejectCooldown <= 0;
		}
	}
}
