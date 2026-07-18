package reika.chromaticraft.magic.network;


public enum CachedPathValidity {

	VALID,
	DORMANT,
	BLOCKED,
	BROKEN;

	public boolean canConduct() {
		return this == VALID;
	}

	public boolean shouldKeep() {
		return this != BROKEN;
	}

}
