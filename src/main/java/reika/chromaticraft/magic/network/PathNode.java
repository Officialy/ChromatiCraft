package reika.chromaticraft.magic.network;

import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalRepeater;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

public class PathNode {

	public final WorldLocation location;
	public final Class<? extends CrystalNetworkTile> tileClass;

	public final int stepIndex;
	public final boolean isFinalTarget;

	private CrystalNetworkTile cachedTile;

	PathNode(WorldLocation loc, int i, boolean end) {
		location = loc;
		CrystalNetworkTile te = PylonFinder.getNetTileAt(location, false);
		if (te == null) {
			throw new IllegalStateException("CC Crystal Network: Created a path node from null?!");
		}
		tileClass = te != null ? te.getClass() : null;
		stepIndex = i;
		isFinalTarget = end;
	}

	PathNode(CrystalNetworkTile te) {
		this(new WorldLocation(te.getWorld(), te.getX(), te.getY(), te.getZ()), 0, false);
	}

	void cacheTile() {
		cachedTile = this.getTile(true);
	}

	void flush() {
		cachedTile = null;
	}

	public boolean isSource() {
		return CrystalSource.class.isAssignableFrom(tileClass);
	}

	public boolean isRepeater() {
		return CrystalRepeater.class.isAssignableFrom(tileClass);
	}

	CrystalNetworkTile getTile(boolean exception) {
		if (cachedTile != null)
			return cachedTile;
		if (this.isSource())
			return PylonFinder.getSourceAt(location, exception);
		else if (CrystalTransmitter.class.isAssignableFrom(tileClass))
			return PylonFinder.getTransmitterAt(location, exception);
		else if (CrystalReceiver.class.isAssignableFrom(tileClass))
			return PylonFinder.getReceiverAt(location, exception);
		else
			return PylonFinder.getNetTileAt(location, exception);
	}

	@Override
	public String toString() {
		return "#"+stepIndex+": "+tileClass+" @ "+location;
	}

	@Override
	public int hashCode() {
		return location.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PathNode && location.equals(((PathNode)o).location);
	}

}
