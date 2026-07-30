package reika.chromaticraft.magic.interfaces;

import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.dragonapi.interfaces.blockentity.AdjacentUpdateWatcher;


public interface LumenConsumer extends LumenTile, NBTTile, AdjacentUpdateWatcher {

	public int getEfficiencyBoost();

	public boolean allowsEfficiencyBoost();
}
