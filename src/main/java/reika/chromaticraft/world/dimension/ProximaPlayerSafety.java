package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;

/**
	 * The survival and arrival rules V33a applies to players in Proxima.
 *
 * <p>Arrivals begin at Y=1024 on purpose, so ordinary vanilla fall damage would make the intended
 * portal trip lethal. For the first minute in the dimension V33a cancels fall damage completely;
 * afterward it compresses long falls to a maximum effective distance of eight blocks. Death itself
 * is also refused: the player is left at one health, sent back to the Overworld through the same
 * teleporter, and loses a random forty-to-ninety percent of every colour in their element buffer.
 *
 * <p>These hooks used to live in the enormous pristine {@code ChromaticEventManager}. Keeping the
 * narrow 26.2 listener here restores the complete Proxima contract without activating unrelated
 * legacy event code.
 */
public final class ProximaPlayerSafety {

	/** V33a's grace period: 1200 entity ticks, or one minute. */
	public static final int ARRIVAL_FALL_GRACE_TICKS = 1200;
	/** V33a waits until this depth before returning a void-falling player to the Overworld. */
	public static final double VOID_EXIT_Y = -1024;

	private ProximaPlayerSafety() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true,
				ProximaPlayerSafety::onLivingFall);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true,
				ProximaPlayerSafety::onLivingDeath);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true,
				ProximaPlayerSafety::onIncomingDamage);
		NeoForge.EVENT_BUS.addListener(ProximaPlayerSafety::onPlayerLogin);
		NeoForge.EVENT_BUS.addListener(ProximaPlayerSafety::onPlayerRespawn);
	}

	private static void onLivingFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().dimension() != ChromaDimensions.PROXIMA)
			return;
		double protectedDistance = protectedFallDistance(player.tickCount, event.getDistance());
		if (protectedDistance == 0)
			event.setCanceled(true);
		else
			event.setDistance(protectedDistance);
	}

	/**
	 * Pure form of V33a's fall calculation, exposed so the boundary and cap can be regression-tested
	 * without manufacturing a custom-dimension player in the GameTest server.
	 */
	public static double protectedFallDistance(int ticksExisted, double distance) {
		if (ticksExisted < ARRIVAL_FALL_GRACE_TICKS)
			return 0;
		return Math.min(8, Math.min(distance, Math.max(1, 0.5 * Math.sqrt(distance))));
	}

	/**
	 * V33a cancels all out-of-world damage in Proxima, but does not actually return the player home
	 * until they have fallen below Y=-1024. Cancelling the earlier vanilla void ticks is essential:
	 * 26.2's default minimum build height would otherwise kill the player long before that threshold.
	 */
	private static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().dimension() != ChromaDimensions.PROXIMA
				|| !event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))
			return;
		event.setCanceled(true);
		if (shouldExitVoid(player.getY()))
			returnToOverworld(player);
	}

	public static boolean shouldExitVoid(double y) {
		return y < VOID_EXIT_Y;
	}

	private static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().dimension() != ChromaDimensions.PROXIMA)
			return;
		event.setCanceled(true);
		player.setHealth(1);
		returnToOverworld(player);

		// Upstream rewrites a copy of the held buffer to 40-90% of each value, then subtracts that
		// copy. In other words, the death penalty removes 40-90% and leaves 10-60%; preserving that
		// order avoids the tempting but incorrect interpretation that 40-90% should remain.
		ElementTagCompound held = PlayerElementBuffer.instance.getPlayerBuffer(player);
		ElementTagCompound removed = new ElementTagCompound();
		for (CrystalElement element : held.elementSet()) {
			float fraction = 0.4F + player.getRandom().nextFloat() * 0.5F;
			removed.setTag(element, Math.max(1, (int)(held.getValue(element) * fraction)));
		}
		PlayerElementBuffer.instance.removeFromPlayer(player, removed);
	}

	private static void returnToOverworld(ServerPlayer player) {
		ServerLevel overworld = player.level().getServer().overworld();
		player.teleport(ChromaTeleporter.arrivalTransition(overworld, player,
				net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
	}

	private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearAreaForPlayer(player);
	}

	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearAreaForPlayer(player);
	}

	/**
	 * V33a {@code DimensionJoinHandler.clearAreaForPlayer}, used on login and respawn to keep a
	 * persistent Proxima player from loading embedded in regenerated terrain. The inner ellipsoid is
	 * air and its one-block shell is ordinary (not reinforced) Cloak Shielding. Fluids, vegetation,
	 * unbreakable blocks and block entities are left alone exactly as upstream leaves them.
	 */
	private static void clearAreaForPlayer(ServerPlayer player) {
		if (player.level().dimension() != ChromaDimensions.PROXIMA)
			return;
		ServerLevel level = player.level();
		double horizontalRadius = 5;
		double verticalRadius = 3.5;
		int x = (int)Math.floor(player.getX());
		int y = (int)Math.floor(player.getY());
		int z = (int)Math.floor(player.getZ());
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState();
		BoundingBox clearing = new BoundingBox(x - 5, y - 4, z - 5, x + 5, y + 4, z + 5);
		// The login capsule is an emergency terrain escape, never a structure-carving tool. Monument
		// and puzzle pieces intentionally contain vanilla mineral blocks and open air, so filtering
		// individual block types cannot protect them; refuse the entire operation when its volume
		// intersects a ChromatiCraft structure piece or one of the controller-backed legacy layouts.
		if (overlapsProtectedStructure(level, clearing))
			return;

		for (double i = -horizontalRadius; i <= horizontalRadius; i++) {
			for (double j = -verticalRadius; j <= verticalRadius; j++) {
				for (double k = -horizontalRadius; k <= horizontalRadius; k++) {
					if (!insideEllipse(i, j, k, horizontalRadius, verticalRadius, horizontalRadius))
						continue;
					BlockPos pos = new BlockPos((int)Math.floor(x + i), (int)Math.floor(y + j),
							(int)Math.floor(z + k));
					if (!canClear(level, pos))
						continue;
					boolean inner = insideEllipse(i, j, k, horizontalRadius - 1,
							verticalRadius - 1, horizontalRadius - 1);
					level.setBlock(pos, inner ? Blocks.AIR.defaultBlockState() : cloak, 3);
				}
			}
		}
	}

	/** Shared by the login guard and its focused regression test. */
	public static boolean overlapsProtectedStructure(ServerLevel level, BoundingBox clearing) {
		var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		java.util.HashSet<net.minecraft.world.level.levelgen.structure.StructureStart> checked =
				new java.util.HashSet<>();
		for (net.minecraft.world.level.ChunkPos chunk : clearing.intersectingChunks().toList()) {
			for (var start : level.structureManager().startsForStructure(chunk, structure -> {
				Identifier key = structures.getKey(structure);
				return key != null && ChromatiCraft.MODID.equals(key.getNamespace());
			})) {
				if (checked.add(start) && start.getPieces().stream()
						.anyMatch(piece -> piece.getBoundingBox().intersects(clearing)))
					return true;
			}
		}
		// Some currently ported puzzle layouts still originate from NBT features rather than a
		// StructureStart. Their controller is the authoritative protection marker until they move to
		// structure pieces; this also makes the rule safe for mixed-version worlds.
		for (BlockPos pos : BlockPos.betweenClosed(
				new BlockPos(clearing.minX(), clearing.minY(), clearing.minZ()),
				new BlockPos(clearing.maxX(), clearing.maxY(), clearing.maxZ()))) {
			if (level.getBlockEntity(pos)
					instanceof reika.chromaticraft.tileentity.TileEntityStructureController controller
					&& (controller.isMonument() || controller.getStructureType() != null))
				return true;
		}
		return false;
	}

	private static boolean insideEllipse(double x, double y, double z, double rx, double ry,
			double rz) {
		return x * x / (rx * rx) + y * y / (ry * ry) + z * z / (rz * rz) <= 1;
	}

	private static boolean canClear(ServerLevel level, BlockPos pos) {
		if (!level.isInWorldBounds(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.LOGS) || state.canBeReplaced() || level.getBlockEntity(pos) != null
				|| state.getDestroySpeed(level, pos) < 0)
			return false;
		return !(state.getBlock() instanceof BlockStructureShield shield)
				|| !shield.isUnbreakable(state);
	}
}
