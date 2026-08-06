package reika.chromaticraft.registry;

/**
 * V33a {@code BlockStructureShield.BlockType}, as concrete per-material identities.
 *
 * <p>Upstream packs two orthogonal things into one metadata byte: the low three bits pick the
 * material, and bit 3 marks the block as structure-grade. Only the material is a variant family, so
 * each material becomes its own registered block; the structure-grade flag is genuine runtime state
 * on a single object and stays a boolean blockstate. That split is what makes V33a's
 * {@code damageDropped = meta % 8} fall out naturally — breaking a reinforced block yields the plain
 * form of the same material.
 *
 * <p>Only {@code CRACK} and {@code CRACKS} are mineable when reinforced; everything else is
 * effectively bedrock inside a structure, which is the whole point of the material.
 */
public enum ChromaShieldTypes {

	CLOAK("cloak", "Cloak"),
	STONE("stone", "Stone"),
	COBBLE("cobble", "Cobble"),
	CRACK("crack", "Crack"),
	MOSS("moss", "Moss"),
	GLASS("glass", "Glass"),
	LIGHT("light", "Light"),
	CRACKS("cracks", "Cracks");

	public static final ChromaShieldTypes[] list = values();

	private final String registryName;
	private final String typeName;

	ChromaShieldTypes(String registryName, String typeName) {
		this.registryName = registryName;
		this.typeName = typeName;
	}

	/** Registry path, e.g. {@code shielding_stone}. */
	public String registryName() {
		return "shielding_" + registryName;
	}

	/** V33a getMultiValuedName: the block's basic name followed by the material. */
	public String displayName() {
		return "Shielding " + typeName;
	}

	public String texture() {
		return "block/shield/" + registryName;
	}

	/** V33a BlockType.isMineable: only the cracked variants can be broken when reinforced. */
	public boolean isMineable() {
		return this == CRACK || this == CRACKS;
	}

	/** V33a BlockType.getLightValue. */
	public int lightValue() {
		return this == LIGHT ? 15 : 0;
	}

	/** V33a BlockType.isTransparent: these do not occlude their neighbours. */
	public boolean isTransparent() {
		return this == CRACK || this == GLASS || this == CRACKS;
	}

	/** V33a BlockType.isTransparentToLight. */
	public boolean isTransparentToLight() {
		return this == GLASS;
	}
}
