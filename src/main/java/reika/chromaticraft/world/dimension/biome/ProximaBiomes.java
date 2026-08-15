package reika.chromaticraft.world.dimension.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * V33a {@code ChromaDimensionManager.Biomes}: Proxima's nine primary biomes.
 *
 * <p>The two numbers are the whole of the distribution contract. {@code spawnWeight} is literally how
 * many separate blobs of this biome {@code BiomeDistributor} paints onto the map, so Glowing Forest
 * (10) is twice as common as Iridescent Archipelago (6) and the three zero-weight entries are never
 * painted at all — they are placed by other means. {@code baseHeightDelta} is the offset the terrain
 * shaper applies to the dimension's base height.
 *
 * <p>The three technical biomes are not scattered: Structure Field and Monument Field are stamped
 * around the puzzle structures and the monument, and Luminescent Sanctuary fills whatever is inside
 * the central region. That is why their weight is zero rather than small.
 *
 * <p>Named after the dimension rather than kept as V33a's bare {@code Biomes}, which would collide
 * with {@link net.minecraft.world.level.biome.Biomes} at every use site.
 */
public enum ProximaBiomes implements ProximaBiomeType {

	PLAINS("Crystal Plains", "crystal_plains", 8, 0, ProximaSubBiomes.MOUNTAINS),
	ISLANDS("Iridescent Archipelago", "iridescent_archipelago", 6, -5, ProximaSubBiomes.DEEPOCEAN),
	SKYLANDS("Lumen Skylands", "lumen_skylands", 2, 0, ProximaSubBiomes.VOIDLANDS),
	FOREST("Glowing Forest", "glowing_forest", 10, 10, ProximaSubBiomes.CRYSFOREST),
	SPARKLE("Sparkling Sands", "sparkling_sands", 4, 0, null),
	GLOWCRACKS("Radiant Fissures", "radiant_fissures", 3, 0, null),
	STRUCTURE("Structure Field", "structure_field", 0, 0, null),
	CENTER("Luminescent Sanctuary", "luminescent_sanctuary", 0, 0, null),
	MONUMENT("Monument Field", "monument_field", 0, 0, null);

	public static final ProximaBiomes[] biomeList = values();

	private final String biomeName;
	private final ResourceKey<Biome> key;
	public final int spawnWeight;
	public final int baseHeightDelta;
	private final ProximaSubBiomes subBiome;

	ProximaBiomes(String biomeName, String path, int spawnWeight, int baseHeightDelta,
			ProximaSubBiomes subBiome) {
		this.biomeName = biomeName;
		key = ProximaBiomeType.key(path);
		this.spawnWeight = spawnWeight;
		this.baseHeightDelta = baseHeightDelta;
		this.subBiome = subBiome;
		if (subBiome != null)
			subBiome.setParent(this);
	}

	@Override
	public String biomeName() {
		return biomeName;
	}

	@Override
	public ResourceKey<Biome> biomeKey() {
		return key;
	}

	public ProximaSubBiomes getSubBiome() {
		return subBiome;
	}

	/** V33a isTechnical: a zero weight means the distributor never scatters this one. */
	public boolean isTechnical() {
		return spawnWeight == 0;
	}

	/** V33a isFarRegions: which biomes the outer regions beyond the central boundary may use. */
	public boolean isFarRegions() {
		return switch (this) {
			case FOREST, GLOWCRACKS, ISLANDS, PLAINS, SKYLANDS, SPARKLE -> true;
			case CENTER, STRUCTURE, MONUMENT -> false;
		};
	}

	@Override
	public boolean isWaterBiome() {
		return this == ISLANDS;
	}

	@Override
	public boolean isReasonablyFlat() {
		return this != SKYLANDS && this != ISLANDS;
	}

	@Override
	public int getBaseHeightDelta() {
		return baseHeightDelta;
	}
}
