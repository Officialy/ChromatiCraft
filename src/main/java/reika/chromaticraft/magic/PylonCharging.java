package reika.chromaticraft.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/**
 * V33a {@code ChromaAux.chargePlayerFromPylon}: draining a charging point into the player's
 * elemental buffer.
 *
 * <p>This is not a right-click. Upstream runs it every tick while the player holds an Elemental
 * Manipulator and looks at a pylon, so charging is a continuous act of standing there rather than a
 * click, and it stops the moment the crosshair leaves the crystal.
 *
 * <p>The drain is deliberately lossy: the player banks {@code chargeSpeed * rateMultiplier} but the
 * source loses that multiplied by {@link PlayerElementBuffer#getChargeInefficiency}, which starts at
 * four and falls to one across progression. Early charging is wasteful on purpose.
 */
public final class PylonCharging {

	private PylonCharging() {}

	/**
	 * @param tick a monotonically increasing counter, used only to phase the beam effect
	 * @return whether anything was actually transferred this tick
	 */
	public static boolean chargePlayerFromPylon(Player player, ChargingPoint te, CrystalElement e,
			int tick, boolean doFX) {
		if (e == null || !te.canConduct() || !te.allowCharging(player, e))
			return false;

		int add = Math.max(1, (int)(PlayerElementBuffer.instance.getChargeSpeed(player)
				* te.getChargeRateMultiplier(player, e)));
		int inefficiency = PlayerElementBuffer.instance.getChargeInefficiency(player);
		int drain = add * inefficiency;
		int energy = te.getEnergy(e);
		if (drain > energy) {
			// Take what is left rather than nothing, and bank only what that pays for.
			drain = energy;
			add = drain / inefficiency;
		}
		if (add <= 0 || !PlayerElementBuffer.instance.canPlayerAccept(player, e, add))
			return false;

		te.onUsedBy(player, e);
		if (PlayerElementBuffer.instance.addToPlayer(player, e, add, true))
			te.drain(e, drain);

		if (player instanceof ServerPlayer) {
			ProgressStage.CHARGE.stepPlayerTo(player);
			if (te instanceof TileEntityCrystalPylon pylon)
				ProgressionManager.instance.setPlayerDiscoveredColor(player, pylon.getColor(), true, true);
			// CHROMA-PORT: V33a also trickles charge into a random held PoweredItem here
			// (ChromaAux.chargePlayerTools). ToolChargingSystem, PoweredItem and
			// ActivatedInventoryItem are all unported, so that branch waits for the tool vertical.
		}
		return true;
	}
}
