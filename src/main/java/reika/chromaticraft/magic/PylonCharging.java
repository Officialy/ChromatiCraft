package reika.chromaticraft.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.interfaces.PoweredItem;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/**
 * V33a {@code ChromaAux.chargePlayerFromPylon}: draining a charging point into the player's
 * elemental buffer.
 *
 * <p>Upstream runs it every tick while the player holds right click with an Elemental Manipulator
 * aimed at a pylon. Charging is therefore a continuous held-use action, and stops when right click
 * is released or the crosshair leaves the crystal.
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
			chargePlayerTools(player, te, e);
		}
		return true;
	}

	/**
	 * V33a {@code ChromaAux.chargePlayerTools}: each charging tick, one random main-inventory slot is tried; a powered
	 * item of the pylon's colour that has been linked to the player's buffer ({@code "bufferlinked"}, set by V33a's
	 * ChargedItemPlayerBufferConnectionRecipe) gains the pylon's held-tool power times its coefficient times its
	 * charge rate.
	 */
	public static void chargePlayerTools(Player player, ChargingPoint te, CrystalElement e) {
		var inv = player.getInventory().getNonEquipmentItems();
		int slot = player.getRandom().nextInt(inv.size());
		ItemStack at = inv.get(slot);
		if (!at.isEmpty()) {
			if (at.getItem() instanceof reika.dragonapi.interfaces.item.ActivatedInventoryItem aii) {
				int size = aii.getInventorySize(at);
				slot = player.getRandom().nextInt(size);
				if (aii.isSlotActive(at, slot)) {
					ItemStack in = aii.getItem(at, slot);
					if (in != null && in.getItem() instanceof PoweredItem) {
						tryChargePoweredItem(in, player, te, e);
					}
				}
			}
			else if (at.getItem() instanceof PoweredItem) {
				tryChargePoweredItem(at, player, te, e);
			}
		}
	}

	private static void tryChargePoweredItem(ItemStack in, Player player, ChargingPoint te, CrystalElement e) {
		net.minecraft.nbt.CompoundTag tag = reika.dragonapi.libraries.registry.ReikaItemHelper.getStackTag(in);
		if (tag == null || !tag.getBooleanOr("bufferlinked", false))
			return;
		PoweredItem pi = (PoweredItem)in.getItem();
		if (pi.getColor(in) == e) {
			float sc = pi.getPlayerChargeCoefficient(player, te, in);
			if (sc > 0) {
				int rate = Math.max(0, (int)(te.getHeldToolChargingPower(player, e, in)*sc*ToolChargingSystem.instance.getChargeRate(in)));
				if (rate > 0)
					ToolChargingSystem.instance.addCharge(in, rate);
			}
		}
	}
}
