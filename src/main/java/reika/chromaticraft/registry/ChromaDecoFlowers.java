package reika.chromaticraft.registry;

/**
 * V33a {@code BlockDecoFlower.Flowers}, as concrete per-flower identities.
 *
 * <p>The legacy metadata family becomes one registered block each; the ordinal appears nowhere, and
 * every asset is named after its identity. Two members of the family — Glowing Daisy and Lumen Root —
 * already landed as concrete identities with the Luminous Cliffs vertical and are not repeated here.
 *
 * <p>Enderflower and Resonant Clover generate only in the Ender Forest, which is registered alongside
 * them; both stand on dirt or grass exactly as Luma Lotus does.
 *
 * <p>{@code sano_bloom} keeps V33a's own identifier rather than a name derived from its display
 * string, because {@code ether_berries} is already taken by the registered item it drops and a block
 * item would collide with it.
 */
public enum ChromaDecoFlowers {

	/** Snowy grass. Drops Frozen Grains. */
	LUMA_LOTUS("luma_lotus", "Luma Lotus", ChromaCraftingItems.ICY_DUST, Siting.GROUND, 2),
	/** Hangs beneath fully-grown jungle leaves. Drops Ether Berries. */
	SANO_BLOOM("sano_bloom", "Ether Berries", ChromaCraftingItems.ETHER_BERRIES, Siting.JUNGLE_LEAVES, 6),
	/** Swamp reeds, growing upward in short runs. Drops Void Essence. */
	VOID_REEDS("void_reeds", "Void Reeds", ChromaCraftingItems.VOID_DUST, Siting.REED, 4),
	/** Clings to stone and hangs downward. The only biome-tinted flower. Drops Nature Fiber. */
	AURA_IVY("aura_ivy", "Aura Ivy", ChromaCraftingItems.LIVING_ESSENCE, Siting.IVY, 1),
	/** Ender Forest only. Drops Distortion Crystal. */
	ENDERFLOWER("enderflower", "Enderflower", ChromaCraftingItems.TELEPORTATION_DUST, Siting.GROUND, 4),
	/** Ender Forest only. Drops Energetic Essence. */
	RESONANT_CLOVER("resonant_clover", "Resonant Clover", ChromaCraftingItems.ENERGY_POWDER, Siting.GROUND, 2);

	/** Which V33a {@code canPlantAt}/generate branch this flower uses. */
	public enum Siting { GROUND, JUNGLE_LEAVES, REED, IVY }

	public static final ChromaDecoFlowers[] list = values();

	private final String registryName;
	private final String displayName;
	private final ChromaCraftingItems drop;
	private final Siting siting;
	private final int generationChance;

	ChromaDecoFlowers(String registryName, String displayName, ChromaCraftingItems drop, Siting siting,
			int generationChance) {
		this.registryName = registryName;
		this.displayName = displayName;
		this.drop = drop;
		this.siting = siting;
		this.generationChance = generationChance;
	}

	public String registryName() { return registryName; }
	public String displayName() { return displayName; }
	public ChromaCraftingItems drop() { return drop; }
	public Siting siting() { return siting; }

	/** V33a {@code getGenerationChance}: a one-in-N roll per chunk. */
	public int generationChance() { return generationChance; }

	/** V33a {@code isBiomeColored}: Aura Ivy alone takes the biome grass colour. */
	public boolean isBiomeColored() { return this == AURA_IVY; }

	/** V33a runs of this flower placed along one column: reeds grow up, ivy hangs down. */
	public int runLength() {
		return switch (siting) {
			case REED -> 4;
			case IVY -> 12;
			default -> 1;
		};
	}

	public String texture() { return "block/plant/" + registryName; }
}
