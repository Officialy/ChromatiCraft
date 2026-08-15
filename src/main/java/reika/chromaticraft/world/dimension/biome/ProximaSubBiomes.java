package reika.chromaticraft.world.dimension.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * V33a {@code ChromaDimensionManager.SubBiomes}: the four biomes that appear only inside a specific
 * parent biome's blob.
 *
 * <p>Here {@code spawnWeight} is a probability rather than a count: {@code BiomeDistributor} walks
 * every blob of the parent biome and, with this chance, stamps a smaller sub-blob somewhere inside it.
 * So Crystal Mountains appear in three quarters of Crystal Plains blobs while Voidlands appear in one
 * Lumen Skylands blob in ten.
 *
 * <p>{@link #getParent()} is wired by {@link ProximaBiomes}' own constructor rather than by a separate
 * {@code create()} pass, because 26.2 has no biome-id registration step to hang it off.
 */
public enum ProximaSubBiomes implements ProximaBiomeType {

	MOUNTAINS("Crystal Mountains", "crystal_mountains", 0.75, 0),
	DEEPOCEAN("Aura Ocean", "aura_ocean", 0.4, -30),
	CRYSFOREST("Crystal Forest", "crystal_forest", 0.2, 15),
	VOIDLANDS("Voidland", "voidland", 0.1, 8);

	public static final ProximaSubBiomes[] biomeList = values();

	private final String biomeName;
	private final ResourceKey<Biome> key;
	public final double spawnWeight;
	public final int baseHeightDelta;
	private ProximaBiomes parent;

	ProximaSubBiomes(String biomeName, String path, double spawnWeight, int baseHeightDelta) {
		this.biomeName = biomeName;
		key = ProximaBiomeType.key(path);
		this.spawnWeight = spawnWeight;
		this.baseHeightDelta = baseHeightDelta;
	}

	void setParent(ProximaBiomes parent) {
		this.parent = parent;
	}

	public ProximaBiomes getParent() {
		return parent;
	}

	@Override
	public String biomeName() {
		return biomeName;
	}

	@Override
	public ResourceKey<Biome> biomeKey() {
		return key;
	}

	/** V33a: a sub-biome floods exactly when its parent does. */
	@Override
	public boolean isWaterBiome() {
		return parent != null && parent.isWaterBiome();
	}

	@Override
	public boolean isReasonablyFlat() {
		return this != MOUNTAINS && this != VOIDLANDS;
	}

	@Override
	public int getBaseHeightDelta() {
		return baseHeightDelta;
	}
}
