package reika.chromaticraft.registry;

/**
 * V33a {@code BlockDimensionDeco.DimDecoTypes}, as concrete per-variant identities.
 *
 * <p>Upstream packs ten decorations into one block's metadata; each is a distinct material with its
 * own hardness rules, drops, collision and effects, so each becomes its own registered block.
 *
 * <h2>The two variants that are not here</h2>
 *
 * <p>{@code AQUA} and {@code GEMSTONE} are deliberately absent, and the reason is upstream's, not the
 * port's. {@code registerBlockIcons} asks every variant for {@code dimgen/<name>/layer_0} through
 * {@code layer_<numIcons-1>} unconditionally, and:
 *
 * <ul>
 * <li>{@code AQUA} has no texture directory anywhere in V33a. It is not dead content — the Moon Pool
 *     generator places it — so upstream draws it as the missing-texture sprite. Its behaviour is
 *     recorded below for whenever art appears: no collision, no silk touch, up to 24 drops, and it
 *     extinguishes any burning entity that walks through it.</li>
 * <li>{@code GEMSTONE} declares three layers and ships only {@code layer_1} and {@code layer_2}. It is
 *     used by the Crystal Mountain terrain pass. Its behaviour: pickaxe-required, silk-touchable,
 *     rainbow-hued by position, full-bright, spawn-proof, and it throws coloured break particles.</li>
 * </ul>
 *
 * <p>V33a carries a {@code logError("...is missing icons!")} fallback for exactly this, so it shipped
 * knowing. Neither is invented here; both wait on real art.
 *
 * <h2>What is deferred rather than missing</h2>
 *
 * <p>Six variants answer upstream's {@code hasBlockRender()} and draw through a custom multi-layer
 * renderer that picks a layer per position at random and runs a second pass — Glow Cave alone is nine
 * layers across two passes. Only {@link #layers} and {@link #hasSecondaryLayers} are recorded here;
 * the compositing itself is a separate dynamic-model effort, and until then each block draws its real
 * {@code layer_0}. Every other behaviour on these blocks is complete.
 */
public enum ProximaDecoTypes {

	/** Walk-through gas that makes a player's good potion effects last twenty minutes. */
	MIASMA("miasma", "Miasma", 1),
	/** Solid, pickaxe-required, and a legal beacon base. */
	FLOATSTONE("floatstone", "Floatstone", 2),
	/** Walk-through water that heals the living and burns the undead. */
	LIFEWATER("lifewater", "Lifewater", 1),
	/** Walk-through lattice. */
	LATTICE("lattice", "Crystal Lattice", 1),
	/** Solid foliage, hue-shifted by position, dropping up to twelve. */
	CRYSTALLEAF("crystalleaf", "Crystal Leaves", 3),
	/** Solid, pickaxe-required, drawn at full brightness. */
	OCEANSTONE("oceanstone", "Ocean Stone", 2),
	/** Solid, pickaxe-required, and the only variant that emits light. */
	CLIFFGLASS("cliffglass", "Cliff Glass", 2),
	/** Solid, drops one to six, and the most heavily layered of the set. */
	GLOWCAVE("glowcave", "Glowing Cave Rock", 9);

	public static final ProximaDecoTypes[] list = values();

	private final String registryName;
	private final String typeName;
	private final int layers;

	ProximaDecoTypes(String registryName, String typeName, int layers) {
		this.registryName = registryName;
		this.typeName = typeName;
		this.layers = layers;
	}

	/** Registry path, e.g. {@code deco_floatstone}. */
	public String registryName() {
		return "deco_" + registryName;
	}

	public String displayName() {
		return typeName;
	}

	/**
	 * The texture folder this variant's layers live in. Lattice is the one that does not follow
	 * upstream's {@code dimgen/<name>/} convention: its art ships as a single shared icon.
	 */
	public String texture(int layer) {
		return this == LATTICE ? "block/icons/lattice" : "block/dimgen/" + registryName + "/layer_" + layer;
	}

	/** V33a {@code numIcons}: how many layers the multi-layer renderer composites. */
	public int layers() {
		return layers;
	}

	/** V33a {@code hasSecondaryIcons}: Glow Cave alone carries a {@code b} layer for its second pass. */
	public boolean hasSecondaryLayers() {
		return this == GLOWCAVE;
	}

	/**
	 * V33a {@code hasBlockRender}: whether the variant is a solid cube. This is not only a rendering
	 * question — {@code getCollisionBoundingBoxFromPool} returns null for the rest, so Miasma,
	 * Lifewater and Lattice are walked through rather than stood on.
	 */
	public boolean isSolid() {
		return this == FLOATSTONE || this == CRYSTALLEAF || this == OCEANSTONE
				|| this == CLIFFGLASS || this == GLOWCAVE;
	}

	/** V33a {@code requiresPickaxe}. */
	public boolean requiresPickaxe() {
		return this == FLOATSTONE || this == OCEANSTONE || this == CLIFFGLASS;
	}

	/** V33a {@code canSilkTouch}. */
	public boolean canSilkTouch() {
		return this != MIASMA && this != LATTICE && this != LIFEWATER;
	}

	/** V33a {@code getMaxDrops}: the ceiling the player's dimension tuning can raise drops to. */
	public int maxDrops() {
		return switch (this) {
			case CRYSTALLEAF -> 12;
			case FLOATSTONE -> 3;
			case GLOWCAVE -> 18;
			case OCEANSTONE -> 2;
			default -> 1;
		};
	}

	/** V33a {@code getLightValue}: Cliff Glass is the only lit variant. */
	public int lightValue() {
		return this == CLIFFGLASS ? 12 : 0;
	}

	/** V33a {@code isBeaconBase}. */
	public boolean isBeaconBase() {
		return this == FLOATSTONE;
	}

	/**
	 * V33a {@code colorMultiplier}: Crystal Leaves take the same position-derived hue Gemstone does,
	 * so a hillside of them runs through the spectrum rather than being uniformly red.
	 */
	public boolean isHueShifted() {
		return this == CRYSTALLEAF;
	}
}
