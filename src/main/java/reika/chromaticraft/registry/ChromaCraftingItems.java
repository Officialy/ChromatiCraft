package reika.chromaticraft.registry;

/**
 * The 35 V33a {@code ChromaItems.CRAFTING} metadata variants, now registered as independent 26.2
 * items so identity is stable without legacy metadata. Declaration order preserves the original
 * metadata ordinal used by recipes and {@code ChromaStacks}.
 */
public enum ChromaCraftingItems {

	VOID_CORE("void_core", "Void Core"),
	CRYSTAL_LENS("crystal_lens", "Crystal Lens"),
	CRYSTAL_FOCUS("crystal_focus", "Crystal Focus"),
	CRYSTAL_MIRROR("crystal_mirror", "Crystal Mirror"),
	RAW_CRYSTAL("raw_crystal", "Raw Crystal"),
	ENERGY_CORE("energy_core", "Energy Core"),
	CRYSTAL_POWDER("crystal_powder", "Crystal Dust"),
	TRANSFORMATION_CORE("transformation_core", "Transformation Core"),
	ELEMENT_UNIT("element_unit", "Elemental Core"),
	IRIDESCENT_CRYSTAL("iridescent_crystal", "Iridescent Crystal Shard"),
	IRIDESCENT_CHUNK("iridescent_chunk", "Iridescent Crystal Chunk"),
	CHROMA_INGOT("chroma_ingot", "Chroma Alloy Ingot"),
	// V33a craftingNames has real chassis0..3 keys, but en_US.lang carries no translation for them
	// (only the unrelated chromacraft.chassis="Crystal Chassis"). No ground-truth text exists for
	// these four tiers; see PORTING notes / issue report rather than inventing one.
	BASIC_CHASSIS("basic_chassis", "Basic Chassis"),
	INTERMEDIATE_CHASSIS("intermediate_chassis", "Intermediate Chassis"),
	ADVANCED_CHASSIS("advanced_chassis", "Advanced Chassis"),
	ULTIMATE_CHASSIS("ultimate_chassis", "Ultimate Chassis"),
	FIERY_INGOT("fiery_ingot", "Firaxite Alloy Ingot"),
	ENDER_INGOT("ender_ingot", "Resonating Ingot"),
	WATER_INGOT("water_ingot", "Fluidic Essence Ingot"),
	CONDUCTIVE_INGOT("conductive_ingot", "Radiative Alloy Ingot"),
	AURA_INGOT("aura_ingot", "Aura Conducting Ingot"),
	COMPLEX_INGOT("complex_ingot", "Chromastone"),
	SPACE_INGOT("space_ingot", "Spatially Warping Ingot"),
	HIGH_VOID_CORE("high_void_core", "Energized Void Core"),
	HIGH_TRANSFORMATION_CORE("high_transformation_core", "Energized Transformation Core"),
	HIGH_ENERGY_CORE("high_energy_core", "Energized Energy Core"),
	TELEPORTATION_DUST("teleportation_dust", "Distortion Crystal"),
	ICY_DUST("icy_dust", "Frozen Grains"),
	ENERGY_POWDER("energy_powder", "Energetic Essence"),
	ETHER_BERRIES("ether_berries", "Ether Berries"),
	VOID_DUST("void_dust", "Void Essence"),
	LIVING_ESSENCE("living_essence", "Nature Fiber"),
	LUMEN_CORE("lumen_core", "Lumen Core"),
	GLOW_CHUNK("glow_chunk", "Radiant Gem"),
	EXPERIENCE_GEM("experience_gem", "Experience Gem");

	public static final ChromaCraftingItems[] list = values();

	private final String registryName;
	private final String displayName;

	ChromaCraftingItems(String registryName, String displayName) {
		this.registryName = registryName;
		this.displayName = displayName;
	}

	public String registryName() {
		return registryName;
	}

	public String displayName() {
		return displayName;
	}
}
