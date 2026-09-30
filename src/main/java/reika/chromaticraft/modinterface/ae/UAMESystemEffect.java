package reika.chromaticraft.modinterface.ae;

import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.cells.StorageCell;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;

/**
 * V33a {@code UABombingEffects.createMESystemEffect}: an Unknown Artefact kept in an ME network's storage cell
 * drains the whole network's stored power every five seconds, unless the cell holding it is a Shielded Cell.
 *
 * <p>1.7.10 reached the cells through a reflected {@code GridStorageCache.activeCellProviders}; AE2 exposes the
 * drives and chests directly as grid machines ({@link IChestOrDrive}) with per-slot cell inventories.
 *
 * <p>Registered from {@code ChromatiCraft}'s constructor when AE2 is loaded (V33a ChromatiCraft.load).
 */
public class UAMESystemEffect extends MESystemReader.ItemInSystemEffect {

	public static void register() {
		MESystemReader.registerMESystemEffect(new UAMESystemEffect());
	}

	public UAMESystemEffect() {
		super(new ItemStack(ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get()));
	}

	@Override
	public int getTickFrequency() {
		return 100;
	}

	@Override
	protected void doEffect(IGrid grid, long amt) {
		IEnergyService ie = grid.getEnergyService();
		if (ie.getStoredPower() <= 0)
			return;
		AEItemKey artefact = AEItemKey.of(ChromaBlocks.UNKNOWN_ARTEFACT_ITEM.get());
		for (IChestOrDrive icp : grid.getMachines(IChestOrDrive.class)) {
			for (int i = 0; i < icp.getCellCount(); i++) {
				if (icp.getCellItem(i) instanceof ItemShieldedCell)
					continue;
				StorageCell cellInv = icp.getOriginalCellInventory(i);
				if (cellInv != null && cellInv.getAvailableStacks().get(artefact) > 0) {
					ie.extractAEPower(ie.getStoredPower(), Actionable.MODULATE, PowerMultiplier.ONE);
					return;
				}
			}
		}
	}

}
