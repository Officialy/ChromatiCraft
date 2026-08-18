package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.dimension.GlowTreeShapes.Shape;

/**
 * V33a {@code WorldGenLightedTree}: Proxima's glowing trees, and the reason the Luminescent Sanctuary
 * is not a bare plain.
 *
 * <p>Each attempt rolls one of three shapes with equal weight — {@link Shape#OAK}, {@link Shape#TALL}
 * and {@link Shape#BALL} — then a stem of {@code rand.nextInt(4)} logs, which is zero a quarter of the
 * time. The canopy is placed from the top of that stem, and every shape carries its own logs as well
 * as its leaves because the trunk continues up through the crown.
 *
 * <p>Upstream's two site rules are both kept and both matter: the ground must be somewhere a sapling
 * could be planted, and the eight blocks above the origin must be air. The second is what stops a
 * stand of these growing through each other — most attempts in an already-wooded chunk fail on it,
 * which is why a fifty-percent chunk chance does not produce fifty trees.
 *
 * <p>Leaves are written only where a leaf may replace what is already there, so a canopy passing
 * through another tree's logs leaves them standing rather than eating them.
 *
 * <h2>What is not carried</h2>
 *
 * <p>Upstream places its leaves with a metadata of {@code rand.nextInt(5)}. That metadata selects one
 * of sixteen glow overlay sprites ({@code dimgen/glowleaf/<i>}), drawn by the second render pass this
 * port has not yet built; the block itself behaves identically across all sixteen. Until that pass
 * exists the value would be carried and never read, so it is recorded here instead — the same choice
 * the glowing log already makes.
 */
public final class GlowTreeFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a canGenerateTree: how much clear air an attempt needs above its origin. */
	private static final int REQUIRED_CLEARANCE = 8;

	public GlowTreeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		if (!canGenerate(world, origin))
			return false;

		BlockState log = ChromaBlocks.GLOW_LOG.get().defaultBlockState();
		BlockState leaves = ChromaBlocks.GLOWING_LEAVES.get().defaultBlockState();
		Shape shape = Shape.list[random.nextInt(Shape.list.length)];

		// V33a's stem: nextInt(4), so a quarter of these trees have no bare trunk at all and their
		// canopy starts at ground level.
		int stem = random.nextInt(4);
		for (int dy = 0; dy < stem; dy++)
			world.setBlock(origin.above(dy), log, 3);
		BlockPos base = origin.above(stem);

		int[] logs = shape.logs();
		for (int i = 0; i < logs.length; i += 3)
			world.setBlock(base.offset(logs[i], logs[i + 1], logs[i + 2]), log, 3);
		int[] canopy = shape.leaves();
		for (int i = 0; i < canopy.length; i += 3)
			placeLeaf(world, base.offset(canopy[i], canopy[i + 1], canopy[i + 2]), leaves);
		return true;
	}

	/**
	 * V33a canGenerateTree: {@code ReikaPlantHelper.SAPLING.canPlantAt} plus eight blocks of air.
	 *
	 * <p>The sapling rule is {@code SUPPORTS_VEGETATION}, and specifically <em>not</em>
	 * {@code BlockTags.DIRT}. In 26.2 grass blocks are their own tag: {@code SUBSTRATE_OVERWORLD} is
	 * {@code DIRT + MUD + MOSS_BLOCKS + GRASS_BLOCKS}, so a grass block does not answer to the dirt
	 * tag, and this feature refused every site in the dimension until a GameTest planted one on grass
	 * and watched it turn thirty-two attempts down. {@code SUPPORTS_VEGETATION} is that union plus
	 * farmland, which is what {@code VegetationBlock.mayPlaceOn} itself tests.
	 */
	private static boolean canGenerate(WorldGenLevel world, BlockPos origin) {
		if (!world.getBlockState(origin.below()).is(BlockTags.SUPPORTS_VEGETATION))
			return false;
		for (int i = 0; i < REQUIRED_CLEARANCE; i++)
			if (!world.getBlockState(origin.above(i)).isAir())
				return false;
		return true;
	}

	/** V33a placeLeaf: a leaf is written only into something a leaf may replace. */
	private static void placeLeaf(WorldGenLevel world, BlockPos pos, BlockState leaves) {
		if (world.getBlockState(pos).canBeReplaced())
			world.setBlock(pos, leaves, 3);
	}
}
