package reika.chromaticraft.magic.interfaces;

/**
 * Implemented by non-network block entities which expose a temporary crystal receiver to the
 * pathfinder (the crystal-music tile is the original V33a implementation).
 */
public interface TemporaryCrystalReceiverProvider {
	CrystalReceiver createTemporaryReceiver();
}
