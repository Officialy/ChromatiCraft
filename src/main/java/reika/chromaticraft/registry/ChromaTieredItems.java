package reika.chromaticraft.registry;

/**
 * All V33a {@code ChromaItems.TIERED} identities, split into stable item registrations.
 * The original metadata is retained for discovery stages and authoritative sprite extraction.
 */
public enum ChromaTieredItems {

	PURITY_DUST(2, "purification_powder", "Purification Powder"),
	FOCUS_DUST(3, "focal_powder", "Focal Powder"),
	ELEMENT_DUST(4, "infused_dust", "Infused Dust"),
	BEACON_DUST(5, "transmissive_dust", "Transmissive Dust"),
	BINDING_CRYSTAL(6, "binding_crystal", "Binding Crystal"),
	RESONANCE_DUST(7, "resonant_dust", "Resonant Dust"),
	CHROMA_DUST(0, "chromic_dust", "Chromic Dust"),
	AURA_DUST(1, "aura_dust", "Aura Dust"),
	ENDER_DUST(8, "enderstone_powder", "Enderstone Powder"),
	WATER_DUST(9, "fluid_essence", "Fluid Essence"),
	FIRAXITE(10, "firaxite", "Firaxite"),
	LUMA_DUST(11, "luma_dust", "Luma Dust"),
	ECHO_CRYSTAL(12, "echo_crystal", "Echo Crystal"),
	FIRE_ESSENCE(16, "fire_essence", "Fire Essence"),
	THERMITIC_CRYSTAL(17, "thermitic_crystal", "Thermitic Crystal"),
	SPACE_DUST(19, "spatial_rifting_powder", "Spatial Rifting Powder"),
	LUMA_BEANS(21, "luma_beans", "Luma Beans"),
	BOOST_ROOT(22, "boost_root", "Enrichment Root"),
	LUMEN_GEM(23, "lumenite", "Lumenite"),
	AVOLITE(24, "avolite", "Avolite"),
	CAVERN_FRAGMENT(13, "cavern_fragment", "Cavern Fragment"),
	BURROW_FRAGMENT(14, "burrow_fragment", "Burrow Fragment"),
	OCEAN_FRAGMENT(15, "ocean_fragment", "Ocean Fragment"),
	RESONANT_CRYSTAL(18, "resonant_crystal", "Resonant Crystal"),
	DESERT_FRAGMENT(20, "desert_fragment", "Desert Fragment"),
	SNOW_FRAGMENT(25, "snow_fragment", "Snow Fragment"),
	// V33a ChromaStacks.bedrockloot / bedrockloot2: bedrock-crack loot and Portal Rift tuning energy.
	PROXIMAL_ESSENCE(26, "proximal_essence", "Proximal Essence"),
	PURE_PROXIMAL_ESSENCE(27, "pure_proximal_essence", "Pure Proximal Essence");

	public static final ChromaTieredItems[] list = values();

	private final int legacyMetadata;
	private final String registryName;
	private final String displayName;

	ChromaTieredItems(int legacyMetadata, String registryName, String displayName) {
		this.legacyMetadata = legacyMetadata;
		this.registryName = registryName;
		this.displayName = displayName;
	}

	public int legacyMetadata() { return legacyMetadata; }
	public String registryName() { return registryName; }
	public String displayName() { return displayName; }
}
