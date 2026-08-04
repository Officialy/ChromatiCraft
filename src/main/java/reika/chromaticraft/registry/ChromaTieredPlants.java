package reika.chromaticraft.registry;

import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * V33a {@code BlockTieredPlant.TieredPlants}, as concrete per-plant identities.
 *
 * <p>Each plant is its own registered block with a stable registry name, and every asset it owns is
 * named after that identity. The legacy metadata ordinal is deliberately absent: it is not a field,
 * not a blockstate property, and not part of any texture, model, or language path. Its only jobs
 * upstream were selecting a sprite and indexing {@code chroma.tieredplant.N}, and both are owned by
 * datagen against the concrete identity now, so nothing is lost by dropping it.
 *
 * <p>Five of the seven V33a plants are accepted. Vibrant Pod and Glowing Roots drop {@code glowbeans}
 * and {@code boostroot}, which have no registered identity yet — the same boundary the tiered ores
 * draw — so their siting (a {@code bitRound} lattice, then {@code findTreeNear} and a walk along the
 * trunk) is deliberately not ported yet rather than given an invented drop.
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
			ChromaTieredItems.BEACON_DUST, Siting.SAND, 5, 2);

	/** Which V33a {@code TieredPlants.generate} branch sites this plant. */
	public enum Siting { SURFACE, CAVE, WATER, LEAVES, SAND }

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

	/** The authoritative V33a sprite pair, renamed onto this identity. */
	public String frontTexture() { return "block/plant/" + registryName + "_front"; }
	public String backTexture() { return "block/plant/" + registryName + "_back"; }

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
		};
		// CHROMA-PORT: V33a passes this through DimensionTuningManager.getTunedDropCount(player, n,
		// 0, 384), which only rescales inside the unported ChromatiCraft pocket dimension and is the
		// identity everywhere else. The source's own 0..384 bounds are retained here so the clamp is
		// not silently lost when that manager lands.
		n = Math.max(0, Math.min(384, n));
		for (int i = 0; i < n; i++)
			into.add(prototype.copy());
	}
}
