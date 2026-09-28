package reika.chromaticraft.world;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code ColorTreeGenerator}: the odd dye tree, and the rarer large rainbow tree, scattered through
 * ordinary overworld biomes.
 *
 * <p>Once per chunk, one column is picked (no +8 offset: V33a ran it as a retro-capable decorator on the
 * chunk's own area). If a dye tree could grow there, the biome's tree count is spent as that many
 * independent 1-in-128 attempts, all at that same column with its top re-read each time. Each attempt is a
 * large rainbow tree one time in 32 when the authored tree fits, otherwise a {@link TreeShaper} dye tree of
 * a random colour, unforced, with every leaf-vine chance at 1. So a Forest (16) sees a tree in about one
 * chunk in eight or nine, a Plains (2) in one chunk in 64.
 *
 * <p>The tree count is read from the biome at the chunk's corner column, as V33a's
 * {@code getBiomeGenForCoords(chunkX*16, chunkZ*16)} was. V33a derived it from the biome decorator's
 * {@code treesPerChunk} plus fixed bonuses for a few vanilla biomes, falling back through the biome's
 * {@code BiomeDictionary} types when that came to zero. 26.2 biomes carry no tree count, so both halves are
 * data in the configured feature: {@link Config#counts} are tags of biomes whose 1.7.10 counterpart's
 * result is known (checked in order, first match wins, zero included), and {@link Config#fallback} is the
 * type table (the largest matching entry wins) used for every other biome.
 *
 * <p>CHROMA-PORT: Thaumcraft's Tainted Land counted zero trees, and a Mystcraft age with the Dense Forest
 * page doubled the count. Neither mod exists for 26.2. V33a's rainbow chance of 1 in 16 in the Twilight
 * Forest was unreachable, since {@code canGenerateTree} refused that dimension outright.
 */
public final class ColorTreeFeature implements Feature {

	/** One tag of biomes and the tree count it stands for. */
	public record Count(TagKey<Biome> biomes, int trees) {
		public static final Codec<Count> CODEC = RecordCodecBuilder.create(i -> i.group(
				TagKey.hashedCodec(Registries.BIOME).fieldOf("biomes").forGetter(Count::biomes),
				ExtraCodecs.NON_NEGATIVE_INT.fieldOf("trees").forGetter(Count::trees)
		).apply(i, Count::new));
	}

	/**
	 * @param counts        V33a's result for biomes with a known 1.7.10 counterpart
	 * @param fallback      V33a's {@code BiomeDictionary} type table for everything else
	 * @param chance        V33a {@code CHANCE}: one attempt in this many places a tree
	 * @param rainbowChance V33a {@code getRainbowChance} outside the Twilight Forest
	 */
	public record Config(List<Count> counts, List<Count> fallback, int chance, int rainbowChance) {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
				Count.CODEC.listOf().fieldOf("counts").forGetter(Config::counts),
				Count.CODEC.listOf().fieldOf("fallback").forGetter(Config::fallback),
				ExtraCodecs.POSITIVE_INT.fieldOf("chance").forGetter(Config::chance),
				ExtraCodecs.POSITIVE_INT.fieldOf("rainbow_chance").forGetter(Config::rainbowChance)
		).apply(i, Config::new));
	}

	public static final MapCodec<ColorTreeFeature> CODEC = Config.CODEC.fieldOf("config")
			.xmap(ColorTreeFeature::new, ColorTreeFeature::config);
	private final Config config;

	public ColorTreeFeature(Config config) {
		this.config = config;
	}

	public Config config() {
		return config;
	}

	@Override
	public MapCodec<ColorTreeFeature> codec() {
		return CODEC;
	}

	/** V33a {@code getTreeCount}. */
	public static int getTreeCount(Holder<Biome> biome, Config config) {
		for (Count c : config.counts()) {
			if (biome.is(c.biomes()))
				return c.trees();
		}
		int trees = 0;
		for (Count c : config.fallback()) {
			if (biome.is(c.biomes()))
				trees = Math.max(trees, c.trees());
		}
		return trees;
	}

	@Override
	public boolean place(WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator,
			RandomSource r, BlockPos origin) {
		int chunkX = origin.getX() & ~15;
		int chunkZ = origin.getZ() & ~15;

		int cornerY = ReikaWorldHelper.getTopSolidOrLiquidBlock(world, chunkX, chunkZ);
		int trees = getTreeCount(world.getBiome(new BlockPos(chunkX, cornerY, chunkZ)), config);
		if (trees <= 0)
			return false;
		int x = chunkX + r.nextInt(16);
		int z = chunkZ + r.nextInt(16);
		if (!TreeShaper.canGenerateTree(world, x, z))
			return false;
		boolean placed = false;
		for (int i = 0; i < trees; i++) {
			if (r.nextInt(config.chance()) == 0) {
				BlockPos pos = new BlockPos(x, ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z), z);
				// V33a asked checkRainbowTreeSpace and then generated; placeLarge is that pair.
				if (r.nextInt(config.rainbowChance()) == 0 && RainbowTreeFeature.placeLarge(world, pos, RainbowTreeFeature.chooseLog(r)))
					placed = true;
				else
					placed |= TreeShaper.generateRandomWeightedTree(world, pos, r,
							CrystalElement.elements[r.nextInt(16)], false, 1, 1);
			}
		}
		return placed;
	}
}
