package reika.chromaticraft.world.dimension;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenCrystalTree}: the crystal trees of Proxima's Crystal Forest.
 *
 * <p>A size class is drawn from upstream's weighting — 10 for the smallest, then 7, 4 and 1, so the
 * tallest class appears about once in twenty-two — a layout is picked from the ones registered at that
 * size, and the crown is lifted onto a trunk whose height comes from that class's own range. The trunk
 * is Stone Shielding, {@link CrystalTreeShapes#trunkWidth} blocks across.
 *
 * <p>Nothing is written until the whole shape has been checked, which is upstream's {@code checkSpace}
 * and matters: a tree that failed halfway would leave a stump of Shielding standing in the forest.
 * The check is asymmetric in the way upstream makes it — a foliage cell may replace anything leaves can
 * grow through, while every other cell demands air — so a canopy can knit into another tree's leaves
 * but a trunk cannot grow through one.
 *
 * <p>The largest class reaches thirty blocks of trunk under a seven-tall crown, and a crown is at most
 * about thirteen across, so a tree fits comfortably inside the chunks this step may write to.
 */
public final class CrystalTreeFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a sizeRand: weights 10, 7, 4 and 1 across the four size classes. */
	private static final int[] SIZE_WEIGHTS = {10, 7, 4, 1};

	public CrystalTreeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		int size = drawSize(random);
		List<CrystalTreeShapes> candidates = shapesOfSize(size);
		if (candidates.isEmpty())
			return false;
		CrystalTreeShapes shape = candidates.get(random.nextInt(candidates.size()));

		int[] heights = CrystalTreeShapes.TREE_HEIGHTS[size];
		int trunkHeight = heights[0] + random.nextInt(heights[1] - heights[0] + 1);

		// The crown is authored around its own origin, so it is offset up by the trunk height before
		// anything is tested or written.
		var crown = new reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray(world.getLevel());
		shape.populate(crown, world.getLevel());
		crown.offset(origin.getX(), origin.getY() + trunkHeight, origin.getZ());

		BlockState leaves = ChromaBlocks.deco(ProximaDecoTypes.CRYSTALLEAF).get().defaultBlockState();
		if (!hasSpace(world, crown, leaves))
			return false;

		BlockState shielding = ChromaBlocks.shielding(ChromaShieldTypes.STONE).get().defaultBlockState();
		for (int i = 0; i < trunkHeight; i++)
			for (int a = 0; a < shape.trunkWidth; a++)
				for (int b = 0; b < shape.trunkWidth; b++)
					// V33a grows the trunk towards -x/-z from the origin column.
					world.setBlock(origin.offset(-a, i, -b), shielding, 3);
		// FilledBlockArray is still Level-backed for the shared V33a shape builders, but a placed
		// feature must write through its WorldGenLevel. Writing through crown.place() would escape
		// the active WorldGenRegion and can trigger unsafe terrain access during parallel generation.
		for (BlockPos pos : crown.keySet())
			world.setBlock(pos, crown.getBlockKeyAt(pos.getX(), pos.getY(), pos.getZ()).blockID, 3);
		return true;
	}

	/**
	 * V33a {@code checkSpace}. A foliage cell only needs somewhere leaves could grow; everything else
	 * needs air outright.
	 */
	private static boolean hasSpace(WorldGenLevel world,
			reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray crown, BlockState leaves) {
		for (BlockPos pos : crown.keySet()) {
			if (world.isOutsideBuildHeight(pos.getY()))
				return false;
			BlockState present = world.getBlockState(pos);
			boolean foliage = crown.getBlockAt(pos.getX(), pos.getY(), pos.getZ()) == leaves.getBlock();
			if (foliage ? !present.isAir() && !present.canBeReplaced() : !present.isAir())
				return false;
		}
		return true;
	}

	/** V33a's weighted size draw, done inline so it is deterministic in the feature's own random. */
	private static int drawSize(RandomSource random) {
		int total = 0;
		for (int weight : SIZE_WEIGHTS)
			total += weight;
		int roll = random.nextInt(total);
		for (int size = 0; size < SIZE_WEIGHTS.length; size++) {
			roll -= SIZE_WEIGHTS[size];
			if (roll < 0)
				return size;
		}
		return 0;
	}

	/** The registered layouts belonging to one size class; XMAS is excluded, as upstream excludes it. */
	private static List<CrystalTreeShapes> shapesOfSize(int size) {
		List<CrystalTreeShapes> result = new ArrayList<>();
		for (CrystalTreeShapes shape : CrystalTreeShapes.list)
			if (shape.isRegistered() && shape.treeSize == size)
				result.add(shape);
		return result;
	}
}
