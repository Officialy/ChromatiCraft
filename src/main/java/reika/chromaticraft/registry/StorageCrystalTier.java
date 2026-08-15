package reika.chromaticraft.registry;

/**
 * The seven V33a {@code ChromaItems.STORAGE} metadata identities, promoted to stable registered
 * items for the metadata-free port. Capacities are per element and retain the source formula
 * {@code 1000 * 8^(metadata - 1)}.
 */
public enum StorageCrystalTier {

	NULA(0, "nula", 125),
	DAYA(1, "daya", 1_000),
	DIVI(2, "divi", 8_000),
	SAMI(3, "sami", 64_000),
	VIER(4, "vier", 512_000),
	LIMA(5, "lima", 4_096_000),
	ARU(6, "aru", 32_768_000);

	public static final StorageCrystalTier[] list = values();

	private final int legacyMetadata;
	private final String registrySuffix;
	private final int capacity;

	StorageCrystalTier(int legacyMetadata, String registrySuffix, int capacity) {
		this.legacyMetadata = legacyMetadata;
		this.registrySuffix = registrySuffix;
		this.capacity = capacity;
	}

	public int legacyMetadata() {
		return legacyMetadata;
	}

	public String registryName() {
		return "storage_crystal_" + registrySuffix;
	}

	public String displayPrefix() {
		return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
	}

	public int capacity() {
		return capacity;
	}

	public StorageCrystalTier previous() {
		return legacyMetadata == 0 ? null : list[legacyMetadata - 1];
	}
}
