package reika.chromaticraft.magic.interfaces;

/**
 * Marks the three V33a receiver families a Wooden/Weak Repeater can feed without overloading:
 * Relay Sources, Ritual Tables, and Personal Chargers. Keeping this as a receiver capability avoids
 * coupling the early-network tile to receiver implementations that have not entered the 26.2 build
 * slice yet; those classes opt in when they land.
 */
public interface WeakRepeaterSafeReceiver extends CrystalReceiver {
}
