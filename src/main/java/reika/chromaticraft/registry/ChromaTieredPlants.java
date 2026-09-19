package reika.chromaticraft.registry;

import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a {@code BlockTieredPlant.TieredPlants}, as concrete per-plant identities.
 *
 * <p>Each plant is its own registered block with a stable registry name. The legacy metadata ordinal
 * is deliberately absent from Java state and blockstate data. Five sprites were renamed onto their
 * identities; the two retained {@code tierplant_5/6} filenames are only immutable source-asset
 * locators selected by the concrete enum entries, never variant state or a saved colour/type value.
 *
 * <p>All seven V33a plants are concrete registrations. The two tree-bound plants retain the shared
 * source lattice/tree search while keeping their distinct trunk-height and root-base siting rules.
 */
public enum ChromaTieredPlants {

	/** Aura Bloom — surface flower on dirt or grass. */
	AURA_BLOOM("aura_bloom", "Aura Bloom", ProgressStage.CRYSTALS, 0x33aaff,
			ChromaTieredItems.AURA_DUST, Siting.SURFACE, 5, 2),
	/** Rock Flower — underground, hanging beneath stone or ore. */
	ROCK_FLOWER("rock_flower", "Rock Flower", ProgressStage.RUNEUSE, 0xffffff,
			ChromaTieredItems.PURITY_DUST, Siting.CAVE, 2, 2),
	/** Essence Lily — floats on still, sky-lit water. */
	ESSENCE_LILY("essence_lily", "Essence Lily", ProgressStage.CHARGE, 0xff00ff,
			ChromaTieredItems.ELEMENT_DUST, Siting.WATER, 5, 1),
	/** Element Bulbs — hangs from the underside of leaves. */
	ELEMENT_BULBS("element_bulbs", "Element Bulbs", ProgressStage.MULTIBLOCK, 0x00ffff,
			ChromaTieredItems.RESONANCE_DUST, Siting.LEAVES, 5, 2),
	/** Radiance Bush — surface bush on sand. */
	RADIANCE_BUSH("radiance_bush", "Radiance Bush", ProgressStage.PYLON, 0xffcc33,
			ChromaTieredItems.BEACON_DUST, Siting.SAND, 5, 2),
	/** Vibrant Pod — attaches to a random height on a nearby tree trunk. */
	VIBRANT_POD("vibrant_pod", "Vibrant Pod", ProgressStage.ALLOY, 0x827c1f,
			ChromaTieredItems.LUMA_BEANS, Siting.TREE_POD, 4, 6),
	/** Glowing Roots — grows beside the lowest log of a nearby tree. */
	GLOWING_ROOTS("glowing_roots", "Glowing Roots", ProgressStage.TURBOCHARGE, 0x871d00,
			ChromaTieredItems.BOOST_ROOT, Siting.TREE_ROOT, 4, 3);

	/** Which V33a {@code TieredPlants.generate} branch sites this plant. */
	public enum Siting { SURFACE, CAVE, WATER, LEAVES, SAND, TREE_POD, TREE_ROOT }

	public static final ChromaTieredPlants[] list = values();

	private final String registryName;
	private final String displayName;
	private final ProgressStage stage;
	private final int color;
	private final ChromaTieredItems drop;
	private final Siting siting;
	private final int generationChance;
	private final int generationCount;

	ChromaTieredPlants(String registryName, String displayName, ProgressStage stage, int color,
			ChromaTieredItems drop, Siting siting, int generationChance, int generationCount) {
		this.registryName = registryName;
		this.displayName = displayName;
		this.stage = stage;
		this.color = color;
		this.drop = drop;
		this.siting = siting;
		this.generationChance = generationChance;
		this.generationCount = generationCount;
	}

	public String registryName() { return registryName; }
	public String displayName() { return displayName; }
	public ProgressStage stage() { return stage; }

	/** V33a's per-plant tint, used by the coloured-light hook and the particle effects. */
	public int color() { return color; }

	public ChromaTieredItems drop() { return drop; }
	public Siting siting() { return siting; }

	/** V33a {@code getGenerationChance}: a one-in-N roll per chunk before any attempt is made. */
	public int generationChance() { return generationChance; }

	/** V33a {@code getGenerationCount}: attempts made once that roll succeeds. */
	public int generationCount() { return generationCount; }

	/** The authoritative V33a sprite pair. */
	public String frontTexture() {
		return switch (this) {
			case VIBRANT_POD -> "block/plant/tierplant_5_front";
			case GLOWING_ROOTS -> "block/plant/tierplant_6_front";
			default -> "block/plant/" + registryName + "_front";
		};
	}

	public String backTexture() {
		return switch (this) {
			case VIBRANT_POD -> "block/plant/tierplant_5_back";
			case GLOWING_ROOTS -> "block/plant/tierplant_6_back";
			default -> "block/plant/" + registryName + "_back";
		};
	}

	/**
	 * V33a {@code getHarvestResources}. Each plant has its own count formula; fortune arrives already
	 * maxed against the player's looting level, exactly as upstream does before switching.
	 */
	public void collectDrops(List<ItemStack> into, int fortune, RandomSource random, Player player,
			ItemStack prototype) {
		int n = switch (this) {
			case AURA_BLOOM -> 1 + fortune * random.nextInt(8);
			case ROCK_FLOWER -> (1 + random.nextInt(3)) * (1 + fortune * (1 + random.nextInt(4)));
			case ESSENCE_LILY -> 4 * (1 + fortune * fortune / 2);
			case ELEMENT_BULBS -> 1 + fortune * 4 + 2 * random.nextInt(9);
			case RADIANCE_BUSH -> (1 + fortune * fortune) + 2 * random.nextInt(5);
			case VIBRANT_POD -> 2 + random.nextInt(1 + fortune * 3 / 2);
			case GLOWING_ROOTS -> 1 + random.nextInt(1 + fortune) / 2;
		};
		// V33a applies the player's Proxima tuning after the plant-specific roll. The manager is an
		// identity outside Proxima, so ordinary overworld harvests retain their exact source counts.
		n = DimensionTuningManager.instance.getTunedDropCount(player, n, 0, 384);
		for (int i = 0; i < n; i++)
			into.add(prototype.copy());
	}
}
