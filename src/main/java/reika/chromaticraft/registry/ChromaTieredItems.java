package reika.chromaticraft.registry;

/**
 * V33a {@code ChromaItems.TIERED} identities currently required by the crystal-group recipe graph.
 * The original metadata is retained explicitly so later tiered-resource slices can join this
 * registry without changing data identities.
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
	FIRE_ESSENCE(16, "fire_essence", "Fire Essence"),
	SPACE_DUST(19, "spatial_rifting_powder", "Spatial Rifting Powder");

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
