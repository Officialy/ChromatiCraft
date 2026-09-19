package reika.chromaticraft.world.dimension;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * V33a {@code ChromaTeleporter}, expressed as a 26.2 {@link TeleportTransition} factory.
 *
 * <p>Upstream's whole implementation is its {@code placeInPortal} override, and it deliberately does
 * nothing clever: it drops the traveller at {@code y = 1024} and lets them fall. Entering Proxima
 * puts them above the origin; returning to the Overworld puts them above their bed, or above world
 * spawn if they have none. {@code placeInExistingPortal} returns true without moving anything and
 * {@code makePortal} returns false, so no portal is ever built at the far end.
 *
 * <p>The 1024-block drop is the mechanism, not an accident: it clears whatever terrain generated
 * beneath, and Proxima's own fall protection catches the landing. V33a's
 * {@code DimensionJoinHandler} carve-out is a separate login/respawn safeguard for a persistent
 * player who loads embedded in terrain; {@link ProximaPlayerSafety} carries that behavior in 26.2.
 */
public final class ChromaTeleporter {

	/** V33a: the fixed altitude every arrival starts from. */
	public static final double ARRIVAL_Y = 1024;

	private ChromaTeleporter() {}

	/**
	 * @param post runs after the entity exists in the destination level, which is where V33a did its
	 *             progression grant, arrival cues and post-join inventory sweep
	 */
	public static TeleportTransition arrivalTransition(ServerLevel target, Entity entity,
			TeleportTransition.PostTeleportTransition post) {
		Vec3 position = arrivalPosition(target, entity);
		return new TeleportTransition(target, position, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
				Set.of(), TeleportTransition.PLACE_PORTAL_TICKET.then(post));
	}

	/**
	 * V33a placeInPortal: {@code setLocationAndAngles(0, 1024, 0, 0, 0)} for any destination, then —
	 * only when the destination is the Overworld — the same altitude above the player's bed, falling
	 * back to world spawn when they have none.
	 */
	public static Vec3 arrivalPosition(ServerLevel target, Entity entity) {
		double x = 0;
		double z = 0;
		if (target.dimension() == Level.OVERWORLD) {
			BlockPos anchor = target.getRespawnData().globalPos().pos();
			if (entity instanceof ServerPlayer player) {
				ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
				if (respawn != null && respawn.respawnData().globalPos().dimension() == Level.OVERWORLD)
					anchor = respawn.respawnData().globalPos().pos();
			}
			x = anchor.getX() + 0.5;
			z = anchor.getZ() + 0.5;
		}
		return new Vec3(x, ARRIVAL_Y, z);
	}

	/** Present for symmetry with V33a's unused Relative set; arrivals are absolute. */
	public static Set<Relative> relatives() {
		return Set.of();
	}
}
