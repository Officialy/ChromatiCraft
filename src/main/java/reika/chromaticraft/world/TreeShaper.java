package reika.chromaticraft.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.neoforge.common.Tags;

import reika.chromaticraft.block.dye26.BlockDyeSapling;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code TreeShaper}: the dye trees — the Rainbow Forest's trees, the trees dye saplings grow into,
 * and (once {@code ColorTreeGenerator} is ported) the odd dye tree across the rest of the overworld.
 *
 * <p>A tree is one of three hand-shaped silhouettes, picked by weight: a basic round tree (60), a tall
 * one with a tiered crown (30) or a lumpy one with four stubby branches in two tiers (10). The trunk is
 * one randomly chosen log for the whole tree; the leaves are the tree's colour. Some leaves on the lower
 * crown trail vines one to three blocks long, and some of those vines end in a fertile tip. Every shape
 * is reproduced layer for layer from the source.
 *
 * <p>Two things differ from 1.7.10 for 26.2's sake. The trunk wood is any overworld log (the tag), where
 * V33a listed vanilla's six woods plus loaded mod woods; that is the same choice
 * {@code RainbowTreeFeature} makes. And the placed leaves are given their true distance to the trunk
 * afterwards ({@link LeafDistances}), because a feature-placed 26.2 leaf otherwise decays at once.
 *
 * <p>CHROMA-PORT: Forestry's {@code ChromaTrees} fed this class a {@code FilledBlockArray} collector and
 * a height scale to plan bee-tree growth. The height scale is kept; the collector returns with the
 * Forestry integration, which has no 26.2 target.
 */
public final class TreeShaper {

	private static final float BASE_VINE_CHANCE = 0.2F;
	private static final float BASE_FERTILE_CHANCE = 0.6F;

	private enum TreeShape {
		BASIC(60),
		TALL(30),
		LUMPY(10);

		private final int spawnWeight;

		TreeShape(int weight) {
			spawnWeight = weight;
		}

		private static TreeShape pick(RandomSource rand) {
			int total = 0;
			for (TreeShape shape : values())
				total += shape.spawnWeight;
			int roll = rand.nextInt(total);
			for (TreeShape shape : values()) {
				roll -= shape.spawnWeight;
				if (roll < 0)
					return shape;
			}
			return BASIC;
		}
	}

	private final WorldGenLevel world;
	private final RandomSource rand;
	private final BlockState log;
	private final BlockState leaf;
	private final CrystalElement color;
	private final float vineChance;
	private final float vineFertility;
	private final List<BlockPos> logs = new ArrayList<>();

	private TreeShaper(WorldGenLevel world, RandomSource rand, CrystalElement color, float vineChance, float vineFertility) {
		this.world = world;
		this.rand = rand;
		this.color = color;
		this.vineChance = vineChance;
		this.vineFertility = vineFertility;
		log = RainbowTreeFeature.chooseLog(rand);
		leaf = ChromaBlocks.dyeLeaves(color).get().defaultBlockState();
	}

	public static boolean generateRandomWeightedTree(WorldGenLevel world, BlockPos pos, RandomSource rand,
			CrystalElement color, boolean forceGen, float vineChance, float vineFertilityFactor) {
		return generateRandomWeightedTree(world, pos, rand, color, forceGen, vineChance, vineFertilityFactor, 1);
	}

	/**
	 * V33a {@code generateRandomWeightedTree}. The shape is rolled first; a height scale of 0.75 or less
	 * always makes the basic shape. Each shape re-checks the site unless forced.
	 */
	public static boolean generateRandomWeightedTree(WorldGenLevel world, BlockPos pos, RandomSource rand,
			CrystalElement color, boolean forceGen, float vineChance, float vineFertilityFactor, float hscale) {
		TreeShape shape = TreeShape.pick(rand);
		if (hscale <= 0.75)
			shape = TreeShape.BASIC;
		if (!forceGen && !canGenerateTree(world, pos.getX(), pos.getZ()))
			return false;
		TreeShaper tree = new TreeShaper(world, rand, color, vineChance, vineFertilityFactor);
		switch (shape) {
			case BASIC -> tree.generateNormalTree(pos, hscale);
			case TALL -> tree.generateTallTree(pos, hscale);
			case LUMPY -> tree.generateLumpyTree(pos, hscale);
		}
		LeafDistances.update(world, tree.logs);
		return true;
	}

	private void generateNormalTree(BlockPos pos, float hscale) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int w = 2;
		int h = 5 + rand.nextInt(3);
		h *= hscale;

