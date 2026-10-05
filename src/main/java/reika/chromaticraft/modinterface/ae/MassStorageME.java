package reika.chromaticraft.modinterface.ae;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.parts.storagebus.StorageBusPart;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * V33a {@code TileEntityMassStorage.buildCache}: the items in a crate's ME network that the crate could take. Only
 * loaded with Applied Energistics 2.
 *
 * <p>V33a walked every cell provider and skipped a storage bus pointed at the crate itself (so a crate never pulled
 * its own contents back through the bus). AE2 merges all storage into one inventory, so the crate's own contents
 * are subtracted instead whenever such a bus exists.
 */
public final class MassStorageME {

	private MassStorageME() {

	}

	public static List<ItemStack> findPullable(MESystemReader reader, BlockEntity self, Predicate<ItemStack> valid, Collection<ItemStack> own) {
		List<ItemStack> ret = new ArrayList<>();
		IGrid grid = reader.getGrid();
		if (grid == null)
			return ret;
		KeyCounter contents = new KeyCounter();
		contents.addAll(grid.getStorageService().getCachedInventory());
		if (hasBusToSelf(grid, self)) {
			for (ItemStack is : own) {
				AEItemKey key = AEItemKey.of(is);
				if (key != null)
					contents.remove(key, is.getCount());
			}
		}
		for (Object2LongMap.Entry<AEKey> e : contents) {
			if (e.getKey() instanceof AEItemKey key && e.getLongValue() > 0) {
				ItemStack is = key.toStack((int)Math.min(Integer.MAX_VALUE, e.getLongValue()));
				if (valid.test(is))
					ret.add(is);
			}
		}
		return ret;
	}

	private static boolean hasBusToSelf(IGrid grid, BlockEntity self) {
		for (StorageBusPart bus : grid.getMachines(StorageBusPart.class)) {
			BlockEntity host = bus.getBlockEntity();
			if (host != null && host.getLevel() == self.getLevel() && host.getBlockPos().relative(bus.getSide()).equals(self.getBlockPos()))
				return true;
		}
		return false;
	}

}