		for (int i = 0; i < h; i++)
			this.placeLog(x, y + i, z);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				this.leafWithVine(x + i, y + h - 3, z + j, true);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				this.leafWithVine(x + i, y + h - 2, z + j, false);
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				this.leafWithVine(x + i, y + h - 1, z + j, false);
		this.plus(x, y + h, z);
	}

	private void generateTallTree(BlockPos pos, float hscale) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int h = 10 + rand.nextInt(3);
		h *= hscale;
		int w = 2;

		for (int i = 0; i < h; i++)
			this.placeLog(x, y + i, z);
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				if (i * j == 0)
					this.leafWithVine(x + i, y + h - 8, z + j, true);
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				this.leafWithVine(x + i, y + h - 7, z + j, true);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				if (i * j != w * w && i * j != -w * w)
					this.leafWithVine(x + i, y + h - 6, z + j, true);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				if (i * j != w * w && i * j != -w * w)
					this.leafWithVine(x + i, y + h - 5, z + j, false);
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				this.leafWithVine(x + i, y + h - 4, z + j, false);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				if (i * j != w * w && i * j != -w * w)
					this.leafWithVine(x + i, y + h - 3, z + j, false);
		for (int i = -w; i <= w; i++)
			for (int j = -w; j <= w; j++)
				if (i * j != w * w && i * j != -w * w)
					this.leafWithVine(x + i, y + h - 2, z + j, false);
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				this.leafWithVine(x + i, y + h - 1, z + j, false);
		this.plus(x, y + h, z);
	}

	/**
	 * V33a {@code generateLumpyTree}. Upstream's branch loop runs only for {@code i = 1}: four stubs at
	 * {@code h-2} and four at {@code h-6}, each wrapped in a 3x3x3 leaf ball with its corners cut, and
	 * only the lower tier trails vines.
	 */
	private void generateLumpyTree(BlockPos pos, float hscale) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int h = 8 + rand.nextInt(4);
		h *= hscale;

		for (int i = 0; i < h; i++)
			this.placeLog(x, y + i, z);

		for (int i = 1; i < 2; i++) {
			int dy = y + h - 2;
			this.branch(x + i, dy, z, false);
			this.branch(x - i, dy, z, false);
			this.branch(x, dy, z - i, false);
			this.branch(x, dy, z + i, false);

			dy = y + h - 6;
			this.branch(x + i, dy, z, true);
			this.branch(x - i, dy, z, true);
			this.branch(x, dy, z - i, true);
			this.branch(x, dy, z + i, true);
		}

		int dy = y + h - 4;
		for (int i = 1; i < 2; i++) {
			this.leafWithVine(x + i, dy, z, false);
			this.leafWithVine(x - i, dy, z, false);
			this.leafWithVine(x, dy, z + i, false);
			this.leafWithVine(x, dy, z - i, false);
		}

		this.plus(x, y + h, z);
	}

	private void branch(int dx, int dy, int dz, boolean vines) {
		this.placeLog(dx, dy, dz);
		for (int j = -1; j <= 1; j++)
			for (int k = -1; k <= 1; k++)
				for (int m = -1; m <= 1; m++)
					if (j * k * m == 0)
						this.leafWithVine(dx + j, dy + k, dz + m, vines);
	}

	/** The four-leaf plus that caps every shape. */
	private void plus(int x, int y, int z) {
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				if (i * j == 0)
					this.leafWithVine(x + i, y, z + j, false);
	}

	private void placeLog(int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		world.setBlock(pos, log, 2);
		logs.add(pos);
	}

	/** A leaf where one may go, then (on vine-bearing layers) the vine roll for the block below it. */
	private void leafWithVine(int x, int y, int z, boolean vines) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!canGenerateLeavesAt(world, pos))
			return;
		world.setBlock(pos, leaf, 2);
		if (vines && vineChance > 0 && rand.nextDouble() < BASE_VINE_CHANCE * vineChance)
			this.generateVine(pos.below());
	}

	/**
	 * V33a {@code generateVine}: one to three vines hanging into air, then possibly a fertile tip on the
	 * lowest. When the first cell below the leaf is not air, upstream's loop places nothing and its
	 * {@code y+1} lands on the leaf itself, so the fertile tip replaces that leaf; that is kept.
	 */
	private void generateVine(BlockPos start) {
		BlockState vine = ChromaBlocks.dyeVine(color, false).get().defaultBlockState();
		BlockPos pos = start;
		int n = 0;
		int max = 1 + rand.nextInt(3);
		boolean air = world.getBlockState(pos).isAir();
		while (n < max && air) {
			world.setBlock(pos, vine, 2);
			pos = pos.below();
			air = world.getBlockState(pos).isAir();
			n++;
		}
		if (vineFertility > 0 && rand.nextDouble() < BASE_FERTILE_CHANCE * vineFertility)
			world.setBlock(pos.above(), ChromaBlocks.dyeVine(color, true).get().defaultBlockState(), 2);
	}

	/**
	 * V33a {@code canGenerateLeavesAt}: a soft block, a vanilla leaf, or anything 1.7.10 let leaves
	 * replace — Forge's default {@code canBeReplacedByLeaves} is "not an opaque cube", whose 26.2 form is
	 * a state that is not a solid render.
	 */
	public static boolean canGenerateLeavesAt(LevelReader world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return ReikaWorldHelper.softBlocks(world, pos) || state.is(BlockTags.LEAVES) || !state.isSolidRender();
	}

	/**
	 * V33a {@code ColorTreeGenerator.canGenerateTree}, which every unforced dye tree consults: never in
	 * the Nether or the End, not in a superflat world unless {@code FLATGEN} allows it, not in ocean,
	 * desert, dry or mushroom biomes, and only where a dye sapling could grow at the column's top.
	 */
	public static boolean canGenerateTree(WorldGenLevel world, int x, int z) {
		var dimension = world.getLevel().dimension();
		if (dimension == net.minecraft.world.level.Level.NETHER || dimension == net.minecraft.world.level.Level.END)
			return false;
		if (world.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource && !ChromaOptions.FLATGEN.getState())
			return false;
		int y = ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z);
		BlockPos top = new BlockPos(x, y, z);
		Holder<Biome> biome = world.getBiome(top);
		if (biome.is(BiomeTags.IS_OCEAN))
			return false;
		if (biome.is(Biomes.DESERT) || biome.is(Tags.Biomes.IS_DRY))
			return false;
		if (biome.is(Biomes.MUSHROOM_FIELDS) || biome.is(Tags.Biomes.IS_MUSHROOM))
			return false;
		return BlockDyeSapling.canGrowAt(world, top, true);
	}
}
